package org.eol.globi.server;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.service.TaxonRichnessLookup;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Controller
public class DietNicheWidthController {

    private static final Log LOG = LogFactory.getLog(DietNicheWidthController.class);

    @Autowired
    private GraphDatabaseService graphDb;


    @RequestMapping(value = "/dietNicheWidth/{consumerTaxonName}", method = RequestMethod.GET)
    public void calculateDietaryNicheWidth(@PathVariable("consumerTaxonName") String consumerTaxonName, HttpServletResponse response) throws IOException {
        OutputStream os = response.getOutputStream();
        LOG.info("dietary niche width for [" + consumerTaxonName + "] calculating...");
        calculate(consumerTaxonName, os);
        LOG.info("calculating dietary niche width for [" + consumerTaxonName + "] calculation done.");
        os.flush();
    }

    public void calculate(final String consumerTaxonName, OutputStream outputStream) throws IOException {
        ExecutionEngine executionEngine = new ExecutionEngine(graphDb);
        int numberOfPreyAcrossAllSpecimen = countDistinctPreyTaxa(executionEngine, createStartClause(consumerTaxonName));
        Collection<String> consumerTaxa = getDistinctConsumerTaxa(executionEngine, createStartClause(consumerTaxonName));
        if (numberOfPreyAcrossAllSpecimen > 1) {
            calculateDietNicheWidth(outputStream, executionEngine, numberOfPreyAcrossAllSpecimen, consumerTaxa);
        }
    }

    private void calculateDietNicheWidth(OutputStream outputStream, ExecutionEngine executionEngine, int numberOfPreyAcrossAllSpecimen, Collection<String> consumerTaxa) throws IOException {
        writeHeader(outputStream);
        boolean isFirst = true;
        TaxonRichnessLookup taxonRichnessLookup = new TaxonRichnessLookup();
        for (String consumerName : consumerTaxa) {
            String query = findPreyForConsumerSpecimenQuery(executionEngine, createStartClause(consumerName));
            LOG.info("niche width for [" + consumerName + "] executing...");
            ExecutionResult results = executionEngine.execute(query.toString());
            LOG.info("niche width for [" + consumerName + "] getting results...");

            for (Map<String, Object> result : results) {
                if (!isFirst) {
                    IOUtils.write("\n", outputStream, "UTF-8");
                }
                inferNicheWidthDiversityAndWrite(result, outputStream, numberOfPreyAcrossAllSpecimen, taxonRichnessLookup);
                isFirst = false;
            }
            LOG.info("niche width for [" + consumerName + "] done.");

        }
    }

    private String createStartClause(String consumerTaxonName) {
        return "START consumerTaxon = node:taxonpaths('path:\"" + consumerTaxonName + "\"')";
    }

    private String findPreyForConsumerSpecimenQuery(ExecutionEngine executionEngine, String startClause) {
        StringBuilder query = new StringBuilder();
        query.append(startClause);
        query.append(" MATCH study-[:COLLECTED]->consumerSpecimen-[:COLLECTED_AT]->loc, consumerTaxon<-[:CLASSIFIED_AS]-consumerSpecimen-[:ATE|PREYS_ON]->preySpecimen-[:CLASSIFIED_AS]->preyTaxon");
        query.append(" RETURN id(study) as studyId, id(consumerSpecimen) as consumerSpecimenId, consumerTaxon.name as consumerName, count(distinct(preyTaxon.name)) as numberOfDistinctPreyItems, loc.latitude as lat, loc.longitude as lng");
        query.append(" LIMIT 50");
        return query.toString();
    }

    private static void writeHeader(OutputStream os) throws IOException {
        String header = "study_id,specimen_id,consumer_name,lat,lng,number_of_distinct_prey_taxa,dietary_niche_width,diversity";
        IOUtils.write(header, os, "UTF-8");
        IOUtils.write("\n", os, "UTF-8");
    }

    private void inferNicheWidthDiversityAndWrite(Map<String, Object> result, OutputStream outputStream, int numberOfPreyAcrossAllSpecimen, TaxonRichnessLookup taxonRichnessLookup) throws IOException {
        IOUtils.write(result.get("studyId").toString(), outputStream, "UTF-8");
        writeSeparator(outputStream);
        IOUtils.write(result.get("consumerSpecimenId").toString(), outputStream, "UTF-8");
        writeSeparator(outputStream);
        IOUtils.write(result.get("consumerName").toString(), outputStream, "UTF-8");
        writeSeparator(outputStream);
        Double lat = (Double) result.get("lat");
        IOUtils.write(lat.toString(), outputStream, "UTF-8");
        writeSeparator(outputStream);
        Double lng = (Double) result.get("lng");
        IOUtils.write(lng.toString(), outputStream, "UTF-8");
        writeSeparator(outputStream);
        Long numberOfDistinctPreyItems = (Long) result.get("numberOfDistinctPreyItems");
        IOUtils.write(numberOfDistinctPreyItems.toString(), outputStream, "UTF-8");
        writeSeparator(outputStream);
        final Double nicheWidth = calculateDietaryNicheWidth(numberOfPreyAcrossAllSpecimen, numberOfDistinctPreyItems);
        IOUtils.write(nicheWidth.toString(), outputStream, "UTF-8");
        writeSeparator(outputStream);
        Double richness = taxonRichnessLookup.lookupRichness(lat, lng);
        IOUtils.write(richness.toString(), outputStream, "UTF-8");
    }

    private void writeSeparator(OutputStream outputStream) throws IOException {
        IOUtils.write(",", outputStream, "UTF-8");
    }

    protected static double calculateDietaryNicheWidth(double numberOfPreyAcrossAllSpecimen, double numberOfDistinctPreyItems) {
        double proportion = 1.0 / numberOfDistinctPreyItems;
        double sumOfProportionSquares = numberOfDistinctPreyItems * (proportion * proportion);
        return (1.0 / sumOfProportionSquares - 1.0) / (numberOfPreyAcrossAllSpecimen - 1.0);
    }

    private int countDistinctPreyTaxa(ExecutionEngine executionEngine, String startClause) {
        StringBuilder query = new StringBuilder(startClause);
        query.append(" MATCH consumerTaxon<-[:CLASSIFIED_AS]-consumerSpecimen-[:ATE|PREYS_ON]->preySpecimen-[:CLASSIFIED_AS]->preyTaxon");
        query.append(" RETURN preyTaxon.name as preyName");
        LOG.info("query [" + query + "] executing...");
        ExecutionResult results = executionEngine.execute(query.toString());
        HashSet<String> names = new HashSet<String>();
        for (Map<String, Object> result : results) {
            names.add((String)result.get("preyName"));
        }
        LOG.info("query [" + query + "] done.");
        return names.size();
    }

    private Collection<String> getDistinctConsumerTaxa(ExecutionEngine executionEngine, String startClause) {
        StringBuilder query = new StringBuilder(startClause);
        query.append(" RETURN consumerTaxon.name as consumerName");
        LOG.info("query [" + query + "] executing...");
        ExecutionResult results = executionEngine.execute(query.toString());
        Set<String> names = new HashSet<String>();
        for (Map<String, Object> result : results) {
            names.add((String) result.get("consumerName"));
        }
        LOG.info("query [" + query + "] done.");
        return names;
    }
}