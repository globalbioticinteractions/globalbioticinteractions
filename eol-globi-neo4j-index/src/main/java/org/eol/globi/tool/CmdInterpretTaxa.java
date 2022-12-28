package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.ResourceService;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.ResourceServiceLocal;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(
        name = "interpret",
        aliases = {"linkNames", "link-names"},
        description = "Interprets taxonomic names using provided translation tables (taxonCache/Map)."
)
public class CmdInterpretTaxa extends CmdNeo4J {

    @CommandLine.Option(
            names = {"-nameIndexCache"},
            defaultValue = "./taxonIndexCache",
            description = "location of cached taxon index"
    )
    private String cacheDir;

    @Override
    public void run() {
        ResourceService resourceService = new ResourceServiceLocal(
                is -> is,
                CmdInterpretTaxa.class,
                System.getProperty("user.dir")
        );

        final TaxonCacheService taxonCacheService = new TaxonCacheService(
                getTaxonCachePath(),
                getTaxonMapPath(),
                resourceService
        );
        taxonCacheService.setCacheDir(new File(cacheDir));
        IndexerNeo4j taxonIndexer = new IndexerTaxa(
                taxonCacheService,
                getGraphServiceFactory()
        );
        try {
            taxonIndexer.index();
        } catch (StudyImporterException e) {
            throw new RuntimeException(e);
        }
    }

}
