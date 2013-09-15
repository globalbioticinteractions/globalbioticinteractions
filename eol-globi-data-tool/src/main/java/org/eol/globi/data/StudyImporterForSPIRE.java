package org.eol.globi.data;

import com.Ostermiller.util.MD5;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.service.EnvoService;
import org.eol.globi.service.EnvoServiceImpl;
import org.eol.globi.service.EnvoServiceException;
import org.eol.globi.service.EnvoTerm;
import uk.me.jstott.jcoord.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForSPIRE extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForSPIRE.class);

    public static final String OF_HABITAT = "ofHabitat";
    public static final String COUNTRY = "Country";
    public static final String STATE = "State";
    public static final String LOCALITY = "Locality";
    public static final String PREDATOR_NAME = "predator";
    public static final String PREY_NAME = "prey";
    private Map<String, LatLng> geoLookup = buildGEOLookup();

    private TrophicLinkListener trophicLinkListener = new TrophicLinkListener() {
        @Override
        public void newLink(Map<String, String> properties) {
            importTrophicLink(properties);
        }
    };

    private EnvoService envoService = new EnvoServiceImpl();


    public StudyImporterForSPIRE(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected static void parseTitlesAndAuthors(String titlesAndAuthors, Map<String, String> properties) {
        String titlesAndAuthors1 = titlesAndAuthors.replace("\n", "");
        String shortened = StringUtils.abbreviate(titlesAndAuthors1.
                replaceAll("(\\w(\\. )+)", "").trim(), 24);

        properties.put(Study.TITLE, shortened + MD5.getHashString(titlesAndAuthors1));
        properties.put(Study.CONTRIBUTOR, "");
        properties.put(Study.INSTITUTION, "");
        properties.put(Study.PERIOD, "");
        properties.put(Study.PUBLICATION_YEAR, "");
        properties.put(Study.DESCRIPTION, titlesAndAuthors1);
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
        Model model;
        try {
            model = buildModel();
        } catch (IOException e) {
            throw new StudyImporterException("failed to import SPIRE", e);
        }

        buildGEOLookup();


        ResIterator resIterator = model.listSubjects();
        Map<String, String> properties = new HashMap<String, String>();
        Long subjectCounter = 0L;
        while (resIterator.hasNext() && getImportFilter().shouldImportRecord(subjectCounter)) {
            properties.clear();
            Resource resource = resIterator.next();
            StmtIterator stmtIterator = resource.listProperties();
            while (stmtIterator.hasNext()) {
                Statement statement = stmtIterator.next();
                String ln = statement.getPredicate().getLocalName();
                if (PREDATOR_NAME.equals(ln)) {
                    properties.put(ln, getTrimmedObject(statement));
                } else if (PREY_NAME.equals(ln)) {
                    properties.put(ln, getTrimmedObject(statement));
                } else if ("observedInStudy".equals(ln)) {
                    parseStudyInfo(properties, statement);
                }
            }
            if (properties.containsKey(PREDATOR_NAME)
                    && properties.containsKey(PREY_NAME)
                    && getTrophicLinkListener() != null) {
                getTrophicLinkListener().newLink(properties);
            }
            subjectCounter++;
        }

        // should probably retire this . . .
        return null;
    }

    private void parseStudyInfo(Map<String, String> properties, Statement observedInStudy) throws StudyImporterException {
        StmtIterator studyProperties = observedInStudy.getObject().asResource().listProperties();
        while (studyProperties.hasNext()) {
            Statement studyProperty = studyProperties.next();
            String localName = studyProperty.getPredicate().getLocalName();
            if ("locality".equals(localName)) {
                parseLocalityInfo(properties, getTrimmedObject(studyProperty));
            } else if (OF_HABITAT.equals(localName)) {
                properties.put(localName, getTrimmedObject(studyProperty));
            } else if ("titleAndAuthors".equals(localName)) {
                parseTitlesAndAuthors(getTrimmedObject(studyProperty), properties);
            }
        }
    }

    private void parseLocalityInfo(Map<String, String> properties, String localityString) throws StudyImporterException {
        String[] split = localityString.split(";");
        for (String s : split) {
            String[] split1 = s.split(":");
            if (split1.length > 1) {
                String key = split1[0].trim();
                String value = split1[1].trim();
                if (COUNTRY.equals(key) || STATE.equals(key) || LOCALITY.equals(key)) {
                    properties.put(key, value);
                } else {
                    throw new StudyImporterException("unsupported locality key [" + key + "]");
                }
            }
        }
    }

    protected void importTrophicLink(Map<String, String> properties) {
        if (properties.containsKey(Study.TITLE)) {
            try {
                Study study = nodeFactory.getOrCreateStudy(properties.get(Study.TITLE),
                        properties.get(Study.CONTRIBUTOR),
                        null, null, properties.get(Study.DESCRIPTION), properties.get(Study.PUBLICATION_YEAR), "SPIRE");
                Specimen predator = createSpecimen(properties.get(PREDATOR_NAME));
                String country = properties.get(COUNTRY);
                LatLng latLng = geoLookup.get(country);
                if (latLng == null) {
                    LOG.warn("failed to find location for county [" + country + "]");
                } else {
                    Location location = nodeFactory.getOrCreateLocation(latLng.getLat(), latLng.getLng(), null);
                    predator.caughtIn(location);
                    String habitat = properties.get(OF_HABITAT);
                    if (StringUtils.isNotBlank(habitat)) {
                        try {
                            List<EnvoTerm> envoTerms = envoService.lookupBySPIREHabitat(habitat);
                            for (EnvoTerm envoTerm : envoTerms) {
                                addEnvironment(location, envoTerm.getId(), envoTerm.getName());
                            }
                            if (envoTerms.size() == 0) {
                                addEnvironment(location, "SPIRE:" + habitat, habitat);
                            }
                        } catch (EnvoServiceException e) {
                            LOG.warn("unexpected problem during lookup environment for habitat [" + habitat + "]", e);
                        }

                    }

                }
                study.collected(predator);
                Specimen prey = createSpecimen(properties.get(PREY_NAME));
                predator.ate(prey);
            } catch (NodeFactoryException e) {
                LOG.warn("failed to import trophic link with properties [" + properties + "]");
            }
        } else {
            LOG.warn("skipping trophic link: missing study title for trophic link properties [" + properties + "]");
        }
    }

    private void addEnvironment(Location location, String id, String name) {
        Environment environment = nodeFactory.getOrCreateEnvironment(id, name);
        location.addEnvironment(environment);
    }

    private Specimen createSpecimen(String taxonName) throws NodeFactoryException {
        taxonName = taxonName.replaceAll("_", " ");
        Specimen specimen = nodeFactory.createSpecimen(taxonName);
        if (taxonName.contains("adult")) {
            specimen.setLifeStage(LifeStage.ADULT);
        } else if (taxonName.contains("juvenile")) {
            specimen.setLifeStage(LifeStage.JUVENILE);
        } else if (taxonName.contains(" egg")) {
            specimen.setLifeStage(LifeStage.JUVENILE);
        } else if (taxonName.contains("larvae")) {
            specimen.setLifeStage(LifeStage.LARVA);
        } else if (taxonName.contains("immature")) {
            specimen.setLifeStage(LifeStage.IMMATURE);
        } else if (taxonName.contains("nymphs")) {
            specimen.setLifeStage(LifeStage.NYMPH);
        }
        return specimen;
    }


    private Model buildModel() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        BufferedReader bufferedReader = FileUtils.getUncompressedBufferedReader(getClass().getResourceAsStream("spire/allFoodWebStudies.owl"), CharsetConstant.UTF8);
        model.read(bufferedReader, null);
        return model;
    }

    private String getTrimmedObject(Statement next1) {
        String s = next1.getObject().toString().replaceAll("http://spire.umbc.edu/ethan/", "");
        s = s.replace("http://spire.umbc.edu/ontologies/SpireEcoConcepts.owl#", "");
        return s.replaceAll("\\^\\^http://www.w3.org/2001/XMLSchema#string", "");
    }

    public ImportFilter getImportFilter() {
        return importFilter;
    }

    public void setEnvoService(EnvoService envoService) {
        this.envoService = envoService;
    }
}
