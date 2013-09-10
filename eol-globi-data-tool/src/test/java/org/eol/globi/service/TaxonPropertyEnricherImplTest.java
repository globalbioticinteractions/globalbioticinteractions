package org.eol.globi.service;

import org.eol.globi.data.GraphDBTestCase;
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

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        assertThat(taxon.getName(), Is.is("Homo sapiens"));
    }

    @Test
    public void enrichTwoService() throws NodeFactoryException, IOException, TaxonPropertyLookupServiceException {
        TaxonPropertyLookupService serviceA = Mockito.mock(TaxonPropertyLookupService.class);
        TaxonPropertyLookupService serviceB = Mockito.mock(TaxonPropertyLookupService.class);
        Map<String, String> properties = enrich("Homo sapiens", serviceA, serviceB);
        verify(serviceA).lookupPropertiesByName(anyString(), eq(properties));
        verify(serviceB).lookupPropertiesByName(anyString(), eq(properties));

    }

    private Map<String, String> enrich(String taxonName, TaxonPropertyLookupService serviceA, TaxonPropertyLookupService serviceB) throws IOException {
        TaxonPropertyEnricherImpl enricher = new TaxonPropertyEnricherImpl(getGraphDb());
        List<TaxonPropertyLookupService> list = new ArrayList<TaxonPropertyLookupService>();
        list.add(serviceA);
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(Taxon.EXTERNAL_ID, null);
                put(Taxon.PATH, null);
            }
        };
        when(serviceA.canLookupProperty(anyString())).thenReturn(true);

        list.add(serviceB);

        when(serviceB.canLookupProperty(anyString())).thenReturn(true);
        enricher.setServices(list);

        Transaction transaction = getGraphDb().beginTx();
        Taxon taxon = new Taxon(getGraphDb().createNode());
        taxon.setName(taxonName);
        transaction.success();
        enricher.enrich(taxon);
        return properties;
    }

    @Test
    public void noEnrichmentNameTooShort() throws NodeFactoryException, IOException, TaxonPropertyLookupServiceException {
        TaxonPropertyLookupService serviceA = Mockito.mock(TaxonPropertyLookupService.class);
        TaxonPropertyLookupService serviceB = Mockito.mock(TaxonPropertyLookupService.class);
        Map<String, String> properties = enrich("H", serviceA, serviceB);
        verify(serviceA, never()).lookupPropertiesByName(anyString(), eq(properties));
        verify(serviceB, never()).lookupPropertiesByName(anyString(), eq(properties));

    }


}
