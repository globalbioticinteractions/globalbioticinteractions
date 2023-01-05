package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.InteractTypeMapperFactoryImpl;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.data.DatasetImporterForINaturalist.PREFIX_OBSERVATION_FIELD;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class DatasetImporterForINaturalistTest extends GraphDBNeo4jTestCase {

    protected DatasetImporterForINaturalist importer;

    @Before
    public void setup() throws DatasetRegistryException {
        Dataset dataset = datasetFor("globalbioticinteractions/inaturalist");
        ParserFactory parserFactory = new ParserFactoryForDataset(dataset);
        importer = new DatasetImporterForINaturalist(parserFactory, nodeFactory);
        importer.setDataset(dataset);
    }

    @Test
    public void parseNullLocation() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.set("latitude", null);
        objectNode.set("longitude", null);

        Location location = DatasetImporterForINaturalist.parseLocationNode(objectNode);
        assertThat(location, is(nullValue()));
    }

    @Test
    public void parseLocation() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("latitude", "10.3");
        objectNode.put("longitude", "12.2");

        Location location = DatasetImporterForINaturalist.parseLocationNode(objectNode);
        assertThat(location, is(notNullValue()));
        assertThat(location.getLatitude(), is(10.3));
        assertThat(location.getLongitude(), is(12.2));
    }

    @Test
    public void importNotSupportedTestResponse() throws IOException, StudyImporterException {
        final ArrayList<String> typesIgnored = new ArrayList<>();
        final TreeMap<String, InteractType> typeMap = new TreeMap<>();
        importer.parseJSON(getClass().getResourceAsStream("inaturalist/unsupported_interaction_type_inaturalist_response.json"),
                InteractTypeMapperFactoryImpl.getTermLookupService(getTermLookupServiceNoOp(), typeMap));
        resolveNames();
        Study study = nodeFactory.findStudy(new StudyImpl("INAT:45209"));
        assertThat(study, is(nullValue()));
    }

    public TermLookupService getTermLookupServiceNoOp() {
        return new TermLookupService() {
            @Override
            public List<Term> lookupTermByName(String name) throws TermLookupServiceException {
                return null;
            }
        };
    }

    @Test
    public void importTestResponse() throws StudyImporterException {
        final ArrayList<String> typesIgnored = new ArrayList<String>() {{
            add(PREFIX_OBSERVATION_FIELD + 47);
        }};
        final TreeMap<String, InteractType> typeMap = new TreeMap<String, InteractType>() {
            {
                put(PREFIX_OBSERVATION_FIELD + 13, InteractType.ATE);
            }
        };
        importer.parseJSON(getClass().getResourceAsStream("inaturalist/sample_inaturalist_response.json"),
                InteractTypeMapperFactoryImpl.getTermLookupService(getTermLookupServiceNoOp(), typeMap));
        resolveNames();

        assertThat(NodeUtil.findAllStudies(getGraphDb()).size(), is(22));

        StudyImpl study = new StudyImpl("INAT:831");
        study.setExternalId("https://www.inaturalist.org/observations/831");
        Study anotherStudy = nodeFactory.findStudy(study);
        assertThat(anotherStudy, is(notNullValue()));
        assertThat(anotherStudy.getCitation(), containsString("Ken-ichi Ueda. 2008. Argiope eating Orthoptera. iNaturalist.org. Accessed at <https://www.inaturalist.org/observations/831> on "));
        assertThat(anotherStudy.getExternalId(), is("https://www.inaturalist.org/observations/831"));

        StudyImpl study1 = new StudyImpl("INAT:97380");
        study1.setExternalId("https://www.inaturalist.org/observations/97380");
        anotherStudy = nodeFactory.findStudy(study1);
        assertThat(anotherStudy, is(notNullValue()));
        assertThat(anotherStudy.getCitation(), containsString("annetanne. 2012. Misumena vatia eating Eristalis nemorum."));
        assertThat(anotherStudy.getExternalId(), is("https://www.inaturalist.org/observations/97380"));

        Taxon sourceTaxonNode = taxonIndex.findTaxonByName("Arenaria interpres");

        assertThat(sourceTaxonNode, is(not(nullValue())));

        Iterable<Relationship> relationships = ((NodeBacked) sourceTaxonNode).getUnderlyingNode().getRelationships(Direction.INCOMING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));
        for (Relationship relationship : relationships) {
            Node sourceSpecimen = relationship.getStartNode();

            assertThat(new SpecimenNode(sourceSpecimen).getBasisOfRecord().getName(), is("HumanObservation"));
            assertThat(new SpecimenNode(sourceSpecimen).getBasisOfRecord().getId(), is("TEST:HumanObservation"));
            assertThat(new SpecimenNode(sourceSpecimen).getExternalId(), containsString(TaxonomyProvider.ID_PREFIX_INATURALIST));
            Relationship ateRel = sourceSpecimen.getSingleRelationship(NodeUtil.asNeo4j(InteractType.ATE), Direction.OUTGOING);
            Node preySpecimen = ateRel.getEndNode();
            assertThat(preySpecimen, is(not(nullValue())));
            Relationship preyClassification = preySpecimen.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING);
            String actualPreyName = (String) preyClassification.getEndNode().getProperty("name");
            assertThat(actualPreyName, is("Crepidula fornicata"));

            Relationship locationRel = sourceSpecimen.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.COLLECTED_AT), Direction.OUTGOING);
            assertThat(locationRel.getEndNode().getProperty("latitude"), is(41.249813));
            assertThat(locationRel.getEndNode().getProperty("longitude"), is(-72.542556));

            Relationship collectedRel = sourceSpecimen.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.COLLECTED), Direction.INCOMING);
            assertThat((String) collectedRel.getProperty(SpecimenConstant.EVENT_DATE), is(any(String.class)));

        }
    }

    @Test
    public void parseTaxon() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode array;
        array = mapper.readTree(getClass().getResourceAsStream("inaturalist/response_with_taxon_ids.json"));
        JsonNode first = array.get(0);
        JsonNode taxonNode = first.get("taxon");
        Taxon targetTaxon = DatasetImporterForINaturalist.parseTaxon(taxonNode);
        Taxon sourceTaxon = DatasetImporterForINaturalist.parseTaxon(first.get("observation").get("taxon"));
        resolveNames();

        assertThat(targetTaxon.getName(), is("Sophora prostrata"));
        assertThat(targetTaxon.getExternalId(), is("INAT_TAXON:406089"));
        assertThat(sourceTaxon.getName(), is("Pseudoidium hardenbergiae"));
        assertThat(sourceTaxon.getExternalId(), is("INAT_TAXON:480390"));

    }

    @Test
    public void importTestResponseWithTaxonId() throws IOException, StudyImporterException {
        final ArrayList<String> typesIgnored = new ArrayList<String>() {{
        }};
        final TreeMap<String, InteractType> typeMap = new TreeMap<String, InteractType>() {
            {
                put(PREFIX_OBSERVATION_FIELD + 47, InteractType.HAS_HOST);
            }
        };
        importer.parseJSON(getClass().getResourceAsStream("inaturalist/response_with_taxon_ids.json"),
                InteractTypeMapperFactoryImpl.getTermLookupService(getTermLookupServiceNoOp(), typeMap));
        resolveNames();
        assertThat(NodeUtil.findAllStudies(getGraphDb()).size(), is(10));

        StudyImpl study = new StudyImpl("INAT:2366807");
        study.setExternalId("https://www.inaturalist.org/observations/2366807");
        Study anotherStudy = nodeFactory.findStudy(study);
        assertThat(anotherStudy, is(notNullValue()));
        assertThat(anotherStudy.getExternalId(), is("https://www.inaturalist.org/observations/2366807"));

        assertThat(taxonIndex.findTaxonById("GBIF:2959023"), is(nullValue()));
        assertThat(taxonIndex.findTaxonById("GBIF:7246356"), is(nullValue()));
        assertThat(taxonIndex.findTaxonById("INAT_TAXON:406089"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonById("INAT_TAXON:480390"), is(notNullValue()));
    }

    @Ignore
    @Test
    public void loadIgnoredInteractions() throws IOException {
        LabeledCSVParser labeledCSVParser = importer.getParserFactory().createParser(InteractTypeMapperFactoryImpl.TYPE_IGNORED_URI_DEFAULT, CharsetConstant.UTF8);
        List<String> typeMap1 = InteractTypeMapperFactoryImpl.buildTypesIgnored(labeledCSVParser, "observation_field_id");

        assertThat(typeMap1.contains(PREFIX_OBSERVATION_FIELD + 13), is(false));
        assertThat(typeMap1.contains(PREFIX_OBSERVATION_FIELD + 1378), is(true));
    }

    @Ignore
    @Test
    public void loadInteractionMap() throws IOException, TermLookupServiceException {
        URI resourceName = InteractTypeMapperFactoryImpl.TYPE_MAP_URI_DEFAULT;
        LabeledCSVParser labeledCSVParser = importer.getParserFactory().createParser(resourceName, CharsetConstant.UTF8);
        Map<String, InteractType> typeMap = InteractTypeMapperFactoryImpl.buildTypeMap(labeledCSVParser, "observation_field_id", "observation_field_name", "interaction_type_id");

        assertThat(typeMap.get(PREFIX_OBSERVATION_FIELD + 13), is(InteractType.ATE));
        assertThat(typeMap.get(PREFIX_OBSERVATION_FIELD + 1685), is(InteractType.ATE));
        assertThat(typeMap.get(PREFIX_OBSERVATION_FIELD + 839), is(InteractType.PREYS_UPON));

    }


}
