package org.eol.globi.data;

import com.Ostermiller.util.MD5;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.LatLng;
import org.eol.globi.util.ExternalIdUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForSPIRE extends BaseStudyImporter {

    public static final String OF_HABITAT = "ofHabitat";
    public static final String COUNTRY = "Country";
    public static final String STATE = "State";
    public static final String LOCALITY = "Locality";
    public static final String PREDATOR_NAME = "predator";
    public static final String PREY_NAME = "prey";
    public static final String LOCALITY_ORIGINAL = "localityOriginal";
    public static final String SOURCE_SPIRE = "Semantic Prototypes in Research Ecoinformatics (SPIRE). Data provided by Joel Sachs. See also http://ebiquity.umbc.edu/get/a/publication/297.pdf .";

    private TrophicLinkListener trophicLinkListener = new TrophicLinkListener() {
        @Override
        public void newLink(Map<String, String> properties) {
            importTrophicLink(properties);
        }
    };

    public StudyImporterForSPIRE(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected static void parseTitlesAndAuthors(String titlesAndAuthors, Map<String, String> properties) {
        String titlesAndAuthors1 = titlesAndAuthors.replace("\n", "");
        String shortened = StringUtils.abbreviate(titlesAndAuthors1.
                replaceAll("(\\w(\\. )+)", "").trim(), 24);

        properties.put(Study.TITLE, shortened + MD5.getHashString(titlesAndAuthors1));
        // see https://github.com/jhpoelen/eol-globi-data/issues/28
        if ("Animal Diversity Web".equals(titlesAndAuthors1)) {
            titlesAndAuthors1 = "Myers, P., R. Espinosa, C. S. Parr, T. Jones, G. S. Hammond, and T. A. Dewey. 2013. The Animal Diversity Web (online). Accessed at http://animaldiversity.org.";
        } else if ("K. H. Mann, R. H. Britton, A. Kowalczewski, T. J. Lack, C. P. Mathews and I. McDonald, Productivity and energy flow at all trophic levels in the River Thames, England. In: Productivity Problems of Freshwaters, Z. Kajak and A.  Hillbricht-Ilkowska, Eds. (P".equals(titlesAndAuthors1)) {
            titlesAndAuthors1 = "Mann KH, Britton RH, Kowalczewski A, Lack TJ, Mathews CP, McDonald I (1972) Productivity and energy flow at all trophic levels in the River Thames, England. In: Kajak Z, Hillbricht-Ilkowska A (eds) Productivity problems of freshwaters. Polish Scientific, Warsaw, pp 579-596";
        }
        properties.put(Study.DESCRIPTION, titlesAndAuthors1);
    }

    public TrophicLinkListener getTrophicLinkListener() {
        return trophicLinkListener;
    }

    public void setTrophicLinkListener(TrophicLinkListener trophicLinkListener) {
        this.trophicLinkListener = trophicLinkListener;
    }


    @Override
    public Study importStudy() throws StudyImporterException {
        Model model;
        try {
            model = buildModel();
        } catch (IOException e) {
            throw new StudyImporterException("failed to import SPIRE", e);
        }

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
        properties.put(LOCALITY_ORIGINAL, localityString);
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
        if (isValid(properties)) {
            importValidLink(properties);
        }
    }

    private boolean isValid(Map<String, String> properties) {
        // see https://github.com/jhpoelen/eol-globi-data/issues/118
        boolean invalidInteraction = "Enhydra lutris".equals(properties.get(PREDATOR_NAME)) && "Castor canadensis".equals(properties.get(PREY_NAME));
        return properties.containsKey(Study.TITLE) && !invalidInteraction;
    }

    private void importValidLink(Map<String, String> properties) {
        Study study = nodeFactory.getOrCreateStudy(properties.get(Study.TITLE),
                SOURCE_SPIRE, properties.get(Study.DESCRIPTION));
        try {
            Specimen predator = createSpecimen(properties.get(PREDATOR_NAME), study);
            String locality = properties.get(LOCALITY_ORIGINAL);
            LatLng latLng = getGeoNamesService().findLatLng(locality);
            if (latLng == null) {
                getLogger().warn(study, "failed to find location for county [" + locality + "]");
            } else {
                Location location = nodeFactory.getOrCreateLocation(latLng.getLat(), latLng.getLng(), null);
                predator.caughtIn(location);
                String habitat = properties.get(OF_HABITAT);
                if (StringUtils.isNotBlank(habitat)) {
                    addEnvironment(location, "SPIRE:" + habitat, habitat);
                }
            }
            Specimen prey = createSpecimen(properties.get(PREY_NAME), study);
            predator.ate(prey);
        } catch (NodeFactoryException e) {
            getLogger().warn(study, "failed to import trophic link with properties [" + properties + "]: " + e.getMessage());
        } catch (IOException e) {
            getLogger().warn(study, "failed to import trophic link with properties [" + properties + "]: " + e.getMessage());
        }
    }

    private void addEnvironment(Location location, String id, String name) throws NodeFactoryException {
        nodeFactory.getOrCreateEnvironments(location, id, name);
    }

    private Specimen createSpecimen(String taxonName, Study study) throws NodeFactoryException {
        taxonName = taxonName.replaceAll("_", " ");
        Specimen specimen = nodeFactory.createSpecimen(study, taxonName);

        if (taxonName.contains("adult")) {
            addLifeStage(specimen, "adult");
        } else if (taxonName.contains("juvenile")) {
            addLifeStage(specimen, "juvenile");
        } else if (taxonName.contains(" egg")) {
            addLifeStage(specimen, "egg");
        } else if (taxonName.contains("larvae")) {
            addLifeStage(specimen, "larvae");
        } else if (taxonName.contains("immature")) {
            addLifeStage(specimen, "immature");
        } else if (taxonName.contains("nymphs")) {
            addLifeStage(specimen, "nymphs");
        }


        return specimen;
    }

    private void addLifeStage(Specimen specimen, String name) throws NodeFactoryException {
        Term terms;
        terms = nodeFactory.getOrCreateLifeStage("SPIRE:" + name, name);
        specimen.setLifeStage(terms);
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

}
