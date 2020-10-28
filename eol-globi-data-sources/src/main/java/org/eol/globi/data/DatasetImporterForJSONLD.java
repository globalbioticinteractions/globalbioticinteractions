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
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.DatasetLocal;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;

public class DatasetImporterForJSONLD extends NodeBasedImporter {

    public URI getResourceURI() {
        return getDataset().getConfigURI();
    }

    public DatasetImporterForJSONLD(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        Model model;
        try {
            model = buildModel();
        } catch (Throwable e) {
            // note that Jena APIs use RuntimeException instead of checked Exceptions
            // making an not-so-preferred catch(Throwable) needed to avoid crashing the entire pipeline
            // because of a non-critical Jena exception.
            throw new StudyImporterException("failed to import [" + getResourceURI() + "]", e);
        }

        Query query;
        try (InputStream resource = new DatasetLocal(inStream -> inStream).retrieve(URI.create("find-jsonld-interactions.rq"))) {
            query = QueryFactory.create(IOUtils.toString(resource, CharsetConstant.UTF8));
        } catch (Throwable e) {
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
                    author = getNodeFactory().getAuthorResolver().findFullName(authorURI);
                } catch (IOException e) {
                    throw new StudyImporterException("failed to query author URI [" + authorURI + "]");
                }
                final String source1 = author + ". " + new DateTime(parseDate(creationDate)).getYear() + ". " + CitationUtil.createLastAccessedString(getResourceURI().toString());
                Study study = getNodeFactory().getOrCreateStudy(new StudyImpl(getResourceURI() + subj, null, subj));
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
                getNodeFactory().setUnixEpochProperty(source, date);
                getNodeFactory().setUnixEpochProperty(target, date);
                Location loc = getNodeFactory().getOrCreateLocation(
                        new LocationImpl(solution.get("collLat").asLiteral().getDouble(), solution.get("collLng").asLiteral().getDouble(), null, null));
                target.caughtIn(loc);
                source.caughtIn(loc);
                source.interactsWith(target, interactType1);
            }
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import jsonld data in [" + getResourceURI() + "]", e);
        } finally {
            exec.close();
        }
    }

    private Date parseDate(String collTime) throws StudyImporterException {
        try {
            return org.eol.globi.util.DateUtil.parsePatternUTC(collTime, "yy-MM-DD").toDate();
        } catch (IllegalArgumentException e) {
            throw new StudyImporterException("not setting collection date, because [" + collTime + "] could not be read as date.", e);
        }
    }

    protected Specimen createSpecimen(QuerySolution solution, Study study, String targetTaxon1) throws NodeFactoryException {
        String targetTaxon = solution.get(targetTaxon1).asResource().getLocalName();
        String taxonId = targetTaxon.replaceAll("NCBITaxon_", TaxonomyProvider.NCBI.getIdPrefix());
        return getNodeFactory().createSpecimen(study, new TaxonImpl(null, taxonId));
    }

    private Model buildModel() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream resource = getDataset().retrieve(getResourceURI())) {
            model.read(resource, getResourceURI().toString(), "JSON-LD");
        }
        return model;
    }

}
