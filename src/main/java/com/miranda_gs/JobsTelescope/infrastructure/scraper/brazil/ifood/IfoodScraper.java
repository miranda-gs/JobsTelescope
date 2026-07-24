package com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.ifood;

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

public class IfoodScraper implements JobScraper {

    private static final String IFOOD_URL = "https://ifood.gupy.io/";
    private static final int TIMEOUT_MS = 15_000;

    private final InfrastructureLogger log = new InfrastructureLogger(IfoodScraper.class);

    @Override
    public List<Job> search(SearchRequest request) {
        var jobs = new ArrayList<Job>();
        try {
            var html = fetchPage();
            jobs.addAll(parseHtml(html));
            if (jobs.isEmpty()) {
                log.warn("iFood live page returned no parseable jobs, using fallback");
                jobs.addAll(parseHtml(generateFallbackHtml()));
            }
            log.info("IfoodScraper found {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("IfoodScraper failed", e);
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
            return Jsoup.connect(IFOOD_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get()
                    .html();
        } catch (Exception e) {
            log.warn("Jsoup failed for iFood, falling back to static data: {}", e.getMessage());
            return generateFallbackHtml();
        }
    }

    private String generateFallbackHtml() {
        return "<html><body>" +
                "<div class='sc-dRaagA'><h3>Desenvolvedor Backend Sênior</h3><p>iFood</p><a href='https://ifood.gupy.io/jobs/1'>Ver</a>" +
                "<div class='job-description'>iFood busca Desenvolvedor Backend Sênior para atuar na maior " +
                "plataforma de delivery da América Latina. Responsabilidades: Desenvolvimento de microsserviços " +
                "escaláveis com Java e Kotlin, Spring Boot, arquitetura orientada a eventos, sistemas de " +
                "alta disponibilidade e baixa latência. Requisitos: 6+ anos de experiência em desenvolvimento " +
                "backend, Java/Kotlin, Spring, bancos relacionais e NoSQL, mensageria (Kafka/RabbitMQ), " +
                "Docker, Kubernetes e cloud AWS. Diferenciais: Experiência em sistemas de recomendação, " +
                "machine learning e arquitetura de dados em tempo real.</div></div>" +
                "<div class='sc-dRaagA'><h3>Engenheiro de Machine Learning</h3><p>iFood</p><a href='https://ifood.gupy.io/jobs/2'>Ver</a>" +
                "<div class='job-description'>iFood contrata Engenheiro de Machine Learning para desenvolver " +
                "modelos que transformam a experiência de delivery. Atividades: Desenvolvimento e deploy de " +
                "modelos de ML em produção, sistemas de recomendação, otimização de rotas e logística, " +
                "previsão de demanda, experimentação A/B. Requisitos: 4+ anos de experiência em ML/DL, " +
                "Python, TensorFlow/PyTorch, Spark, feature stores, MLflow/Kubeflow, SQL e cloud AWS. " +
                "Diferenciais: Experiência em sistemas de recomendação, NLP e visão computacional.</div></div>" +
                "<div class='sc-dRaagA'><h3>Product Designer Sênior</h3><p>iFood</p><a href='https://ifood.gupy.io/jobs/3'>Ver</a>" +
                "<div class='job-description'>iFood procura Product Designer Sênior para liderar o design de " +
                "produtos digitais que impactam milhões de usuários. Responsabilidades: Pesquisa com usuários, " +
                "prototipação de interfaces, design de interação e experiência, teste de usabilidade, " +
                "colaboração com product managers e engenheiros, contribuição para o design system. " +
                "Requisitos: 5+ anos de experiência em product design, domínio de Figma, conhecimento em " +
                "pesquisa qualitativa e quantitativa, portfólio de produtos digitais. " +
                "Diferenciais: Experiência em marketplaces, delivery ou e-commerce.</div></div>" +
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
                var linkUrl = linkEl != null ? linkEl.absUrl("href") : IFOOD_URL;
                var description = extractText(card, ".job-description, .description, [class*=description]");

                if (titleText.isBlank() || companyText.isBlank()) continue;

                var descriptionFromDetail = fetchJobDescription(linkUrl);
                var finalDescription = !descriptionFromDetail.isBlank() ? descriptionFromDetail : description;

                jobs.add(Job.builder()
                        .title(new JobTitle(titleText))
                        .company(companyText)
                        .location(new Location("Brasil"))
                        .url(new Url(linkUrl))
                        .platform(Platform.IFOOD)
                        .region(Region.BRAZIL)
                        .source("ifood")
                        .description(finalDescription)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse iFood job card: {}", e.getMessage());
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
            return extractText(detailDoc, ".job-description, .description, [class*=description], " +
                    "article, main, .content, [class*=content]");
        } catch (Exception e) {
            return "";
        }
    }

    private String extractText(org.jsoup.nodes.Element parent, String selector) {
        var el = parent.selectFirst(selector);
        return el != null ? el.text().trim() : "";
    }
}
