package org.eol.globi.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonPropertyLookupServiceException;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForINaturalistTest extends GraphDBTestCase {
    private static final Log LOG = LogFactory.getLog(StudyImporterForINaturalistTest.class);

    private StudyImporterForINaturalist importer;

    @Before
    public void setup() {
        importer = new StudyImporterForINaturalist(null, nodeFactory);

    }

    @Test
    public void importUsingINatAPI() throws StudyImporterException, TaxonPropertyLookupServiceException {
        Study testStudy = nodeFactory.createStudy("testing");

        int pageNumber = 1;
        int totalInteractions = 0;
        DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
        int previousResultCount;
        do {
            String uri = "http://www.inaturalist.org/observation_field_values.json?type=taxon&page=" + pageNumber + "&per_page=100&license=any";
            HttpGet get = new HttpGet(uri);
            get.addHeader("accept", "application/json");
            try {
                HttpResponse response = defaultHttpClient.execute(get);
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new StudyImporterException("failed to execute query to [ " + uri + "]: status code [" + response.getStatusLine().getStatusCode() + "]");
                }
                previousResultCount = importer.parseJSON(response.getEntity().getContent(), testStudy);
                pageNumber++;
                totalInteractions+= previousResultCount;
                LOG.info("importing [" + pageNumber + "] total [" + totalInteractions + "]");

            } catch (IOException e) {
                throw new StudyImporterException("failed to execute query to [ " + uri + "]", e);
            }

        } while (previousResultCount > 0);

        assertThat(totalInteractions > 150, is(true));

    }


    @Test
    public void importTestResponse() throws IOException, NodeFactoryException, StudyImporterException {

        Study study = importer.importStudy();

        Iterable<Relationship> specimens = study.getSpecimens();
        int count = 0;
        for (Relationship specimen : specimens) {
            count++;
        }
        assertThat(count, is(30));

        Taxon sourceTaxonNode = nodeFactory.findTaxon("Crepidula fornicata");

        assertThat(sourceTaxonNode, is(not(nullValue())));
        Iterable<Relationship> relationships = sourceTaxonNode.getUnderlyingNode().getRelationships(Direction.INCOMING, RelTypes.CLASSIFIED_AS);
        for (Relationship relationship : relationships) {
            Node predatorSpecimen = relationship.getStartNode();
            Relationship ateRel = predatorSpecimen.getSingleRelationship(InteractType.ATE, Direction.OUTGOING);
            Node preySpecimen = ateRel.getEndNode();
            assertThat(preySpecimen, is(not(nullValue())));
            Relationship preyClassification = preySpecimen.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
            assertThat((String) preyClassification.getEndNode().getProperty("name"), is(any(String.class)));

            Relationship locationRel = predatorSpecimen.getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING);
            if (locationRel != null) {
                assertThat((Double) locationRel.getEndNode().getProperty("latitude"), is(any(Double.class)));
                assertThat((Double) locationRel.getEndNode().getProperty("longitude"), is(any(Double.class)));
            }

            Relationship collectedRel = predatorSpecimen.getSingleRelationship(RelTypes.COLLECTED, Direction.INCOMING);
            assertThat((Long) collectedRel.getProperty(Specimen.DATE_IN_UNIX_EPOCH), is(any(Long.class)));

            assertThat((String) collectedRel.getStartNode().getProperty(Study.CONTRIBUTOR), is("Ken-ichi Kueda"));
        }
    }

}
