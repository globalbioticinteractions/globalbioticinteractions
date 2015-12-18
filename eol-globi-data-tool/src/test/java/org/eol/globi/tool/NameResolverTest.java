package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class NameResolverTest extends GraphDBTestCase {

    @Test
    public void doNameResolving() throws NodeFactoryException, PropertyEnricherException {
        Specimen human = nodeFactory.createSpecimen(nodeFactory.createStudy("bla"), new TaxonImpl("Homo sapiens", null));
        Specimen animal = nodeFactory.createSpecimen(nodeFactory.createStudy("bla"), new TaxonImpl("Canis lupus", "EOL:1"));
        human.ate(animal);
        Specimen fish = nodeFactory.createSpecimen(nodeFactory.createStudy("bla"), new TaxonImpl("Arius felis", null));
        human.ate(fish);

        assertNull(taxonIndex.findTaxonById("EOL:1"));
        assertNull(taxonIndex.findTaxonByName("Homo sapiens"));

        new NameResolver(getGraphDb()).resolve();

        assertAnimalia(taxonIndex.findTaxonById("EOL:1"));

        assertThat(taxonIndex.findTaxonByName("Arius felis"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Ariopsis felis"), is(notNullValue()));

        TaxonNode homoSapiens = taxonIndex.findTaxonByName("Homo sapiens");
        assertNotNull(homoSapiens);
        assertThat(homoSapiens.getExternalId(), is("EOL:327955"));

        Iterable<Relationship> rels = homoSapiens.getUnderlyingNode().getRelationships(Direction.OUTGOING, InteractType.ATE);
        int count = 0;
        for (Relationship rel : rels) {
            count++;
            if (count == 0) {
                assertThat((String) rel.getEndNode().getProperty("name"), is("Animalia"));
            } else {
                assertThat((String) rel.getEndNode().getProperty("name"), is("Ariopsis felis"));
            }
        }

    }

    public void assertAnimalia(TaxonNode animalia) {
        assertNotNull(animalia);
        assertThat(animalia.getName(), is("Animalia"));
        assertThat(animalia.getPath(), is("Animalia"));
    }

    @Test
    public void seeminglyGoodName() {
        assertFalse(NameResolver.seeminglyGoodNameOrId("sp", null));
        assertTrue(NameResolver.seeminglyGoodNameOrId("sp", "EOL:1234"));
        assertTrue(NameResolver.seeminglyGoodNameOrId("something long", null));
        assertTrue(NameResolver.seeminglyGoodNameOrId(null, "EOL:123"));
    }

}
