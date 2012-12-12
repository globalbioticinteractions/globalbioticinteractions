package org.trophic.graph.export;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.NodeFactoryException;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.RelTypes;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

public class RDFExporterTest extends GraphDBTestCase {

    @Test
    public void exportToRDF() throws NodeFactoryException {

        Study study = nodeFactory.createStudy("A Study");

        Specimen man = nodeFactory.createSpecimen();
        man.setLengthInMm(193.0 * 10);

        Taxon manSpecies = nodeFactory.getOrCreateSpecies(null, "Homo sapiens");
        man.classifyAs(manSpecies);
        Location sampleLocation = nodeFactory.getOrCreateLocation(40.714623, -74.006605, 0.0);
        man.caughtIn(sampleLocation);
        study.collected(man);

        Specimen dog = nodeFactory.createSpecimen();
        Taxon dogSpecies = nodeFactory.getOrCreateSpecies(null, "Canis lupus");
        dog.classifyAs(dogSpecies);
        Relationship preysUponRelationship = man.createRelationshipTo(dog, RelTypes.ATE);


        // some definitions

        String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";


        String entityPrefix = "http://tinyurl.com/9dqkp2m/";

        String geoNS = "http://www.w3.org/2003/01/geo/wgs84_pos#";

        // create an empty Model
        Model model = ModelFactory.createDefaultModel();
        String trank = "http://rs.tdwg.org/ontology/voc/TaxonRank#";
        model.setNsPrefix("trank", trank);

        String tn = "http://rs.tdwg.org/ontology/voc/TaxonName#";
        model.setNsPrefix("tn", tn);
        String tinter = "http://rs.tdwg.org/ontology/voc/TaxonOccurrenceInteraction#";
        model.setNsPrefix("tinter", tinter);

        // create the resource
        //   and add the properties cascading style
        Resource specimenResource = model.createResource("http://rs.tdwg.org/ontology/voc/Specimen");

        Resource dogSpecimen = model.createResource(entityPrefix + dog.getNodeID())
                .addProperty(model.createProperty(tn + "rank"), model.createResource(tn + "Species"))
                .addProperty(model.createProperty(tn + "nameComplete"), dogSpecies.getName())
                .addProperty(model.createProperty(rdf + "type"), specimenResource);


        Resource interaction = model.createResource(entityPrefix + preysUponRelationship.getId());

        Resource manSpecimen = model.createResource(entityPrefix + man.getNodeID())
                .addProperty(model.createProperty(tn + "rank"), model.createResource(tn + "Species"))
                .addProperty(model.createProperty(tn + "nameComplete"), "Homo sapiens")
                .addProperty(model.createProperty(rdf + "type"), specimenResource);

        interaction
                .addProperty(model.createProperty(rdf + "type"), model.createResource("http://rs.tdwg.org/ontology/voc/TaxonOccurrenceInteraction"))
                .addProperty(model.createProperty(tinter + "fromOccurance"), manSpecimen)
                .addProperty(model.createProperty(tinter + "interactionCategory"), model.createResource("http://rs.tdwg.org/ontology/voc/TaxonOccurrenceInteraction#PreysUpon"))
                .addProperty(model.createProperty(tinter + "toOccurance"), dogSpecimen);

        // now write the model in XML form to a file
        model.write(System.out);

    }
}
