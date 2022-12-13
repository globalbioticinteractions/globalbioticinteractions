package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.ResourceServiceLocal;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(
        name = "interpret",
        aliases = {"link", "linkNames", "link-names"},
        description = "Interprets taxonomic names using provided translation tables (taxonCache/Map)."
)
public class CmdInterpretTaxa extends CmdNeo4J {

    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_TAXON_CACHE_PATH},
            description = "location of taxonCache.tsv.gz"
    )
    private String taxonCachePath = "./taxonCache.tsv.gz";

    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_TAXON_MAP_PATH},
            description = "location of taxonCache.tsv.gz"
    )
    private String taxonMapPath = "./taxonMap.tsv.gz";

    @CommandLine.Option(
            names = {CmdOptionConstants.OPTION_NAME_INDEX_DIR},
            description = "location of cached taxon index"
    )
    private String cacheDir = "./taxonMap.tsv.gz";



    @Override
    public void run() {
        final TaxonCacheService taxonCacheService = new TaxonCacheService(
                taxonCachePath,
                taxonMapPath,
                new ResourceServiceLocal()
        );
        taxonCacheService.setCacheDir(new File(cacheDir));
        IndexerNeo4j taxonIndexer = new IndexerTaxa(taxonCacheService, getGraphServiceFactory());
        try {
            taxonIndexer.index();
        } catch (StudyImporterException e) {
            throw new RuntimeException(e);
        }
    }
}
