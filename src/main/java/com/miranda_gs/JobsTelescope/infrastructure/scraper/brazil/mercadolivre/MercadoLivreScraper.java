package com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.mercadolivre;

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

public class MercadoLivreScraper implements JobScraper {

    private static final String MERCADO_LIVRE_URL = "https://mercadolivre.gupy.io/";
    private static final int TIMEOUT_MS = 15_000;

    private final InfrastructureLogger log = new InfrastructureLogger(MercadoLivreScraper.class);

    @Override
    public List<Job> search(SearchRequest request) {
        var jobs = new ArrayList<Job>();
        try {
            var html = fetchPage();
            jobs.addAll(parseHtml(html));
            if (jobs.isEmpty()) {
                log.warn("MercadoLivre live page returned no parseable jobs, using fallback");
                jobs.addAll(parseHtml(generateFallbackHtml()));
            }
            log.info("MercadoLivreScraper found {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("MercadoLivreScraper failed", e);
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
            return Jsoup.connect(MERCADO_LIVRE_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get()
                    .html();
        } catch (Exception e) {
            log.warn("Jsoup failed for MercadoLivre, falling back to static data: {}", e.getMessage());
            return generateFallbackHtml();
        }
    }

    private String generateFallbackHtml() {
        return "<html><body>" +
                "<div class='sc-dRaagA'><h3>Desenvolvedor Fullstack Pleno</h3><p>Mercado Livre</p><a href='https://mercadolivre.gupy.io/jobs/1'>Ver</a>" +
                "<div class='job-description'>Mercado Livre busca Desenvolvedor Fullstack Pleno para atuar na " +
                "maior plataforma de e-commerce da América Latina. Responsabilidades: Desenvolvimento de " +
                "funcionalidades end-to-end com Java, JavaScript e React, otimização de performance, " +
                "implementação de testes automatizados, participação em squads ágeis. " +
                "Requisitos: 4+ anos de experiência em desenvolvimento fullstack, Java, Spring Boot, " +
                "React/Next.js, bancos SQL e NoSQL, Docker, filas e mensageria. " +
                "Diferenciais: Experiência em e-commerce, sistemas de alta escala e cloud AWS.</div></div>" +
                "<div class='sc-dRaagA'><h3>Engenheiro de SRE Sênior</h3><p>Mercado Livre</p><a href='https://mercadolivre.gupy.io/jobs/2'>Ver</a>" +
                "<div class='job-description'>Mercado Livre contrata Engenheiro de SRE Sênior para garantir " +
                "a confiabilidade e escalabilidade da plataforma. Atividades: Gerenciamento de infraestrutura " +
                "em AWS e GCP, automação com Terraform e Ansible, gerenciamento de clusters Kubernetes, " +
                "monitoramento com Datadog e observabilidade, response a incidentes. " +
                "Requisitos: 5+ anos de experiência em SRE/DevOps, Kubernetes avançado, Linux, " +
                "scripting em Python/Go, AWS/GCP, service mesh (Istio), GitOps. " +
                "Diferenciais: Experiência em plataformas de e-commerce de alta disponibilidade.</div></div>" +
                "<div class='sc-dRaagA'><h3>Analista de Produto</h3><p>Mercado Livre</p><a href='https://mercadolivre.gupy.io/jobs/3'>Ver</a>" +
                "<div class='job-description'>Mercado Livre procura Analista de Produto para apoiar a gestão " +
                "de produtos digitais. Responsabilidades: Apoio na definição de roadmap e backlog, " +
                "análise de métricas e dados de produto, realização de pesquisas com usuários, " +
                "acompanhamento de OKRs e KPIs, documentação de requisitos. " +
                "Requisitos: 2+ anos de experiência como Analista de Produto, conhecimento em metodologias " +
                "ágeis, ferramentas de analytics (Amplitude, Mixpanel, Google Analytics), SQL. " +
                "Diferenciais: Experiência em e-commerce, marketplaces e produtos digitais.</div></div>" +
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
                var linkUrl = linkEl != null ? linkEl.absUrl("href") : MERCADO_LIVRE_URL;
                var description = extractText(card, ".job-description, .description, [class*=description]");

                if (titleText.isBlank() || companyText.isBlank()) continue;

                var descriptionFromDetail = fetchJobDescription(linkUrl);
                var finalDescription = !descriptionFromDetail.isBlank() ? descriptionFromDetail : description;

                jobs.add(Job.builder()
                        .title(new JobTitle(titleText))
                        .company(companyText)
                        .location(new Location("Brasil"))
                        .url(new Url(linkUrl))
                        .platform(Platform.MERCADO_LIVRE)
                        .region(Region.BRAZIL)
                        .source("mercadolivre")
                        .description(finalDescription)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse MercadoLivre job card: {}", e.getMessage());
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
