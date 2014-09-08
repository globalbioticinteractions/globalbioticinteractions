package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class LinkerGlobalNamesTest extends GraphDBTestCase {

    @Test
    public void threeTaxa() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Homo sapiens");
        nodeFactory.getOrCreateTaxon("Ariopsis felis");
        nodeFactory.getOrCreateTaxon("Canis lupus");

        new LinkerGlobalNames().link(getGraphDb());

        LinkTestUtil.assertHasOther("Homo sapiens", 4, nodeFactory);
        LinkTestUtil.assertHasOther("Canis lupus", 4, nodeFactory);
        LinkTestUtil.assertHasOther("Ariopsis felis", 5, nodeFactory);

    }

    @Test
    public void australianTaxa() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Gilippus hostilis");
        nodeFactory.getOrCreateTaxon("Euander lacertosus");

        new LinkerGlobalNames().link(getGraphDb());

        LinkTestUtil.assertHasOther("Euander lacertosus", 2, nodeFactory);
        LinkTestUtil.assertHasOther("Gilippus hostilis", 2, nodeFactory);

    }

    @Test
    public void frogs() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Anura");
        new LinkerGlobalNames().link(getGraphDb());
        LinkTestUtil.assertHasOther("Anura", 4, nodeFactory);
    }

}
