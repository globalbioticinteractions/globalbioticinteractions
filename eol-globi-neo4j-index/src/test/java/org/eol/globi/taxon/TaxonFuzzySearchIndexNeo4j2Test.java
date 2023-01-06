package org.eol.globi.taxon;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.tool.LinkerTaxonIndexNeo4j2;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TaxonFuzzySearchIndexNeo4j2Test extends GraphDBNeo4jTestCase {

    @Test
    public void fuzzyMatch() throws NodeFactoryException {
        Taxon taxonFound = new TaxonImpl("Homo sapiens", "Bar:123");
        taxonFound.setPath("Animalia | Mammalia | Homo sapiens");
        Taxon taxon = taxonIndex.getOrCreateTaxon(taxonFound);
        TaxonImpl taxon1 = new TaxonImpl("Homo sapiens also", "FOO:444");
        taxon1.setPathIds("BARZ:111 | FOOZ:777");
        TaxonImpl taxon2 = new TaxonImpl("Homo sapiens also2", "FOO:444");
        taxon1.setPathIds("BARZ:111 | FOOZ:777");
        NodeUtil.connectTaxa(taxon1, (TaxonNode) taxon, getGraphDb(), RelTypes.SAME_AS);
        NodeUtil.connectTaxa(taxon2, (TaxonNode) taxon, getGraphDb(), RelTypes.SAME_AS);

        resolveNames();

        createIndexer().index();

        TaxonFuzzySearchIndex index = getFuzzySearch();

        assertThat(index.query("name:sapienz~").stream().count(), is(1L));
        assertThat(index.query("name:sapienz").stream().count(), is(0L));

    }

    private LinkerTaxonIndexNeo4j2 createIndexer() {
        return new LinkerTaxonIndexNeo4j2(new GraphServiceFactoryProxy(getGraphDb()), getNodeIdCollector());
    }

    @Test
    public void findByName() throws StudyImporterException {
        assertQueryHits("name:name", 1L);

    }

    @Test
    public void findByFuzzyANDClause() throws StudyImporterException {
        assertQueryHits("name:hmo~ AND name:SApiens~", 1L);
    }

    @Test
    public void findByClauseWithEscapedWhitespace() throws StudyImporterException {
        assertQueryHits("name:s\\ nme~", 1L);
    }

    @Test
    public void findByFuzzyCapitalization() throws StudyImporterException {
        assertQueryHits("name:geRman~", 1L);
    }

    @Test
    public void findByFuzzyAndClause() throws StudyImporterException {
        assertQueryHits("name:geRman~ AND name:som~", 1L);
    }

    @Test
    public void findByFuzzyAndClause2() throws StudyImporterException {
        assertQueryHits("name:hmo~ AND name:sapiens~", 1L);
    }

    @Test
    public void findByFuzzyAndClause3() throws StudyImporterException {
        // queries are case sensitive . . . should all be lower cased.
        assertQueryHits("name:HMO~ AND name:saPIENS~", 0L);
    }

    private void assertQueryHits(String query, long expectedNumberOfHits) throws NodeFactoryException {
        NonResolvingTaxonIndexNeo4j2 taxonService = new NonResolvingTaxonIndexNeo4j2(getGraphDb());
        taxonService.getOrCreateTaxon(setTaxonProps(new TaxonImpl("Homo sapiens")));
        resolveNames();
        resolveNames();
        createIndexer().index();


        TaxonFuzzySearchIndex fuzzySearch = getFuzzySearch();


        ResourceIterator<Node> hits = fuzzySearch.query(query);
        assertThat(hits.stream().count(), is(expectedNumberOfHits));
    }

    private TaxonFuzzySearchIndex getFuzzySearch() {
        return new TaxonFuzzySearchIndexNeo4j2(getGraphDb());
    }

    public static Taxon setTaxonProps(Taxon taxon) {
        taxon.setPath("kingdom" + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "Homo sapiens" + CharsetConstant.SEPARATOR);
        taxon.setExternalId("anExternalId");
        taxon.setCommonNames(ResolvingTaxonIndexNoTxNeo4j2Test.EXPECTED_COMMON_NAMES);
        taxon.setName("this is the actual name");
        return taxon;
    }


}