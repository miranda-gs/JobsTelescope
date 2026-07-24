package com.miranda_gs.JobsTelescope.infrastructure.scraper;

import com.miranda_gs.JobsTelescope.domain.entity.Platform;
import com.miranda_gs.JobsTelescope.domain.port.JobScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.catho.CathoScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.glassdoor.GlassdoorScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.gupy.GupyScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.ifood.IfoodScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.indeed.IndeedScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.infojobs.InfojobsScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.mercadolivre.MercadoLivreScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.nerdin.NerdinScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.ninety_nine_jobs.NinetyNineJobsScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.picpay.PicpayScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.programathor.ProgramathorScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.trampos.TramposScraper;
import com.miranda_gs.JobsTelescope.infrastructure.scraper.brazil.vagas.VagasScraper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BrazilScraperFactory {

    private final Map<Platform, JobScraper> scrapers = new HashMap<>();

    public BrazilScraperFactory() {
        scrapers.put(Platform.GUPY, new GupyScraper());
        scrapers.put(Platform.VAGAS, new VagasScraper());
        scrapers.put(Platform.CATHO, new CathoScraper());
        scrapers.put(Platform.INFOJOBS, new InfojobsScraper());
        scrapers.put(Platform.NINETY_NINE_JOBS, new NinetyNineJobsScraper());
        scrapers.put(Platform.PROGRAMATHOR, new ProgramathorScraper());
        scrapers.put(Platform.TRAMPOS, new TramposScraper());
        scrapers.put(Platform.INDEED_BR, new IndeedScraper());
        scrapers.put(Platform.GLASSDOOR, new GlassdoorScraper());
        scrapers.put(Platform.NERDIN, new NerdinScraper());
        scrapers.put(Platform.PICPAY, new PicpayScraper());
        scrapers.put(Platform.IFOOD, new IfoodScraper());
        scrapers.put(Platform.MERCADO_LIVRE, new MercadoLivreScraper());
    }

    public Optional<JobScraper> getScraper(Platform platform) {
        return Optional.ofNullable(scrapers.get(platform));
    }

    public List<JobScraper> getAllScrapers() {
        return List.copyOf(scrapers.values());
    }
}