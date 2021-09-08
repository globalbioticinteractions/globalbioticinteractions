package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.taxon.TaxonCacheService;

public class CmdInterpretTaxa implements Cmd {

    private final GraphServiceFactory graphServiceFactory;

    public CmdInterpretTaxa(GraphServiceFactory graphServiceFactory) {
        this.graphServiceFactory = graphServiceFactory;
    }

    @Override
    public void run() throws StudyImporterException {
        final TaxonCacheService taxonCacheService = new TaxonCacheService(
                "/taxa/taxonCache.tsv.gz",
                "/taxa/taxonMap.tsv.gz");
        IndexerNeo4j taxonIndexer = new IndexerTaxa(taxonCacheService, graphServiceFactory);
        taxonIndexer.index();
    }
}
