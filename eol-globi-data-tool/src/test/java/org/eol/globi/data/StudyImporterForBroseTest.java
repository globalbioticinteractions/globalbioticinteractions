package org.eol.globi.data;

import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.taxon.UberonLookupService;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForBroseTest extends GraphDBTestCase {

    @Override
    protected TermLookupService getTermLookupService() {
        return new UberonLookupService();
    }

    @Test
    public void importHeadAndTail() throws IOException, StudyImporterException {
        final String headAndTail = "Link ID\tLink reference \tBody size reference\tGeographic location\tGeneral habitat\tSpecific habitat\tLink methodology\tBody size methodology\tTaxonomy consumer\tLifestage consumer\tCommon name(s) consumer\tMetabolic category consumer\tType of feeding interaction\tMinimum length (m) consumer\tMean length (m) consumer\tMaximum length (m) consumer\tMinimum mass (g) consumer\tMean mass (g) consumer\tMaximum mass (g) consumer\tTaxonomy resource\tLifestage - resource\tCommon name(s) resource\tMetabolic category resource\tMinimum length (m) resource\tMean length (m) resource\tMaximum length (m) resource\tMinimum mass (g) resource\tMean mass (g) resource\tMaximum mass (g) resource\tConsumer/resource body mass ratio\tNotes\n" +
                "1\tYodzis (1998)\tYodzis (1998)\tAfrica, Benguela ecosystem\tmarine\tpelagic food web\tpublished account\t\"published account; expert; regression\"\t-999\tadults\tBacteria\theterotrophic bacteria\therbivorous\t-999\t-999\t-999\t-999\t0.00000001\t-999\t-999\tadults\tPhytoplankton\tphoto-autotroph\t-999\t-999\t-999\t-999\t0.0001\t-999\t0.0001\t-999\n" +
                "2\tYodzis (1998)\tYodzis (1998)\tAfrica, Benguela ecosystem\tmarine\tpelagic food web\tpublished account\t\"published account; expert; regression\"\t-999\tadults\tBenthic carnivores\tinvertebrate\tpredacious\t-999\t-999\t-999\t-999\t10\t-999\t-999\tadults\tBenthic filter feeders\tinvertebrate\t-999\t-999\t-999\t-999\t10\t-999\t1\t-999\n" +
                "3\tYodzis (1998)\tYodzis (1998)\tAfrica, Benguela ecosystem\tmarine\tpelagic food web\tpublished account\t\"published account; expert; regression\"\t-999\tadults\tMicrozooplankton\tinvertebrate\therbivorous\t-999\t-999\t-999\t-999\t0.0001\t-999\t-999\tadults\tPhytoplankton\tphoto-autotroph\t-999\t-999\t-999\t-999\t0.0001\t-999\t1\t-999\n" +
                "4\tYodzis (1998)\tYodzis (1998)\tAfrica, Benguela ecosystem\tmarine\tpelagic food web\tpublished account\t\"published account; expert; regression\"\t-999\tadults\tMicrozooplankton\tinvertebrate\tbacterivorous\t-999\t-999\t-999\t-999\t0.0001\t-999\t-999\tadults\tBacteria\theterotrophic bacteria\t-999\t-999\t-999\t-999\t0.00000001\t-999\t10000\t-999\n" +
                "5\tYodzis (1998)\tYodzis (1998)\tAfrica, Benguela ecosystem\tmarine\tpelagic food web\tpublished account\t\"published account; expert; regression\"\t-999\tadults\tMicrozooplankton\tinvertebrate\tpredacious\t-999\t-999\t-999\t-999\t0.0001\t-999\t-999\tadults\tMicrozooplankton\tinvertebrate\t-999\t-999\t-999\t-999\t0.0001\t-999\t1\t-999\n" +
                "6\tYodzis (1998)\tYodzis (1998)\tAfrica, Benguela ecosystem\tmarine\tpelagic food web\tpublished account\t\"published account; expert; regression\"\t-999\tadults\tMesozooplankton\tinvertebrate\therbivorous\t-999\t-999\t-999\t-999\t0.01\t-999\t-999\tadults\tPhytoplankton\tphoto-autotroph\t-999\t-999\t-999\t-999\t0.0001\t-999\t100\t-999\n" +
                "7\tYodzis (1998)\tYodzis (1998)\tAfrica, Benguela ecosystem\tmarine\tpelagic food web\tpublished account\t\"published account; expert; regression\"\t-999\tadults\tMesozooplankton\tinvertebrate\tpredacious\t-999\t-999\t-999\t-999\t0.01\t-999\t-999\tadults\tMicrozooplankton\tinvertebrate\t-999\t-999\t-999\t-999\t0.0001\t-999\t100\t-999\n" +
                "8\tYodzis (1998)\tYodzis (1998)\tAfrica, Benguela ecosystem\tmarine\tpelagic food web\tpublished account\t\"published account; expert; regression\"\t-999\tadults\tMacrozooplankton\tinvertebrate\therbivorous\t-999\t-999\t-999\t-999\t1\t-999\t-999\tadults\tPhytoplankton\tphoto-autotroph\t-999\t-999\t-999\t-999\t0.0001\t-999\t10000\t-999\n" +
                "9\tYodzis (1998)\tYodzis (1998)\tAfrica, Benguela ecosystem\tmarine\tpelagic food web\tpublished account\t\"published account; expert; regression\"\t-999\tadults\tMacrozooplankton\tinvertebrate\tpredacious\t-999\t-999\t-999\t-999\t1\t-999\t-999\tadults\tMesozooplankton\tinvertebrate\t-999\t-999\t-999\t-999\t0.01\t-999\t100\t-999\n" +
                "16857\tCohen et al. (2005)\tCohen et al. (2005)\t\"Country: United Kingdom; UTM: 51.24'N, 0.34'W; Silwood Park, Berkshire\"\tterrestrial\tabandoned field\trearing\t\"measurement; regression\"\tPraon dorsale\tadult\tparasitoid wasp\tinvertebrate\tparasitoid\t0.002034483\t0.002197044\t0.002344828\t-999\t-999\t-999\tAcyrthosiphon pisum\tadult\taphid\tinvertebrate\t0.002448276\t0.00264532\t0.002793103\t-999\t-999\t-999\t0.614787036\t\"newly emerged adult parsitoids were measured; resource length was measured without cauda; for data on individual body sizes and body sizes of hyperparasitoids and mummy parasitoids see Cohen et al. (2005); 7 pairs of individuals were measured\"\n" +
                "16858\tCohen et al. (2005)\tCohen et al. (2005)\t\"Country: United Kingdom; UTM: 51.24'N, 0.34'W; Silwood Park, Berkshire\"\tterrestrial\tabandoned field\trearing\t\"measurement; regression\"\tAphidius urticae\tadult\tparasitoid wasp\tinvertebrate\tparasitoid\t0.002\t0.002665025\t0.003517241\t-999\t-999\t-999\tMacrosiphum funestum\tadult\taphid\tinvertebrate\t0.002482759\t0.003214286\t0.003965517\t-999\t-999\t-999\t0.61203447\t\"newly emerged adult parsitoids were measured; resource length was measured without cauda; for data on individual body sizes and body sizes of hyperparasitoids and mummy parasitoids see Cohen et al. (2005); 14 pairs of individuals were measured\"\n" +
                "16859\tCohen et al. (2005)\tCohen et al. (2005)\t\"Country: United Kingdom; UTM: 51.24'N, 0.34'W; Silwood Park, Berkshire\"\tterrestrial\tabandoned field\trearing\t\"measurement; regression\"\tPraon dorsale\tadult\tparasitoid wasp\tinvertebrate\tparasitoid\t0.001965517\t0.001965517\t0.001965517\t-999\t-999\t-999\tAmphorophora rubi\tadult\taphid\tinvertebrate\t0.002482759\t0.002482759\t0.002482759\t-999\t-999\t-999\t0.542226802\t\"newly emerged adult parsitoids were measured; resource length was measured without cauda; for data on individual body sizes and body sizes of hyperparasitoids and mummy parasitoids see Cohen et al. (2005); 1 pair of individuals were measured\"\n" +
                "16860\tCohen et al. (2005)\tCohen et al. (2005)\t\"Country: United Kingdom; UTM: 51.24'N, 0.34'W; Silwood Park, Berkshire\"\tterrestrial\tabandoned field\trearing\t\"measurement; regression\"\tPraon dorsale\tadult\tparasitoid wasp\tinvertebrate\tparasitoid\t0.001896552\t0.001896552\t0.001896552\t-999\t-999\t-999\tMegoura viciaei\tadult\taphid\tinvertebrate\t0.002862069\t0.002862069\t0.002862069\t-999\t-999\t-999\t0.340224547\t\"newly emerged adult parsitoids were measured; resource length was measured without cauda; for data on individual body sizes and body sizes of hyperparasitoids and mummy parasitoids see Cohen et al. (2005); 1 pair of individuals were measured\"\n" +
                "16861\tCohen et al. (2005)\tCohen et al. (2005)\t\"Country: United Kingdom; UTM: 51.24'N, 0.34'W; Silwood Park, Berkshire\"\tterrestrial\tabandoned field\trearing\t\"measurement; regression\"\tAphelinus abdominalis\tadult\tparasitoid wasp\tinvertebrate\tparasitoid\t0.000793103\t0.000926724\t0.001206897\t-999\t-999\t-999\tSitobion fragariae / Sitobion avenae\tnymphs\taphid\tinvertebrate\t0.001310345\t0.001599138\t0.001896552\t-999\t-999\t-999\t0.239457874\t\"newly emerged adult parsitoids were measured; resource length was measured without cauda; for data on individual body sizes and body sizes of hyperparasitoids and mummy parasitoids see Cohen et al. (2005); 16 pairs of individuals were measured\"\n" +
                "16862\tCohen et al. (2005)\tCohen et al. (2005)\t\"Country: United Kingdom; UTM: 51.24'N, 0.34'W; Silwood Park, Berkshire\"\tterrestrial\tabandoned field\trearing\t\"measurement; regression\"\tAphelinus abdominalis\tadult\tparasitoid wasp\tinvertebrate\tparasitoid\t0.000965517\t0.000965517\t0.000965517\t-999\t-999\t-999\tAmphorophora rubi\tadult\taphid\tinvertebrate\t0.001689655\t0.001689655\t0.001689655\t-999\t-999\t-999\t0.230802396\t\"newly emerged adult parsitoids were measured; resource length was measured without cauda; for data on individual body sizes and body sizes of hyperparasitoids and mummy parasitoids see Cohen et al. (2005); 1 pair of individuals were measured\"\n" +
                "16863\tCohen et al. (2005)\tCohen et al. (2005)\t\"Country: United Kingdom; UTM: 51.24'N, 0.34'W; Silwood Park, Berkshire\"\tterrestrial\tabandoned field\trearing\t\"measurement; regression\"\tAphelinus abdominalis\tadult\tparasitoid wasp\tinvertebrate\tparasitoid\t0.000896552\t0.001132626\t0.001310345\t-999\t-999\t-999\tSitobion fragariae / Sitobion avenae\tadult\taphid\tinvertebrate\t0.001448276\t0.00201061\t0.002517241\t-999\t-999\t-999\t0.222324699\t\"newly emerged adult parsitoids were measured; resource length was measured without cauda; for data on individual body sizes and body sizes of hyperparasitoids and mummy parasitoids see Cohen et al. (2005); 13 pairs of individuals were measured\"\n" +
                "16864\tCohen et al. (2005)\tCohen et al. (2005)\t\"Country: United Kingdom; UTM: 51.24'N, 0.34'W; Silwood Park, Berkshire\"\tterrestrial\tabandoned field\trearing\t\"measurement; regression\"\tAphelinus abdominalis\tadult\tparasitoid wasp\tinvertebrate\tparasitoid\t0.001068966\t0.001206897\t0.001344828\t-999\t-999\t-999\tAcyrthosiphon pisum\tnymphs\taphid\tinvertebrate\t0.002034483\t0.002206897\t0.00237931\t-999\t-999\t-999\t0.205715371\t\"newly emerged adult parsitoids were measured; resource length was measured without cauda; for data on individual body sizes and body sizes of hyperparasitoids and mummy parasitoids see Cohen et al. (2005); 2 pairs of individuals were measured\"\n" +
                "16865\tCohen et al. (2005)\tCohen et al. (2005)\t\"Country: United Kingdom; UTM: 51.24'N, 0.34'W; Silwood Park, Berkshire\"\tterrestrial\tabandoned field\trearing\t\"measurement; regression\"\tAphelinus abdominalis\tadult\tparasitoid wasp\tinvertebrate\tparasitoid\t0.001103448\t0.00112069\t0.001137931\t-999\t-999\t-999\tMetopolophium albidum\tadult\taphid\tinvertebrate\t0.002034483\t0.002172414\t0.002310345\t-999\t-999\t-999\t0.176547741\t\"newly emerged adult parsitoids were measured; resource length was measured without cauda; for data on individual body sizes and body sizes of hyperparasitoids and mummy parasitoids see Cohen et al. (2005); 2 pairs of individuals were measured\"\n" +
                "16866\tCohen et al. (2005)\tCohen et al. (2005)\t\"Country: United Kingdom; UTM: 51.24'N, 0.34'W; Silwood Park, Berkshire\"\tterrestrial\tabandoned field\trearing\t\"measurement; regression\"\tAphelinus varipes\tadult\tparasitoid wasp\tinvertebrate\tparasitoid\t0.000896552\t0.000896552\t0.000896552\t-999\t-999\t-999\tSitobion fragariae / Sitobion avenae\tnymphs\taphid\tinvertebrate\t0.001965517\t0.001965517\t0.001965517\t-999\t-999\t-999\t0.127890431\t\"newly emerged adult parsitoids were measured; resource length was measured without cauda; for data on individual body sizes and body sizes of hyperparasitoids and mummy parasitoids see Cohen et al. (2005); 1 pair of individuals were measured\"\n";


        TestParserFactory parserFactory = new TestParserFactory(new HashMap<String, String>() {
            {
                put(StudyImporterForBrose.RESOURCE_PATH, headAndTail);
                put(StudyImporterForBrose.REFERENCE_PATH, "short,full\nCohen et al. (2005),something long\nYodzis (1998),something longer");
            }
        });
        StudyImporter importer = new StudyImporterForBrose(parserFactory, nodeFactory);
        importStudy(importer);

        Taxon taxon = taxonIndex.findTaxonByName("Praon dorsale");
        Iterable<Relationship> relationships = ((NodeBacked)taxon).getUnderlyingNode().getRelationships(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));
        for (Relationship relationship : relationships) {
            Node predatorSpecimenNode = relationship.getStartNode();
            assertThat((String) predatorSpecimenNode.getProperty(SpecimenConstant.LIFE_STAGE_LABEL), is("post-juvenile adult stage"));
            assertThat((String) predatorSpecimenNode.getProperty(SpecimenConstant.LIFE_STAGE_ID), is("UBERON:0000113"));

        }
        assertThat(taxon, is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Aphelinus abdominalis"), is(notNullValue()));

        Location location = nodeFactory.findLocation(new LocationImpl(51.24, -0.34, null, null));
        assertThat("missing location", location, is(notNullValue()));

        List<Environment> environments = location.getEnvironments();
        assertThat(environments.size(), is(1));
        assertThat(environments.get(0).getExternalId(), is("TEST:terrestrial abandoned field"));
        assertThat(environments.get(0).getName(), is("terrestrial abandoned field"));

    }

}
