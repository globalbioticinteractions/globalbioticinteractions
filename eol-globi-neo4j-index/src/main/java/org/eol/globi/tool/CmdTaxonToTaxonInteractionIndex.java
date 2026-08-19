package org.eol.globi.tool;

import org.eol.globi.data.GraphDatabaseServiceProxy;
import org.eol.globi.data.ResolvingTaxonIndex;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.ResourceService;
import org.eol.globi.taxon.ResolvingTaxonIndexImpl;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.neo4j.graphdb.Transaction;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

@CommandLine.Command(
        name = "index-interactions",
        description = "create taxon level interaction shortcuts"
)
public class CmdTaxonToTaxonInteractionIndex extends CmdNeo4J {


    @Override
    public void run() {
        new TaxonInteractionIndexer(
                getGraphServiceFactory()
        ).index();
    }

}
