package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.NodeListener;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eol.globi.data.DatasetImporterForTSV.ASSOCIATED_TAXA;
import static org.eol.globi.data.DatasetImporterForTSV.HABITAT_ID;
import static org.eol.globi.data.DatasetImporterForTSV.HABITAT_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_URL;
import static org.eol.globi.data.DatasetImporterForTSV.DATASET_CITATION;
import static org.eol.globi.domain.PropertyAndValueDictionary.NETWORK_ID;
import static org.eol.globi.domain.PropertyAndValueDictionary.NETWORK_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class DatasetImporterForTSVTest extends GraphDBNeo4jTestCase {

    private static final String firstFewLinesTSV = "sourceTaxonId\tsourceTaxonName\tinteractionTypeId\tinteractionTypeName\ttargetTaxonId\ttargetTaxonName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\tobservationDateTime\treferenceDoi\treferenceCitation\n" +
            "\tLeptoconchus incycloseris\tRO:0002444\tparasite of\t\tFungia (Cycloseris) costulata\t\t\t\t\t\tdoi:10.1007/s13127-011-0039-1\tGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\n" +
            "\tLeptoconchus infungites\tRO:0002444\tparasite of\t\tFungia (Fungia) fungites\t\t\t\t\t\tdoi:10.1007/s13127-011-0039-1\tGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\n" +
            "\tLeptoconchus ingrandifungi\tRO:0002444\tparasite of\t\tSandalolitha dentata\t\t\t\t\t\tdoi:10.1007/s13127-011-0039-1\tGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\n" +
            "\tLeptoconchus ingranulosa\tRO:0002444\tparasite of\t\tFungia (Wellsofungia) granulosa\t\t\t\t\t\tdoi:10.1007/s13127-011-0039-1\tGittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\n";

    private static final String firstFewLinesCSV = "sourceTaxonId,sourceTaxonName,interactionTypeId,interactionTypeName,targetTaxonId,targetTaxonName,localityId,localityName,decimalLatitude,decimalLongitude,observationDateTime,referenceDoi,referenceCitation\n" +
            ",TESTLeptoconchus incycloseris,RO:0002444,parasite of,,Fungia (Cycloseris) costulata,,,,,,doi:10.1007/s13127-011-0039-1,\"Gittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\"\n" +
            ",Leptoconchus infungites,RO:0002444,parasite of,,Fungia (Fungia) fungites,,,,,,doi:10.1007/s13127-011-0039-1,\"Gittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\"\n" +
            ",Leptoconchus ingrandifungi,RO:0002444,parasite of,,Sandalolitha dentata,,,,,,doi:10.1007/s13127-011-0039-1,\"Gittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\"\n" +
            ",Leptoconchus ingranulosa,RO:0002444,parasite of,,Fungia (Wellsofungia) granulosa,,,,,,doi:10.1007/s13127-011-0039-1,\"Gittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1\"\n";

    @Test
    public void importFewLinesTSV() throws StudyImporterException {
        Dataset dataset = getDataset(new TreeMap<URI, String>() {{
            put(URI.create("/interactions.tsv"), firstFewLinesTSV);
        }});

        DatasetImporterForTSV importer = new DatasetImporterForTSV(null, nodeFactory);
        importer.setDataset(dataset);
        importStudy(importer);

        assertExists("Leptoconchus incycloseris");
        assertExists("Sandalolitha dentata");

        assertStudyTitles("someRepodoi:10.1007/s13127-011-0039-1");
    }

    @Test
    public void importFewLinesCSV() throws StudyImporterException {

        Dataset dataset = getDataset(new TreeMap<URI, String>() {{
            put(URI.create("/interactions.csv"), firstFewLinesCSV);
        }});


        DatasetImporterForTSV importer = new DatasetImporterForTSV(null, nodeFactory);
        importer.setDataset(dataset);
        importStudy(importer);

        assertExists("TESTLeptoconchus incycloseris");
        assertExists("Sandalolitha dentata");

        assertStudyTitles("someRepodoi:10.1007/s13127-011-0039-1");
    }

    @Test
    public void importFewLinesCSVAndTSV() throws StudyImporterException {
        DatasetImpl dataset = getDataset(new TreeMap<URI, String>() {{
            put(URI.create("/interactions.tsv"), firstFewLinesTSV);
            put(URI.create("/interactions.csv"), firstFewLinesCSV);
        }});

        DatasetImporterForTSV importer = new DatasetImporterForTSV(null, nodeFactory);
        importer.setDataset(dataset);
        importStudy(importer);

        assertExists("Leptoconchus incycloseris");
        assertExists("TESTLeptoconchus incycloseris");
        assertExists("Sandalolitha dentata");

        assertStudyTitles("someRepodoi:10.1007/s13127-011-0039-1");
    }

    public DatasetImpl getDataset(TreeMap<URI, String> treeMap) {
        return new DatasetWithResourceMapping("someRepo", URI.create("http://example.com"), new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop())) {
                @Override
                public InputStream retrieve(URI resource) throws IOException {
                    String input = treeMap.get(resource);
                    return input == null ? null : IOUtils.toInputStream(input, StandardCharsets.UTF_8);
                }
            };
    }

    @Test
    public void importFewLines() throws StudyImporterException {
        Dataset dataset = getDataset(new TreeMap<URI, String>() {{
            put(URI.create("/interactions.tsv"), firstFewLinesTSV);
        }});


        DatasetImporterForTSV importer = new DatasetImporterForTSV(null, nodeFactory);
        importer.setDataset(dataset);
        importStudy(importer);

        assertExists("Leptoconchus incycloseris");
        assertExists("Sandalolitha dentata");

        assertStudyTitles("someRepodoi:10.1007/s13127-011-0039-1");
    }

    public void assertStudyTitles(String element) {
        final List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
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

        DatasetImpl dataset = getDataset(new TreeMap<URI, String>() {{
            put(URI.create("/interactions.tsv"), minimalLines);
        }});

        DatasetImporterForTSV importer = new DatasetImporterForTSV(null, nodeFactory);
        importer.setDataset(dataset);
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

        DatasetImporterForTSV importer = new DatasetImporterForTSV(null, nodeFactory);

        DatasetImpl dataset = getDataset(new TreeMap<URI, String>() {{
            put(URI.create("/interactions.tsv"), firstFewLines);
        }});

        importer.setDataset(dataset);

        importStudy(importer);
        Taxon taxon = taxonIndex.findTaxonById("EOL:2912748");
        assertThat(taxon, is(notNullValue()));
        assertThat(taxon.getName(), is("bacillus subtilis"));
        assertThat(taxon.getExternalId(), is("EOL:2912748"));
        final List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
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
    public void withGenus() throws StudyImporterException {
        String firstFewLines = "sourceTaxonFamily\tsourceTaxonGenus\tinteractionTypeId\ttargetTaxonName\n" +
                "Bacillaceae\tBacillus\tRO:0002454\tMorus alba\n";

        Dataset dataset = getDataset(new TreeMap<URI, String>() {{
            put(URI.create("/interactions.tsv"), firstFewLines);
        }});


        DatasetImporterForTSV importer = new DatasetImporterForTSV(null, nodeFactory);
        importer.setDataset(dataset);
        importStudy(importer);
        Taxon taxon = taxonIndex.findTaxonByName("Bacillus");
        assertThat(taxon, is(notNullValue()));
        assertThat(taxon.getName(), is("Bacillus"));
        assertThat(taxon.getPath(), is("Bacillaceae | Bacillus"));
        assertThat(taxon.getPathNames(), is("family | genus"));
        final List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
    }

    @Test
    public void seltmannRefutes() throws StudyImporterException {
        String firstFewLines = "InteractionID\tBasisOfRecord\tsourceTaxonId\tsourceTaxonName\tinteractionTypeId\targumentTypeId\tinteractionTypeName\ttargetBodyPartName\ttargetBodyPartId\texperimentalConditionName\texperimentalConditionId\tsexName\tsexID\ttargetTaxonId\ttargetTaxonName\ttargetCommonName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\tobservationDateTime\treferenceDoi\treferenceCitation\n" +
                "75\tLiteratureRecord\thttps://www.gbif.org/species/110462368\tCalyptra ophideroides\thttp://purl.obolibrary.org/obo/RO_0002470\thttps://en.wiktionary.org/wiki/refute\teats\tblood\thttp://purl.obolibrary.org/obo/NCIT_C12434\tunder experimental conditions\thttp://purl.obolibrary.org/obo/ENVO_01001405\t\t\thttps://www.gbif.org/species/2436436\tHomo sapiens\thuman\t\t\t\t\t\t\tBänziger, H. 1989. Skin-piercing blood-sucking moths V: Attacks on man by 5 Calyptra spp.(Lepidoptera: Noctuidae) in S. and S.E. Asia. Mittellungen der Schweizerischen Entomologischen Gesellschaft, 62: 215-233.\n";

        assertArgumentType(firstFewLines, RelTypes.REFUTES);
    }

    @Test
    public void seltmannSupportsExplicit() throws StudyImporterException {
        String firstFewLines = "InteractionID\tBasisOfRecord\tsourceTaxonId\tsourceTaxonName\tinteractionTypeId\targumentTypeId\tinteractionTypeName\ttargetBodyPartName\ttargetBodyPartId\texperimentalConditionName\texperimentalConditionId\tsexName\tsexID\ttargetTaxonId\ttargetTaxonName\ttargetCommonName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\tobservationDateTime\treferenceDoi\treferenceCitation\n" +
                "75\tLiteratureRecord\thttps://www.gbif.org/species/110462368\tCalyptra ophideroides\thttp://purl.obolibrary.org/obo/RO_0002470\thttps://en.wiktionary.org/wiki/support\teats\tblood\thttp://purl.obolibrary.org/obo/NCIT_C12434\tunder experimental conditions\thttp://purl.obolibrary.org/obo/ENVO_01001405\t\t\thttps://www.gbif.org/species/2436436\tHomo sapiens\thuman\t\t\t\t\t\t\tBänziger, H. 1989. Skin-piercing blood-sucking moths V: Attacks on man by 5 Calyptra spp.(Lepidoptera: Noctuidae) in S. and S.E. Asia. Mittellungen der Schweizerischen Entomologischen Gesellschaft, 62: 215-233.\n";

        assertArgumentType(firstFewLines, RelTypes.SUPPORTS);
    }

    @Test
    public void seltmannSupportsImplicit() throws StudyImporterException {
        String firstFewLines = "InteractionID\tBasisOfRecord\tsourceTaxonId\tsourceTaxonName\tinteractionTypeId\targumentTypeId\tinteractionTypeName\ttargetBodyPartName\ttargetBodyPartId\texperimentalConditionName\texperimentalConditionId\tsexName\tsexID\ttargetTaxonId\ttargetTaxonName\ttargetCommonName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\tobservationDateTime\treferenceDoi\treferenceCitation\n" +
                "75\tLiteratureRecord\thttps://www.gbif.org/species/110462368\tCalyptra ophideroides\thttp://purl.obolibrary.org/obo/RO_0002470\t\teats\tblood\thttp://purl.obolibrary.org/obo/NCIT_C12434\tunder experimental conditions\thttp://purl.obolibrary.org/obo/ENVO_01001405\t\t\thttps://www.gbif.org/species/2436436\tHomo sapiens\thuman\t\t\t\t\t\t\tBänziger, H. 1989. Skin-piercing blood-sucking moths V: Attacks on man by 5 Calyptra spp.(Lepidoptera: Noctuidae) in S. and S.E. Asia. Mittellungen der Schweizerischen Entomologischen Gesellschaft, 62: 215-233.\n";

        assertArgumentType(firstFewLines, RelTypes.SUPPORTS);
    }

    @Test
    public void seltmannUnsupportedArgumentTypeId() throws StudyImporterException {
        String firstFewLines = "InteractionID\tBasisOfRecord\tsourceTaxonId\tsourceTaxonName\tinteractionTypeId\targumentTypeId\tinteractionTypeName\ttargetBodyPartName\ttargetBodyPartId\texperimentalConditionName\texperimentalConditionId\tsexName\tsexID\ttargetTaxonId\ttargetTaxonName\ttargetCommonName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\tobservationDateTime\treferenceDoi\treferenceCitation\n" +
                "75\tLiteratureRecord\thttps://www.gbif.org/species/110462368\tCalyptra ophideroides\thttp://purl.obolibrary.org/obo/RO_0002470\thttp://example.org/unsupportedArgumentType\teats\tblood\thttp://purl.obolibrary.org/obo/NCIT_C12434\tunder experimental conditions\thttp://purl.obolibrary.org/obo/ENVO_01001405\t\t\thttps://www.gbif.org/species/2436436\tHomo sapiens\thuman\t\t\t\t\t\t\tBänziger, H. 1989. Skin-piercing blood-sucking moths V: Attacks on man by 5 Calyptra spp.(Lepidoptera: Noctuidae) in S. and S.E. Asia. Mittellungen der Schweizerischen Entomologischen Gesellschaft, 62: 215-233.\n";

        assertArgumentType(firstFewLines, RelTypes.SUPPORTS);
    }

    private void assertArgumentType(String firstFewLines, final RelTypes argumentType) throws StudyImporterException {
        DatasetImpl dataset = getDataset(new TreeMap<URI, String>() {{
            put(URI.create("/interactions.tsv"), firstFewLines);
        }});

        DatasetImporterForTSV importer = new DatasetImporterForTSV(null, nodeFactory);
        importer.setDataset(dataset);
        importStudy(importer);
        final AtomicBoolean foundStudy = new AtomicBoolean(false);
        NodeUtil.findStudies(getGraphDb(), new NodeListener() {

            @Override
            public void on(Node node) {
                assertTrue(node.hasRelationship(Direction.OUTGOING, NodeUtil.asNeo4j(argumentType)));
                foundStudy.set(true);
            }
        });
        assertThat(foundStudy.get(), is(true));
    }

    @Test
    public void seltmannNegated() throws StudyImporterException {
        String firstFewLines = "InteractionID\tBasisOfRecord\tsourceTaxonId\tsourceTaxonName\tinteractionTypeId\tisNegated\tinteractionTypeName\ttargetBodyPartName\ttargetBodyPartId\texperimentalConditionName\texperimentalConditionId\tsexName\tsexID\ttargetTaxonId\ttargetTaxonName\ttargetCommonName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\tobservationDateTime\treferenceDoi\treferenceCitation\n" +
                "75\tLiteratureRecord\thttps://www.gbif.org/species/110462368\tCalyptra ophideroides\thttp://purl.obolibrary.org/obo/RO_0002470\tTRUE\teats\tblood\thttp://purl.obolibrary.org/obo/NCIT_C12434\tunder experimental conditions\thttp://purl.obolibrary.org/obo/ENVO_01001405\t\t\thttps://www.gbif.org/species/2436436\tHomo sapiens\thuman\t\t\t\t\t\t\tBänziger, H. 1989. Skin-piercing blood-sucking moths V: Attacks on man by 5 Calyptra spp.(Lepidoptera: Noctuidae) in S. and S.E. Asia. Mittellungen der Schweizerischen Entomologischen Gesellschaft, 62: 215-233.\n";

        assertArgumentType(firstFewLines, RelTypes.REFUTES);
    }

    @Test
    public void seltmannNotNegated() throws StudyImporterException {
        String firstFewLines = "InteractionID\tBasisOfRecord\tsourceTaxonId\tsourceTaxonName\tinteractionTypeId\tisNegated\tinteractionTypeName\ttargetBodyPartName\ttargetBodyPartId\texperimentalConditionName\texperimentalConditionId\tsexName\tsexID\ttargetTaxonId\ttargetTaxonName\ttargetCommonName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\tobservationDateTime\treferenceDoi\treferenceCitation\n" +
                "75\tLiteratureRecord\thttps://www.gbif.org/species/110462368\tCalyptra ophideroides\thttp://purl.obolibrary.org/obo/RO_0002470\tFALSE\teats\tblood\thttp://purl.obolibrary.org/obo/NCIT_C12434\tunder experimental conditions\thttp://purl.obolibrary.org/obo/ENVO_01001405\t\t\thttps://www.gbif.org/species/2436436\tHomo sapiens\thuman\t\t\t\t\t\t\tBänziger, H. 1989. Skin-piercing blood-sucking moths V: Attacks on man by 5 Calyptra spp.(Lepidoptera: Noctuidae) in S. and S.E. Asia. Mittellungen der Schweizerischen Entomologischen Gesellschaft, 62: 215-233.\n";

        assertArgumentType(firstFewLines, RelTypes.SUPPORTS);
    }

    @Test
    public void generateReferenceId() throws StudyImporterException {
        String id = DatasetImporterForTSV.generateReferenceId(new TreeMap<String, String>() {{
            put(REFERENCE_URL, "http://bla");
        }});

        assertThat(id, is("http://bla"));
    }

    @Test
    public void generateReferenceIdFromCitation() throws StudyImporterException {
        String id = DatasetImporterForTSV.generateReferenceId(new TreeMap<String, String>() {{
            put(DATASET_CITATION, "http://source");
            put(REFERENCE_CITATION, "http://bla");
        }});

        assertThat(id, is("http://bla"));
    }

    @Test
    public void generateReferenceCitation() throws StudyImporterException {
        String id = DatasetImporterForTSV.generateReferenceCitation(new TreeMap<String, String>() {{
            put(REFERENCE_URL, "http://bla");
        }});

        assertThat(id, is("http://bla"));
    }

    @Test
    public void associatedTaxaNotSupported() throws StudyImporterException {
        AtomicInteger atomicInteger = new AtomicInteger(0);

        String firstFewLines = "sourceTaxonName\tassociatedTaxa\n" +
                "Homo sapiens\teats: Canis lupus | eats: Felis catus";

        Dataset dataset = getDataset(new TreeMap<URI, String>() {{
            put(URI.create("/interactions.tsv"), firstFewLines);
        }});


        DatasetImporterForTSV importer = new DatasetImporterForTSV(null, nodeFactory);
        importer.setDataset(dataset);
        importer.setInteractionListener(link -> {
            int i = atomicInteger.incrementAndGet();
            assertThat(link.get(INTERACTION_TYPE_ID), is(nullValue()));
            assertThat(link.get(INTERACTION_TYPE_NAME), is(nullValue()));
            assertThat(link.get(TARGET_TAXON_NAME), is(nullValue()));
            assertThat(link.get(ASSOCIATED_TAXA), is(notNullValue()));
        });
        importStudy(importer);
        assertThat(atomicInteger.get(), greaterThan(0));
    }


    @Test
    public void networkIdAndName() throws StudyImporterException {
        String firstFewLines = "networkName\tnetworkId\n" +
                "some name\tsome id";
        Map<String, String> expected = new TreeMap<String, String>() {{
            put(NETWORK_NAME, "some name");
            put(NETWORK_ID, "some id");
        }};

        assertTermValues(firstFewLines, expected);
    }

    @Test
    public void habitatIdAndName() throws StudyImporterException {
        String firstFewLines = "habitatName\thabitatId\n" +
                "some name\tsome id";
        Map<String, String> expected = new TreeMap<String, String>() {{
            put(HABITAT_NAME, "some name");
            put(HABITAT_ID, "some id");
        }};

        assertTermValues(firstFewLines, expected);
    }

    public void assertTermValues(String firstFewLines, Map<String, String> expected) throws StudyImporterException {
        AtomicInteger atomicInteger = new AtomicInteger(0);


        Dataset dataset = getDataset(new TreeMap<URI, String>() {{
            put(URI.create("/interactions.tsv"), firstFewLines);
        }});


        DatasetImporterForTSV importer = new DatasetImporterForTSV(null, nodeFactory);
        importer.setDataset(dataset);
        importer.setInteractionListener(link -> {
            int i = atomicInteger.incrementAndGet();
            expected.values().forEach(termName -> {
                assertThat(link.get(termName), is(expected.get(termName)));

            });
        });
        importStudy(importer);
        assertThat(atomicInteger.get(), greaterThan(0));
    }


    @Test
    public void eventDateRange() throws StudyImporterException {
        assertEventDate("sourceOccurrenceId\tsourceTaxonId\tsourceTaxonName\tsourceBodyPartId\tsourceBodyPartName\tsourceLifeStageId\tsourceLifeStageName\tinteractionTypeId\tinteractionTypeName\ttargetOccurrenceId\ttargetTaxonId\ttargetTaxonName\ttargetBodyPartId\ttargetBodyPartName\ttargetLifeStageId\ttargetLifeStageName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\tobservationDateTime\treferenceDoi\treferenceUrl\treferenceCitation\n");
    }

    @Test
    public void eventDateRange2() throws StudyImporterException {
        assertEventDate("sourceOccurrenceId\tsourceTaxonId\tsourceTaxonName\tsourceBodyPartId\tsourceBodyPartName\tsourceLifeStageId\tsourceLifeStageName\tinteractionTypeId\tinteractionTypeName\ttargetOccurrenceId\ttargetTaxonId\ttargetTaxonName\ttargetBodyPartId\ttargetBodyPartName\ttargetLifeStageId\ttargetLifeStageName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\thttp://rs.tdwg.org/dwc/terms/eventDate\treferenceDoi\treferenceUrl\treferenceCitation\n");
    }

    @Test
    public void eventDateRange3() throws StudyImporterException {
        assertEventDate("sourceOccurrenceId\tsourceTaxonId\tsourceTaxonName\tsourceBodyPartId\tsourceBodyPartName\tsourceLifeStageId\tsourceLifeStageName\tinteractionTypeId\tinteractionTypeName\ttargetOccurrenceId\ttargetTaxonId\ttargetTaxonName\ttargetBodyPartId\ttargetBodyPartName\ttargetLifeStageId\ttargetLifeStageName\tlocalityId\tlocalityName\tdecimalLatitude\tdecimalLongitude\teventDate\treferenceDoi\treferenceUrl\treferenceCitation\n");
    }

    private void assertEventDate(String header) throws StudyImporterException {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        String firstFewLines = header +
                "\tITIS:632267\tRousettus aegyptiacus\t\t\t\t\thttp://purl.obolibrary.org/obo/RO_0002470\teats\t\tITIS:28882\tCitrus sp.\thttp://purl.obolibrary.org/obo/PO_0009001\tfruit\t\t\tGEONAMES:298795\tHatay, Adana, Mersin, and Antalya, Turkey\t\t\t1999-09/2003-09\t\thttp://journals.tubitak.gov.tr/zoology/issues/zoo-08-32-1/zoo-32-1-2-0604-8.pdf\tAlbayrak, I., Aşan, N., & Yorulmaz, T. (2008). The natural history of the Egyptian fruit bat, Rousettus aegyptiacus, in Turkey (Mammalia: Chiroptera). Turkish Journal of Zoology, 32(1), 11-18.";

        Dataset dataset = getDataset(new TreeMap<URI, String>() {{
            put(URI.create("/interactions.tsv"), firstFewLines);
        }});


        DatasetImporterForTSV importer = new DatasetImporterForTSV(null, nodeFactory);
        importer.setDataset(dataset);
        importer.setInteractionListener(link -> {
            atomicInteger.incrementAndGet();
            assertThat(link.get(DatasetImporterForMetaTable.EVENT_DATE), is(not(nullValue())));
            assertThat(link.get(SOURCE_TAXON_NAME), is("Rousettus aegyptiacus"));
            assertThat(link.get(TARGET_TAXON_NAME), is("Citrus sp."));
        });
        importStudy(importer);
        assertThat(atomicInteger.get(), is(1));
    }

}