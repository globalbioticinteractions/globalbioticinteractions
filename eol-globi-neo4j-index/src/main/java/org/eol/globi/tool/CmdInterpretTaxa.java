package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.ResourceServiceLocal;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

@CommandLine.Command(
        name = "interpret",
        aliases = {"linkNames", "link-names"},
        description = "Interprets taxonomic names using provided translation tables (taxonCache/Map)."
)
public class CmdInterpretTaxa extends CmdNeo4J {

    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_NAME_INDEX_DIR},
            description = "location of cached taxon index"
    )
    private String cacheDir = "./taxonIndexCache";

    @Override
    public void run() {

        final TaxonCacheService taxonCacheService = new TaxonCacheService(
                getTaxonCachePath(),
                getTaxonMapPath(),
                new ResourceServiceLocal(is -> is, CmdInterpretTaxa.class)
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
