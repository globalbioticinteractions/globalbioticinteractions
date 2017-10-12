package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.InteractType;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForSaproxylicTest {

    @Test
    public void parseAssociations() throws IOException, StudyImporterException {
        String someAssociationRows = "Oid\tOccurrenceA\tOccurrenceB\tRoleA\tRoleB\tCountA\tCountB\tCountUnitB\tCountUnitA\tScoreBPrefA\tScoreBPrefAStage\tReference\tOptimisticLockField\tGCRecord\n" +
                "{505B49F3-1798-451C-99B0-00011BE9614D}\t{8F0B633B-C7B0-43C3-B210-8E6488B42405}\t{E3A0CB88-26EF-46DF-9BED-0AF88231C050}\t{8613FD4F-784D-4818-9E26-92B648B1C733}\t{93D3102F-5CC8-48CF-88E7-FEE74F8E54AD}\t0\t0\t\t\tx\t\t{DF56D217-13B1-42B1-81A6-64553F91F7B6}\t0\t\n" +
                "{EAB80A48-DE82-454E-8A90-000157CE4E29}\t{D255447F-A514-4901-9C68-1B027AF67CEE}\t{575D9556-BE91-4E85-8BCC-85A159F5FCD3}\t{EC69560A-C33B-4D45-97D2-8D6AE8365B3C}\t{93D3102F-5CC8-48CF-88E7-FEE74F8E54AD}\t0\t0\t\t\tx\t\t{F90595F4-6872-4B42-B1B2-6F5D392A44F0}\t0\t";

        final TreeSet<Triple<String, String, String>> triples = new TreeSet<>();
        StudyImporterForSaproxylic.parseAssociations(triples::add, IOUtils.toInputStream(someAssociationRows));

        assertThat(triples.size(), is(10));
        assertThat(triples, hasItem(StudyImporterForSaproxylic.asTriple("{505B49F3-1798-451C-99B0-00011BE9614D}", "mentioned_by", "{DF56D217-13B1-42B1-81A6-64553F91F7B6}")));
    }

    @Test
    public void parseOccurrences() throws IOException, StudyImporterException {
        String someOccurrenceRows = "Oid\tProject\tLicense\tLocality\tColl_Ins\tCollection\tColl_refnumber\tDigitizedBy\tDigitizedYear\tStartOn\tEndOn\tUsedName\tTaxon\tExpertTaxon\tStage\tReference\tComment\tSourceComment\tOptimisticLockField\tGCRecord\n" +
                "{ACF1CDC2-BABD-4B63-AA57-0002A1C918E2}\t\t\t{02759CAD-38BF-4353-8B37-00919DE7F65F}\t\t\t\tAgnieszka Napierala\t2007\t\t\t\t{BE612DC1-8E9E-4D77-936A-543E865DF829}\t{BE612DC1-8E9E-4D77-936A-543E865DF829}\twoody material\t{E4014B48-1D7A-4EC9-BE0B-C43DC82F717F}\t\t\t0\t\n" +
                "{2C80197C-4783-4C02-AF8F-0006B450F240}\t\t\t{02759CAD-38BF-4353-8B37-00919DE7F65F}\t\t\t\tAgnieszka Napierala\t2007\t\t\t\t{6E58CEC1-5EEA-43F5-AE6C-0B696776DD18}\t{6E58CEC1-5EEA-43F5-AE6C-0B696776DD18}\tadult\t{E4014B48-1D7A-4EC9-BE0B-C43DC82F717F}\t\tDecay well decayed, beech, well decayed, humid, lying log (6m/50-60 m).\t0\t\n" +
                "{DF656380-877A-4EB4-82BB-0006D111F833}\t\t\t{02759CAD-38BF-4353-8B37-00919DE7F65F}\t\t\t\tAgnieszka Napierala\t2007\t\t\t\t{E75ADA70-586E-4C4E-A626-0663C81DC35D}\t{E75ADA70-586E-4C4E-A626-0663C81DC35D}\twoody material\t{E4014B48-1D7A-4EC9-BE0B-C43DC82F717F}\t\t\t0\t\n" +
                "{1C0C24A1-4860-424E-BDBE-0006FFC47BF1}\t\t\t{02759CAD-38BF-4353-8B37-00919DE7F65F}\t\t\t\t\t\t\t\t\t{51121BF1-F06A-4BE4-8657-E39779A06AB1}\t{51121BF1-F06A-4BE4-8657-E39779A06AB1}\tadult\t{368E9A85-68DB-4557-8874-369445AEFECB}\t\tRecorded in the galleries of Trypodendron signatum (I).\t0\t\n" +
                "{2FDA6035-024C-4811-B318-0007FD35066C}\t\t\t{2976B8B6-D04C-47C0-8406-BF73C6FE1DF7}\t\t\t\tJogeir N. Stokland\t2007-2008\t\t\t\t{E6234FAD-896A-4D8E-89DB-0CACC67CD493}\t{E6234FAD-896A-4D8E-89DB-0CACC67CD493}\timago\t{DF56D217-13B1-42B1-81A6-64553F91F7B6}\t\tIn leaves at base of old oaks (Quercus)\t0\t\n" +
                "{14FAAEEC-1FDA-49CA-AF92-00088736D9C3}\t\t\t{84D0E1DE-452A-4BC0-9E65-BE0AF38A2956}\t\t\t\tIneta  Salmane\t\t\t\t\t{B60BDE2D-5DA2-4FEB-8E15-888AE2EC7F63}\t{B60BDE2D-5DA2-4FEB-8E15-888AE2EC7F63}\t\t{C991F8F7-29C0-4178-AF6B-DD37CEB425F2}\t\tRecorded on Coriolus hirsutus on fallen birches in the dark taiga.\t0\t\n" +
                "{0A992435-37A5-4487-9E9B-000EE13AA5A0}\t\t\t{02759CAD-38BF-4353-8B37-00919DE7F65F}\t\t\t\tIneta  Salmane\t\t\t\t\t{32CA58DF-20BA-48D4-B8B6-D7685712C6ED}\t{32CA58DF-20BA-48D4-B8B6-D7685712C6ED}\twoody material\t{3E1845B1-2441-4986-8B31-D6C50167A271}\t\t\t0\t\n";

        final Set<Triple<String, String, String>> triples = new TreeSet<>();

        StudyImporterForSaproxylic.parseOccurrences(triples::add, IOUtils.toInputStream(someOccurrenceRows));

        assertThat(triples, hasItem(StudyImporterForSaproxylic.asTriple("{ACF1CDC2-BABD-4B63-AA57-0002A1C918E2}", "classifiedAs", "{BE612DC1-8E9E-4D77-936A-543E865DF829}")));
    }

    @Test
    public void parseTaxa() throws IOException, StudyImporterException {
        String someTaxaRows = "Oid\tName\tRank\tParent\tKingdom\tPhylum\tClass\tOrder\tSuborder\tFamily\tGenus\tSpecies\tFullName\tAuthor\tYear\tSaproxylicStatus\tNorway\tSweden\tFinland\tDenmark\tEstonia\tLatvia\tLithuania\tPoland\tEngland\tScotland\tFrance\tRealSpecies\tSpeciesName\tSynonyms\tComment\tOtherDistribution\tOptimisticLockField\tGCRecord\tObjectType\n" +
                "{DCCD2CFE-D002-40AB-B93B-000DD0B62999}\tDrosophilidae\t{4665ED8F-3734-406C-8987-E01B25E7D917}\t{A2ECAD2A-6B0A-4BED-B475-6E27D1BA2275}\t{775D6D50-1B93-42CF-9411-9573DDA4CD33}\t{ACDD42F9-63A4-4736-A1A3-66A4A515AC23}\t{A36FF57C-6577-4E1F-89D2-6788F9273E9F}\t{4442B3BF-4756-4258-8AF1-7F5B951346EF}\t{A2ECAD2A-6B0A-4BED-B475-6E27D1BA2275}\t{DCCD2CFE-D002-40AB-B93B-000DD0B62999}\t\t\tDrosophilidae\t\t0\t0\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t0\t\t13\n" +
                "{2F6610B4-7BC7-486F-B877-00484DC6AD2F}\tcinnabarinus\t{F4EB5A5C-34D0-4849-AC4F-FB2081067DF2}\t{1A72333E-92BB-40CC-A1DB-48302794436B}\t{BD93BDBC-8F91-44E2-BE59-9772B3EDB4B4}\t{3F1C39CD-FBBA-42B0-BC7C-028234A2AF4F}\t{F3BD253F-A0C7-440C-A5E0-51ECAA478978}\t{D8C4799E-A6B8-43B6-A0AC-7A01408D77FD}\t{91B32813-913B-4E34-9B2A-1A46F36EE91D}\t{F2E073EF-F4EB-4019-A29A-4B8A776E58EF}\t{1A72333E-92BB-40CC-A1DB-48302794436B}\t{2F6610B4-7BC7-486F-B877-00484DC6AD2F}\tPycnoporus cinnabarinus\t\t0\t2\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t\tPycnoporus cinnabarinus\t\t\t\t0\t\t15\n";

        final Set<Triple<String, String, String>> triples = new TreeSet<>();
        StudyImporterForSaproxylic.parseTaxa(triples::add, IOUtils.toInputStream(someTaxaRows));
        assertThat(triples, hasItem(StudyImporterForSaproxylic.asTriple("{DCCD2CFE-D002-40AB-B93B-000DD0B62999}", "hasName", "Drosophilidae")));
        assertThat(triples, hasItem(StudyImporterForSaproxylic.asTriple("{DCCD2CFE-D002-40AB-B93B-000DD0B62999}", "hasRank", "{4665ED8F-3734-406C-8987-E01B25E7D917}")));
    }

    @Test
    public void parseLocality() throws IOException, StudyImporterException {
        String someLocalities = "Oid\tAdm1\tAdm2\tAdm3\tX\tY\tAltitude\tName\tPlot\tLabel\tCoordinateSystem\tYear\tMonth\tDay\tStatus\tArea\tPerimeter\tOptimisticLockField\tGCRecord\n" +
                "{02759CAD-38BF-4353-8B37-00919DE7F65F}\t\t\t\t0\t0\t0\tPoland\t\t\t\t0\t0\t0\t\t0\t0\t0\t\n" +
                "{730FBE10-7E46-4D9D-B6BC-09D387453063}\t\t\t\t0\t0\t0\tEastern Europe\t\t\t\t0\t0\t0\t\t0\t0\t0\t\n" +
                "{A1F1BA2D-7E7D-40F2-B221-0E6A5060597A}\t\t\t\t0\t0\t0\tAustria\t\t\t\t0\t0\t0\t\t0\t0\t0\t\n" +
                "{8BDC6E8B-24AA-46C0-B579-11E5515F3B53}\t\t\t\t0\t0\t0\tColorado\t\t\t\t0\t0\t0\t\t0\t0\t0";

        final Set<Triple<String, String, String>> triples = new TreeSet<>();

        StudyImporterForSaproxylic.parseLocalities(triples::add, IOUtils.toInputStream(someLocalities));

        assertThat(triples, hasItem(StudyImporterForSaproxylic.asTriple("{02759CAD-38BF-4353-8B37-00919DE7F65F}", "hasName", "Poland")));
    }

    @Test
    public void parseReferences() throws IOException, StudyImporterException {
        String someLocalities = "Oid\tAuthors\tDescription\tComment\tYear\tUserId\tTitle\tType\tTitle2\tTitle3\tDate2\tJourfull\tJourab\tPubcity\tPublisher\tAvailability\tPubaddress\tSN\tPrintst\tVolume\tIssue\tPage1\tPage2\tUrl\tKeywords\tOptimisticLockField\tGCRecord\n" +
                "{9D773EF5-E522-4527-A404-00297A952B91}\tJ. Vanhara\t\t\t1982\t293\tThe moravian species of flat footed flies (Diptera, Opetiidae and Platypezidae)\t0\t\t\t\tFolia prirodoved. fak. UJEP Brne.\t\t\t\t\t\t\t\t23\t\t137-142\t\t\t\t0\t\n" +
                "{3B74121B-C819-4EDA-897B-0066CA6E52E1}\tP. Ma_an\t\t\t1998\t4221\tAmeroseius fungicolis sp. n. and A. callosus sp. n., two new ameroseiid species (Acarina, Mesostigmata) associated with wood-destroying fungi\t0\t\t\t\tBiologia Bratislava\t\t\t\t\t\t\t\t53\t5\t645-649\t\t\t\t0\t\n" +
                "{36A39A57-191F-4867-B0CF-00F1E245CCDC}\tJ. J.  Kieffer\t\t\t1894\t304\tUeber die Heteropezinae\t0\t\t\t\tWiener Entomologische Zeitung\t\t\t\t\t\t\t\t13\t\t200Ð212, pl. I\t\t\t\t0\t\n" +
                "{E340D95F-8B42-48DC-A914-010C78D16106}\tGraham E. Rotheray\t\t\t1990\t75\tLarval and puparial records of some hoverflies associated with dead wood (Diptera, Syrphidae)\t0\t\t\t\tDipterists Digest\t\t\t\t\t\t\t\t7\t\t2-7\t\t\t\t0\t\n";

        final Set<Triple<String, String, String>> triples = new TreeSet<>();
         StudyImporterForSaproxylic.parseReferences(triples::add, IOUtils.toInputStream(someLocalities));

        assertThat(triples, hasItem(StudyImporterForSaproxylic.asTriple("{9D773EF5-E522-4527-A404-00297A952B91}", "hasName", "J. Vanhara. 1982. The moravian species of flat footed flies (Diptera, Opetiidae and Platypezidae). vol 23. p 137-142. Folia prirodoved. fak. UJEP Brne.")));
    }

    @Test
    public void parseRanksTest() throws IOException, StudyImporterException {
        String someRanks = "Oid\tName\tOptimisticLockField\tGCRecord\n" +
                "{FDDADABD-8AD7-40CA-A7C5-14038E988AC1}\tClass\t0\t\n" +
                "{62253267-A76C-4326-9948-6A5BD50334C8}\tGenus\t0\t\n" +
                "{E9C5CB38-D6CF-4AAD-B96C-711C204A1206}\tPhylum\t0\t\n" +
                "{D399EA24-21B1-4D5C-9BAE-CEEDAF5BB7FF}\tOrder\t0\t\n" +
                "{4665ED8F-3734-406C-8987-E01B25E7D917}\tFamily\t0\t\n" +
                "{1DC0CA4B-1FF3-4A32-B390-E048F3D9A219}\tKingdom\t0\t\n" +
                "{BF719A88-8ED0-4804-B92A-EE02C0B36351}\tSuborder\t0\t\n" +
                "{1AA731EF-E05C-4F1E-96CD-F523D9635960}\tSubspecies\t0\t\n" +
                "{F4EB5A5C-34D0-4849-AC4F-FB2081067DF2}\tSpecies\t0\t";

        final Set<Triple<String, String, String>> triples = new TreeSet<>();

        StudyImporterForSaproxylic.parseTaxonRanks(triples::add, IOUtils.toInputStream(someRanks));

        assertThat(triples, hasItem(StudyImporterForSaproxylic.asTriple("{BF719A88-8ED0-4804-B92A-EE02C0B36351}", "hasName", "Suborder")));
    }

    @Test
    public void interactionTypeMap() throws IOException, StudyImporterException {
        final Set<Triple<String, String, String>> triples = new TreeSet<>();

        String aMapping = "sourceInteractionId\tsourceInteractionLabel\ttargetInteractionId\ttargetInteractionLabel\n" +
                "{F67E5D97-887B-41D3-9758-257F33767101}\tparasitized by\thttp://purl.obolibrary.org/obo/RO_0002445\thasParasite\n" +
                "{69F47164-2DEF-49CB-90FE-271CEA4A288F}\toccurs as omnivore\thttp://purl.obolibrary.org/obo/RO_0002470\teats\n";
        StudyImporterForSaproxylic.parseInteractionTypeMap(triples::add, IOUtils.toInputStream(aMapping));

        assertThat(triples, hasItem(StudyImporterForSaproxylic.asTriple("{F67E5D97-887B-41D3-9758-257F33767101}", "equivalentTo", InteractType.HAS_PARASITE.getIRI())));
    }

}
