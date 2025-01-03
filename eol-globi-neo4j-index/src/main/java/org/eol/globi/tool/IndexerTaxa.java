package org.eol.globi.tool;

import org.eol.globi.data.ResolvingTaxonIndex;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.Taxon;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.NodeIdCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class IndexerTaxa implements IndexerNeo4j {
    private static final Logger LOG = LoggerFactory.getLogger(IndexerTaxa.class);

    private final TaxonCacheService taxonCacheService;
    private final GraphServiceFactory factory;
    private final ResolvingTaxonIndex index;
    private final NodeIdCollector nodeIdCollector;

    public IndexerTaxa(TaxonCacheService taxonCacheService,
                       GraphServiceFactory factory,
                       ResolvingTaxonIndex index,
                       NodeIdCollector nodeIdCollector) {
        this.taxonCacheService = taxonCacheService;
        this.factory = factory;
        this.index = index;
        this.nodeIdCollector = nodeIdCollector;
    }


    @Override
    public void index() throws StudyImporterException {
        LOG.info("resolving names with taxon cache ...");
        try {
            index.setIndexResolvedTaxaOnly(true);

            TaxonFilter taxonCacheFilter = new TaxonFilter() {

                private KnownBadNameFilter knownBadNameFilter = new KnownBadNameFilter();

                @Override
                public boolean shouldInclude(Taxon taxon) {
                    return taxon != null
                            && knownBadNameFilter.shouldInclude(taxon);
                }
            };

            new NameResolver(factory, index, nodeIdCollector, taxonCacheFilter)
                    .index();

            LOG.info("adding same and similar terms for resolved taxa...");
            List<IndexerNeo4j> linkers = new ArrayList<>();

            for (IndexerNeo4j linker : linkers) {
                new IndexerTimed(linker)
                        .index();
            }
            LOG.info("adding same and similar terms for resolved taxa done.");

        } finally {
            taxonCacheService.shutdown();
        }
        LOG.info("resolving names with taxon cache done.");
    }

}
