package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_URL;
import static org.eol.globi.data.StudyImporterForTSV.STUDY_SOURCE_CITATION;
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
    public void importFewLines() throws StudyImporterException {
        StudyImporterForTSV importer = new StudyImporterForTSV(new TestParserFactory(firstFewLines), nodeFactory);
        importer.setDataset(new DatasetImpl("someRepo", URI.create("http://example.com")));
        importStudy(importer);

        assertExists("Leptoconchus incycloseris");
        assertExists("Sandalolitha dentata");

        assertStudyTitles("someRepodoi:10.1007/s13127-011-0039-1");
    }

    public void assertStudyTitles(String element) {
        final List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        final List<String> titles = new ArrayList<String>();
        for (Study study : allStudies) {
            titles.add(study.getTitle());
        }
        assertThat(titles, hasItem(element));
    }

    @Test
    public void importMinimal() throws StudyImporterException {
        String minimalLines = "sourceTaxonId\tsourceTaxonName\tinteractionTypeId\tinteractionTypeName\ttargetTaxonId\ttargetTaxonName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\tobservationDateTime\treferenceDoi\treferenceCitation\n" +
                "EOL:123\t\tRO:0002444\t\tEOL:111\t\t\t\t\t\t\t\tGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\n" +
                "EOL:456\t\tRO:0002444\t\tEOL:222\t\t\t\t\t\t\t\tGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\n" +
                "EOL:678\t\tRO:0002444\t\tEOL:333\t\t\t\t\t\t\t\tGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\n" +
                "EOL:912\t\tRO:0002444\t\tEOL:444\t\t\t\t\t\t\t\tGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\n";

        StudyImporterForTSV importer = new StudyImporterForTSV(new TestParserFactory(minimalLines), nodeFactory);
        importer.setDataset(new DatasetImpl("someRepo", URI.create("http://example.com")));
        importStudy(importer);
        Taxon taxon = taxonIndex.findTaxonById("EOL:123");
        assertThat(taxon, is(notNullValue()));
        assertThat(taxon.getName(), is("no name"));
        assertThat(taxon.getExternalId(), is("EOL:123"));
        assertStudyTitles("someRepoGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1");
    }

    protected void assertExists(String taxonName) throws NodeFactoryException {
        Taxon taxon = taxonIndex.findTaxonByName(taxonName);
        assertThat(taxon, is(notNullValue()));
        assertThat(taxon.getName(), is(taxonName));
    }


    @Test
    public void wardeh() throws StudyImporterException {
        String firstFewLines = "sourceTaxonId\tsourceTaxonName\tinteractionTypeId\tinteractionTypeName\ttargetTaxonId\ttargetTaxonName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\tobservationDateTime\treferenceUrl\tsourceDoi\tsourceCitation\n" +
                "EOL:2912748\tbacillus subtilis\tRO:0002454\thasHost\tEOL:594885\tmorus alba\t\t\t\t\t\thttp://www.ncbi.nlm.nih.gov/nuccore/100172732\tdoi: 10.1038/sdata.2015.49\tWardeh, M. et al. Database of host-pathogen and related species interactions, and their global distribution. Sci. Data 2:150049 doi: 10.1038/sdata.2015.49 (2015)\n" +
                "EOL:741039\tbovine adenovirus c\tRO:0002454\thasHost\tEOL:328699\tbos taurus\t\t\t\t\t\thttp://www.ncbi.nlm.nih.gov/nuccore/1002418\tdoi: 10.1038/sdata.2015.49\tWardeh, M. et al. Database of host-pathogen and related species interactions, and their global distribution. Sci. Data 2:150049 doi: 10.1038/sdata.2015.49 (2015)\n" +
                "EOL:12141292\tichthyophonus hoferi\tRO:0002454\thasHost\tEOL:205418\tlimanda ferruginea\t\t\t\t\t\thttp://www.ncbi.nlm.nih.gov/nuccore/1002422\tdoi: 10.1038/sdata.2015.49\tWardeh, M. et al. Database of host-pathogen and related species interactions, and their global distribution. Sci. Data 2:150049 doi: 10.1038/sdata.2015.49 (2015)\n";

        StudyImporterForTSV importer = new StudyImporterForTSV(new TestParserFactory(firstFewLines), nodeFactory);
        importer.setDataset(new DatasetImpl("someRepo", URI.create("http://example.com")));
        importStudy(importer);
        Taxon taxon = taxonIndex.findTaxonById("EOL:2912748");
        assertThat(taxon, is(notNullValue()));
        assertThat(taxon.getName(), is("bacillus subtilis"));
        assertThat(taxon.getExternalId(), is("EOL:2912748"));
        final List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        final List<String> titles = new ArrayList<String>();
        final List<String> ids = new ArrayList<String>();
        for (Study study : allStudies) {
            titles.add(study.getTitle());
            ids.add(study.getExternalId());
        }
        assertThat(titles, hasItem("someRepohttp://www.ncbi.nlm.nih.gov/nuccore/100172732"));
        assertThat(ids, hasItem("http://www.ncbi.nlm.nih.gov/nuccore/100172732"));
    }

    @Test
    public void generateReferenceId() throws StudyImporterException {
        String id = StudyImporterForTSV.generateReferenceId(new HashMap<String, String>() {{
            put(REFERENCE_URL, "http://bla");
        }});

        assertThat(id, is("http://bla"));
    }

    @Test
    public void generateReferenceIdFromCitation() throws StudyImporterException {
        String id = StudyImporterForTSV.generateReferenceId(new HashMap<String, String>() {{
            put(STUDY_SOURCE_CITATION, "http://source");
            put(REFERENCE_CITATION, "http://bla");
        }});

        assertThat(id, is("http://bla"));
    }

    @Test
    public void generateReferenceCitation() throws StudyImporterException {
        String id = StudyImporterForTSV.generateReferenceCitation(new HashMap<String, String>() {{
            put(REFERENCE_URL, "http://bla");
        }});

        assertThat(id, is("http://bla"));
    }


}