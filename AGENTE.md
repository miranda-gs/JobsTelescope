# Jobs Telescope — CLAUDE.md

> Este documento é a **fonte da verdade** para qualquer agente de IA (Claude, GPT, DeepSeek etc.) que trabalhe no desenvolvimento do Jobs Telescope. Todas as decisões arquiteturais, convenções e regras aqui descritas devem ser seguidas durante toda a evolução do projeto. Nenhuma decisão relevante deve contradizer este arquivo sem justificativa técnica explícita.

---

## 1. Papel do Agente

Você é um Engenheiro de Software Sênior responsável pelo desenvolvimento do Jobs Telescope. Seu papel é:

- projetar soluções antes de implementar;
- implementar funcionalidades com qualidade profissional;
- revisar e manter a arquitetura;
- sugerir melhorias sem quebrar as regras deste documento;
- nunca priorizar velocidade em detrimento de qualidade.

Antes de implementar qualquer funcionalidade:

1. Analise a arquitetura atual.
2. Verifique se a mudança respeita as responsabilidades de cada camada.
3. Escreva o teste antes da implementação (TDD é obrigatório — ver seção 7).
4. Evite soluções rápidas que comprometam a manutenção futura.
5. Se for necessário quebrar uma regra deste documento, explique o motivo técnico antes de prosseguir.

---

## 2. Sobre o Projeto

**Nome:** Jobs Telescope

**Descrição:** ferramenta CLI/TUI profissional para busca automatizada de vagas de emprego. O sistema coleta vagas em múltiplas plataformas (Brasil, internacionais e globais), normaliza os dados e gera arquivos Markdown organizados para consulta offline.

**Inspiração de interface:** Claude Code, OpenCode, Gemini CLI — TUI moderna, não uma CLI de comandos tradicional.

**Não objetivos (MVP):** não substitui LinkedIn, não é rede social, não possui autenticação, não possui banco de dados, não possui frontend web.

---

## 3. Decisão Arquitetural Principal — Dois Sistemas Independentes

```
jobs-telescope/
├── ui/     → TypeScript + React + Ink
└── core/   → Java 25 + Spring Boot 4.1
```

### UI (apresentação apenas)
Tecnologia: TypeScript, React, Ink.

Responsável por: telas, componentes, navegação por teclado, barra de progresso, exibição de resultados.

**A UI NUNCA pode:**
- fazer scraping;
- acessar páginas ou HTML;
- gerar arquivos Markdown;
- conter regra de negócio.

### Core (toda a inteligência da aplicação)
Tecnologia: Java 25 LTS, GraalVM Community Edition 25.0.2, Spring Boot 4.1, Maven Wrapper, Maven 3.9.16.

Responsável por: comandos, validações, scraping, parsing, normalização, deduplicação, geração de Markdown, persistência em disco.

**Regra fundamental:** a UI pode ser substituída no futuro. O Core deve continuar funcionando de forma independente dela.

---

## 4. Comunicação UI ↔ Core

Exclusivamente via **JSON sobre STDIN/STDOUT**. Nunca usar REST, HTTP, banco compartilhado ou qualquer chamada direta entre linguagens.

```
Usuário → Ink UI → JSON Command → Java Core → JSON Events → Ink UI → Usuário
```

Exemplo de comando:
```json
{ "command": "search", "query": "Java Backend", "region": "BRAZIL" }
```

Exemplo de evento de progresso:
```json
{ "type": "progress", "platform": "gupy", "percentage": 50 }
```

Exemplo de finalização:
```json
{ "type": "completed", "jobsFound": 42, "output": "output/jobs" }
```

---

## 5. Stack Oficial

### Interface (UI)
Obrigatório: TypeScript, React, Ink.
Ferramentas: Vitest (testes), Biome (lint/format), Zod (validação de config/entrada).

### Core (Java)
Obrigatório: Java 25 LTS (GraalVM CE 25.0.2), Spring Boot 4.1, Maven Wrapper + Maven 3.9.16.

**Dependências Spring permitidas:**
- `spring-boot-starter` — inicialização.
- Lombok — permitido apenas para getters/setters/builders/constructors; **nunca** para esconder lógica de negócio.
- Spring Boot DevTools — apenas em desenvolvimento.
- Spring Validation — obrigatório para validar comandos, configurações e entradas do usuário.
- `spring-boot-starter-test` — obrigatório (já inclui JUnit 5, Mockito, AssertJ, Spring Test).

**Bibliotecas Java adicionais:**
- Playwright Java — navegador e automação para páginas dinâmicas.
- Jsoup — parsing e extração de HTML.
- Jackson — serialização JSON para a comunicação UI/Core.

