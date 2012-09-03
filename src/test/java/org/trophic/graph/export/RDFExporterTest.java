package org.trophic.graph.export;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.VCARD;
import org.junit.Test;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.NodeFactoryException;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;
import sun.rmi.rmic.iiop.SpecialInterfaceType;

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
        man.ate(dog);


        // some definitions
        String uri = "http://trophicgraph.com/rdf/1.0#";

        String entityPrefix = "http://ec2-50-112-48-206.us-west-2.compute.amazonaws.com:7474/db/data/node/";
        String dataURI = entityPrefix + "4";
        String specimenURI = entityPrefix + "123";
        String givenName = "John";
        String familyName = "Smith";
        String fullName = givenName + " " + familyName;

        // create an empty Model
        Model model = ModelFactory.createDefaultModel();
        Resource manSpeciesResource = model.createResource(entityPrefix + manSpecies.getNodeID())
                .addProperty(model.createProperty(uri + "is_a"), model.createResource(uri + "species"))
                .addLiteral(model.createProperty(uri + "name"), manSpecies.getName());

        Resource dogSpeciesResource = model.createResource(entityPrefix + dogSpecies.getNodeID())
                .addProperty(model.createProperty(uri + "is_a"), model.createResource(uri + "species"))
                .addLiteral(model.createProperty(uri + "name"), dogSpecies.getName());

        // create the resource
        //   and add the properties cascading style
        Resource locationResource = model.createResource(entityPrefix + sampleLocation.getNodeID())
                .addProperty(model.createProperty(uri + "is_a"), model.createResource(uri + "location"))
                .addLiteral(model.createProperty(uri + "longitude"), sampleLocation.getLongitude())
                .addLiteral(model.createProperty(uri + "latitude"), sampleLocation.getLatitude())
                .addLiteral(model.createProperty(uri + "altitudeInMeters"), sampleLocation.getAltitude());

        Resource specimenResource = model.createResource(uri + "specimen");

        Resource dogSpecimen = model.createResource(entityPrefix + man.getNodeID())
                .addProperty(model.createProperty(uri + "classifiedAs"), dogSpeciesResource)
                .addProperty(model.createProperty(uri + "is_a"), specimenResource);

        model.createResource(entityPrefix + man.getNodeID())
                .addLiteral(model.createProperty(uri + "lengthInMm"), 193.0 * 10.0)
                .addProperty(model.createProperty(uri + "classifiedAs"), manSpeciesResource)
                .addProperty(model.createProperty(uri + "is_a"), specimenResource)
                .addProperty(model.createProperty(uri + "collected_at"), locationResource)
                .addProperty(model.createProperty(uri + "ate"), dogSpecimen);

        // now write the model in XML form to a file
        model.write(System.out);
    }
}
