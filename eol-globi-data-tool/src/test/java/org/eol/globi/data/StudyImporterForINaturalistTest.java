package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceUtil;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForINaturalistTest extends GraphDBTestCase {

    private StudyImporterForINaturalist importer;

    @Before
    public void setup() {
        importer = new StudyImporterForINaturalist(null, nodeFactory);
    }

    @Test
    public void config() {
        assertThat(importer.shouldCrossCheckReference(), is(false));
    }

    @Test
    public void importUsingINatAPI() throws StudyImporterException, PropertyEnricherException {
        importStudy(importer);
        assertThat(NodeUtil.findAllStudies(getGraphDb()).size() > 150, is(true));
    }

    @Test
    public void loadInteractionMap() throws IOException {
        InputStream is = ResourceUtil.asInputStream(StudyImporterForINaturalist.TYPE_MAP_URI_DEFAULT, null);
        Map<Integer, InteractType> typeMap = StudyImporterForINaturalist.buildTypeMap(StudyImporterForINaturalist.TYPE_MAP_URI_DEFAULT, is);

        assertThat(typeMap.get(13), is(InteractType.ATE));
        assertThat(typeMap.get(1685), is(InteractType.ATE));
        assertThat(typeMap.get(839), is(InteractType.PREYS_UPON));

    }

    @Test
    public void loadIgnoredInteractions() throws IOException {
        String resource = StudyImporterForINaturalist.TYPE_IGNORED_URI_DEFAULT;
        InputStream is = ResourceUtil.asInputStream(resource, null);
        List<Integer> typeMap1 = StudyImporterForINaturalist.buildTypesIgnored(is);

        assertThat(typeMap1.contains(13), is(false));
        assertThat(typeMap1.contains(1378), is(true));
    }

    @Test
    public void importNotSupportedTestResponse() throws IOException, NodeFactoryException, StudyImporterException {
        importer.parseJSON(getClass().getResourceAsStream("inaturalist/unsupported_interaction_type_inaturalist_response.json"),
                new ArrayList<Integer>(),
                new HashMap<Integer, InteractType>());
        resolveNames();
        assertThat(nodeFactory.findStudy("INAT:45209"), is(nullValue()));
    }

    @Test
    public void importTestResponse() throws IOException, NodeFactoryException, StudyImporterException {
        importer.parseJSON(getClass().getResourceAsStream("inaturalist/sample_inaturalist_response.json"),
                new ArrayList<Integer>() {{
                    add(47);
                }},
                new HashMap<Integer, InteractType>() {
                    {
                        put(13, InteractType.ATE);
                    }
                });
        resolveNames();

        assertThat(NodeUtil.findAllStudies(getGraphDb()).size(), is(22));

        Study anotherStudy = nodeFactory.findStudy("INAT:831");
        assertThat(anotherStudy, is(notNullValue()));
        assertThat(anotherStudy.getCitation(), containsString("Ken-ichi Ueda. 2008. Argiope eating Orthoptera. iNaturalist.org. Accessed at http://www.inaturalist.org/observations/831 on "));
        assertThat(anotherStudy.getExternalId(), is("http://www.inaturalist.org/observations/831"));

        anotherStudy = nodeFactory.findStudy("INAT:97380");
        assertThat(anotherStudy, is(notNullValue()));
        assertThat(anotherStudy.getCitation(), containsString("annetanne. 2012. Misumena vatia eating Eristalis nemorum."));
        assertThat(anotherStudy.getExternalId(), is("http://www.inaturalist.org/observations/97380"));


        TaxonNode sourceTaxonNode = taxonIndex.findTaxonByName("Arenaria interpres");

        assertThat(sourceTaxonNode, is(not(nullValue())));
        Iterable<Relationship> relationships = sourceTaxonNode.getUnderlyingNode().getRelationships(Direction.INCOMING, RelTypes.CLASSIFIED_AS);
        for (Relationship relationship : relationships) {
            Node sourceSpecimen = relationship.getStartNode();

            assertThat(new Specimen(sourceSpecimen).getBasisOfRecord().getName(), is("HumanObservation"));
            assertThat(new Specimen(sourceSpecimen).getBasisOfRecord().getId(), is("TEST:HumanObservation"));
            assertThat(new Specimen(sourceSpecimen).getExternalId(), containsString(TaxonomyProvider.ID_PREFIX_INATURALIST));
            Relationship ateRel = sourceSpecimen.getSingleRelationship(InteractType.ATE, Direction.OUTGOING);
            Node preySpecimen = ateRel.getEndNode();
            assertThat(preySpecimen, is(not(nullValue())));
            Relationship preyClassification = preySpecimen.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
            String actualPreyName = (String) preyClassification.getEndNode().getProperty("name");
            assertThat(actualPreyName, is("Crepidula fornicata"));

            Relationship locationRel = sourceSpecimen.getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING);
            assertThat((Double) locationRel.getEndNode().getProperty("latitude"), is(41.249813));
            assertThat((Double) locationRel.getEndNode().getProperty("longitude"), is(-72.542556));

            Relationship collectedRel = sourceSpecimen.getSingleRelationship(RelTypes.COLLECTED, Direction.INCOMING);
            assertThat((Long) collectedRel.getProperty(Specimen.DATE_IN_UNIX_EPOCH), is(any(Long.class)));

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
        assertThat(targetTaxon.getExternalId(), is("GBIF:2959023"));
        assertThat(sourceTaxon.getName(), is("Pseudoidium hardenbergiae"));
        assertThat(sourceTaxon.getExternalId(), is("GBIF:7246356"));

    }

    @Test
    public void importTestResponseWithTaxonId() throws IOException, NodeFactoryException, StudyImporterException {
        importer.parseJSON(getClass().getResourceAsStream("inaturalist/response_with_taxon_ids.json"),
                new ArrayList<Integer>() {{
                }},
                new HashMap<Integer, InteractType>() {
                    {
                        put(47, InteractType.HAS_HOST);
                    }
                });
        resolveNames();
        assertThat(NodeUtil.findAllStudies(getGraphDb()).size(), is(10));

        Study anotherStudy = nodeFactory.findStudy("INAT:2366807");
        assertThat(anotherStudy, is(notNullValue()));
        assertThat(anotherStudy.getExternalId(), is("http://www.inaturalist.org/observations/2366807"));

        assertThat(taxonIndex.findTaxonById("GBIF:2959023"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonById("GBIF:7246356"), is(notNullValue()));
    }

}
