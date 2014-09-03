package org.eol.globi.service;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
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
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class TaxonEnricherImplTest extends GraphDBTestCase {

    @Test
    public void enrichNoServices() throws NodeFactoryException, IOException, PropertyEnricherException {
        List<PropertyEnricher> list = new ArrayList<PropertyEnricher>();
        TaxonEnricherImpl enricher = new TaxonEnricherImpl();
        enricher.setServices(list);
        Transaction transaction = getGraphDb().beginTx();
        TaxonNode taxon = new TaxonNode(getGraphDb().createNode());
        taxon.setName("Homo sapiens");
        transaction.success();
        enricher.enrich(taxon);
        assertThat(taxon.getName(), is("Homo sapiens"));
    }

    @Test
    public void enrichTwoService() throws NodeFactoryException, IOException, PropertyEnricherException {
        PropertyEnricher serviceA = Mockito.mock(PropertyEnricher.class);
        PropertyEnricher serviceB = Mockito.mock(PropertyEnricher.class);
        enrich("Homo sapiens", serviceA, serviceB);
        verify(serviceA).enrich(anyMap());
        verify(serviceB).enrich(anyMap());
    }

    @Test
    public void enrichTwoServiceFirstComplete() throws NodeFactoryException, IOException, PropertyEnricherException {
        PropertyEnricher serviceA = new PropertyEnricher() {

            @Override
            public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
                properties = new HashMap<String, String>(properties);
                properties.put(PropertyAndValueDictionary.EXTERNAL_ID, "FIRST:123");
                properties.put(PropertyAndValueDictionary.PATH, "one | two | three");
                properties.put(PropertyAndValueDictionary.COMMON_NAMES, "four | five | six");
                return properties;
            }

            @Override
            public void shutdown() {

            }
        };
        PropertyEnricher serviceB = Mockito.mock(PropertyEnricher.class);

        Taxon taxon = enrich("Homo sapiens", serviceA, serviceB);
        assertThat(taxon.getExternalId(), is("FIRST:123"));
        assertThat(taxon.getPath(), is("one | two | three"));
        assertThat(taxon.getCommonNames(), is("four | five | six"));
        assertThat(taxon.getName(), is("Homo sapiens"));
        verifyZeroInteractions(serviceB);
    }

    private Taxon enrich(String taxonName, PropertyEnricher serviceA, PropertyEnricher serviceB) throws IOException, PropertyEnricherException {
        TaxonEnricherImpl enricher = new TaxonEnricherImpl();
        List<PropertyEnricher> list = new ArrayList<PropertyEnricher>();
        list.add(serviceA);
        list.add(serviceB);
        enricher.setServices(list);

        Taxon taxon = new TaxonImpl();
        taxon.setName(taxonName);
        return enricher.enrich(taxon);
    }

}
