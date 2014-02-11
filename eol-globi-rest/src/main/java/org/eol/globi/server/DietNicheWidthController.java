package org.eol.globi.server;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.service.TaxonRichnessLookup;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import scala.collection.convert.Wrappers;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Controller
public class DietNicheWidthController {

    private static final Log LOG = LogFactory.getLog(DietNicheWidthController.class);

    @Autowired
    private GraphDatabaseService graphDb;

    @RequestMapping(value = "/dietNicheWidth/{consumerTaxonName}", method = RequestMethod.GET)
    public void calculateDietaryNicheWidth(@PathVariable(value = "consumerTaxonName") String consumerTaxonName,
                                           @RequestParam(value = "preyTaxon") String[] preyTaxa,
                                           HttpServletResponse response) throws IOException {
        OutputStream os = response.getOutputStream();
        writeHeader(os);
        LOG.info("dietary niche width for [" + consumerTaxonName + "] calculating...");
        calculate(consumerTaxonName, preyTaxa, os);
        LOG.info("calculating dietary niche width for [" + consumerTaxonName + "] calculation done.");

        os.flush();
    }

    public void calculate(final String consumerTaxonName, String[] preyTaxa, OutputStream outputStream) throws IOException {
        ExecutionEngine executionEngine = new ExecutionEngine(graphDb);
        Collection<String> consumerTaxa = getDistinctConsumerTaxa(executionEngine, createStartClause(consumerTaxonName));
        if ((preyTaxa == null ? 0 : preyTaxa.length) > 1) {
            calculateDietNicheWidth(outputStream, executionEngine, preyTaxa, consumerTaxa);
        }
    }

    private void calculateDietNicheWidth(OutputStream outputStream, ExecutionEngine executionEngine, String[] preyTaxa, Collection<String> consumerTaxa) throws IOException {
        boolean isFirst = true;
        TaxonRichnessLookup taxonRichnessLookup = new TaxonRichnessLookup();
        for (String consumerName : consumerTaxa) {
            if (StringUtils.split(consumerName).length > 1) {
                isFirst = calcNicheWidthForConsumer(outputStream, executionEngine, preyTaxa, isFirst, taxonRichnessLookup, consumerName);
            }


        }
    }

    private boolean calcNicheWidthForConsumer(OutputStream outputStream, ExecutionEngine executionEngine, String[] preyTaxa, boolean first, TaxonRichnessLookup taxonRichnessLookup, String consumerName) throws IOException {
        StringBuilder startClause = new StringBuilder();
        startClause.append("START consumerTaxon = node:taxons('name:\"").append(consumerName).append("\"')");
        startClause.append(", preyTaxon = node:taxonpaths('");
        boolean isFirst = true;
        for (String preyTaxon : preyTaxa) {
            if (!isFirst) {
                startClause.append(" OR ");
            }
            startClause.append("path:\"").append(preyTaxon).append("\"");
            isFirst = false;
        }
        startClause.append("')");
        String query = findPreyForConsumerSpecimenQuery(startClause);
        LOG.info("niche width query for [" + query + "] executing...");
        ExecutionResult results = executionEngine.execute(query);
        LOG.info("niche width query done.");
        ResourceIterator<Map<String, Object>> iterator = results.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> result = iterator.next();
            if (!first) {
                IOUtils.write("\n", outputStream, "UTF-8");
            }
            inferNicheWidthDiversityAndWrite(result, outputStream, preyTaxa, taxonRichnessLookup);
            first = false;
        }
        iterator.close();
        LOG.info("niche width for [" + consumerName + "] done.");
        return first;
    }

    private String createStartClause(String consumerTaxonName) {
        return "START consumerTaxon = node:taxonpaths('path:\"" + consumerTaxonName + "\"')";
    }

    private String findPreyForConsumerSpecimenQuery(StringBuilder query) {
        query.append(" MATCH study-[:COLLECTED]->consumerSpecimen-[:COLLECTED_AT]->loc, consumerTaxon<-[:CLASSIFIED_AS]-consumerSpecimen-[:ATE|PREYS_ON]->preySpecimen-[:CLASSIFIED_AS]->preyTaxon");
        query.append(",loc-[:IN_ECO_REGION]->ecoRegion");
        query.append(" RETURN id(study) as studyId, id(consumerSpecimen) as consumerSpecimenId, consumerTaxon.name as consumerName, collect(preyTaxon.path) as preyPaths, loc.latitude as lat, loc.longitude as lng, ecoRegion.externalId as ecoRegionId");
        return query.toString();
    }

    private static void writeHeader(OutputStream os) throws IOException {
        String header = "study_id,specimen_id,consumer_name,lat,lng,number_of_distinct_prey_taxa,dietary_niche_width,diversity,eco_region_id";
        IOUtils.write(header, os, "UTF-8");
        IOUtils.write("\n", os, "UTF-8");
    }

    private void inferNicheWidthDiversityAndWrite(Map<String, Object> result, OutputStream outputStream, String[] preyTaxa, TaxonRichnessLookup taxonRichnessLookup) throws IOException {
        Long studyId = (Long) result.get("studyId");
        Map<Long, Integer> counterMap = new HashMap<Long, Integer>();
        Integer integer = counterMap.get(studyId);
        IOUtils.write(studyId.toString(), outputStream, "UTF-8");
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
        Collection<String> distinctPreyPaths = (Collection<String>) result.get("preyPaths");
        int numberOfDistinctPreyPaths = 0;
        for (String preyTaxon : preyTaxa) {
            for (String distinctPreyPath : distinctPreyPaths) {
                if (StringUtils.contains(distinctPreyPath, preyTaxon)) {
                    numberOfDistinctPreyPaths++;
                    break;
                }
            }
        }
        IOUtils.write(Integer.toString(numberOfDistinctPreyPaths), outputStream, "UTF-8");
        writeSeparator(outputStream);
        final Double nicheWidth = calculateDietaryNicheWidth(preyTaxa.length, numberOfDistinctPreyPaths);
        IOUtils.write(nicheWidth.toString(), outputStream, "UTF-8");
        writeSeparator(outputStream);
        Double richness = taxonRichnessLookup.lookupRichness(lat, lng);
        IOUtils.write(richness == null ? "" : richness.toString(), outputStream, "UTF-8");
        writeSeparator(outputStream);
        IOUtils.write(result.get("ecoRegionId").toString(), outputStream, "UTF-8");
        outputStream.flush();
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
        ResourceIterator<Map<String, Object>> iterator = results.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> result = iterator.next();
            names.add((String) result.get("preyName"));
        }
        iterator.close();
        LOG.info("query [" + query + "] done.");
        return names.size();
    }

    private Collection<String> getDistinctConsumerTaxa(ExecutionEngine executionEngine, String startClause) {
        StringBuilder query = new StringBuilder(startClause);
        query.append(" RETURN consumerTaxon.name as consumerName");
        LOG.info("query [" + query + "] executing...");
        ExecutionResult results = executionEngine.execute(query.toString());
        ResourceIterator<Map<String, Object>> iterator = results.iterator();
        Set<String> names = new HashSet<String>();
        while (iterator.hasNext()) {
            Map<String, Object> result = iterator.next();
            names.add((String) result.get("consumerName"));
        }
        iterator.close();
        LOG.info("query [" + query + "] done.");
        return names;
    }
}