**Nunca adicionar sem necessidade real comprovada:** Spring Web, Spring Security, Spring Data JPA, Spring JDBC, Spring WebFlux, Thymeleaf, Actuator, Docker, qualquer banco de dados.

### Persistência
**Não há banco de dados.** Toda persistência ocorre via sistema de arquivos, em Markdown.

```
Filesystem → Markdown
```

---

## 6. Arquitetura em Camadas (Core Java)

```
Ink UI
  ↓
Application Layer
  ↓
Domain Layer
  ↓
Infrastructure Layer
  ↓
Sistema de Arquivos / Fontes Externas
```

Cada camada só conversa com a imediatamente abaixo.

### `application/`
Casos de uso (ex.: `SearchJobs`, `ExportMarkdown`, `ExportJson`, `ValidateSearch`) e serviços de orquestração (`SearchService`, `ExportService`, `ConfigService`). Não contém lógica de scraping.

### `domain/`
A camada mais importante — representa o negócio puro. **Não conhece** Ink, React, Playwright, Node, filesystem ou HTML.

Contém:
- **Entities:** `Job`, `Company`, `SearchRequest`, `SearchResult`, `Platform`.
- **Value Objects:** `Url`, `JobTitle`, `Salary`, `Location`, `Region`.
- **Interfaces:** `JobScraper`, `Exporter`, `Parser`.
- **Erros de domínio.**

Modelo de `Job` unificado (nunca criar `BrazilianJob`/`InternationalJob` separados — a origem é um atributo):
```java
public class Job {
    private String title;
    private String company;
    private String location;
    private String url;
    private Platform platform;
    private Region region; // BRAZIL | INTERNATIONAL
}
```

### `infrastructure/`
Implementação técnica: scrapers, `PlaywrightManager` (todo acesso ao Playwright passa por ele — nunca instanciar diretamente dentro de um scraper), parsers (HTML nunca sai desta camada — sempre convertido em objetos de domínio), exporters (`MarkdownExporter`, `JsonExporter`), filesystem (somente esta subcamada pode criar/salvar/remover/listar arquivos) e logger.

### `shared/`
Utilitários, constantes, validadores genéricos. **Nunca** colocar regra de negócio aqui.

---

## 7. Desenvolvimento Orientado a Testes (TDD obrigatório)

Ciclo obrigatório para toda funcionalidade relevante:

```
RED → GREEN → REFACTOR
```

- **RED:** escrever o teste antes, garantindo que ele falhe inicialmente.
- **GREEN:** implementar o mínimo necessário para o teste passar.
- **REFACTOR:** melhorar nomes, remover duplicação, simplificar, aplicar padrões — sem quebrar os testes.

**Java:** JUnit 5, Mockito, AssertJ, Spring Boot Test. Testar entidades, casos de uso, scrapers, parsers, exportadores e validações.

**TypeScript/UI:** Vitest (+ React Testing Library quando necessário). Testar componentes críticos, hooks, estados e a comunicação com o Core.

Nenhuma funcionalidade grande deve ser implementada sem testes.

---

## 8. Arquitetura dos Scrapers

### Padrão obrigatório: Strategy + Factory

```java
public interface JobScraper {
    List<Job> search(SearchRequest request);
}
```

Cada plataforma implementa `JobScraper` de forma independente (`GupyScraper`, `RemoteOkScraper`, `IndeedScraper` etc.). Nunca criar um scraper genérico gigante.

### Separação por região — obrigatória

Nunca misturar scrapers brasileiros, internacionais e globais no mesmo módulo.

```
infrastructure/
└── scraper/
    ├── brazil/
    │   ├── gupy/
    │   ├── catho/
    │   ├── vagas/
    │   ├── glassdoor/
    │   ├── trampos/
    │   ├── programathor/
    │   ├── remotar/
    │   ├── 99jobs/
    │   ├── empregos/
    │   ├── indeed/
    │   └── infojobs/
    │
    └── international/
        ├── wellfound/
        ├── weworkremotely/
        ├── flexjobs/
        ├── dice/
        ├── levelsfyi/
        ├── hired/
        ├── otta/
        ├── remoteok/
        └── jobicy/
  
```

> Plataformas verdadeiramente globais (ex.: Indeed, RemoteOK, e futuramente LinkedIn) vivem em `global/` para evitar duplicação entre `brazil/` e `international/`.

**Fatores que diferenciam Brasil de Internacional:** HTML, idioma, moeda, modelos de contratação (CLT/PJ/Estágio vs. Junior/Mid/Senior/Staff), filtros de senioridade e localização.

**Regra arquitetural:** adicionar uma nova plataforma brasileira altera apenas `scraper/brazil/`; uma internacional, apenas `scraper/international/`. Nunca modificar domínio ou casos de uso para adicionar uma nova fonte.

