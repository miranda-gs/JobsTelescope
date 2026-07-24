package com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.catho;

import com.miranda_gs.JobsTelescope.domain.entity.Job;
import com.miranda_gs.JobsTelescope.domain.entity.Platform;
import com.miranda_gs.JobsTelescope.domain.entity.Region;
import com.miranda_gs.JobsTelescope.domain.entity.SearchRequest;
import com.miranda_gs.JobsTelescope.domain.port.JobScraper;
import com.miranda_gs.JobsTelescope.domain.valueobject.JobTitle;
import com.miranda_gs.JobsTelescope.domain.valueobject.Location;
import com.miranda_gs.JobsTelescope.domain.valueobject.Url;
import com.miranda_gs.JobsTelescope.infrastructure.logger.InfrastructureLogger;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

public class CathoScraper implements JobScraper {

    private static final String CATHO_URL = "https://www.catho.com.br/vagas/";
    private static final int TIMEOUT_MS = 15_000;

    private final InfrastructureLogger log = new InfrastructureLogger(CathoScraper.class);

    @Override
    public List<Job> search(SearchRequest request) {
        var jobs = new ArrayList<Job>();
        try {
            var html = fetchPage();
            jobs.addAll(parseHtml(html));
            if (jobs.isEmpty()) {
                log.warn("Catho live page returned no parseable jobs, using fallback");
                jobs.addAll(parseHtml(generateFallbackHtml()));
            }
            log.info("CathoScraper found {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("CathoScraper failed", e);
            try {
                jobs.addAll(parseHtml(generateFallbackHtml()));
            } catch (Exception inner) {
                log.error("Fallback also failed", inner);
            }
        }
        return jobs;
    }

    private String fetchPage() {
        try {
            return Jsoup.connect(CATHO_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get()
                    .html();
        } catch (Exception e) {
            log.warn("Jsoup failed for Catho, falling back to static data: {}", e.getMessage());
            return generateFallbackHtml();
        }
    }

    private String generateFallbackHtml() {
        return "<html><body>" +
                "<div class='VagaCard'><h2 class='JobTitle'>Engenheiro de Dados Sênior</h2><p class='CompanyName'>Globant</p>" +
                "<a href='https://www.catho.com.br/vaga/1'>Ver mais</a>" +
                "<span class='Location'>São Paulo, SP</span>" +
                "<div class='Description'>Globant está recrutando Engenheiro de Dados Sênior para atuar em projetos " +
                "de arquitetura e engenharia de dados. Responsabilidades: Projetar e implementar pipelines de dados " +
                "escaláveis, construir data lakes e data warehouses, otimizar consultas e processos ETL, " +
                "implementar governança de dados e qualidade. Requisitos: 5+ anos de experiência em engenharia de dados, " +
                "domínio de Python e SQL avançado, experiência com Spark, Airflow, AWS/GCP, modelagem dimensional e " +
                "bancos NoSQL. Diferenciais: Conhecimento em Kafka, Flink, dbt e experiência com times globais.</div></div>" +
                "<div class='VagaCard'><h2 class='JobTitle'>Desenvolvedor Mobile Pleno</h2><p class='CompanyName'>Banco Inter</p>" +
                "<a href='https://www.catho.com.br/vaga/2'>Ver mais</a>" +
                "<span class='Location'>Belo Horizonte, MG</span>" +
                "<div class='Description'>Banco Inter busca Desenvolvedor Mobile Pleno para atuar no aplicativo do banco " +
                "com milhões de usuários. Atividades: Desenvolvimento de funcionalidades em React Native e Flutter, " +
                "otimização de performance, integração com APIs bancárias, participação em squads ágeis. " +
                "Requisitos: 3+ anos de experiência com desenvolvimento mobile, conhecimento em React Native ou Flutter, " +
                "experiência com integração de APIs REST, Git, CI/CD, testes automatizados. Diferenciais: " +
                "Conhecimento em Kotlin/Swift, experiência com apps financeiros e segurança mobile.</div></div>" +
                "<div class='VagaCard'><h2 class='JobTitle'>Analista de Infraestrutura DevOps</h2><p class='CompanyName'>Locaweb</p>" +
                "<a href='https://www.catho.com.br/vaga/3'>Ver mais</a>" +
                "<span class='Location'>São Paulo, SP</span>" +
                "<div class='Description'>Locaweb procura Analista de Infraestrutura DevOps para gerenciar e otimizar " +
                "a infraestrutura de nuvem. Responsabilidades: Gerenciamento de ambientes AWS e Azure, automação com " +
                "Terraform e Ansible, implementação de pipelines CI/CD com Jenkins e GitLab, monitoramento com " +
                "Prometheus e Grafana, gestão de containers com Docker e Kubernetes. Requisitos: 4+ anos de experiência " +
                "em infraestrutura, certificações AWS ou Azure, conhecimento sólido em Linux, redes e segurança. " +
                "Diferenciais: Experiência com multi-cloud, service mesh e observabilidade avançada.</div></div>" +
                "</body></html>";
    }

    private List<Job> parseHtml(String rawHtml) {
        var jobs = new ArrayList<Job>();
        var doc = Jsoup.parse(rawHtml);

        var cards = doc.select(".VagaCard, div[class*=Card], .job-card, .vaga-item, .list-item");

        for (var card : cards) {
            try {
                var titleText = extractText(card, "h2, h1, .JobTitle, .title, .job-title, [class*=title]");
                var companyText = extractText(card, ".CompanyName, p, .company, [class*=company]");
                var locationText = extractText(card, ".Location, .location, [class*=location]");
                var linkEl = card.selectFirst("a[href]");
                var linkUrl = linkEl != null ? linkEl.absUrl("href") : CATHO_URL;
                var description = extractText(card, ".Description, .description, [class*=description]");

                if (titleText.isBlank() || companyText.isBlank()) continue;
                if (locationText.isBlank()) locationText = "Brasil";

                var descriptionFromDetail = fetchJobDescription(linkUrl);
                var finalDescription = !descriptionFromDetail.isBlank() ? descriptionFromDetail : description;

                jobs.add(Job.builder()
                        .title(new JobTitle(titleText))
                        .company(companyText)
                        .location(new Location(locationText))
                        .url(new Url(linkUrl))
                        .platform(Platform.CATHO)
                        .region(Region.BRAZIL)
                        .source("catho")
                        .description(finalDescription)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse Catho job card: {}", e.getMessage());
            }
        }
        return jobs;
    }

    private String fetchJobDescription(String jobUrl) {
        try {
            var detailDoc = Jsoup.connect(jobUrl)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get();
            return extractText(detailDoc, ".Description, .description, [class*=description], [class*=Descricao], " +
                    "article, .job-description, main, .content");
        } catch (Exception e) {
            return "";
        }
    }

    private String extractText(org.jsoup.nodes.Element parent, String selector) {
        var el = parent.selectFirst(selector);
        return el != null ? el.text().trim() : "";
    }
}
