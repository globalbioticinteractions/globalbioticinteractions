package org.trophic.graph.data;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class StudyImporterForSPIRE extends BaseStudyImporter {


    private TrophicLinkListener trophicLinkListener = new TrophicLinkListener() {
        @Override
        public void newLink(Study study, String predatorName, String preyName) {
            importTrophicLink(study, predatorName, preyName);
        }
    };

    public StudyImporterForSPIRE(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    public TrophicLinkListener getTrophicLinkListener() {
        return trophicLinkListener;
    }

    public void setTrophicLinkListener(TrophicLinkListener trophicLinkListener) {
        this.trophicLinkListener = trophicLinkListener;
    }


    @Override
    public Study importStudy() throws StudyImporterException {
        Model model = null;
        try {
            model = buildModel();
        } catch (IOException e) {
            throw new StudyImporterException("failed to import SPIRE", e);
        }

        final Study study = nodeFactory.createStudy(StudyLibrary.Study.SPIRE.toString());

        ResIterator resIterator = model.listSubjects();
        while (resIterator.hasNext()) {
            Resource next = resIterator.next();
            String predatorName = null;
            String preyName = null;
            StmtIterator stmtIterator = next.listProperties();
            while (stmtIterator.hasNext()) {
                Statement next1 = stmtIterator.next();
                String ln = next1.getPredicate().getLocalName();
                if ("predator".equals(ln)) {
                    predatorName = getTrimmedObject(next1);
                } else if ("prey".equals(ln)) {
                    preyName = getTrimmedObject(next1);
                } else if ("observedInStudy".equals(ln)) {
                    StmtIterator stmtIterator1 = next1.getObject().asResource().listProperties();
                    while (stmtIterator1.hasNext()) {
                        Statement next2 = stmtIterator1.next();
                        String localName = next2.getPredicate().getLocalName();
                        if ("locality".equals(localName)) {
                            // locality
                        } else if ("ofHabitat".equals(localName)) {
                            // habitat
                        }
                    }
                }
            }
            if (predatorName != null && preyName != null && getTrophicLinkListener() != null) {
                getTrophicLinkListener().newLink(study, predatorName, preyName);
            }
        }

        return study;
    }

    private void importTrophicLink(Study study, String predatorName, String preyName) {
        try {
            Specimen predator = createSpecimen(predatorName);
            study.collected(predator);
            Specimen prey = createSpecimen(preyName);
            predator.ate(prey);
        } catch (NodeFactoryException e) {

        }
    }

    private Specimen createSpecimen(String taxonName) throws NodeFactoryException {
        taxonName = taxonName.replaceAll("_", " ");
        Taxon predatorTaxon = nodeFactory.getOrCreateTaxon(taxonName);
        Specimen predator = nodeFactory.createSpecimen();
        predator.classifyAs(predatorTaxon);
        return predator;
    }


    private Model buildModel() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        GZIPInputStream is = new GZIPInputStream(getClass().getResourceAsStream("spire/allFoodWebStudies.owl.gz"));
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        model.read(bufferedReader, null);
        return model;
    }

    private String getTrimmedObject(Statement next1) {
        return next1.getObject().toString().replaceAll("http://spire.umbc.edu/ethan/", "");
    }
}