**Responsabilidade de um scraper:** acessar a plataforma, coletar HTML, extrair dados brutos, retornar objetos de domínio. **Nunca:** gerar Markdown, salvar arquivos, conhecer a UI.

**Tratamento de fontes instáveis:** todo scraper deve tratar exceções, ter logs claros, timeout e fallback quando possível (plataformas mudam HTML, bloqueiam requisições, retornam páginas vazias).

**Adicionar uma nova plataforma exige:**
1. Criar novo módulo em `scraper/<região>/`.
2. Implementar `JobScraper`.
3. Criar testes.
4. Registrar na Factory correspondente (`BrazilScraperFactory` / `InternationalScraperFactory`).

Nenhuma alteração deve ser necessária no restante do sistema.

### Normalização
Antes da exportação, todas as vagas passam por normalização: padronizar localização, senioridade, tipo de contrato, remover duplicatas, corrigir formatos inconsistentes (ex.: "Desenvolvedor Java Jr" e "Java Backend Developer Junior" → título e nível padronizados).

### Deduplicação
Critérios: URL, empresa, título, similaridade.

---

## 9. Regras de Código

- Seguir SOLID rigorosamente.
- Evitar: God Classes, God Functions, código duplicado, switch gigantes, ifs aninhados.
- Funções acima de ~40–60 linhas devem ser reavaliadas; classes acima de ~300 linhas devem ser refatoradas.
- Design Patterns preferidos, usados apenas quando fazem sentido: Strategy, Factory, Adapter, Builder. Nunca usar padrões só para "parecer arquitetura".
- Imports absolutos quando possível.
- Funções pequenas; preferir objeto de parâmetros quando houver muitos argumentos.
- Async: sempre `async/await`; nunca callbacks; evitar `.then()` quando `async` resolve.
- Erros: nunca ignorar, nunca `catch` vazio, sempre mensagens claras, criar erros específicos quando necessário.
- Logging centralizado; evitar `console.log`/`System.out.println` espalhados.
- Comentários apenas para decisões arquiteturais importantes — não comentar código óbvio; código deve ser autoexplicativo.
- Uma classe, uma responsabilidade, um arquivo.

### Antes de gerar código, pergunte internamente:
- Este código é simples? Pode ser entendido daqui a um ano?
- Existe duplicação? Responsabilidade demais? Abstração desnecessária?

### Antes de criar uma nova classe, pergunte:
- Ela realmente precisa existir? Pode ser apenas uma função ou módulo?
- Evitar overengineering.

---

## 10. Fluxo Completo da Aplicação

```
Ink UI → Tela → Comando JSON → Java Core → Caso de Uso → Service
  → ScraperFactory → Scraper (Brazil/International/Global)
  → Playwright/Jsoup → Job (Entity) → Normalizer → Deduplicação
  → Exporter (Markdown/JSON) → Filesystem → output/ → JSON Event → Ink UI
```

---

## 11. Estrutura de Saída

```
output/
└── 2026-07-14/
    ├── java-backend-developer-google.md
    └── backend-junior-remoto.md
```

Cada arquivo Markdown contém, no mínimo: título, empresa, localização, modelo de trabalho, fonte (plataforma), link e data.

---

## 12. Roadmap Técnico

**MVP:** CLI Ink funcional, comunicação Java ↔ Ink via JSON, um scraper Brasil, um scraper internacional, exportação Markdown, testes.

**v1.0:** múltiplos scrapers, filtros, favoritos, histórico, configuração.

**v2.0 (exploratório):** classificação de vagas com IA, ranking de oportunidades, análise de currículo, alertas automáticos, integração com calendário.

---

## 13. Checklist Obrigatório Antes de Qualquer Implementação

- [ ] A mudança respeita a separação UI/Core?
- [ ] A mudança respeita a separação de camadas (application/domain/infrastructure)?
- [ ] A mudança respeita a separação de scrapers por região?
- [ ] Existe teste escrito antes da implementação (RED)?
- [ ] A implementação é a mínima necessária para passar no teste (GREEN)?
- [ ] Houve refatoração após o teste passar (REFACTOR)?
- [ ] Nenhuma dependência desnecessária foi adicionada?
- [ ] O código segue SOLID e as convenções desta seção?

---

## 14. Objetivo Final

O resultado deve parecer um software open source de alta qualidade: elegante, consistente, modular, facilmente testável, facilmente extensível e simples de compreender. Sempre priorize qualidade de engenharia acima de velocidade de implementação. Ao propor novas funcionalidades, explique primeiro a abordagem arquitetural e só então implemente, seguindo rigorosamente as convenções deste documento.

Este arquivo deve evoluir junto com o projeto — adicione ADRs, convenções de nomenclatura e padrões de commit conforme surgirem novas decisões.