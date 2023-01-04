package org.eol.globi.sparql;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eol.globi.data.GraphDBNeo4j2TestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.export.ExportTestUtil;
import org.eol.globi.export.ExportUtil;
import org.eol.globi.export.ExporterRDF;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class SPARQLTest extends GraphDBNeo4j2TestCase {

    @Test
    public void executeQuerySampleGloBIData() throws NodeFactoryException, ParseException, IOException {
        ExporterRDF exporter = new ExporterRDF();
        StringWriter writer = new StringWriter();
        StudyNode study = ExportTestUtil.createTestData(nodeFactory);
        exporter.exportStudy(study, ExportUtil.AppenderWriter.of(writer, new ExportUtil.NQuadValueJoiner()), true);

        Model model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(writer.toString().getBytes("UTF-8")), null, "N-TRIPLE");

        System.out.println(writer.toString());

        String queryString =
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "SELECT ?individual WHERE { " +
                        " ?individual rdf:type <http://purl.obolibrary.org/obo/CARO_0010004> . " +
                        "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution exec = QueryExecutionFactory.create(query, model);
        try {
            ResultSet results = exec.execSelect();
            int numberOfOrganisms = 0;
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                assertThat(solution.get("individual"), is(notNullValue()));
                numberOfOrganisms++;
            }
            final int expectedNumberOfOrganisms = 3;
            assertThat(numberOfOrganisms, is(expectedNumberOfOrganisms));
        } finally {
            exec.close();
        }


    }

    @Test
    public void executeQuerySampleData() {
        Model model = ModelFactory.createDefaultModel();
        model.read(getClass().getResourceAsStream("data.ttl"), null, "TTL");

        String queryString =
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                        "SELECT ?name WHERE { " +
                        " ?person foaf:mbox <mailto:alice@example.org> . " +
                        " ?person foaf:name ?name . " +
                        "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution exec = QueryExecutionFactory.create(query, model);
        try {
            ResultSet results = exec.execSelect();
            int counter = 0;
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                Literal name = solution.getLiteral("name");
                assertThat(name.toString(), is("Alice"));
                counter++;
            }
            assertThat(counter, is(1));
        } finally {
            exec.close();
        }
    }


}
