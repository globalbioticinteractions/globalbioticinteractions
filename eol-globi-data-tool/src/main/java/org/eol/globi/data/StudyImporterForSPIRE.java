package org.eol.globi.data;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import uk.me.jstott.jcoord.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForSPIRE extends BaseStudyImporter {


    private Map<String, LatLng> geoLookup = buildGEOLookup();

    private TrophicLinkListener trophicLinkListener = new TrophicLinkListener() {
        @Override
        public void newLink(Study study, String predatorName, String preyName, String country, String state, String locality) {
            importTrophicLink(study, predatorName, preyName, geoLookup.get(country));
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


    private Map<String, LatLng> buildGEOLookup() {
        Map<String, LatLng> geo = new HashMap<String, LatLng>();
        addGeo("Portugal", 39.5, -8, geo);
        addGeo("Southern Ocean", -60, 90, geo);
        addGeo("Hong Kong", 22.28552, 114.15769, geo);
        addGeo("Costa Rica", 10, -84, geo);
        addGeo("Europe", 48.69096, 9.14062, geo);
        addGeo("Chile", -30, -71, geo);
        addGeo("Sri Lanka", 7, 81, geo);
        addGeo("India", 20, 77, geo);
        addGeo("Puerto Rico", 18.24829, -66.49989, geo);
        addGeo("Namibia", -22, 17, geo);
        addGeo("Arctic", -90, 0, geo);
        addGeo("Panama", 9, -80, geo);
        addGeo("UK", 54.75844, -2.69531, geo);
        addGeo("Great Britain", 54.75844, -2.69531, geo);
        addGeo("Wales", 52.5, -3.5, geo);
        addGeo("Scotland", 56, -4, geo);
        addGeo("Mexico", 23, -102, geo);
        addGeo("Canada", 60.10867, -113.64258, geo);
        addGeo("Venezuela", 8, -66, geo);
        addGeo("Africa", 7.1881, 21.09375, geo);
        addGeo("Marshall Islands", 7.113, 171.236, geo);
        addGeo("Madagascar", -20, 47, geo);
        addGeo("Quebec", 52.00017, -71.99907, geo);
        addGeo("Ireland", 53, -8, geo);
        addGeo("Barbados", 13.16667, -59.53333, geo);
        addGeo("England", 52.16045, -0.70312, geo);
        addGeo("Antarctic", -90, 0, geo);
        addGeo("Ethiopia", 8, 38, geo);
        addGeo("Malaysia", 2.5, 112.5, geo);
        addGeo("Pacific", 0, 180, geo);
        addGeo("St. Martin", 18.06667, -63.06667, geo);
        addGeo("Tibet", 32, 90, geo);
        addGeo("Peru", -10, -76, geo);
        addGeo("Australia", -25, 135, geo);
        addGeo("South Africa", -29, 24, geo);
        addGeo("Malawi", -13.5, 34, geo);
        addGeo("Netherlands", 52.5, 5.75, geo);
        addGeo("Finland", 64, 26, geo);
        addGeo("Seychelles", -4.58333, 55.66667, geo);
        addGeo("Uganda", 2, 33, geo);
        addGeo("Austria", 47.33333, 13.33333, geo);
        addGeo("USA", 39.76, -98.5, geo);
        addGeo("Russia", 60, 100, geo);
        addGeo("Polynesia", -17.6859, -143.87695, geo);
        addGeo("Germany", 51.5, 10.5, geo);
        addGeo("Norway", 62, 10, geo);
        addGeo("Japan", 35.68536, 139.75309, geo);
        addGeo("New Zealand", -42, 174, geo);
        return geo;
    }

    private void addGeo(String country, double lat, double lng, Map<String, LatLng> geo) {
        geo.put(country, new LatLng(lat, lng));

    }


    @Override
    public Study importStudy() throws StudyImporterException {
        Model model = null;
        try {
            model = buildModel();
        } catch (IOException e) {
            throw new StudyImporterException("failed to import SPIRE", e);
        }

        Map<String, LatLng> stringLatLngMap = buildGEOLookup();

        final Study study = nodeFactory.createStudy(StudyLibrary.Study.SPIRE.toString(),
                "Joel Sachs",
                "Dept. of Computer Science and Electrical Engineering, University of Maryland Baltimore County, Baltimore, MD, USA.",
                "",
                "Semantic Prototypes in Research Ecoinformatics (SPIRE).");

        ResIterator resIterator = model.listSubjects();
        while (resIterator.hasNext()) {
            Resource next = resIterator.next();
            String predatorName = null;
            String preyName = null;
            String locality = null;
            String country = null;
            String state = null;
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
                            String localityString = getTrimmedObject(next2);
                            localityString = localityString.replaceAll("\\^\\^http://www.w3.org/2001/XMLSchema#string", "");
                            String[] split = localityString.split(";");
                            for (String s : split) {
                                String[] split1 = s.split(":");
                                if (split1.length > 1) {
                                    String key = split1[0].trim();
                                    String value = split1[1].trim();
                                    if ("Country".equals(key)) {
                                        country = value;
                                    } else if ("State".equals(key)) {
                                        state = value;
                                    } else if ("Locality".equals(key)) {
                                        locality = value;
                                    } else {
                                        throw new StudyImporterException("unsupported locality key [" + key + "]");
                                    }
                                }
                            }
                        } else if ("ofHabitat".equals(localName)) {
                            // habitat
                        }
                    }
                }
            }
            if (predatorName != null && preyName != null && getTrophicLinkListener() != null) {
                getTrophicLinkListener().newLink(study, predatorName, preyName, country, state, locality);
            }
        }

        return study;
    }

    private void importTrophicLink(Study study, String predatorName, String preyName, LatLng latLng) {
        try {
            Specimen predator = createSpecimen(predatorName);
            if (latLng != null) {
                predator.caughtIn(nodeFactory.getOrCreateLocation(latLng.getLat(), latLng.getLng(), null));
            }
            study.collected(predator);
            Specimen prey = createSpecimen(preyName);
            predator.ate(prey);
        } catch (NodeFactoryException e) {

        }
    }

    private Specimen createSpecimen(String taxonName) throws NodeFactoryException {
        taxonName = taxonName.replaceAll("_", " ");
        Specimen predator = nodeFactory.createSpecimen(taxonName);
        return predator;
    }


    private Model buildModel() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        BufferedReader bufferedReader = FileUtils.getBufferedReader(getClass().getResourceAsStream("spire/allFoodWebStudies.owl.gz"), CharsetConstant.UTF8);
        model.read(bufferedReader, null);
        return model;
    }

    private String getTrimmedObject(Statement next1) {
        return next1.getObject().toString().replaceAll("http://spire.umbc.edu/ethan/", "");
    }
}
