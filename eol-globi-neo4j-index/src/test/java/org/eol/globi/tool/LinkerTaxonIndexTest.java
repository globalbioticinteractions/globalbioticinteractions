package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.data.ResolvingTaxonIndex;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class LinkerTaxonIndexTest extends GraphDBTestCase {

    @Override
    protected PropertyEnricher getPropertyEnricher() {
        return new PropertyEnricher() {

            @Override
            public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
                List<Map<String, String>> maps = enrichAllMatches(properties);
                return maps.isEmpty() ? null : maps.get(0);
            }

            @Override
            public List<Map<String, String>> enrichAllMatches(Map<String, String> properties) throws PropertyEnricherException {
                if ("Homo sapiens".equals(TaxonUtil.mapToTaxon(properties).getName())) {
                    TaxonImpl taxon1 = new TaxonImpl("Homo sapiens also", "FOO:444");
                    taxon1.setPathIds("BARZ:111 | FOOZ:777");
                    TaxonImpl taxon2 = new TaxonImpl("Homo sapiens also2", "FOO:445");
                    taxon1.setPathIds("BARZ:111 | FOOZ:777");
                    return Arrays.asList(properties, TaxonUtil.taxonToMap(taxon1), TaxonUtil.taxonToMap(taxon2));
                } else {
                    return Collections.emptyList();
                }
            }

            @Override
            public void shutdown() {

            }
        };
    }

    @Test
    public void linking() throws StudyImporterException {
        Taxon taxonFound = new TaxonImpl("Homo sapiens", "Bar:123");
        taxonFound.setPath("Animalia | Mammalia | Homo sapiens");
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);

            taxonIndex.getOrCreateTaxon(taxonFound);

            Taxon anotherTaxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Bla blaus", null));
            anotherTaxon.setExternalId("FOO 1234");
            tx.commit();
        }

        new NameResolver(
                new GraphServiceFactoryProxy(getGraphDb()),
                getTaxonIndexFactory()
        ).index();


        resolveNames();

        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);

            Taxon node = taxonIndex.findTaxonByName("Homo sapiens");

            Node taxonNode = ((NodeBacked) node).getUnderlyingNode();
            assertTrue(taxonNode.hasProperty(PropertyAndValueDictionary.NAME_IDS));
            assertTrue(taxonNode.hasProperty(PropertyAndValueDictionary.EXTERNAL_IDS));

            assertThat(taxonNode.getProperty(PropertyAndValueDictionary.EXTERNAL_IDS).toString()
                    , is("| Animalia | BARZ:111 | Bar:123 | FOO:444 | FOO:445 | FOOZ:777 | Homo sapiens | Homo sapiens also | Homo sapiens also2 | Mammalia |"));
            assertThat(taxonNode.getProperty(PropertyAndValueDictionary.NAME_IDS).toString()
                    , is("| Bar:123 | FOO:444 | FOO:445 |"));
        }

        try (Transaction tx1 = getGraphDb().beginTx()) {
            Node next = tx1.findNode(NodeLabel.Taxon, PropertyAndValueDictionary.EXTERNAL_ID, "Bar:123");
            assertThat(new TaxonNode(next).getName(), is("Homo sapiens"));

//            assertSingleHit(PropertyAndValueDictionary.PATH + ":BAR\\:123");
//            assertSingleHit(PropertyAndValueDictionary.PATH + ":FOO\\:444");
//            assertSingleHit(PropertyAndValueDictionary.PATH + ":FOO\\:444 " + PropertyAndValueDictionary.PATH + ":BAR\\:123");
//            assertSingleHit(PropertyAndValueDictionary.PATH + ":BAR\\:*");
//            assertSingleHit(PropertyAndValueDictionary.PATH + ":Homo");
//            assertSingleHit(PropertyAndValueDictionary.PATH + ":\"Homo sapiens\"");
            tx1.commit();
        }


    }

    protected IndexerNeo4j createIndexer() {
        return new LinkerTaxonIndexNeo4j(
                new GraphServiceFactoryProxy(getGraphDb())
        );
    }

    @Ignore
    @Test
    public void linkingWithNameOnly() throws StudyImporterException {
        Taxon taxonFound = new TaxonImpl("urn:catalog:AMNH:Mammals:M-39582", null);
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);

            taxonIndex.getOrCreateTaxon(taxonFound);
            Taxon foundTaxon = taxonIndex.findTaxonByName("urn:catalog:AMNH:Mammals:M-39582");
            assertThat(foundTaxon, is(not(nullValue())));
            assertThat(foundTaxon.getName(), is("urn:catalog:AMNH:Mammals:M-39582"));
            tx.commit();
        }
        resolveNames();

        Node next = null;
