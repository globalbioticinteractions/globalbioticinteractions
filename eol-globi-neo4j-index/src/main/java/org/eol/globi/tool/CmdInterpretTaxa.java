package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.ResourceService;
import org.eol.globi.taxon.ResolvingTaxonIndexNoTx;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.InputStreamFactoryNoop;
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
                new InputStreamFactoryNoop(),
                CmdInterpretTaxa.class,
                System.getProperty("user.dir")
        );

        final TaxonCacheService taxonCacheService = new TaxonCacheService(
                getTaxonCachePath(),
                getTaxonMapPath(),
                resourceService,
                new File(getCacheDir())
        );

        try {
            new IndexerTaxa(
                    taxonCacheService,
                    getGraphServiceFactory(),
                    new ResolvingTaxonIndexNoTx(taxonCacheService, getGraphServiceFactory().getGraphService()),
                    new NodeIdCollectorNeo4j3()
            ).index();
        } catch (StudyImporterException e) {
            throw new RuntimeException(e);
        }
    }

}
