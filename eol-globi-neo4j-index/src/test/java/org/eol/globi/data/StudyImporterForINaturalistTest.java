package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.InteractTypeMapperFactoryImpl;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetFinderException;
import org.eol.globi.util.NodeUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.StudyImporterForINaturalist.PREFIX_OBSERVATION_FIELD;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForINaturalistTest extends GraphDBTestCase {

    protected StudyImporterForINaturalist importer;

    @Before
    public void setup() throws DatasetFinderException {
        Dataset dataset = datasetFor("globalbioticinteractions/inaturalist");
        ParserFactory parserFactory = new ParserFactoryForDataset(dataset);
        importer = new StudyImporterForINaturalist(parserFactory, nodeFactory);
        importer.setDataset(dataset);
    }

    @Test
    public void importNotSupportedTestResponse() throws IOException, StudyImporterException {
        final ArrayList<String> typesIgnored = new ArrayList<>();
        final HashMap<String, InteractType> typeMap = new HashMap<>();
        importer.parseJSON(getClass().getResourceAsStream("inaturalist/unsupported_interaction_type_inaturalist_response.json"),
                InteractTypeMapperFactoryImpl.getTermLookupService(typesIgnored, typeMap));
        resolveNames();
        Study study = nodeFactory.findStudy("INAT:45209");
        assertThat(study, is(nullValue()));
    }

    @Test
    public void importTestResponse() throws StudyImporterException {
        final ArrayList<String> typesIgnored = new ArrayList<String>() {{
            add(PREFIX_OBSERVATION_FIELD + 47);
        }};
        final HashMap<String, InteractType> typeMap = new HashMap<String, InteractType>() {
            {
                put(PREFIX_OBSERVATION_FIELD + 13, InteractType.ATE);
            }
        };
        importer.parseJSON(getClass().getResourceAsStream("inaturalist/sample_inaturalist_response.json"),
                InteractTypeMapperFactoryImpl.getTermLookupService(typesIgnored, typeMap));
        resolveNames();

        assertThat(NodeUtil.findAllStudies(getGraphDb()).size(), is(22));

        Study anotherStudy = nodeFactory.findStudy("INAT:831");
        assertThat(anotherStudy, is(notNullValue()));
        assertThat(anotherStudy.getCitation(), containsString("Ken-ichi Ueda. 2008. Argiope eating Orthoptera. iNaturalist.org. Accessed at <https://www.inaturalist.org/observations/831> on "));
        assertThat(anotherStudy.getExternalId(), is("https://www.inaturalist.org/observations/831"));

        anotherStudy = nodeFactory.findStudy("INAT:97380");
        assertThat(anotherStudy, is(notNullValue()));
        assertThat(anotherStudy.getCitation(), containsString("annetanne. 2012. Misumena vatia eating Eristalis nemorum."));
        assertThat(anotherStudy.getExternalId(), is("https://www.inaturalist.org/observations/97380"));

        Taxon sourceTaxonNode = taxonIndex.findTaxonByName("Arenaria interpres");

        assertThat(sourceTaxonNode, is(not(nullValue())));

        Transaction transaction = getGraphDb().beginTx();
        try {
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
                assertThat((Long) collectedRel.getProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH), is(any(Long.class)));

            }
            transaction.success();
        } finally {
            transaction.close();
        }
    }

    @Test
    public void parseTaxon() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode array;
        array = mapper.readTree(getClass().getResourceAsStream("inaturalist/response_with_taxon_ids.json"));
        JsonNode first = array.get(0);
        JsonNode taxonNode = first.get("taxon");
        Taxon targetTaxon = StudyImporterForINaturalist.parseTaxon(taxonNode);
        Taxon sourceTaxon = StudyImporterForINaturalist.parseTaxon(first.get("observation").get("taxon"));
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
        final HashMap<String, InteractType> typeMap = new HashMap<String, InteractType>() {
            {
                put(PREFIX_OBSERVATION_FIELD + 47, InteractType.HAS_HOST);
            }
        };
        importer.parseJSON(getClass().getResourceAsStream("inaturalist/response_with_taxon_ids.json"),
                InteractTypeMapperFactoryImpl.getTermLookupService(typesIgnored, typeMap));
        resolveNames();
        assertThat(NodeUtil.findAllStudies(getGraphDb()).size(), is(10));

        Study anotherStudy = nodeFactory.findStudy("INAT:2366807");
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
        LabeledCSVParser labeledCSVParser = importer.parserFactory.createParser(InteractTypeMapperFactoryImpl.TYPE_IGNORED_URI_DEFAULT, CharsetConstant.UTF8);
        List<String> typeMap1 = InteractTypeMapperFactoryImpl.buildTypesIgnored(labeledCSVParser, "observation_field_id");

        assertThat(typeMap1.contains(PREFIX_OBSERVATION_FIELD + 13), is(false));
        assertThat(typeMap1.contains(PREFIX_OBSERVATION_FIELD + 1378), is(true));
    }

    @Ignore
    @Test
    public void loadInteractionMap() throws IOException, TermLookupServiceException {
        URI resourceName = InteractTypeMapperFactoryImpl.TYPE_MAP_URI_DEFAULT;
        LabeledCSVParser labeledCSVParser = importer.parserFactory.createParser(resourceName, CharsetConstant.UTF8);
        Map<String, InteractType> typeMap = InteractTypeMapperFactoryImpl.buildTypeMap(labeledCSVParser, "observation_field_id", "observation_field_name", "interaction_type_id");

        assertThat(typeMap.get(PREFIX_OBSERVATION_FIELD + 13), is(InteractType.ATE));
        assertThat(typeMap.get(PREFIX_OBSERVATION_FIELD + 1685), is(InteractType.ATE));
        assertThat(typeMap.get(PREFIX_OBSERVATION_FIELD + 839), is(InteractType.PREYS_UPON));

    }


}