//        try (IndexHits<Node> hits = getGraphDb().index().forNodes(LinkerTaxonIndexNeo4j3.INDEX_TAXON_NAMES_AND_IDS)
//                .query("path:\"urn:catalog:AMNH:Mammals:M-39582\"")) {
//            next = hits.next();
//            assertThat(hits.hasNext(), is(false));
//        }

        assertThat(new TaxonNode(next).getName(), is("urn:catalog:AMNH:Mammals:M-39582"));

    }

    @Ignore
    @Test
    public void linkingWithIdOnlyNoPath() throws StudyImporterException {
        Taxon taxonFound = new TaxonImpl(null, "some id");
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);
            taxonIndex.getOrCreateTaxon(taxonFound);
            tx.commit();
        }

        resolveNames();

        Node next = null;

//        try (IndexHits<Node> hits = getGraphDb()
//                .index()
//                .forNodes(LinkerTaxonIndexNeo4j2.INDEX_TAXON_NAMES_AND_IDS)
//                .query("path:\"some id\"")) {
//
//            assertThat(hits.hasNext(), is(true));
//            next = hits.next();
//            assertThat(hits.hasNext(), is(false));
//        }

        assertThat(new TaxonNode(next).getExternalId(), is("some id"));
    }

    @Ignore
    @Test
    public void linkingWithLiteratureReference() throws StudyImporterException {
        indexTaxaWithLiteratureLink();

        Node next = null;
//        try (IndexHits<Node> hits = getGraphDb()
//                .index()
//                .forNodes(LinkerTaxonIndexNeo4j2.INDEX_TAXON_NAMES_AND_IDS)
//                .query("path:\"doi:10.123/456\"")) {
//            assertThat(hits.hasNext(), is(true));
//            next = hits.next();
//            assertThat(hits.hasNext(), is(false));
//
//        }
        assertThat(new TaxonNode(next).getExternalId(), is("bar:123"));

    }

    private void indexTaxaWithLiteratureLink() throws StudyImporterException {
        Taxon taxonFound = new TaxonImpl("Homo sapiens", "bar:123");
        taxonFound.setPath("Animalia | Mammalia | Homo sapiens");
        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);

            Taxon taxon = taxonIndex.getOrCreateTaxon(taxonFound);
            TaxonImpl taxon1 = new TaxonImpl("doi:10.123/456", "doi:10.123/456");
            taxon1.setPath("doi:10.123/456");
            taxon1.setPathIds("doi:10.123/456");
            NodeUtil.connectTaxa(taxon1, (TaxonNode) taxon, getGraphDb(), RelTypes.SAME_AS);
            tx.commit();
        }

        resolveNames();
    }

    protected void assertSingleHit(String query) {
//        IndexHits<Node> hits;
        Node next = null;
//        hits = getGraphDb()
//                .index()
//                .forNodes(LinkerTaxonIndexNeo4j2.INDEX_TAXON_NAMES_AND_IDS)
//                .query(query);
//        next = hits.next();
        assertThat(new TaxonNode(next).getName(), is("Homo sapiens"));
//        assertThat(hits.hasNext(), is(false));
//        hits.close();
    }
}
