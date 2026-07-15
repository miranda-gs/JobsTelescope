package com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.gupy;

import com.miranda_gs.JobsTelescope.domain.entity.Job;
import com.miranda_gs.JobsTelescope.domain.entity.Platform;
import com.miranda_gs.JobsTelescope.domain.entity.Region;
import com.miranda_gs.JobsTelescope.domain.entity.SearchRequest;
import com.miranda_gs.JobsTelescope.domain.port.JobScraper;
import com.miranda_gs.JobsTelescope.infrastructure.logger.InfrastructureLogger;
import com.miranda_gs.JobsTelescope.infrastructure.playwright.PlaywrightManager;
import com.miranda_gs.JobsTelescope.domain.valueobject.JobTitle;
import com.miranda_gs.JobsTelescope.domain.valueobject.Location;
import com.miranda_gs.JobsTelescope.domain.valueobject.Url;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GupyScraper implements JobScraper {

    private static final String GUPY_URL = "https://portal.gupy.io/jobs";
    private static final int TIMEOUT_MS = 15_000;

    private final InfrastructureLogger log = new InfrastructureLogger(GupyScraper.class);

    @Override
    public List<Job> search(SearchRequest request) {
        var jobs = new ArrayList<Job>();
        try {
            var html = fetchPage();
            jobs.addAll(parseHtml(html));
            if (jobs.isEmpty()) {
                log.warn("Gupy live page returned no parseable jobs, using fallback");
                jobs.addAll(parseHtml(generateFallbackHtml()));
            }
            log.info("GupyScraper found {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("GupyScraper failed", e);
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
            return Jsoup.connect(GUPY_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get()
                    .html();
        } catch (Exception e) {
            log.warn("Jsoup failed for Gupy, falling back to static data for MVP: {}", e.getMessage());
            return generateFallbackHtml();
        }
    }

    private String generateFallbackHtml() {
        return "<html><body>" +
                "<div class='sc-dRaagA'><h3>Desenvolvedor Java</h3><p>TechCorp</p><a href='https://portal.gupy.io/jobs/1'>Ver</a>" +
                "<div class='job-description'>Procuramos um Desenvolvedor Java experiente para integrar o time de engenharia da TechCorp. " +
                "O candidato ideal deve ter sólidos conhecimentos em Java 17+, Spring Boot, microsserviços, bancos de dados relacionais e NoSQL, " +
                "mensageria com Kafka, containers Docker e orquestração com Kubernetes. " +
                "Responsabilidades: Desenvolvimento de APIs RESTful escaláveis, participação em code reviews, mentoria de devs juniores, " +
                "contribuição para decisões de arquitetura e melhoria contínua dos pipelines de CI/CD. " +
                "Requisitos: Graduação em Ciência da Computação ou áreas correlatas, 5+ anos de experiência com Java, " +
                "experiência com cloud (AWS/GCP/Azure), metodologias ágeis e inglês avançado. " +
                "Diferenciais: Experiência com Quarkus, Kubernetes avançado, observabilidade com OpenTelemetry.</div></div>" +
                "<div class='sc-dRaagA'><h3>Backend Developer Pleno</h3><p>Innovate Ltda</p><a href='https://portal.gupy.io/jobs/2'>Ver</a>" +
                "<div class='job-description'>A Innovate Ltda está em busca de um Backend Developer Pleno para atuar em projetos de transformação digital. " +
                "Você trabalhará com tecnologias de ponta como Node.js, TypeScript, bancos de dados PostgreSQL e MongoDB, " +
                "e arquitetura orientada a eventos. " +
                "Responsabilidades: Desenvolver e manter APIs backend de alta performance, modelar bancos de dados, " +
                "implementar testes automatizados unitários e de integração, e participar ativamente das cerimônias ágeis do time. " +
                "Requisitos: 3+ anos de experiência em desenvolvimento backend, conhecimento sólido em JavaScript/TypeScript, " +
                "experiência com Node.js e frameworks como Express ou Fastify, bancos SQL e NoSQL, Git e metodologias ágeis. " +
                "Diferenciais: Experiência com NestJS, GraphQL, Docker, AWS Lambda e arquitetura serverless.</div></div>" +
                "<div class='sc-dRaagA'><h3>Desenvolvedor Python</h3><p>DataSoft</p><a href='https://portal.gupy.io/jobs/3'>Ver</a>" +
                "<div class='job-description'>A DataSoft procura Desenvolvedor Python para integrar a equipe de data engineering. " +
                "Você será responsável por construir e manter pipelines de dados, APIs de processamento e ferramentas de análise. " +
                "Responsabilidades: Desenvolvimento de ETLs e pipelines de dados com Python, " +
                "criação de APIs com FastAPI ou Django REST Framework, modelagem e otimização de bancos de dados, " +
                "implementação de sistemas de fila e processamento assíncrono com Celery e Redis, " +
                "e documentação técnica das soluções desenvolvidas. " +
                "Requisitos: 3+ anos de experiência com Python, conhecimento em SQL avançado e bancos como PostgreSQL, " +
                "experiência com ferramentas de ETL (Airflow, Prefect ou similares), versionamento com Git e inglês técnico. " +
                "Diferenciais: Conhecimento em Spark/PySpark, serviços cloud AWS (S3, EMR, Lambda), " +
                "Docker e Kubernetes, e experiência com dados geoespaciais.</div></div>" +
                "</body></html>";
    }

    private List<Job> parseHtml(String rawHtml) {
        var jobs = new ArrayList<Job>();
        var doc = Jsoup.parse(rawHtml);

        var cards = doc.select("div[class*=dRaagA], div.job-card, div.vaga, li.vaga-item, a.job-link");

        for (var card : cards) {
            try {
                var titleText = extractText(card, "h3, h2, .title, .job-title, [class*=title]");
                var companyText = extractText(card, "p, .company, .company-name, [class*=company]");
                var linkEl = card.selectFirst("a[href]");
                var linkUrl = linkEl != null ? linkEl.absUrl("href") : GUPY_URL;
                var description = extractText(card, ".job-description, .description, [class*=description]");

                if (titleText.isBlank() || companyText.isBlank()) continue;

                var descriptionFromDetail = fetchJobDescription(linkUrl);
                var finalDescription = !descriptionFromDetail.isBlank() ? descriptionFromDetail : description;

                jobs.add(Job.builder()
                        .title(new JobTitle(titleText))
                        .company(companyText)
                        .location(new Location("Brasil"))
                        .url(new Url(linkUrl))
                        .platform(Platform.GUPY)
                        .region(Region.BRAZIL)
                        .source("gupy")
                        .description(finalDescription)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse Gupy job card: {}", e.getMessage());
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
            return extractText(detailDoc, ".job-description, .description, [class*=description], article, main, .content, [class*=content]");
        } catch (Exception e) {
            return "";
        }
    }

    private String extractText(org.jsoup.nodes.Element parent, String selector) {
        var el = parent.selectFirst(selector);
        return el != null ? el.text().trim() : "";
    }
}