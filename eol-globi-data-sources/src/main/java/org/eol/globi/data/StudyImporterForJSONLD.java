package org.eol.globi.data;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.ResourceUtil;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StudyImporterForJSONLD extends BaseStudyImporter {

    public URI getResourceURI() {
        return getDataset().getConfigURI();
    }

    public StudyImporterForJSONLD(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Model model;
        try {
            model = buildModel();
        } catch (IOException e) {
            throw new StudyImporterException("failed to import [" + getResourceURI() + "]", e);
        }

        Query query;
        try {
            query = QueryFactory.create(IOUtils.toString(ResourceUtil.asInputStream("/org/eol/globi/data/find-jsonld-interactions.rq", getClass()), CharsetConstant.UTF8));
        } catch (IOException e) {
            throw new StudyImporterException("failed to find sparql query", e);
        }

        QueryExecution exec = QueryExecutionFactory.create(query, model);
        try {
            ResultSet results = exec.execSelect();
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                String subj = solution.get("subj").asResource().getURI();
                String creationDate = solution.get("creationDate").asLiteral().getString();
                String authorURI = solution.get("author").toString();
                String author;
                try {
                    author = nodeFactory.getAuthorResolver().findFullName(authorURI);
                } catch (IOException e) {
                    throw new StudyImporterException("failed to resolve author URI [" + authorURI + "]");
                }
                Study study = nodeFactory.getOrCreateStudy(getResourceURI() + subj, author + ". " + new DateTime(parseDate(creationDate)).getYear() + ". " + ReferenceUtil.createLastAccessedString(getResourceURI().toString()), subj);
                study.setExternalId(subj);
                Specimen source = createSpecimen(solution, study, "subjTaxon");
                Specimen target = createSpecimen(solution, study, "targetTaxon");

                String interactType = solution.get("p").asResource().getLocalName();
                InteractType interactType1 = InteractType.typeOf(StringUtils.replace(interactType, "RO_", "RO:"));
                if (interactType1 == null) {
                    throw new StudyImporterException("failed to map interaction type [" + interactType + "]");
                }
                String collTime = solution.get("collTime").asLiteral().getString();
                Date date = parseDate(collTime);
                nodeFactory.setUnixEpochProperty(source, date);
                                    nodeFactory.setUnixEpochProperty(target, date);
                Location loc = nodeFactory.getOrCreateLocation(solution.get("collLat").asLiteral().getDouble(),
                        solution.get("collLng").asLiteral().getDouble(), null);
                target.caughtIn(loc);
                source.caughtIn(loc);
                source.interactsWith(target,interactType1);
            }
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import jsonld data in [" + getResourceURI() + "]", e);
        } finally {
            exec.close();
        }
        return null;
    }

    private Date parseDate(String collTime) throws StudyImporterException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-DD");
        try {
            return dateFormat.parse(collTime);
        } catch (ParseException e) {
            throw new StudyImporterException("not setting collection date, because [" + collTime + "] could not be read as date.", e);
        }
    }

    protected Specimen createSpecimen(QuerySolution solution, Study study, String targetTaxon1) throws NodeFactoryException {
        String targetTaxon = solution.get(targetTaxon1).asResource().getLocalName();
        String taxonId = targetTaxon.replaceAll("NCBITaxon_", TaxonomyProvider.NCBI.getIdPrefix());
        return nodeFactory.createSpecimen(study, null, taxonId);
    }

    private Model buildModel() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        model.read(ResourceUtil.asInputStream(getResourceURI(), getClass()), getResourceURI().toString(), "JSON-LD");
        return model;
    }

}
