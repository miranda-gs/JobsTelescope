package com.miranda_gs.JobsTelescope.infrastructure.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CliRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CliRunner.class);

    @Override
    public void run(String... args) throws Exception {
        log.info("Jobs Telescope Core started. Waiting for commands on STDIN...");

        var processor = new CommandProcessor();

        while (processor.processNext()) {
        }

        log.info("Jobs Telescope Core shutting down.");
    }
}
