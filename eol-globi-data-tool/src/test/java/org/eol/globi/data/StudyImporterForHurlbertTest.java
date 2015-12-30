package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForHurlbertTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException {
        StudyImporter importer = new StudyImporterForHurlbert(new ParserFactoryImpl(), nodeFactory);
        importStudy(importer);
        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size() > 10, is(true));

        TaxonNode formicidae = taxonIndex.findTaxonByName("Formicidae");
        assertThat(formicidae.getStatus(), is(notNullValue()));
    }

    @Test
    public void importSome() throws StudyImporterException {
        String csvString = aFewLines();
        TestParserFactory factory = new TestParserFactory(csvString);
        StudyImporter importer = new StudyImporterForHurlbert(factory, nodeFactory);
        importStudy(importer);

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));

        Study study = allStudies.get(0);
        assertThat(study.getSource(), containsString("Allen Hurlbert. Avian Diet Database (https://github.com/hurlbertlab/dietdatabase/). Accessed at https://raw.githubusercontent.com/hurlbertlab/dietdatabase/master/AvianDietDatabase.txt"));
        assertThat(study.getCitation(), is("Strong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392."));

        assertThat(taxonIndex.findTaxonByName("Seiurus aurocapillus"), is(notNullValue()));
        TaxonNode formicidae = taxonIndex.findTaxonByName("Formicidae");
        assertThat(formicidae, is(notNullValue()));
        assertThat(formicidae.getStatus().getId(), is("HURLBERT:someStatus"));
        assertThat(formicidae.getStatus().getName(), is("someStatus"));
        assertThat(taxonIndex.findTaxonByName("Coleoptera"), is(notNullValue()));

    }

    public String aFewLines() {
        return "Common_Name\tScientific_Name\tTaxonomy\tLongitude\tLatitude\tAltitude_min_m\tAltitude_mean_m\tAltitude_max_m\tLocation_Region\tLocation_Specific\tHabitat_type\tObservation_Month_Begin\tObservation_Year_Begin\tObservation_Month_End\tObservation_Year_End\tObservation_Season\tPrey_Name_Status\tPrey_Kingdom\tPrey_Phylum\tPrey_Class\tPrey_Order\tPrey_Suborder\tPrey_Family\tPrey_Genus\tPrey_Scientific_Name\tPrey_Stage\tPrey_Part\tPrey_Common_Name\tFraction_Diet_By_Wt_or_Vol\tFraction_Diet_By_Items\tFraction_Occurrence\tFraction_Diet_Unspecified\tItem Sample Size\tBird Sample size\tSites\tStudy Type\tNotes\tSource\n" +
                "Ovenbird\tSeiurus aurocapillus\t\"AOU 7th ed., 52nd supplement\"\t\t\t5\t\t630\tJamaica\t\t\"shade coffee\t second-growth scrub\t and undisturbed dry limestone forest\"\t11\t1993\t3\t1997\twinter\tsomeStatus\tAnimalia\tArthropoda\tInsecta\tHymenoptera\t\tFormicidae\t\t\t\t\t\t\t0.619\t1.0000\t\t2137\t53\t4\tEmetic\t\"When seeds were excluded from the analysis\t Ovenbird diets were similar across habitats\"\tStrong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird\tSeiurus aurocapillus\t\"AOU 7th ed., 52nd supplement\"\t\t\t5\t\t630\tJamaica\t\t\"shade coffee\t second-growth scrub\t and undisturbed dry limestone forest\"\t11\t1993\t3\t1997\twinter\tsomeStatus\tPlantae\t\t\t\t\t\t\t\t\tseeds\t\t\t0.185\t0.4900\t\t2137\t53\t4\tEmetic\t\"When seeds were excluded from the analysis\t Ovenbird diets were similar across habitats\"\tStrong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird\tSeiurus aurocapillus\t\"AOU 7th ed., 52nd supplement\"\t\t\t5\t\t630\tJamaica\t\t\"shade coffee\t second-growth scrub\t and undisturbed dry limestone forest\"\t11\t1993\t3\t1997\twinter\tsomeStatus\tAnimalia\tArthropoda\tInsecta\tColeoptera\t\t\t\t\t\t\t\t\t0.093\t0.9600\t\t2137\t53\t4\tEmetic\t\"When seeds were excluded from the analysis\t Ovenbird diets were similar across habitats\"\tStrong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird\tSeiurus aurocapillus\t\"AOU 7th ed., 52nd supplement\"\t\t\t5\t\t630\tJamaica\t\t\"shade coffee\t second-growth scrub\t and undisturbed dry limestone forest\"\t11\t1993\t3\t1997\twinter\tsomeStatus\tAnimalia\tArthropoda\tInsecta\tunknown\t\t\t\t\tlarvae\t\t\t\t0.025\t0.5100\t\t2137\t53\t4\tEmetic\t\"When seeds were excluded from the analysis\t Ovenbird diets were similar across habitats\"\tStrong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird\tSeiurus aurocapillus\t\"AOU 7th ed., 52nd supplement\"\t\t\t5\t\t630\tJamaica\t\t\"shade coffee\t second-growth scrub\t and undisturbed dry limestone forest\"\t11\t1993\t3\t1997\twinter\tsomeStatus\tAnimalia\tMollusca\tGastropoda\t\t\t\t\t\t\t\tsnails\t\t0.015\t0.4200\t\t2137\t53\t4\tEmetic\t\"When seeds were excluded from the analysis\t Ovenbird diets were similar across habitats\"\tStrong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird\tSeiurus aurocapillus\t\"AOU 7th ed., 52nd supplement\"\t\t\t5\t\t630\tJamaica\t\t\"shade coffee\t second-growth scrub\t and undisturbed dry limestone forest\"\t11\t1993\t3\t1997\twinter\tsomeStatus\tAnimalia\tArthropoda\tArachnida\tAraneae\t\t\t\t\t\t\tspiders\t\t0.015\t0.3800\t\t2137\t53\t4\tEmetic\t\"When seeds were excluded from the analysis\t Ovenbird diets were similar across habitats\"\tStrong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird\tSeiurus aurocapillus\t\"AOU 7th ed., 52nd supplement\"\t\t\t5\t\t630\tJamaica\t\t\"shade coffee\t second-growth scrub\t and undisturbed dry limestone forest\"\t11\t1993\t3\t1997\twinter\tsomeStatus\tAnimalia\tArthropoda\tInsecta\tHymenoptera\t\tnon-Formicidae\t\t\t\t\t\t\t0.009\t0.3000\t\t2137\t53\t4\tEmetic\t\"When seeds were excluded from the analysis\t Ovenbird diets were similar across habitats\"\tStrong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird\tSeiurus aurocapillus\t\"AOU 7th ed., 52nd supplement\"\t\t\t5\t\t630\tJamaica\t\t\"shade coffee\t second-growth scrub\t and undisturbed dry limestone forest\"\t11\t1993\t3\t1997\twinter\tsomeStatus\tAnimalia\tArthropoda\tInsecta\tOrthoptera\t\t\t\t\t\t\t\t\t0.007\t0.2500\t\t2137\t53\t4\tEmetic\t\"When seeds were excluded from the analysis\t Ovenbird diets were similar across habitats\"\tStrong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird\tSeiurus aurocapillus\t\"AOU 7th ed., 52nd supplement\"\t\t\t5\t\t630\tJamaica\t\t\"shade coffee\t second-growth scrub\t and undisturbed dry limestone forest\"\t11\t1993\t3\t1997\twinter\tsomeStatus\tAnimalia\tArthropoda\tInsecta\tDermaptera\t\t\t\t\t\t\t\t\t0.004\t0.0800\t\t2137\t53\t4\tEmetic\t\"When seeds were excluded from the analysis\t Ovenbird diets were similar across habitats\"\tStrong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n";
    }

}
