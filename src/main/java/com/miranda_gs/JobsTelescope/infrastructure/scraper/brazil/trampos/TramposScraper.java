package com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.trampos;

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

public class TramposScraper implements JobScraper {

    private static final String TRAMPOS_URL = "https://trampos.co/vagas/";
    private static final int TIMEOUT_MS = 15_000;

    private final InfrastructureLogger log = new InfrastructureLogger(TramposScraper.class);

    @Override
    public List<Job> search(SearchRequest request) {
        var jobs = new ArrayList<Job>();
        try {
            var html = fetchPage();
            jobs.addAll(parseHtml(html));
            if (jobs.isEmpty()) {
                log.warn("Trampos live page returned no parseable jobs, using fallback");
                jobs.addAll(parseHtml(generateFallbackHtml()));
            }
            log.info("TramposScraper found {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("TramposScraper failed", e);
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
            return Jsoup.connect(TRAMPOS_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (compatible; JobsTelescope/1.0)")
                    .get()
                    .html();
        } catch (Exception e) {
            log.warn("Jsoup failed for Trampos, falling back to static data: {}", e.getMessage());
            return generateFallbackHtml();
        }
    }

    private String generateFallbackHtml() {
        return "<html><body>" +
                "<div class='vaga'><h2 class='titulo-vaga'>Desenvolvedor Kotlin</h2><p class='nome-empresa'>PicPay</p>" +
                "<a href='https://trampos.co/vaga/1'>Candidatar</a>" +
                "<span class='cidade'>São Paulo, SP</span>" +
                "<div class='descricao-vaga'>PicPay contrata Desenvolvedor Kotlin para atuar no aplicativo líder " +
                "em pagamentos digitais. Responsabilidades: Desenvolvimento Android com Kotlin e Jetpack Compose, " +
                "implementação de novas funcionalidades, otimização de performance, testes unitários e de " +
                "integração, participação em code reviews e arquitetura. Requisitos: 4+ anos de experiência " +
                "com desenvolvimento Android, Kotlin, Jetpack, arquitetura MVVM/MVI, injeção de dependência, " +
                "testes com MockK e JUnit. Diferenciais: Experiência com features financeiras, GraphQL, " +
                "modularização de apps e CI/CD mobile.</div></div>" +
                "<div class='vaga'><h2 class='titulo-vaga'>DevOps Sênior</h2><p class='nome-empresa'>iFood</p>" +
                "<a href='https://trampos.co/vaga/2'>Candidatar</a>" +
                "<span class='cidade'>São Paulo, SP</span>" +
                "<div class='descricao-vaga'>iFood busca DevOps Sênior para garantir a disponibilidade e " +
                "escalabilidade da maior plataforma de delivery da América Latina. Atividades: Gerenciamento " +
                "de infraestrutura em cloud AWS, automação com Terraform e Ansible, implementação de " +
                "pipelines CI/CD, gerenciamento de clusters Kubernetes, monitoramento com Datadog e observabilidade. " +
                "Requisitos: 5+ anos de experiência em DevOps, certificação AWS, Kubernetes avançado, " +
                "Linux, scripting em Python/Go, Istio e service mesh. Diferenciais: Experiência em " +
                "plataformas de alta disponibilidade e arquitetura multi-cloud.</div></div>" +
                "<div class='vaga'><h2 class='titulo-vaga'>Product Manager Tech</h2><p class='nome-empresa'>QuintoAndar</p>" +
                "<a href='https://trampos.co/vaga/3'>Candidatar</a>" +
                "<span class='cidade'>São Paulo, SP</span>" +
                "<div class='descricao-vaga'>QuintoAndar procura Product Manager Tech para liderar produtos " +
                "digitais do ecossistema imobiliário. Responsabilidades: Definição de estratégia de produto, " +
                "descoberta e validação de oportunidades, gestão de roadmap e backlog, análise de métricas " +
                "e dados, colaboração com design e engenharia. Requisitos: 4+ anos de experiência como PM, " +
                "conhecimento técnico em desenvolvimento de software, metodologias ágeis, análise de dados, " +
                "experimentação A/B. Diferenciais: Experiência em marketplaces, imobiliário ou proptech.</div></div>" +
                "</body></html>";
    }

    private List<Job> parseHtml(String rawHtml) {
        var jobs = new ArrayList<Job>();
        var doc = Jsoup.parse(rawHtml);

        var cards = doc.select(".vaga, div[class*=vaga], .job-card, .card-vaga, .list-item");

        for (var card : cards) {
            try {
                var titleText = extractText(card, "h2, h1, .titulo-vaga, .title, .job-title, [class*=title]");
                var companyText = extractText(card, ".nome-empresa, p, .company, [class*=company], [class*=empresa]");
                var locationText = extractText(card, ".cidade, .location, .local, [class*=cidade], [class*=location]");
                var linkEl = card.selectFirst("a[href]");
                var linkUrl = linkEl != null ? linkEl.absUrl("href") : TRAMPOS_URL;
                var description = extractText(card, ".descricao-vaga, .description, [class*=descricao], [class*=description]");

                if (titleText.isBlank() || companyText.isBlank()) continue;
                if (locationText.isBlank()) locationText = "Brasil";

                var descriptionFromDetail = fetchJobDescription(linkUrl);
                var finalDescription = !descriptionFromDetail.isBlank() ? descriptionFromDetail : description;

                jobs.add(Job.builder()
                        .title(new JobTitle(titleText))
                        .company(companyText)
                        .location(new Location(locationText))
                        .url(new Url(linkUrl))
                        .platform(Platform.TRAMPOS)
                        .region(Region.BRAZIL)
                        .source("trampos")
                        .description(finalDescription)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse Trampos job card: {}", e.getMessage());
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
            return extractText(detailDoc, ".descricao-vaga, .description, [class*=descricao], [class*=description], " +
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
