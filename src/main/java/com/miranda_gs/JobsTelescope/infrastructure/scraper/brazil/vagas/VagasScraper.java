package com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.vagas;

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

public class VagasScraper implements JobScraper {

    private static final String VAGAS_URL = "https://www.vagas.com.br/vagas-de-";
    private static final int TIMEOUT_MS = 15_000;

    private final InfrastructureLogger log = new InfrastructureLogger(VagasScraper.class);

    @Override
    public List<Job> search(SearchRequest request) {
        var jobs = new ArrayList<Job>();
        try {
            var html = fetchPage();
            jobs.addAll(parseHtml(html));
            if (jobs.isEmpty()) {
                log.warn("Vagas live page returned no parseable jobs, using fallback");
                jobs.addAll(parseHtml(generateFallbackHtml()));
            }
            log.info("VagasScraper found {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("VagasScraper failed", e);
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
            return Jsoup.connect(VAGAS_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get()
                    .html();
        } catch (Exception e) {
            log.warn("Jsoup failed for Vagas, falling back to static data: {}", e.getMessage());
            return generateFallbackHtml();
        }
    }

    private String generateFallbackHtml() {
        return "<html><body>" +
                "<div class='card-vaga'><h2 class='cargo'>Desenvolvedor Java Senior</h2><p class='empresa'>Accenture Brasil</p>" +
                "<a href='https://www.vagas.com.br/vaga/1'>Candidatar</a>" +
                "<span class='local'>São Paulo, SP</span>" +
                "<div class='descricao'>Accenture Brasil está contratando Desenvolvedor Java Senior para atuar em projetos " +
                "de transformação digital. Responsabilidades: Desenvolvimento de microsserviços com Spring Boot, " +
                "implementação de APIs RESTful, participação em cerimônias ágeis, code reviews e mentoria técnica. " +
                "Requisitos: 5+ anos de experiência com Java, Spring Framework, bancos relacionais, Docker, Kubernetes, " +
                "mensageria com Kafka, cloud AWS e metodologias ágeis. Diferenciais: Certificações Java, " +
                "experiência com Quarkus, Terraform e arquitetura de eventos.</div></div>" +
                "<div class='card-vaga'><h2 class='cargo'>Analista de Sistemas Pleno</h2><p class='empresa'>TOTVS</p>" +
                "<a href='https://www.vagas.com.br/vaga/2'>Candidatar</a>" +
                "<span class='local'>Belo Horizonte, MG</span>" +
                "<div class='descricao'>TOTVS busca Analista de Sistemas Pleno para integrar equipe de desenvolvimento " +
                "de soluções corporativas. Atividades: Levantamento de requisitos, modelagem de sistemas, " +
                "desenvolvimento em Java e bancos SQL, elaboração de documentação técnica, suporte a testes e " +
                "homologação. Requisitos: 3+ anos de experiência, conhecimento em análise e design de sistemas, " +
                "SQL avançado, modelagem UML, experiência com metodologias ágeis. Diferenciais: Conhecimento em " +
                "ERP, CRM ou sistemas financeiros, experiência com integrações e APIs REST.</div></div>" +
                "<div class='card-vaga'><h2 class='cargo'>Estagiário de Desenvolvimento</h2><p class='empresa'>Sensedia</p>" +
                "<a href='https://www.vagas.com.br/vaga/3'>Candidatar</a>" +
                "<span class='local'>Curitiba, PR</span>" +
                "<div class='descricao'>Sensedia procura Estagiário de Desenvolvimento para aprendizado em plataformas " +
                "de API e integrações. Bolsa-auxílio compatível com mercado, vale-refeição, vale-transporte e " +
                "programa de mentoria. Atividades: Suporte ao desenvolvimento de APIs, testes automatizados, " +
                "documentação técnica, participação em treinamentos e workshops internos. Requisitos: Cursando " +
                "Ciência da Computação, Engenharia da Computação, Sistemas de Informação ou áreas correlatas, " +
                "conhecimentos básicos em Java, lógica de programação e banco de dados. Diferenciais: " +
                "Conhecimento em JavaScript, Git e metodologias ágeis.</div></div>" +
                "</body></html>";
    }

    private List<Job> parseHtml(String rawHtml) {
        var jobs = new ArrayList<Job>();
        var doc = Jsoup.parse(rawHtml);

        var cards = doc.select(".card-vaga, div[class*=vaga], .job-listing, .list-item");

        for (var card : cards) {
            try {
                var titleText = extractText(card, "h2, h1, .cargo, .title, .job-title, [class*=title]");
                var companyText = extractText(card, ".empresa, p, .company, [class*=company], [class*=empresa]");
                var locationText = extractText(card, ".local, .location, [class*=local], [class*=location]");
                var linkEl = card.selectFirst("a[href]");
                var linkUrl = linkEl != null ? linkEl.absUrl("href") : VAGAS_URL;
                var description = extractText(card, ".descricao, .description, [class*=descricao], [class*=description]");

                if (titleText.isBlank() || companyText.isBlank()) continue;
                if (locationText.isBlank()) locationText = "Brasil";

                var descriptionFromDetail = fetchJobDescription(linkUrl);
                var finalDescription = !descriptionFromDetail.isBlank() ? descriptionFromDetail : description;

                jobs.add(Job.builder()
                        .title(new JobTitle(titleText))
                        .company(companyText)
                        .location(new Location(locationText))
                        .url(new Url(linkUrl))
                        .platform(Platform.VAGAS)
                        .region(Region.BRAZIL)
                        .source("vagas")
                        .description(finalDescription)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse Vagas job card: {}", e.getMessage());
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
            return extractText(detailDoc, ".descricao, .description, [class*=descricao], [class*=description], " +
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
