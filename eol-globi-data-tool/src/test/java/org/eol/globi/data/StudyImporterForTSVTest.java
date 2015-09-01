package org.eol.globi.data;

import org.eol.globi.domain.TaxonNode;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForTSVTest extends GraphDBTestCase {

    private static final String firstFewLines = "sourceTaxonId\tsourceTaxonName\tinteractionTypeId\tinteractionTypeName\ttargetTaxonId\ttargetTaxonName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\tobservationDateTime\treferenceDoi\treferenceCitation\n" +
            "\tLeptoconchus incycloseris\tRO:0002444\tparasite of\t\tFungia (Cycloseris) costulata\t\t\t\t\t\tdoi:10.1007/s13127-011-0039-1\tGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\n" +
            "\tLeptoconchus infungites\tRO:0002444\tparasite of\t\tFungia (Fungia) fungites\t\t\t\t\t\tdoi:10.1007/s13127-011-0039-1\tGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\n" +
            "\tLeptoconchus ingrandifungi\tRO:0002444\tparasite of\t\tSandalolitha dentata\t\t\t\t\t\tdoi:10.1007/s13127-011-0039-1\tGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\n" +
            "\tLeptoconchus ingranulosa\tRO:0002444\tparasite of\t\tFungia (Wellsofungia) granulosa\t\t\t\t\t\tdoi:10.1007/s13127-011-0039-1\tGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\n";

    @Test
    public void importFewLines() throws StudyImporterException, NodeFactoryException {
        StudyImporterForTSV importer = new StudyImporterForTSV(new TestParserFactory(firstFewLines), nodeFactory);
        importer.importStudy();

        assertExists("Leptoconchus incycloseris");
        assertExists("Sandalolitha dentata");
    }

    protected void assertExists(String taxonName) throws NodeFactoryException {
        TaxonNode taxon = nodeFactory.findTaxonByName(taxonName);
        assertThat(taxon, is(notNullValue()));
        assertThat(taxon.getName(), is(taxonName));
    }

}