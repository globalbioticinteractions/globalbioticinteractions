package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.taxon.NonResolvingTaxonIndex;

import java.util.Arrays;
import java.util.List;

public class CmdIndexTaxa implements Cmd {

    private final GraphServiceFactory graphServiceFactory;

    public CmdIndexTaxa(GraphServiceFactory graphServiceFactory) {
        this.graphServiceFactory = graphServiceFactory;
    }

    @Override
    public void run() throws StudyImporterException {
        final NonResolvingTaxonIndex taxonIndex = new NonResolvingTaxonIndex(graphServiceFactory.getGraphService());
        final IndexerNeo4j nameResolver = new NameResolver(graphServiceFactory, taxonIndex);
        final IndexerNeo4j taxonInteractionIndexer = new TaxonInteractionIndexer(graphServiceFactory);

        List<IndexerNeo4j> indexers = Arrays.asList(nameResolver, taxonInteractionIndexer);
        for (IndexerNeo4j indexer : indexers) {
            indexer.index();
        }

    }
}
