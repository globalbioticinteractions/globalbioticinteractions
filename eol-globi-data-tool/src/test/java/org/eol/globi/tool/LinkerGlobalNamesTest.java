package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class LinkerGlobalNamesTest extends GraphDBTestCase {

    @Test
    public void threeTaxa() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Homo sapiens");
        nodeFactory.getOrCreateTaxon("Ariopsis felis");
        nodeFactory.getOrCreateTaxon("Canis lupus");

        new LinkerGlobalNames().link(getGraphDb());

        LinkerTestUtil.assertHasOther("Homo sapiens", 4, nodeFactory);
        LinkerTestUtil.assertHasOther("Canis lupus", 4, nodeFactory);
        LinkerTestUtil.assertHasOther("Ariopsis felis", 5, nodeFactory);

    }

    @Test
    public void australianTaxa() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Gilippus hostilis");
        nodeFactory.getOrCreateTaxon("Euander lacertosus");

        new LinkerGlobalNames().link(getGraphDb());

        LinkerTestUtil.assertHasOther("Gilippus hostilis", 2, nodeFactory);
        LinkerTestUtil.assertHasOther("Euander lacertosus", 2, nodeFactory);

    }

    @Test
    public void frogs() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Anura");
        new LinkerGlobalNames().link(getGraphDb());
        List<String> ids = LinkerTestUtil.assertHasOther("Anura", 7, nodeFactory);

        assertThat(ids, hasItems("ITIS:173423"
                , "NCBI:8342", "IRMNG:10211", "GBIF:952"
                , "IRMNG:1284513", "GBIF:3242458", "GBIF:3089470"));

    }

}
