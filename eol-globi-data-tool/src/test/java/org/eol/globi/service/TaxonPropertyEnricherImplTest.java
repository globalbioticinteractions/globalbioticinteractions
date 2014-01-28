package org.eol.globi.service;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonNode;
import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class TaxonPropertyEnricherImplTest extends GraphDBTestCase {

    @Test
    public void enrichNoServices() throws NodeFactoryException, IOException {
        List<TaxonPropertyLookupService> list = new ArrayList<TaxonPropertyLookupService>();
        TaxonPropertyEnricherImpl enricher = new TaxonPropertyEnricherImpl();
        enricher.setServices(list);
        Transaction transaction = getGraphDb().beginTx();
        TaxonNode taxon = new TaxonNode(getGraphDb().createNode());
        taxon.setName("Homo sapiens");
        transaction.success();
        enricher.enrich(taxon);
        assertThat(taxon.getName(), is("Homo sapiens"));
    }

    @Test
    public void enrichTwoService() throws NodeFactoryException, IOException, TaxonPropertyLookupServiceException {
        TaxonPropertyLookupService serviceA = Mockito.mock(TaxonPropertyLookupService.class);
        TaxonPropertyLookupService serviceB = Mockito.mock(TaxonPropertyLookupService.class);
        enrich("Homo sapiens", serviceA, serviceB);
        verify(serviceA).lookupPropertiesByName(eq("Homo sapiens"), anyMap());
        verify(serviceB).lookupPropertiesByName(eq("Homo sapiens"), anyMap());
    }

    @Test
    public void enrichTwoServiceFirstIncomplete() throws NodeFactoryException, IOException, TaxonPropertyLookupServiceException {
        TaxonPropertyLookupService serviceA = new TaxonPropertyLookupService() {
            @Override
            public void lookupPropertiesByName(String name, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
                properties.put(PropertyAndValueDictionary.EXTERNAL_ID, "FIRST:123");
            }

            @Override
            public void shutdown() {

            }
        };
        TaxonPropertyLookupService serviceB = new TaxonPropertyLookupService() {
            @Override
            public void lookupPropertiesByName(String name, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
                properties.put(PropertyAndValueDictionary.PATH, "one | two | three");
            }

            @Override
            public void shutdown() {

            }
        };
        TaxonNode taxon = enrich("Homo sapiens", serviceA, serviceB);
        assertThat(taxon.getExternalId(), is("FIRST:123"));
        assertThat(taxon.getPath(), is("one | two | three"));

    }

    @Test
    public void enrichTwoServiceFirstComplete() throws NodeFactoryException, IOException, TaxonPropertyLookupServiceException {
        TaxonPropertyLookupService serviceA = new TaxonPropertyLookupService() {
            @Override
            public void lookupPropertiesByName(String name, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
                properties.put(PropertyAndValueDictionary.EXTERNAL_ID, "FIRST:123");
                properties.put(PropertyAndValueDictionary.PATH, "one | two | three");
                properties.put(PropertyAndValueDictionary.COMMON_NAMES, "four | five | six");
            }

            @Override
            public void shutdown() {

            }
        };
        TaxonPropertyLookupService serviceB = Mockito.mock(TaxonPropertyLookupService.class);

        TaxonNode taxon = enrich("Homo sapiens", serviceA, serviceB);
        assertThat(taxon.getExternalId(), is("FIRST:123"));
        assertThat(taxon.getPath(), is("one | two | three"));
        assertThat(taxon.getCommonNames(), is("four | five | six"));
        assertThat(taxon.getName(), is("Homo sapiens"));
        verifyZeroInteractions(serviceB);

    }

    private TaxonNode enrich(String taxonName, TaxonPropertyLookupService serviceA, TaxonPropertyLookupService serviceB) throws IOException {
        TaxonPropertyEnricherImpl enricher = new TaxonPropertyEnricherImpl();
        List<TaxonPropertyLookupService> list = new ArrayList<TaxonPropertyLookupService>();
        list.add(serviceA);
        list.add(serviceB);
        enricher.setServices(list);

        Transaction transaction = getGraphDb().beginTx();
        TaxonNode taxon = new TaxonNode(getGraphDb().createNode());
        taxon.setName(taxonName);
        transaction.success();
        enricher.enrich(taxon);
        return taxon;
    }

}
