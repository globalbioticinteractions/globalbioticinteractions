package org.eol.globi.service;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.NodeBacked;
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
    private final List<TaxonPropertyLookupService> services = new ArrayList<TaxonPropertyLookupService>();
    private final HashMap<Class, Integer> errorCounts = new HashMap<Class, Integer>();
    public static final String[] PROPERTY_NAMES = new String[]{NodeBacked.EXTERNAL_ID, Taxon.PATH};
    private final GraphDatabaseService graphDbService;

    public TaxonPropertyEnricherImpl(GraphDatabaseService graphDbService) {
        this.graphDbService = graphDbService;
    }

    @Override
    public boolean enrich(Taxon taxon) throws IOException {
        boolean didEnrichAtLeastOneProperty = false;
        if (isEnriched(taxon)) {
            LOG.warn("taxon [" + taxon.getName() + "] already has properties [" + PROPERTY_NAMES + "]");
        } else {
            didEnrichAtLeastOneProperty = doEnrichment(taxon, didEnrichAtLeastOneProperty);
        }
        return didEnrichAtLeastOneProperty;
    }

    private boolean isEnriched(Taxon taxon) {
        boolean hasAllProperties = true;
        Node underlyingNode = taxon.getUnderlyingNode();
        for (String propertyName : PROPERTY_NAMES) {
            hasAllProperties = hasAllProperties && underlyingNode.hasProperty(propertyName);
        }
        return hasAllProperties;
    }

    private boolean doEnrichment(Taxon taxon, boolean didEnrichAtLeastOneProperty) {
        if (services.size() == 0) {
            initServices();
        }

        Node taxonNode = taxon.getUnderlyingNode();

        for (String propertyName : PROPERTY_NAMES) {
            for (TaxonPropertyLookupService service : services) {
                if (service.canLookupProperty(propertyName)) {
                    try {
                        if (enrichTaxonWithPropertyValue(errorCounts, taxonNode, service, propertyName)) {
                            didEnrichAtLeastOneProperty = true;
                            break;
                        }
                    } catch (TaxonPropertyLookupServiceException e) {
                        LOG.warn("problem with taxon lookup", e);
                        service.shutdown();
                    }
                }
            }
        }

        return didEnrichAtLeastOneProperty;
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
        if (stopwatch.getTime() > 3000) {
            String responseTime = "(took " + stopwatch.getTime() + "ms) for [" + service.getClass().getSimpleName() + "]";
            String msg = "slow query for [" + taxonName + "] with " + propertyName + " [" + propertyValue + "] in [" + service.getClass().getSimpleName() + "] " + responseTime;
            LOG.warn(msg);
        }

        if (propertyValue != null) {
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
        services.add(new NoMatchService());
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

}
