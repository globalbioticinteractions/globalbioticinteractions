package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.NodeUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

public class LinkerTermMatcherTest extends GraphDBTestCase {

    @Ignore
    @Test
    public void holorchisCastexMissedLink() throws NodeFactoryException {
        // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/448
        String classifiedId = "EOL_V2:11987314";
        assertTaxonMapping(classifiedId);
    }

    @Test
    public void holorchisCastexNonMissedLink() throws NodeFactoryException {
        // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/448
        assertTaxonMapping("EOL:11987314");
    }

    private void assertTaxonMapping(String classifiedId) throws NodeFactoryException {
        Taxon taxon2 = new TaxonImpl("Holorchis castex", classifiedId);

        Taxon createdTaxon = taxonIndex.getOrCreateTaxon(taxon2);
        Node specimenDummy = getGraphDb().createNode();
        Node originalTaxonDummy = getGraphDb().createNode();
        originalTaxonDummy.setProperty("name", "holorchis castex");
        originalTaxonDummy.setProperty("externalId", "EOL:11987314");
        specimenDummy.createRelationshipTo(
                originalTaxonDummy,
                NodeUtil.asNeo4j(RelTypes.ORIGINALLY_DESCRIBED_AS));

        specimenDummy.createRelationshipTo(
                ((TaxonNode) createdTaxon).getUnderlyingNode(),
                NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));

        TaxonCacheService taxonCacheService = new TaxonCacheService(
                "/org/eol/globi/taxon/taxonCacheHolorchis.tsv",
                "/org/eol/globi/taxon/taxonMapHolorchis.tsv");

        new LinkerTermMatcher(taxonCacheService)
                .index(new GraphServiceFactoryProxy(getGraphDb()));

        Collection<String> externalIds = LinkerTestUtil.sameAsCountForNode(RelTypes.SAME_AS, (TaxonNode) createdTaxon);
        assertThat(externalIds, hasItem("EOL_V2:11987314"));
        assertThat(externalIds, hasItem("GBIF:5890922"));
    }


}
