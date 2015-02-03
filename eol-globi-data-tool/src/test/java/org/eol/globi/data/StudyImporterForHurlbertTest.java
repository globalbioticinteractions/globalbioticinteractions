package org.eol.globi.data;

import org.eol.globi.domain.Study;
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
        importer.importStudy();
        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size() > 10, is(true));
    }

    @Test
    public void importSome() throws StudyImporterException, NodeFactoryException {
        String csvString = aFewLines();
        TestParserFactory factory = new TestParserFactory(csvString);
        StudyImporter importer = new StudyImporterForHurlbert(factory, nodeFactory);
        importer.importStudy();

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));

        Study study = allStudies.get(0);
        assertThat(study.getSource(), containsString("Allen Hurlbert. Avian Diet Database (https://github.com/hurlbertlab/dietdatabase/). Accessed at https://raw.githubusercontent.com/hurlbertlab/dietdatabase/master/AvianDietDatabase.txt"));
        assertThat(study.getCitation(), is("Strong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392."));

        assertThat(nodeFactory.findTaxonByName("Seiurus aurocapillus"), is(notNullValue()));
        assertThat(nodeFactory.findTaxonByName("Formicidae"), is(notNullValue()));
        assertThat(nodeFactory.findTaxonByName("Coleoptera"), is(notNullValue()));

    }

    public String aFewLines() {
        return "Common_Name	Scientific_Name	Taxonomy	Longitude	Latitude	Altitude_min_m	Altitude_mean_m	Altitude_max_m	Location_Region	Location_Specific	Habitat_type	Observation_Month_Begin	Observation_Year_Begin	Observation_Month_End	Observation_Year_End	Observation_Season	Prey_Kingdom	Prey_Phylum	Prey_Class	Prey_Order	Prey_Suborder	Prey_Family	Prey_Genus	Prey_Scientific_Name	Prey_Stage	Prey_Part	Prey_Common_Name	Fraction_Diet_By_Wt_or_Vol	Fraction_Diet_By_Items	Fraction_Occurrence	Fraction_Diet_Unspecified	Item Sample Size	Bird Sample size	Sites	Study Type	Notes	Source\n" +
                "Ovenbird	Seiurus aurocapillus	\"AOU 7th ed., 52nd supplement\"			5		630	Jamaica		\"shade coffee	 second-growth scrub	 and undisturbed dry limestone forest\"	11	1993	3	1997	winter	Animalia	Arthropoda	Insecta	Hymenoptera		Formicidae							0.619	1.0000		2137	53	4	Emetic	\"When seeds were excluded from the analysis	 Ovenbird diets were similar across habitats\"	Strong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird	Seiurus aurocapillus	\"AOU 7th ed., 52nd supplement\"			5		630	Jamaica		\"shade coffee	 second-growth scrub	 and undisturbed dry limestone forest\"	11	1993	3	1997	winter	Plantae									seeds			0.185	0.4900		2137	53	4	Emetic	\"When seeds were excluded from the analysis	 Ovenbird diets were similar across habitats\"	Strong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird	Seiurus aurocapillus	\"AOU 7th ed., 52nd supplement\"			5		630	Jamaica		\"shade coffee	 second-growth scrub	 and undisturbed dry limestone forest\"	11	1993	3	1997	winter	Animalia	Arthropoda	Insecta	Coleoptera									0.093	0.9600		2137	53	4	Emetic	\"When seeds were excluded from the analysis	 Ovenbird diets were similar across habitats\"	Strong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird	Seiurus aurocapillus	\"AOU 7th ed., 52nd supplement\"			5		630	Jamaica		\"shade coffee	 second-growth scrub	 and undisturbed dry limestone forest\"	11	1993	3	1997	winter	Animalia	Arthropoda	Insecta	unknown					larvae				0.025	0.5100		2137	53	4	Emetic	\"When seeds were excluded from the analysis	 Ovenbird diets were similar across habitats\"	Strong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird	Seiurus aurocapillus	\"AOU 7th ed., 52nd supplement\"			5		630	Jamaica		\"shade coffee	 second-growth scrub	 and undisturbed dry limestone forest\"	11	1993	3	1997	winter	Animalia	Mollusca	Gastropoda								snails		0.015	0.4200		2137	53	4	Emetic	\"When seeds were excluded from the analysis	 Ovenbird diets were similar across habitats\"	Strong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird	Seiurus aurocapillus	\"AOU 7th ed., 52nd supplement\"			5		630	Jamaica		\"shade coffee	 second-growth scrub	 and undisturbed dry limestone forest\"	11	1993	3	1997	winter	Animalia	Arthropoda	Arachnida	Araneae							spiders		0.015	0.3800		2137	53	4	Emetic	\"When seeds were excluded from the analysis	 Ovenbird diets were similar across habitats\"	Strong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird	Seiurus aurocapillus	\"AOU 7th ed., 52nd supplement\"			5		630	Jamaica		\"shade coffee	 second-growth scrub	 and undisturbed dry limestone forest\"	11	1993	3	1997	winter	Animalia	Arthropoda	Insecta	Hymenoptera		non-Formicidae							0.009	0.3000		2137	53	4	Emetic	\"When seeds were excluded from the analysis	 Ovenbird diets were similar across habitats\"	Strong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird	Seiurus aurocapillus	\"AOU 7th ed., 52nd supplement\"			5		630	Jamaica		\"shade coffee	 second-growth scrub	 and undisturbed dry limestone forest\"	11	1993	3	1997	winter	Animalia	Arthropoda	Insecta	Orthoptera									0.007	0.2500		2137	53	4	Emetic	\"When seeds were excluded from the analysis	 Ovenbird diets were similar across habitats\"	Strong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n" +
                "Ovenbird	Seiurus aurocapillus	\"AOU 7th ed., 52nd supplement\"			5		630	Jamaica		\"shade coffee	 second-growth scrub	 and undisturbed dry limestone forest\"	11	1993	3	1997	winter	Animalia	Arthropoda	Insecta	Dermaptera									0.004	0.0800		2137	53	4	Emetic	\"When seeds were excluded from the analysis	 Ovenbird diets were similar across habitats\"	Strong, A. M. 2000. Divergent foraging strategies of two neotropical migrant warblers: Implications for winter habitat use. Auk 117(2):381-392.\n";
    }

}
