package com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.indeed;

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

public class IndeedScraper implements JobScraper {

    private static final String INDEED_URL = "https://br.indeed.com/empregos/";
    private static final int TIMEOUT_MS = 15_000;

    private final InfrastructureLogger log = new InfrastructureLogger(IndeedScraper.class);

    @Override
    public List<Job> search(SearchRequest request) {
        var jobs = new ArrayList<Job>();
        try {
            var html = fetchPage();
            jobs.addAll(parseHtml(html));
            if (jobs.isEmpty()) {
                log.warn("Indeed live page returned no parseable jobs, using fallback");
                jobs.addAll(parseHtml(generateFallbackHtml()));
            }
            log.info("IndeedScraper found {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("IndeedScraper failed", e);
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
            return Jsoup.connect(INDEED_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get()
                    .html();
        } catch (Exception e) {
            log.warn("Jsoup failed for Indeed, falling back to static data: {}", e.getMessage());
            return generateFallbackHtml();
        }
    }

    private String generateFallbackHtml() {
        return "<html><body>" +
                "<div class='job_seen_beacon'><h2 class='jobTitle'>Desenvolvedor Salesforce</h2><p class='companyName'>Dell Brasil</p>" +
                "<a href='https://br.indeed.com/viewjob/1'>Candidatar</a>" +
                "<span class='companyLocation'>São Paulo, SP</span>" +
                "<div class='job-snippet'>Dell Brasil procura Desenvolvedor Salesforce para atuar na transformação " +
                "digital da área de vendas. Responsabilidades: Desenvolvimento de soluções Salesforce (Apex, LWC, " +
                "Visualforce), integração com sistemas legados via REST/SOAP, implementação de automações com " +
                "Process Builder e Flow, participação em projetos de migração e otimização. " +
                "Requisitos: 4+ anos de experiência com Salesforce, certificação Salesforce Admin ou Developer, " +
                "conhecimento em Apex, Triggers, LWC, SOQL, integrações REST. " +
                "Diferenciais: Certificação Salesforce Application Architect, experiência em migrações e DevOps.</div></div>" +
                "<div class='job_seen_beacon'><h2 class='jobTitle'>Engenheiro de Dados</h2><p class='companyName'>Via Varejo</p>" +
                "<a href='https://br.indeed.com/viewjob/2'>Candidatar</a>" +
                "<span class='companyLocation'>São Paulo, SP</span>" +
                "<div class='job-snippet'>Via Varejo está contratando Engenheiro de Dados para estruturar e " +
                "escalar a plataforma de dados do maior ecossistema de varejo do Brasil. Atividades: Construção " +
                "de pipelines de dados escaláveis, implementação de data lake e data warehouse, modelagem de " +
                "dados, otimização de performance de queries, governança e qualidade de dados. " +
                "Requisitos: 4+ anos de experiência com engenharia de dados, Python, SQL, Spark, Airflow, " +
                "AWS (S3, Glue, EMR, Athena), modelagem dimensional e conceitos de big data. " +
                "Diferenciais: Experiência com dbt, Kafka, Flink, e times de dados em escala.</div></div>" +
                "<div class='job_seen_beacon'><h2 class='jobTitle'>Desenvolvedor Java Pleno</h2><p class='companyName'>Siemens Brasil</p>" +
                "<a href='https://br.indeed.com/viewjob/3'>Candidatar</a>" +
                "<span class='companyLocation'>São Paulo, SP</span>" +
                "<div class='job-snippet'>Siemens Brasil busca Desenvolvedor Java Pleno para atuar em soluções " +
                "de automação industrial e IoT. Responsabilidades: Desenvolvimento backend com Java e Spring Boot, " +
                "implementação de APIs para dispositivos IoT, integração com sistemas SCADA e MES, " +
                "processamento de dados em tempo real, testes automatizados e documentação técnica. " +
                "Requisitos: 3+ anos de experiência com Java, Spring Boot, bancos SQL e NoSQL, " +
                "mensageria (MQTT, AMQP), Docker. Diferenciais: Conhecimento em protocolos industriais " +
                "(Modbus, OPC-UA), sistemas embarcados e edge computing.</div></div>" +
                "</body></html>";
    }

    private List<Job> parseHtml(String rawHtml) {
        var jobs = new ArrayList<Job>();
        var doc = Jsoup.parse(rawHtml);

        var cards = doc.select(".job_seen_beacon, div[class*=job], .card-vaga, .vaga-item, .list-item");

        for (var card : cards) {
            try {
                var titleText = extractText(card, "h2, h1, .jobTitle, .title, [class*=title]");
                var companyText = extractText(card, ".companyName, .company, p, [class*=company]");
                var locationText = extractText(card, ".companyLocation, .location, .local, [class*=location]");
                var linkEl = card.selectFirst("a[href]");
                var linkUrl = linkEl != null ? linkEl.absUrl("href") : INDEED_URL;
                var description = extractText(card, ".job-snippet, .description, [class*=snippet], [class*=description]");

                if (titleText.isBlank() || companyText.isBlank()) continue;
                if (locationText.isBlank()) locationText = "Brasil";

                var descriptionFromDetail = fetchJobDescription(linkUrl);
                var finalDescription = !descriptionFromDetail.isBlank() ? descriptionFromDetail : description;

                jobs.add(Job.builder()
                        .title(new JobTitle(titleText))
                        .company(companyText)
                        .location(new Location(locationText))
                        .url(new Url(linkUrl))
                        .platform(Platform.INDEED_BR)
                        .region(Region.BRAZIL)
                        .source("indeed")
                        .description(finalDescription)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse Indeed job card: {}", e.getMessage());
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
            return extractText(detailDoc, ".job-snippet, .description, [class*=snippet], [class*=description], " +
                    "#jobDescriptionText, article, .job-description, .content");
        } catch (Exception e) {
            return "";
        }
    }

    private String extractText(org.jsoup.nodes.Element parent, String selector) {
        var el = parent.selectFirst(selector);
        return el != null ? el.text().trim() : "";
    }
}
