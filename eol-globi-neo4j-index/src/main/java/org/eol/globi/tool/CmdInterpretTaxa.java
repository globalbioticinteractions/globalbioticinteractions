package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.taxon.TaxonCacheService;

public class CmdInterpretTaxa implements Cmd {

    private final GraphServiceFactory graphServiceFactory;
    private String taxonCachePath;
    private String taxonMapPath;

    public CmdInterpretTaxa(GraphServiceFactory graphServiceFactory) {
        this(graphServiceFactory,
                "/taxa/taxonCache.tsv.gz",
                "/taxa/taxonMap.tsv.gz"
        );
    }

    public CmdInterpretTaxa(GraphServiceFactory graphServiceFactory,
                            String taxonCachePath,
                            String taxonMapPath) {
        this.graphServiceFactory = graphServiceFactory;
        this.taxonCachePath = taxonCachePath;
        this.taxonMapPath = taxonMapPath;
    }

    @Override
    public void run() throws StudyImporterException {
        final TaxonCacheService taxonCacheService = new TaxonCacheService(
                taxonCachePath,
                taxonMapPath);
        IndexerNeo4j taxonIndexer = new IndexerTaxa(taxonCacheService, graphServiceFactory);
        taxonIndexer.index();
    }
}
