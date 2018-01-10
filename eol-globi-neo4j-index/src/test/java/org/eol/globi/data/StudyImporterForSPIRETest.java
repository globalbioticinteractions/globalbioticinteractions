package org.eol.globi.data;

import com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.GeoNamesServiceImpl;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForSPIRETest extends GraphDBTestCase {

    @Test
    public void parseIllegalTitle() throws StudyImporterException {
        HashMap<String, String> properties = new HashMap<String, String>();
        StudyImporterForSPIRE.parseTitlesAndAuthors("this is really not supported, and is unformatted", properties);
        assertThat(properties.get(StudyConstant.TITLE), is("this is really not su...e9154c16f07ad2470849d90a8a0b9dab"));
        assertThat(properties.get(StudyConstant.DESCRIPTION), is("this is really not supported, and is unformatted"));

    }

    @Test
    public void parseAnotherYetYetOtherTitlesAndAuthorsFormat() throws StudyImporterException {
        Map<String, String> properties = new HashMap<String, String>();
        String titlesAndAuthors = "G. A. Knox, Antarctic marine ecosystems. In: Antarctic Ecology, M. W. Holdgate, Ed. (Academic Press, New York, 1970) 1:69-96, from p. 87.";
        StudyImporterForSPIRE.parseTitlesAndAuthors(titlesAndAuthors, properties);
        assertThat(properties.get(StudyConstant.DESCRIPTION), is("G. A. Knox, Antarctic marine ecosystems. In: Antarctic Ecology, M. W. Holdgate, Ed. (Academic Press, New York, 1970) 1:69-96, from p. 87."));
        assertThat(properties.get(StudyConstant.TITLE), is("Knox, Antarctic marin...984ae066666743823ac7b57da0e01f2d"));
    }

    @Test
    public void parseAnotherYetOtherTitlesAndAuthorsFormat() throws StudyImporterException {
        Map<String, String> properties = new HashMap<String, String>();
        StudyImporterForSPIRE.parseTitlesAndAuthors("B. A. Hawkins and R. D. Goeden, 1984.  Organization of a parasitoid community associated with a complex of galls on Atriplex spp. in southern California.  Ecol. Entomol. 9:271-292, from p. 274.", properties);
        assertThat(properties.get(StudyConstant.TITLE), is("Hawkins and Goeden, 1...fcebc21f82937fa4ab9f77a0ecbd62e3"));
        assertThat(properties.get(StudyConstant.DESCRIPTION), is("B. A. Hawkins and R. D. Goeden, 1984.  Organization of a parasitoid community associated with a complex of galls on Atriplex spp. in southern California.  Ecol. Entomol. 9:271-292, from p. 274."));
    }

    @Test
    public void mergeIdenticalReferences() throws StudyImporterException {
        // see https://github.com/danielabar/globi-proto/issues/59#issuecomment-92150679
        Map<String, String> properties = new HashMap<String, String>();
        StudyImporterForSPIRE.parseTitlesAndAuthors("G. W. Minshall, Role of allochthonous detritus in the trophic structure\n" +
                " of a woodland springbrook community, Ecology 48(1):139-149, from p. 148 (1967).\n" +
                "", properties);
        assertThat(properties.get(StudyConstant.DESCRIPTION), is("G. W. Minshall, 1967.  Role of allochthonous detritus in the trophic structure of a woodland springbrook community.  Ecology 48:139-149, from pp. 145, 148."));
        assertThat(properties.get(StudyConstant.TITLE), is("Minshall, 196 Role of...e40765a49c84da8d9e0c2a527f1fd111"));
    }

    @Test
    public void parseYetOtherTitlesAndAuthorsFormat() throws StudyImporterException {
        Map<String, String> properties = new HashMap<String, String>();
        StudyImporterForSPIRE.parseTitlesAndAuthors("Townsend, CR, Thompson, RM, McIntosh, AR, Kilroy, C, Edwards, ED, Scarsbrook, MR. 1998.  Disturbance, resource supply and food-web architecture in streams.  Ecology Letters 1:200-209.", properties);
        assertThat(properties.get(StudyConstant.TITLE), is("Townsend, CR, Thompso...db61dcc043a135ac2fa8b440e11165e3"));
        assertThat(properties.get(StudyConstant.DESCRIPTION), is("Townsend, CR, Thompson, RM, McIntosh, AR, Kilroy, C, Edwards, ED, Scarsbrook, MR. 1998.  Disturbance, resource supply and food-web architecture in streams.  Ecology Letters 1:200-209."));
    }

    @Test
    public void importSingleLink() throws NodeFactoryException {
        assertSingleImport("habitat", "TEST:habitat", "habitat");

    }

    private void assertSingleImport(String spireHabitat, String envoId, String envoLabel) throws NodeFactoryException {
        StudyImporterForSPIRE studyImporterForSPIRE = createImporter();
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(StudyConstant.TITLE, "the study of men eating dogs");
        properties.put(StudyImporterForSPIRE.PREY_NAME, "dog");
        properties.put(StudyImporterForSPIRE.PREDATOR_NAME, "man");
        properties.put(StudyImporterForSPIRE.LOCALITY_ORIGINAL, "something");
        properties.put(StudyImporterForSPIRE.OF_HABITAT, spireHabitat);
        studyImporterForSPIRE.importTrophicLink(properties);
        resolveNames();

        Taxon dog = taxonIndex.findTaxonByName("dog");
        assertThat(dog, is(notNullValue()));
        Taxon man = taxonIndex.findTaxonByName("man");
        assertThat(man, is(notNullValue()));
        Iterable<Relationship> specimenRels = ((NodeBacked)man).getUnderlyingNode().getRelationships(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));

        int count = 0;
        for (Relationship specimenRel : specimenRels) {
            count++;
            Specimen specimen = new SpecimenNode(specimenRel.getStartNode());
            assertThat(specimen.getSampleLocation().getLatitude(), is(1.0));
            assertThat(specimen.getSampleLocation().getLongitude(), is(2.0));

            List<Environment> environments = specimen.getSampleLocation().getEnvironments();
            assertThat(environments.size(), is(1));
            Environment environment = environments.get(0);
            assertThat(environment.getExternalId(), is(envoId));
            assertThat(environment.getName(), is(envoLabel));
        }
        assertThat(count, is(1));
    }

    private StudyImporterForSPIRE createImporter() {
        StudyImporterForSPIRE studyImporterForSPIRE = new StudyImporterForSPIRE(null, nodeFactory);
        studyImporterForSPIRE.setDataset(new DatasetLocal());
        studyImporterForSPIRE.setGeoNamesService(new GeoNamesService() {

            @Override
            public boolean hasTermForLocale(String locality) {
                return "something".equals(locality);
            }

            @Override
            public LatLng findLatLng(String locality) throws IOException {
                return hasTermForLocale(locality) ? new LatLng(1.0, 2.0) : null;
            }

        });
        return studyImporterForSPIRE;
    }


    @Test
    public void phytoplanktonUnlikelyPredators() throws IOException, StudyImporterException {
        RDFDefaultErrorHandler.silent = true;
        StudyImporterForSPIRE importer = createImporter();
        final List<String> predators = new ArrayList<String>();
        importer.setInteractionListener(new InteractionListener() {
            @Override
            public void newLink(Map<String, String> properties) {
                if (!StudyImporterForSPIRE.isValid(properties)) {
                    predators.add(properties.get(StudyImporterForSPIRE.PREDATOR_NAME));
                }
            }
        });
        importStudy(importer);
        assertThat(predators, hasItem("phytoplankton"));
    }

    @Test
    public void importStudy() throws IOException, StudyImporterException {
        RDFDefaultErrorHandler.silent = true;
        StudyImporterForSPIRE importer = createImporter();
        TestInteractionListener listener = new TestInteractionListener();
        importer.setInteractionListener(listener);
        importStudy(importer);

        assertGAZMapping(listener);

        GeoNamesServiceImpl geoNamesServiceImpl = new GeoNamesServiceImpl();
        for (String locality : listener.localities) {
            assertThat(geoNamesServiceImpl.hasTermForLocale(locality), is(true));
        }
        assertThat(listener.getCount(), is(30196));

        assertThat(listener.descriptions, not(hasItem("http://spire.umbc.edu/ontologies/SpireEcoConcepts.owl#")));
        assertThat(listener.titles, not(hasItem("http://spire.umbc.edu/")));
        assertThat(listener.environments, not(hasItem("http://spire.umbc.edu/ontologies/SpireEcoConcepts.owl#")));

        assertThat(listener.invalidInteractions.size(), is(greaterThan(0)));
    }

    private void assertGAZMapping(TestInteractionListener listener) {
        Map<String, TermImpl> gazMap = new HashMap<String, TermImpl>() {{
            put("Country: New Zealand;   State: Otago;   Locality: Catlins, Craggy Tor catchment", new TermImpl("GAZ:00146864", "The Catlins"));
            put("Country: Scotland", new TermImpl("GAZ:00002639", "Scotland"));
            put("Country: USA;   State: Georgia", new TermImpl("GAZ:00002611", "State of Georgia"));
            put("Country: USA;   State: Iowa", new TermImpl("GAZ:00004438", "State of Iowa"));
            put("Country: Southern Ocean", new TermImpl("GAZ:00000373", "Southern Ocean"));
            put("Country: USA", new TermImpl("GAZ:00002459", "United States of America"));
            put("Country: USA;   State: Iowa;   Locality: Mississippi River", new TermImpl("GAZ:00004438", "State of Iowa"));
            put("Country: Japan", new TermImpl("GAZ:00002747", "Japan"));
            put("Country: Malaysia;   Locality: W. Malaysia", new TermImpl("GAZ:00003902", "Malaysia"));
            put("Country: Chile;   Locality: central Chile", new TermImpl("GAZ:00002825", "Chile"));
            put("Country: USA;   State: New Mexico;   Locality: Aden Crater", new TermImpl("GAZ:00004427", "State of New Mexico"));
            put("Country: USA;   State: Alaska;   Locality: Torch Bay", new TermImpl("GAZ:00002521", "State of Alaska"));
            put("Country: USA;   State: Pennsylvania", new TermImpl("GAZ:00002542", " Commonwealth of Pennsylvania"));
            put("Country: Costa Rica", new TermImpl("GAZ:00002901", "Costa Rica"));
            put("Country: Pacific", new TermImpl("GAZ:00000360", "Pacific Ocean"));
            put("Country: USA;   State: California;   Locality: Cabrillo Point", new TermImpl("GAZ:00002461", "State of California"));
            put("Country: USA;   State: Texas", new TermImpl("GAZ:00002580", "State of Texas"));
            put("Country: Portugal", new TermImpl("GAZ:00004125", "Autonomous Region (Portugal)"));
            put("Country: USA;   Locality: Northeastern US contintental shelf", new TermImpl("GAZ:00002459", "United States of America"));
            put("Country: Sri Lanka", new TermImpl("GAZ:00003924", "Sri Lanka"));
            put("Country: USA;   State: Maine;   Locality: Troy", new TermImpl("GAZ:00002602", "State of Maine"));
            put("Country: New Zealand", new TermImpl("GAZ:00000469", "New Zealand"));
            put("Country: USA;   State: Maine;   Locality: Gulf of Maine", new TermImpl("GAZ:00002876", "Gulf of Maine"));
            put("Country: New Zealand;   State: Otago;   Locality: Dempster's Stream, Taieri River, 3 O'Clock catchment", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: Panama;   Locality: Gatun Lake", new TermImpl("GAZ:00002898", "Lake Gatun"));
            put("Country: USA;   State: Maryland;   Locality: Chesapeake Bay", new TermImpl("GAZ:00002604", "Chesapeake Bay"));
            put("Country: India;   Locality: Cochin", new TermImpl("GAZ:00002839", "India"));
            put("Country: Ethiopia;   Locality: Lake Abaya", new TermImpl("GAZ:00041560", "Lake Abaya"));
            put("Country: unknown;   State: Black Sea", new TermImpl("GAZ:00008171", "Black Sea"));
            put("Country: St. Martin;   Locality: Caribbean", new TermImpl("GAZ:00044587", "Saint-Martin Island"));
            put("Country: USA;   State: Yellowstone", new TermImpl("GAZ:00002534", "Yellowstone National Park"));
            put("Country: Scotland;   Locality: Loch Leven", new TermImpl("GAZ:00002639", "Scotland"));
            put("Country: New Zealand;   State: Otago;   Locality: Sutton Stream, Taieri River, Sutton catchment", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: Alaska;   Locality: Barrow", new TermImpl("GAZ:00198344", "City Of Barrow"));
            put("Country: Malawi;   Locality: Lake Nyasa", new TermImpl("GAZ:00000058", "Lake Malawi"));
            put("Country: USA;   State: Alaska;   Locality: Aleutian Islands", new TermImpl("GAZ:00005858", "Aleutian Islands"));
            put("Country: USA;   State: California;   Locality: Southern California", new TermImpl("GAZ:00168979", "Southern California"));
            put("Country: Canada;   State: Manitoba", new TermImpl("GAZ:00002571", "Province of Manitoba"));
            put("Country: USA;   State: Maine", new TermImpl("GAZ:00002602", "State Of Maine"));
            put("Country: Polynesia", new TermImpl("GAZ:00005861", "Polynesia"));
            put("Country: South Africa", new TermImpl("GAZ:00000553", "South Africa"));
            put("Country: New Zealand;   State: Otago;   Locality: Berwick, Meggatburn", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: New Zealand;   State: Otago;   Locality: Venlaw, Mimihau catchment", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: Montana", new TermImpl("GAZ:00002606", "State of Montana"));
            put("Country: UK;   State: Yorkshire;   Locality: Aire,  Nidd & Wharfe Rivers", new TermImpl("GAZ:00003688", "Yorkshire and the Humber"));
            put("Country: Hong Kong", new TermImpl("GAZ:00003203", "Hong Kong"));
            put("Country: Pacific;   State: Bay of Panama", new TermImpl("GAZ:00047280", "Panama Bay"));
            put("Country: Netherlands;   State: Wadden Sea;   Locality: Ems estuary", new TermImpl("GAZ:00008137", "Wadden See"));
            put("Country: New Zealand;   State: Otago;   Locality: North Col, Silver catchment", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: North Carolina", new TermImpl("GAZ:00002520", "State of North Carolina"));
            put("Country: USA;   State: Washington", new TermImpl("GAZ:00002553", "State of Washington"));
            put("Country: USA;   State: Alaska", new TermImpl("GAZ:00002521", "State of Alaska"));
            put("Country: USA;   State: Hawaii", new TermImpl("GAZ:00003939", "State of Hawaii"));
            put("Country: Uganda;   Locality: Lake George", new TermImpl("GAZ:00001102", "Uganda"));
            put("Country: Costa Rica;   State: Guanacaste", new TermImpl("GAZ:00003210", "Guanacaste Province"));
            put("Country: USA;   State: Massachusetts;   Locality: Cape Ann", new TermImpl("GAZ:00002537", "Commonwealth of Massachusetts"));
            put("Country: USA;   State: Maine;   Locality: Martins", new TermImpl("GAZ:00002602", "State of Maine"));
            put("Country: USA;   State: New York", new TermImpl("GAZ:00002514", "State of New York"));
            put("Country: General;   Locality: General", new TermImpl("GAZ:00000448", "geographic location"));
            put("Country: New Zealand;   State: Otago;   Locality: Stony, Sutton catchment", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: Tibet", new TermImpl("GAZ:00004219", "Tibet Autonomous Region"));
            put("Country: USA;   State: Texas;   Locality: Franklin Mtns", new TermImpl("GAZ:00002580", "State of Texas"));
            put("Country: Russia", new TermImpl("GAZ:00002721", "Russia"));
            put("Country: New Zealand;   State: Otago;   Locality: Broad, Lee catchment", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: Africa;   Locality: Lake McIlwaine", new TermImpl("GAZ:00016177", "Lake Chivero"));
            put("Country: England;   State: River Medway", new TermImpl("GAZ:00046238", "River Medway"));
            put("Country: South Africa;   Locality: Southwest coast", new TermImpl("GAZ:00001094", "Republic of South Africa"));
            put("Country: USA;   State: Kentucky", new TermImpl("GAZ:00004440", "Commonwealth of Kentucky"));
            put("Country: USA;   State: Washington;   Locality: Cape Flattery", new TermImpl("GAZ:00049988", "Cape Flattery"));
            put("Country: USA;   State: New Jersey", new TermImpl("GAZ:00002557", "State of New Jersey"));
            put("Country: India;   Locality: Rajasthan Desert", new TermImpl("GAZ:00002839", "India"));
            put("Country: England", new TermImpl("GAZ:00002641", "England"));
            put("Country: Austria;   Locality: Hafner Lake", new TermImpl("GAZ:00002942", "Austria"));
            put("Country: USA;   State:  NE USA", new TermImpl("GAZ:00002459", "United States of America"));
            put("Country: England;   Locality: Sheffield", new TermImpl("GAZ:00004871", "City of Sheffield"));
            put("Country: Uganda", new TermImpl("GAZ:00001102", "Uganda"));
            put("Country: USA;   State:  California;   Locality: Monterey Bay", new TermImpl("GAZ:00002509", "Monterey Bay"));
            put("Country: Germany", new TermImpl("GAZ:00002646", "Germany"));
            put("Country: England;   Locality: Skipwith Pond", new TermImpl("GAZ:00002641", "England"));
            put("Country: USA;   State: Wisconsin;   Locality: Little Rock Lake", new TermImpl("GAZ:00002586", "State of Wisconsin"));
            put("Country: USA;   State: California;   Locality: Coachella Valley", new TermImpl("GAZ:00002461", "State of California"));
            put("Country: Arctic", new TermImpl("GAZ:00000323", "Arctic Ocean"));
            put("Country: USA;   State: Michigan", new TermImpl("GAZ:00003152", "State of Michigan"));
            put("Country: Mexico;   State: Guerrero", new TermImpl("GAZ:00010927", "State of Guerrero"));
            put("Country: Norway;   State: Spitsbergen", new TermImpl("GAZ:00005397", "Spitzbergen"));
            put("Country: USA;   State: Kentucky;   Locality: Station 1", new TermImpl("GAZ:00004440", "Commonwealth of Kentucky"));
            put("Country: New Zealand;   State: Otago;   Locality: Kye Burn", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: New Zealand;   State: Otago;   Locality: Little Kye, Kye Burn catchment", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: North Carolina;   Locality: Pamlico", new TermImpl("GAZ:00002520", "State of North Carolina"));
            put("Country: Antarctic", new TermImpl("GAZ:00000462", "Antarctica"));
            put("Country: USA;   State: Arizona", new TermImpl("GAZ:00002518", "State of Arizona"));
            put("Country: England;   Locality: Lancaster", new TermImpl("GAZ:04000224", "City of Lancaster"));
            put("Country: USA;   State: Florida;   Locality: Everglades", new TermImpl("GAZ:00082878", "Everglades"));
            put("Country: Barbados", new TermImpl("GAZ:00001251", "Barbados"));
            put("Country: USA;   State: New York;   Locality: Bridge Brook", new TermImpl("GAZ:00002514", "State of New York"));
            put("Country: England;   Locality: Oxshott Heath", new TermImpl("GAZ:00002641", "England"));
            put("Country: New Zealand;   State: Otago;   Locality: Blackrock, Lee catchment", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: Canada;   State: Ontario", new TermImpl("GAZ:00002563", "Province of Ontario"));
            put("Country: Puerto Rico;   Locality: El Verde", new TermImpl("GAZ:00006935", "Commonwealth of Puerto Rico"));
            put("Country: Quebec", new TermImpl("GAZ:00002569", "Province of Quebec"));
            put("Country: Ireland", new TermImpl("GAZ:00002943", "Republic of Ireland"));
            put("Country: Wales;   Locality: Dee River", new TermImpl("GAZ:00007857", "River Dee [Wales]"));
            put("Country: Marshall Islands", new TermImpl("GAZ:00006470", "Republic of the Marshall Islands"));
            put("Country: New Zealand;   State: South Island;   Locality: Canton Creek, Taieri River, Lee catchment", new TermImpl("GAZ:00004764", "South Island"));
            put("Country: Seychelles", new TermImpl("GAZ:00006922", "The Seychelles"));
            put("Country: Namibia;   Locality: Namib Desert", new TermImpl("GAZ:00007516", "Namib Desert"));
            put("Country: USA;   State: Rhode Island", new TermImpl("GAZ:00002531", "State of Rhode Island"));
            put("Country: USA;   State: Idaho-Utah;   Locality: Deep Creek", new TermImpl("GAZ:00000448", "geographic location"));
            put("Country: Malawi", new TermImpl("GAZ:00001105", "Malawi"));
            put("Country: Malaysia", new TermImpl("GAZ:00003902", "GAZ:00003902"));
            put("Country: Europe;   State: Central Europe", new TermImpl("GAZ:00000464", "Europe"));
            put("Country: USA;   State: Florida", new TermImpl("GAZ:00002888", "State of Florida"));
            put("Country: Norway;   State: Oppland;   Locality: Ovre Heimdalsvatn Lake", new TermImpl("GAZ:00005244", "Oppland County"));
            put("Country: Austria;   Locality: Vorderer Finstertaler Lake", new TermImpl("GAZ:00002942", "Austria"));
            put("Country: Canada;   Locality: high Arctic", new TermImpl("GAZ:00002560", "Canada"));
            put("Country: unknown", new TermImpl("GAZ:00000448", "geographic location"));
            put("Country: Peru", new TermImpl("GAZ:00002932", "Peru"));
            put("Country: USA;   State: New England", new TermImpl("GAZ:00006323", "New England Division"));
            put("Country: Great Britain", new TermImpl("GAZ:00002637", "United Kingdom"));
            put("Country: New Zealand;   State: Otago;   Locality: German, Kye Burn catchment", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: Colorado", new TermImpl("GAZ:00006254", "State of Colorado"));
            put("Country: USA;   State: Texas;   Locality: Hueco Tanks", new TermImpl("GAZ:00002580", "State of Texas"));
            put("Country: Canada;   State: Ontario;   Locality: Mad River", new TermImpl("GAZ:00002563", "Province of Ontario"));
            put("Country: Wales;   Locality: River Rheidol", new TermImpl("GAZ:00002640", "Wales"));
            put("Country: Costa Rica;   State: de Osa", new TermImpl("GAZ:00002901", "Costa Rica"));
            put("Country: Finland", new TermImpl("GAZ:00002937", "Finland"));
            put("Country: Africa;   Locality: Crocodile Creek,  Lake Nyasa", new TermImpl("GAZ:00000058", "Lake Malawi"));
            put("Country: USA;   State: Florida;   Locality: South Florida", new TermImpl("GAZ:00004412", "Southern Florida"));
            put("Country: USA;   State: Illinois", new TermImpl("GAZ:00003142", "State of Illinois"));
            put("Country: Puerto Rico;   Locality: Puerto Rico-Virgin Islands shelf", new TermImpl("GAZ:00002822", "Puerto Rico"));
            put("Country: England;   Locality: River Thames", new TermImpl("GAZ:00007824", "River Thames"));
            put("Country: Madagascar", new TermImpl("GAZ:00006934", "Madagascar"));
            put("Country: USA;   State: New Mexico;   Locality: White Sands", new TermImpl("GAZ:00004427", "State of New Mexico"));
            put("Country: England;   Locality: River Cam", new TermImpl("GAZ:00002641", "England"));
            put("Country: Australia", new TermImpl("GAZ:00000463", "Australia"));
            put("Country: USA;   State: North Carolina;   Locality: Coweeta", new TermImpl("GAZ:00002520", "State of North Carolina"));
            put("Country: Scotland;   Locality: Ythan estuary", new TermImpl("GAZ:00002639", "Scotland"));
            put("Country: Wales;   Locality: River Clydach", new TermImpl("GAZ:00052132", "South Wales"));
            put("Country: USA;   State: Texas;   Locality: Hueco Mountains", new TermImpl("GAZ:00002580", "State of Texas"));
            put("Country: Wales", new TermImpl("GAZ:00002640", "Wales"));
            put("Country: USA;   State: Arizona;   Locality: Sonora Desert", new TermImpl("GAZ:00006847", "Sonoran Desert"));
            put("Country: England;   Locality: Silwood Park", new TermImpl("GAZ:00052254", "Silwood Park"));
            put("Country: Austria;   Locality: Neusiedler Lake", new TermImpl("GAZ:00002942", "Austria"));
            put("Country: New Zealand;   State: Otago;   Locality: Narrowdale catchment", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: California", new TermImpl("GAZ:00002461", "State of California"));
            put("Country: England;   State: Oxfordshire;   Locality: Wytham Wood", new TermImpl("GAZ:00052249", "Wytham Woods"));
            put("Country: USA;   State: Michigan;   Locality: Tuesday Lake", new TermImpl("GAZ:00003152", "State of Michigan"));
            put("Country: USA;   State: Alabama", new TermImpl("GAZ:00006881", "State of Alabama"));
            put("Country: New Zealand;   State: Otago;   Locality: Healy Stream, Taieri River, Kye Burn catchment", new TermImpl("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: New York;   Locality: Long Island", new TermImpl("GAZ:00002584", "Long Island"));
            put("Country: Venezuela", new TermImpl("GAZ:00002931", "Venezuela"));
            put("Country: New Zealand;   State: Otago;   Locality: Akatore, Akatore catchment", new TermImpl("GAZ:00004767", "Otago Region"));
        }};

        int gazHit = 0;
        for (String locality : listener.localities) {
            if (gazMap.containsKey(locality) && gazMap.get(locality).getId().startsWith("GAZ:")) {
                gazHit++;
            } else {
                System.out.println("put(\"" + locality + "\", new Term(\"externalid\", \"name\"));");
            }
        }
        assertThat(gazHit, is(listener.localities.size()));
    }


    private static class TestInteractionListener implements InteractionListener {
        public int getCount() {
            return count;
        }

        private int count = 0;
        List<Map<String, String>> invalidInteractions = new ArrayList<Map<String, String>>();
        Set<String> localities = new HashSet<String>();
        Set<String> descriptions = new HashSet<String>();
        Set<String> titles = new HashSet<String>();
        List<String> environments = new ArrayList<String>();
        List<String> publicationYears = new ArrayList<String>();

        @Override
        public void newLink(Map<String, String> properties) {
            if (properties.containsKey(StudyImporterForSPIRE.LOCALITY_ORIGINAL)) {
                localities.add(properties.get(StudyImporterForSPIRE.LOCALITY_ORIGINAL));
            }

            if (properties.containsKey(StudyConstant.DESCRIPTION)) {
                descriptions.add(properties.get(StudyConstant.DESCRIPTION));
            }
            if (properties.containsKey(StudyConstant.TITLE)) {
                titles.add(properties.get(StudyConstant.TITLE));
            }

            if (properties.containsKey(StudyConstant.TITLE)) {
                titles.add(properties.get(StudyConstant.TITLE));
            }
            if (properties.containsKey(StudyImporterForSPIRE.OF_HABITAT)) {
                environments.add(properties.get(StudyImporterForSPIRE.OF_HABITAT));
            }
            if (properties.containsKey(StudyConstant.PUBLICATION_YEAR)) {
                publicationYears.add(properties.get(StudyConstant.PUBLICATION_YEAR));
            }


            if (!StudyImporterForSPIRE.isValid(properties)) {
                invalidInteractions.add(new TreeMap<String, String>(properties));
            }

            count++;
        }
    }


}
