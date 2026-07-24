# JobsTelescope

Ferramenta CLI para busca automatizada de vagas de emprego em múltiplas plataformas.

## Pré-requisitos

- **Java 25+** (JRE - recomendo a versão GraalCM - é suficiente para executar, JDK apenas se for compilar)
- **Node.js 18+** (com npm)
- **Git** (para clonar o repositório)

## Instalação

### Linux / macOS

```bash
curl -fsSL https://raw.githubusercontent.com/miranda-gs/JobsTelescope/main/install.sh | bash
```

O script instala automaticamente Java, Node.js, clona o repositório, baixa o JAR pré-compilado e registra o comando `jtelescope` globalmente.

### Windows (PowerShell)

Abra o PowerShell como Administrador e execute:

```powershell
irm https://raw.githubusercontent.com/miranda-gs/JobsTelescope/main/install.ps1 | iex
```

### Instalação manual

```bash
git clone https://github.com/miranda-gs/JobsTelescope.git
cd JobsTelescope

# Compilar o backend (requer JDK)
./mvnw package -DskipTests

# Instalar dependências do UI e registrar comando global
cd ui
npm install
npm link
```

## Uso
Digite o seguinte comando dentro da pasta onde foi instalado o JobsTelescope
```bash
jtelescope
```

Para usar um JAR customizado:

```bash
jtelescope /caminho/para/seu.jar
```

### Fluxo

1. Entre na pasta do código fonte
2. Digite jtelescope
3. Digite a vaga desejada e pressione Enter
4. Selecione a região com Tab e confirme com Enter
5. Acompanhe o progresso da busca
6. Os resultados são salvos automaticamente em:

|Diretório de saída| |
|---|---|
| `./JobsTelescope/output/` |
## Desenvolvimento

```bash
# UI (com hot reload via tsx)
cd ui
npm run dev

# Backend
./mvnw package -DskipTests

# Testes
cd ui && npm test
```

## Estrutura

```
JobsTelescope/
├── output/
├── ui/                  # Frontend (React + Ink)
│   ├── src/
│   │   ├── components/  # Componentes TUI
│   │   ├── hooks/       # Hooks React
│   │   └── lib/         # Core client, tipos, validação
│   └── bin/             # Entry point CLI
├── src/                 # Backend (Spring Boot)
│   └── main/java/.../
│       ├── application/ # Casos de uso
│       ├── domain/      # Entidades e portas
│       └── infrastructure/ # Scrapers, exportadores, IO
├── install.sh           # Instalador Linux/macOS
├── install.ps1          # Instalador Windows
└── mvnw                 # Maven Wrapper
```
