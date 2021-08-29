package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.Taxon;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.taxon.ResolvingTaxonIndexNoTx;
import org.eol.globi.taxon.TaxonCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class IndexerTaxa implements IndexerNeo4j {
    private static final Logger LOG = LoggerFactory.getLogger(IndexerTaxa.class);

    private final TaxonCacheService taxonCacheService;

    public IndexerTaxa(TaxonCacheService taxonCacheService) {
        this.taxonCacheService = taxonCacheService;
    }

    public static void indexTaxa(GraphServiceFactory graphService, TaxonCacheService taxonCacheService) {
        LOG.info("resolving names with taxon cache ...");
        try {
            ResolvingTaxonIndexNoTx index = new ResolvingTaxonIndexNoTx(taxonCacheService, graphService.getGraphService());
            index.setIndexResolvedTaxaOnly(true);

            TaxonFilter taxonCacheFilter = new TaxonFilter() {

                private KnownBadNameFilter knownBadNameFilter = new KnownBadNameFilter();

                @Override
                public boolean shouldInclude(Taxon taxon) {
                    return taxon != null
                            && knownBadNameFilter.shouldInclude(taxon);
                }
            };

            new NameResolver(index, taxonCacheFilter).index(graphService);

            LOG.info("adding same and similar terms for resolved taxa...");
            List<IndexerNeo4j> linkers = new ArrayList<>();
            //appendOpenTreeTaxonLinker(linkers);

            linkers.forEach(x -> new IndexerTimed(x)
                    .index(graphService));
            LOG.info("adding same and similar terms for resolved taxa done.");

        } finally {
            taxonCacheService.shutdown();
        }
        LOG.info("resolving names with taxon cache done.");
    }

    private static void appendOpenTreeTaxonLinker(List<IndexerNeo4j> linkers) {
        String ottUrl = System.getProperty("ott.url");
        try {
            if (StringUtils.isNotBlank(ottUrl)) {
                linkers.add(new LinkerOpenTreeOfLife(new OpenTreeTaxonIndex(new URI(ottUrl).toURL())));
            }
        } catch (MalformedURLException | URISyntaxException e) {
            LOG.warn("failed to link against OpenTreeOfLife", e);
        }
    }

    @Override
    public void index(GraphServiceFactory graphService) {
        indexTaxa(graphService, taxonCacheService);
    }

}
