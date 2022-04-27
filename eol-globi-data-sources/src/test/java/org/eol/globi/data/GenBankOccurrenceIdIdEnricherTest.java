package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_BODY_PART_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

public class GenBankOccurrenceIdIdEnricherTest {

    @Test
    public void lookupSourceOccurrenceId() throws StudyImporterException {
        Map<String, String> properties
                = new GenBankOccurrenceIdIdEnricher(null, null, getResourceService())
                .enrich(new TreeMap<String, String>() {{
                    put("sourceOccurrenceId", "https://www.ncbi.nlm.nih.gov/nuccore/EU241689");
                    put("targetTaxonName", "Oligoryzomys longicaudatus");
                }});

        assertThat(properties.get(SOURCE_TAXON_NAME), is("Andes orthohantavirus"));
        assertThat(properties.get(SOURCE_TAXON_ID), is("NCBI:1980456"));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(InteractType.HAS_HOST.getIRI()));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is(InteractType.HAS_HOST.getLabel()));
        assertThat(properties.get(TARGET_TAXON_NAME), is("Oligoryzomys longicaudatus"));
        assertThat(properties.get(LOCALITY_NAME), is("Chile"));
    }

    public ResourceService getResourceService() {
        return new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return getClass().getResourceAsStream("genbank-EU241689.txt");
            }
        };
    }

    @Test
    public void lookupTargetOccurrenceId() throws StudyImporterException {
        Map<String, String> properties
                = new GenBankOccurrenceIdIdEnricher(null, null, getResourceService())
                .enrich(new TreeMap<String, String>() {{
                    put("targetOccurrenceId", "https://www.ncbi.nlm.nih.gov/nuccore/EU241689");
                }});

        assertThat(properties.get(TARGET_TAXON_NAME), is("Andes orthohantavirus"));
        assertThat(properties.get(INTERACTION_TYPE_ID), is(InteractType.HOST_OF.getIRI()));
        assertThat(properties.get(INTERACTION_TYPE_NAME), is(InteractType.HOST_OF.getLabel()));
        assertThat(properties.get(TARGET_TAXON_ID), is("NCBI:1980456"));
        assertThat(properties.get(SOURCE_TAXON_NAME), is("Oligoryzomys longicaudatus"));
        assertThat(properties.get("localityName"), is("Chile"));
    }


    @Test
    public void parse() {
        String resp = "LOCUS       EU241689                 924 bp    cRNA    linear   VRL 26-JUL-2016\n" +
                "DEFINITION  Andes virus isolate NK96865 nucleocapsid protein gene, partial cds.\n" +
                "ACCESSION   EU241689\n" +
                "VERSION     EU241689.1\n" +
                "KEYWORDS    .\n" +
                "SOURCE      Andes orthohantavirus\n" +
                "  ORGANISM  Andes orthohantavirus\n" +
                "            Viruses; Riboviria; Orthornavirae; Negarnaviricota;\n" +
                "            Polyploviricotina; Ellioviricetes; Bunyavirales; Hantaviridae;\n" +
                "            Mammantavirinae; Orthohantavirus.\n" +
                "REFERENCE   1  (bases 1 to 924)\n" +
                "  AUTHORS   Medina,R.A., Torres-Perez,F., Galeno,H., Navarrete,M., Vial,P.A.,\n" +
                "            Palma,R.E., Ferres,M., Cook,J.A. and Hjelle,B.\n" +
                "  TITLE     Ecology, genetic diversity, and phylogeographic structure of andes\n" +
                "            virus in humans and rodents in Chile\n" +
                "  JOURNAL   J. Virol. 83 (6), 2446-2459 (2009)\n" +
                "   PUBMED   19116256\n" +
                "REFERENCE   2  (bases 1 to 924)\n" +
                "  AUTHORS   Medina,R.A., Torres-Perez,F., Galeno,H., Navarrete,M., Vial,P.A.,\n" +
                "            Palma,R.E., Cook,J.A. and Hjelle,B.\n" +
                "  TITLE     Direct Submission\n" +
                "  JOURNAL   Submitted (23-OCT-2007) Pathology and Biology Departments,\n" +
                "            University of New Mexico, MSC08-4640, CRF 327, Albuquerque, NM\n" +
                "            87131, USA\n" +
                "FEATURES             Location/Qualifiers\n" +
                "     source          1..924\n" +
                "                     /organism=\"Andes orthohantavirus\"\n" +
                "                     /mol_type=\"viral cRNA\"\n" +
                "                     /isolate=\"NK96865\"\n" +
                "                     /host=\"Oligoryzomys longicaudatus\"\n" +
                "                     /db_xref=\"taxon:1980456\"\n" +
                "                     /segment=\"S\"\n" +
                "                     /country=\"Chile\"\n" +
                "     CDS             <1..>924\n" +
                "                     /codon_start=2\n" +
                "                     /product=\"nucleocapsid protein\"\n" +
                "                     /protein_id=\"ABY79078.1\"\n" +
                "                     /translation=\"TKLGELKRQLADLVAAQKLATKPVDPTGLEPDDHLKEKSSLRYG\n" +
                "                     NVLDVNSIDLEEPSGQTADWKAIGAYILGFAIPIILKALYMLSTRGRQTVKDNKGTRI\n" +
                "                     RFKDDSSFEEVNGIRKPKHLYVSMPTAQSTMKAEEITPGRFRTIACGLFPAQVKARNI\n" +
                "                     ISPVMGVIGFGFFVKDWMDRIEEFLAAECPFLPKPKVASESFMSTNKMYFLNRQRQVN\n" +
                "                     ESKVQDIVDLIDHAETESATLFTEIATPHSVWVFACAPDRCPPTALYVAGVPELGAFF\n" +
                "                     SILQDMRNTIMASKSVGTAEEKLKKKSAFYQS\"\n" +
                "ORIGIN      \n" +
                "        1 gaccaaactc ggagaactca agaggcagct tgcggatttg gtggcagctc agaaactggc\n" +
                "       61 tacaaaacca gttgatccaa cagggcttga gcctgatgac catttaaagg agaaatcatc\n" +
                "      121 tttaagatat gggaatgttc tggatgtcaa ctcaattgat ctagaagaac caagtgggca\n" +
                "      181 gactgctgac tggaaggcta taggagcata tattctagga tttgcaatcc cgattatctt\n" +
                "      241 aaaagcttta tacatgctgt caactcgtgg gagacaaact gtgaaggaca acaaagggac\n" +
                "      301 caggataaga ttcaaagatg attcctcctt tgaagaggtc aatgggatac gcaaaccaaa\n" +
                "      361 acatctttac gtttcaatgc caactgcaca atctacaatg aaagctgaag aaatcacacc\n" +
                "      421 ggggcggttt aggacaattg cttgtggcct ttttccagca caggtcaaag ctaggaacat\n" +
                "      481 aataagccct gtaatgggtg taattgggtt tggtttcttt gtgaaagact ggatggaccg\n" +
                "      541 gatagaagaa ttcttggctg cagagtgccc atttttgcct aaaccaaagg tggcttctga\n" +
                "      601 atcctttatg tccactaaca aaatgtattt cctaaacagg caaaggcagg tcaacgagtc\n" +
                "      661 caaggttcaa gatatcgttg atttaataga tcatgctgaa actgagtctg ctaccttatt\n" +
                "      721 cacggagatt gcaacacccc attcagtctg ggtgtttgca tgtgcacctg atcgatgccc\n" +
                "      781 tccaactgca ttatatgttg caggtgttcc ggagttgggt gcatttttct ctattcttca\n" +
                "      841 ggatatgcgg aataccatta tggcatccaa atctgtaggg actgcagaag aaaagttgaa\n" +
                "      901 gaaaaaatct gcattttatc aatc\n" +
                "//\n" +
                "\n";

        InputStream is = IOUtils.toInputStream(resp, StandardCharsets.UTF_8);

        String taxonNameField = SOURCE_TAXON_NAME;
        String taxonIdField = SOURCE_TAXON_ID;
        String hostTaxonNameField = TARGET_TAXON_NAME;
        String hostBodyPartField = TARGET_BODY_PART_NAME;
        String localeField = LocationConstant.LOCALITY;

        Map<String, String> properties = new TreeMap<>();

        try {
            GenBankOccurrenceIdIdEnricher.enrichWithGenBankRecord(is,
                    taxonNameField,
                    taxonIdField,
                    hostTaxonNameField,
                    hostBodyPartField,
                    localeField,
                    InteractType.HAS_HOST,
                    properties);

        } catch (IOException e) {


        }
        assertThat(properties.get(taxonNameField), is("Andes orthohantavirus"));
        assertThat(properties.get(hostTaxonNameField), is("Oligoryzomys longicaudatus"));
        assertThat(properties.get(hostBodyPartField), is(nullValue()));
        assertThat(properties.get(taxonIdField), is("NCBI:1980456"));
        assertThat(properties.get(localeField), is("Chile"));
        assertThat(properties.get("interactionTypeName"), is("hasHost"));
        assertThat(properties.get("interactionTypeId"), is("http://purl.obolibrary.org/obo/RO_0002454"));

    }
    @Test
    public void parse2() {
        String resp = "LOCUS       KM201411                1627 bp    cRNA    linear   VRL 31-JAN-2016\n" +
                "DEFINITION  Amga virus strain MSB148558 nucleocapsid gene, complete cds.\n" +
                "ACCESSION   KM201411\n" +
                "VERSION     KM201411.1\n" +
                "KEYWORDS    .\n" +
                "SOURCE      Amga virus\n" +
                "  ORGANISM  Amga virus\n" +
                "            Viruses; Riboviria; Orthornavirae; Negarnaviricota;\n" +
                "            Polyploviricotina; Ellioviricetes; Bunyavirales; Hantaviridae;\n" +
                "            Mammantavirinae; Orthohantavirus.\n" +
                "REFERENCE   1  (bases 1 to 1627)\n" +
                "  AUTHORS   Kang,H.J., Gu,S.H., Cook,J.A. and Yanagihara,R.\n" +
                "  TITLE     Amga virus, a newly identified hantavirus in the Laxmann's shrew\n" +
                "            (Sorex caecutiens)\n" +
                "  JOURNAL   Unpublished\n" +
                "REFERENCE   2  (bases 1 to 1627)\n" +
                "  AUTHORS   Kang,H.J., Gu,S.H., Cook,J.A. and Yanagihara,R.\n" +
                "  TITLE     Direct Submission\n" +
                "  JOURNAL   Submitted (15-JUL-2014) Department of Tropical Medicine and Medical\n" +
                "            Microbiology and Pediatrics, John A. Burns School of Medicine,\n" +
                "            University of Hawaii at Manoa, 651 Ilalo Street, Honolulu, HI\n" +
                "            96813, USA\n" +
                "FEATURES             Location/Qualifiers\n" +
                "     source          1..1627\n" +
                "                     /organism=\"Amga virus\"\n" +
                "                     /mol_type=\"viral cRNA\"\n" +
                "                     /strain=\"MSB148558\"\n" +
                "                     /isolation_source=\"lung\"\n" +
                "                     /host=\"Sorex caecutiens\"\n" +
                "                     /db_xref=\"taxon:1511732\"\n" +
                "                     /country=\"Russia\"\n" +
                "                     /collection_date=\"10-Aug-2006\"\n" +
                "     CDS             32..1321\n" +
                "                     /codon_start=1\n" +
                "                     /product=\"nucleocapsid\"\n" +
                "                     /protein_id=\"AKD00015.1\"\n" +
                "                     /translation=\"MDDIKQLEAELKSVTDQLEVAQKKLSKATSDFQADGDDTNKQTY\n" +
                "                     ERRTLEVSHLQAKVTQLKKALADAAATGKQSMAAAEDPTGKESDDYLSQRSMLRYGNT\n" +
                "                     IDVNAIDLDEPSGQTADWLTIITYVVSFVDTILLKGLYMLTTRGRQTVKDNKGTRIRL\n" +
                "                     KDDTSYDETATGRKPRHLYISMPNAQSSMRADEITPGRYRTVVCGLYPAQIRNRQMIS\n" +
                "                     PVMGVVGFPVIAKNWPDRIEKFLEDDCPFLKQTLQITLSKPDKNKDFLNDRQSVLTSM\n" +
                "                     ETEEAKKIMEVVTGASQTVPDSLNSPYAIWVFAGAPDRCPPTSLYVAGMAELGAFFSV\n" +
                "                     LQDMRNTIIASKTVGTAEEKLKKKSSFYQSYLRRTQSMGVQLDQRIIILYMTAWGKEA\n" +
                "                     VDHFHLGDDMDPELRATAQNLIDQKVKEISNMEPMKL\"\n" +
                "ORIGIN      \n" +
                "        1 tagtagtaga ctcctaaaca aggagcaaaa aatggatgat atcaagcaat tagaagcaga\n" +
                "       61 gctgaaaagt gtcacagatc agcttgaggt ggcacagaaa aagttaagta aggccacatc\n" +
                "      121 tgactttcag gctgatgggg atgacaccaa taaacaaact tatgagagga ggacattaga\n" +
                "      181 ggtgagccat ttacaggcaa aggtgactca gctcaaaaag gcattggctg atgcagctgc\n" +
                "      241 cactggtaag caatcaatgg cagctgcaga agatcccaca ggaaaagaat ctgatgatta\n" +
                "      301 tttgtcccaa cggtctatgt tacgctatgg caataccatc gatgtgaatg caattgatct\n" +
                "      361 tgacgagcct agtggacaga cagccgattg gttgactata ataacttatg ttgtgtcatt\n" +
                "      421 tgtggatacc atcttattga agggccttta catgctgaca acaaggggaa gacagactgt\n" +
                "      481 taaggataat aaagggacac gtattcggtt gaaggatgac acttcctatg atgagactgc\n" +
                "      541 aaccggccgt aagccaaggc atttgtatat ctctatgcct aatgcacagt ccagcatgcg\n" +
                "      601 agcagatgag ataactcctg gccgataccg gactgtggtg tgtggattat accctgctca\n" +
                "      661 aataaggaat agacaaatga tcagtcctgt gatgggagtt gtaggttttc ctgtgattgc\n" +
                "      721 taagaactgg cctgatagga tagagaagtt tttggaggat gactgcccat tccttaaaca\n" +
                "      781 gacacttcag attacattga gcaaaccaga taagaataaa gattttctca atgacagaca\n" +
                "      841 aagtgtttta acatccatgg agactgagga ggcaaagaag ataatggaag ttgtgactgg\n" +
                "      901 tgcatcccag actgtaccag atagtctaaa ttcaccatat gcaatctggg tttttgcagg\n" +
                "      961 tgccccggat cgttgccctc ctacaagttt atatgtggca gggatggctg aactcggtgc\n" +
                "     1021 atttttctct gttctgcagg atatgagaaa cactatcatt gcatctaaga cagttggaac\n" +
                "     1081 agcagaagaa aaacttaaaa agaagtcctc gttttaccag tcgtatttac gtcgaaccca\n" +
                "     1141 atctatgggt gttcaactag atcagagaat catcatcctc tacatgactg cctggggtaa\n" +
                "     1201 agaggctgtg gatcacttcc atcttggtga tgatatggat cctgagttac gggctacggc\n" +
                "     1261 tcagaatctg attgaccaga aagtcaagga gatttctaac atggagccca tgaaactgta\n" +
                "     1321 gataggtatt gggatgggag gagggggggg cactgctgct ttgcatacac ggggggggct\n" +
                "     1381 gggtactgta tgctgtgaac taatgatagg tgctaatgat aagtttacaa tcaatcaatc\n" +
                "     1441 aataagctat aatggtaagg ttctcatttc tacgtcgtga taatccacga cttaattccc\n" +
                "     1501 tttgaaatgt gatgatttta attttcttat ctattccaat caaccacacc aacatacact\n" +
                "     1561 ccactacctc aaacactcta cctcaacata tgcttcctga atttgctttt caaggagtat\n" +
                "     1621 actacta\n";

        InputStream is = IOUtils.toInputStream(resp, StandardCharsets.UTF_8);

        String taxonNameField = SOURCE_TAXON_NAME;
        String taxonIdField = SOURCE_TAXON_ID;
        String hostTaxonNameField = TARGET_TAXON_NAME;
        String hostBodyPartField = TARGET_BODY_PART_NAME;
        String localeField = LocationConstant.LOCALITY;

        Map<String, String> properties = new TreeMap<>();

        try {
            GenBankOccurrenceIdIdEnricher.enrichWithGenBankRecord(is,
                    taxonNameField,
                    taxonIdField,
                    hostTaxonNameField,
                    hostBodyPartField,
                    localeField,
                    InteractType.HAS_HOST,
                    properties);

        } catch (IOException e) {


        }
        assertThat(properties.get(taxonNameField), is("Amga virus"));
        assertThat(properties.get(hostTaxonNameField), is("Sorex caecutiens"));
        assertThat(properties.get(hostBodyPartField), is("lung"));
        assertThat(properties.get(taxonIdField), is("NCBI:1511732"));
        assertThat(properties.get(localeField), is("Russia"));
        assertThat(properties.get("interactionTypeName"), is("hasHost"));
        assertThat(properties.get("interactionTypeId"), is("http://purl.obolibrary.org/obo/RO_0002454"));

    }


}