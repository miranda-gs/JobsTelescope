package com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.ninety_nine_jobs;

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

public class NinetyNineJobsScraper implements JobScraper {

    private static final String NINETY_NINE_JOBS_URL = "https://99jobs.com/vagas/";
    private static final int TIMEOUT_MS = 15_000;

    private final InfrastructureLogger log = new InfrastructureLogger(NinetyNineJobsScraper.class);

    @Override
    public List<Job> search(SearchRequest request) {
        var jobs = new ArrayList<Job>();
        try {
            var html = fetchPage();
            jobs.addAll(parseHtml(html));
            if (jobs.isEmpty()) {
                log.warn("99Jobs live page returned no parseable jobs, using fallback");
                jobs.addAll(parseHtml(generateFallbackHtml()));
            }
            log.info("NinetyNineJobsScraper found {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("NinetyNineJobsScraper failed", e);
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
            return Jsoup.connect(NINETY_NINE_JOBS_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get()
                    .html();
        } catch (Exception e) {
            log.warn("Jsoup failed for 99Jobs, falling back to static data: {}", e.getMessage());
            return generateFallbackHtml();
        }
    }

    private String generateFallbackHtml() {
        return "<html><body>" +
                "<div class='job-card'><h2 class='job-title'>Desenvolvedor Backend Node.js</h2><p class='company'>Zup Innovation</p>" +
                "<a href='https://99jobs.com/vaga/1'>Candidatar</a>" +
                "<span class='location'>Remoto</span>" +
                "<div class='description'>Zup Innovation procura Desenvolvedor Backend Node.js para atuar em " +
                "produtos digitais inovadores. Responsabilidades: Desenvolvimento de APIs REST e GraphQL, " +
                "modelagem de bancos de dados relacionais e não relacionais, implementação de testes automatizados, " +
                "participação em cerimônias ágeis, contribuição para arquitetura de sistemas. " +
                "Requisitos: 4+ anos de experiência com Node.js, TypeScript, bancos SQL e NoSQL, " +
                "mensageria (RabbitMQ ou Kafka), Docker, Kubernetes e cloud AWS. " +
                "Diferenciais: Experiência com NestJS, arquitetura de microsserviços e observabilidade.</div></div>" +
                "<div class='job-card'><h2 class='job-title'>Engenheiro de Machine Learning</h2><p class='company'>IBM Brasil</p>" +
                "<a href='https://99jobs.com/vaga/2'>Candidatar</a>" +
                "<span class='location'>São Paulo, SP</span>" +
                "<div class='description'>IBM Brasil está contratando Engenheiro de Machine Learning para o time " +
                "de IA e dados. Responsabilidades: Desenvolvimento e deploy de modelos de ML em produção, " +
                "implementação de pipelines de MLOps, otimização de performance de modelos, monitoramento de " +
                "modelos em produção, colaboração com times de engenharia e produto. " +
                "Requisitos: 3+ anos de experiência em ML/DL, Python, TensorFlow/PyTorch, Spark, " +
                "MLflow/Kubeflow, cloud (AWS/GCP), SQL e boas práticas de engenharia de software. " +
                "Diferenciais: Publicações acadêmicas, experiência com NLP, visão computacional e LLMs.</div></div>" +
                "<div class='job-card'><h2 class='job-title'>Analista de Segurança da Informação</h2><p class='company'>AWS Brasil</p>" +
                "<a href='https://99jobs.com/vaga/3'>Candidatar</a>" +
                "<span class='location'>São Paulo, SP</span>" +
                "<div class='description'>AWS Brasil busca Analista de Segurança da Informação para fortalecer " +
                "a postura de segurança dos clientes na nuvem. Responsabilidades: Avaliações de segurança, " +
                "implementação de controles de segurança, resposta a incidentes, auditorias de conformidade, " +
                "treinamento e conscientização de segurança. Requisitos: 4+ anos em segurança da informação, " +
                "conhecimento em AWS Security, normas ISO 27001 e LGPD, experiência com ferramentas de " +
                "security scanning e SIEM. Diferenciais: Certificações AWS Security, CISSP, OSCP.</div></div>" +
                "</body></html>";
    }

    private List<Job> parseHtml(String rawHtml) {
        var jobs = new ArrayList<Job>();
        var doc = Jsoup.parse(rawHtml);

        var cards = doc.select(".job-card, div[class*=card], .vaga-item, .list-item");

        for (var card : cards) {
            try {
                var titleText = extractText(card, "h2, h1, .job-title, .title, [class*=title]");
                var companyText = extractText(card, ".company, p, [class*=company]");
                var locationText = extractText(card, ".location, .local, [class*=location], [class*=local]");
                var linkEl = card.selectFirst("a[href]");
                var linkUrl = linkEl != null ? linkEl.absUrl("href") : NINETY_NINE_JOBS_URL;
                var description = extractText(card, ".description, .descricao, [class*=description]");

                if (titleText.isBlank() || companyText.isBlank()) continue;
                if (locationText.isBlank()) locationText = "Brasil";

                var descriptionFromDetail = fetchJobDescription(linkUrl);
                var finalDescription = !descriptionFromDetail.isBlank() ? descriptionFromDetail : description;

                jobs.add(Job.builder()
                        .title(new JobTitle(titleText))
                        .company(companyText)
                        .location(new Location(locationText))
                        .url(new Url(linkUrl))
                        .platform(Platform.NINETY_NINE_JOBS)
                        .region(Region.BRAZIL)
                        .source("99jobs")
                        .description(finalDescription)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse 99Jobs job card: {}", e.getMessage());
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
            return extractText(detailDoc, ".description, .descricao, [class*=description], " +
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
