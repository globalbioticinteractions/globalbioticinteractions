package org.eol.globi.service;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.eol.globi.domain.Taxon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TaxonPropertyEnricherImpl implements TaxonPropertyEnricher {
    private static final Log LOG = LogFactory.getLog(TaxonPropertyEnricherImpl.class);
    public static final String NO_MATCH = "no:match";
    private final List<TaxonPropertyLookupService> services = new ArrayList<TaxonPropertyLookupService>();
    private final HashMap<Class, Integer> errorCounts = new HashMap<Class, Integer>();
    public static final String[] PROPERTY_NAMES = new String[]{Taxon.EXTERNAL_ID, Taxon.PATH};
    private final GraphDatabaseService graphDbService;

    public TaxonPropertyEnricherImpl(GraphDatabaseService graphDbService) {
        this.graphDbService = graphDbService;
    }

    @Override
    public void enrich(Taxon taxon) throws IOException {
        if (services.size() == 0) {
            initServices();
        }

        Node taxonNode = taxon.getUnderlyingNode();
        try {
            for (String propertyName : PROPERTY_NAMES) {
                for (TaxonPropertyLookupService service : services) {
                    if (service.canLookupProperty(propertyName)) {
                        if (enrichTaxonWithPropertyValue(errorCounts, taxonNode, service, propertyName)) {
                            break;
                        }
                    }
                }
            }
        } catch (TaxonPropertyLookupServiceException e) {
            LOG.warn("problem with taxon lookup", e);
            shutdownServices();
            initServices();
        }

    }

    private boolean enrichTaxonWithPropertyValue(HashMap<Class, Integer> errorCounts, Node taxonNode, TaxonPropertyLookupService service, String propertyName1) throws TaxonPropertyLookupServiceException {
        Integer errorCount = errorCounts.get(service.getClass());
        if (errorCount != null && errorCount > 10) {
            LOG.error("skipping taxon match against [" + service.getClass().toString() + "], error count [" + errorCount + "] too high.");
        } else {
            if (enrichTaxon(errorCounts, taxonNode, service, errorCount, propertyName1)) {
                return true;
            }
        }
        return false;
    }

    private boolean enrichTaxon(HashMap<Class, Integer> errorCounts, Node taxonNode, TaxonPropertyLookupService service, Integer errorCount, String propertyName) throws TaxonPropertyLookupServiceException {
        String taxonName = (String) taxonNode.getProperty(Taxon.NAME);
        try {
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            if (lookupAndSetProperty(taxonNode, service, taxonName, stopwatch, propertyName)) {
                return true;
            }
        } catch (TaxonPropertyLookupServiceException ex) {
            LOG.warn("failed to find a match for [" + taxonName + "] in [" + service.getClass().getSimpleName() + "]", ex);
            incrementErrorCount(errorCounts, service, errorCount);
            throw new TaxonPropertyLookupServiceException("re-throwing", ex);
        }
        return false;
    }

    private boolean lookupAndSetProperty(Node taxonNode, TaxonPropertyLookupService service, String taxonName, StopWatch stopwatch, String propertyName) throws TaxonPropertyLookupServiceException {
        String propertyValue = service.lookupPropertyValueByTaxonName(taxonName, propertyName);
        stopwatch.stop();
        String responseTime = "(took " + stopwatch.getTime() + "ms)";
        String msg = "for [" + taxonName + "] with " + propertyName + " [" + propertyValue + "] in [" + service.getClass().getSimpleName() + "] " + responseTime;
        if (propertyValue == null) {
            LOG.info("no match found " + msg);
        } else {
            LOG.info("found match " + msg);
            setProperty(taxonNode, propertyName, propertyValue);
            return true;
        }
        return false;
    }

    private void incrementErrorCount(HashMap<Class, Integer> errorCounts, TaxonPropertyLookupService service, Integer errorCount) {
        if (errorCounts.containsKey(service.getClass()) && errorCount != null) {
            errorCounts.put(service.getClass(), ++errorCount);
        } else {
            errorCounts.put(service.getClass(), 0);
        }
    }

    private void initServices() {
        services.add(new EOLOfflineService());
        services.add(new EOLService());
        services.add(new WoRMSService());
        services.add(new ITISService());
        services.add(new GulfBaseService());
        services.add(new NullExternalIdService());
    }

    private void shutdownServices() {
        for (TaxonPropertyLookupService service : services) {
            service.shutdown();
        }
        services.clear();
    }

    private void setProperty(Node node, String propertyName, String propertyValue) {
        Transaction transaction = graphDbService.beginTx();
        try {
            node.setProperty(propertyName, propertyValue);
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    private static class NullExternalIdService implements TaxonPropertyLookupService {
        @Override
        public String lookupPropertyValueByTaxonName(String taxonName, String propertyName) throws TaxonPropertyLookupServiceException {
            return NO_MATCH;
        }

        @Override
        public void shutdown() {

        }

        @Override
        public boolean canLookupProperty(String propertyName) {
            return Taxon.EXTERNAL_ID.equals(propertyName);
        }
    }
}
