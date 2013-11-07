package org.eol.globi.service;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Taxon;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class TaxonPropertyEnricherImplTest extends GraphDBTestCase {

    @Test
    public void enrichNoServices() throws NodeFactoryException, IOException {
        List<TaxonPropertyLookupService> list = new ArrayList<TaxonPropertyLookupService>();
        TaxonPropertyEnricherImpl enricher = new TaxonPropertyEnricherImpl(getGraphDb());
        enricher.setServices(list);
        Transaction transaction = getGraphDb().beginTx();
        Taxon taxon = new Taxon(getGraphDb().createNode());
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
            public void lookupPropertiesByName(String taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
                properties.put(Taxon.EXTERNAL_ID, "FIRST:123");
            }

            @Override
            public void shutdown() {

            }
        };
        TaxonPropertyLookupService serviceB = new TaxonPropertyLookupService() {
            @Override
            public void lookupPropertiesByName(String taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
                properties.put(Taxon.PATH, "one | two | three");
            }

            @Override
            public void shutdown() {

            }
        };
        Taxon taxon = enrich("Homo sapiens", serviceA, serviceB);
        assertThat(taxon.getExternalId(), is("FIRST:123"));
        assertThat(taxon.getPath(), is("one | two | three"));

    }

    @Test
    public void enrichTwoServiceFirstComplete() throws NodeFactoryException, IOException, TaxonPropertyLookupServiceException {
        TaxonPropertyLookupService serviceA = new TaxonPropertyLookupService() {
            @Override
            public void lookupPropertiesByName(String taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
                properties.put(Taxon.EXTERNAL_ID, "FIRST:123");
                properties.put(Taxon.PATH, "one | two | three");
                properties.put(Taxon.COMMON_NAMES, "four | five | six");
            }

            @Override
            public void shutdown() {

            }
        };
        TaxonPropertyLookupService serviceB = Mockito.mock(TaxonPropertyLookupService.class);

        Taxon taxon = enrich("Homo sapiens", serviceA, serviceB);
        assertThat(taxon.getExternalId(), is("FIRST:123"));
        assertThat(taxon.getPath(), is("one | two | three"));
        assertThat(taxon.getCommonNames(), is("four | five | six"));
        assertThat(taxon.getName(), is("Homo sapiens"));
        verifyZeroInteractions(serviceB);

    }

    private Taxon enrich(String taxonName, TaxonPropertyLookupService serviceA, TaxonPropertyLookupService serviceB) throws IOException {
        TaxonPropertyEnricherImpl enricher = new TaxonPropertyEnricherImpl(getGraphDb());
        List<TaxonPropertyLookupService> list = new ArrayList<TaxonPropertyLookupService>();
        list.add(serviceA);
        list.add(serviceB);
        enricher.setServices(list);

        Transaction transaction = getGraphDb().beginTx();
        Taxon taxon = new Taxon(getGraphDb().createNode());
        taxon.setName(taxonName);
        transaction.success();
        enricher.enrich(taxon);
        return taxon;
    }

}
