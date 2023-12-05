package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.ResourceService;
import org.eol.globi.taxon.ResolvingTaxonIndexNoTxNeo4j2;
import org.eol.globi.taxon.ResolvingTaxonIndexNoTxNeo4j3;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.NodeIdCollectorNeo4j2;
import org.eol.globi.util.NodeIdCollectorNeo4j3;
import org.eol.globi.util.ResourceServiceLocal;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(
        name = "interpret",
        aliases = {"linkNames", "link-names"},
        description = "Interprets taxonomic names using provided translation tables (taxonCache/Map)."
)
public class CmdInterpretTaxa extends CmdNeo4J {


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
                resourceService,
                new File(getCacheDir())
        );

        IndexerNeo4j taxonIndexer = null;
        if ("2".equals(getNeo4jVersion())) {
            taxonIndexer = new IndexerTaxa(
                    taxonCacheService,
                    getGraphServiceFactory(),
                    new ResolvingTaxonIndexNoTxNeo4j2(taxonCacheService, getGraphServiceFactory().getGraphService()),
                    new NodeIdCollectorNeo4j2()
            );
        } else {
            taxonIndexer = new IndexerTaxa(
                    taxonCacheService,
                    getGraphServiceFactory(),
                    new ResolvingTaxonIndexNoTxNeo4j3(taxonCacheService, getGraphServiceFactory().getGraphService()),
                    new NodeIdCollectorNeo4j3()
            );
        }
        try {
            taxonIndexer.index();
        } catch (StudyImporterException e) {
            throw new RuntimeException(e);
        }
    }

}
