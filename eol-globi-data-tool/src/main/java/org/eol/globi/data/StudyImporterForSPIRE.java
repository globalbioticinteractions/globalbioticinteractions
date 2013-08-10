package org.eol.globi.data;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import uk.me.jstott.jcoord.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
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
    public static final String KNOX = "G. A. Knox, Antarctic marine ecosystems. In: Antarctic Ecology, M. W. Holdgate, Ed. (Academic Press, New York, 1970) 1:69-96, from p. 87.";
    public static final String AULIO = "K. Aulio, K. Jumppanen, H. Molsa, J. Nevalainen, M. Rajasilta, I. Vuorinen, Litoraalin merkitys Pyhajarven kalatuotannolle, Sakylan Pyhajarven Tila Ja Biologinen Tuotanto (Lounais-Suomen Vesiensuojeluyhdistys R. Y., Turku, Finland, 1981) 47:173-176.";
    private Map<String, LatLng> geoLookup = buildGEOLookup();

    private TrophicLinkListener trophicLinkListener = new TrophicLinkListener() {
        @Override
        public void newLink(Map<String, String> properties) {
            importTrophicLink(properties);
        }
    };


    public StudyImporterForSPIRE(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected static void parseTitlesAndAuthors(String titlesAndAuthors, Map<String, String> properties) throws StudyImporterException {
        if (titlesAndAuthors.contains(KNOX)) {
            properties.put(Study.PUBLICATION_YEAR, "1970");
            properties.put(Study.TITLE, "Knox 1970");
            properties.put(Study.DESCRIPTION, "Antarctic marine ecosystems. In: Antarctic Ecology, M. W. Holdgate, Ed. (Academic Press, New York, 1970) 1:69-96, from p. 87.");
            properties.put(Study.CONTRIBUTOR, "G. A. Knox");
        } else if (titlesAndAuthors.contains(AULIO)) {
            properties.put(Study.PUBLICATION_YEAR, "1981");
            properties.put(Study.TITLE, "Aulio 1981");
            properties.put(Study.DESCRIPTION, "Litoraalin merkitys Pyhajarven kalatuotannolle, Sakylan Pyhajarven Tila Ja Biologinen Tuotanto (Lounais-Suomen Vesiensuojeluyhdistys R. Y., Turku, Finland, 1981) 47:173-176.");
            properties.put(Study.CONTRIBUTOR, "K. Aulio, K. Jumppanen, H. Molsa, J. Nevalainen, M. Rajasilta, I. Vuorinen");
        } else if (titlesAndAuthors.contains("Animal Diversity Web")) {
            properties.put(Study.PUBLICATION_YEAR, "n/a");
            properties.put(Study.TITLE, "Animal Diversity Web");
            properties.put(Study.DESCRIPTION, "Animal Diversity Web (ADW) is an online database of animal natural history, distribution, classification, and conservation biology at the University of Michigan. http://animaldiversity.ummz.umich.edu/");
            properties.put(Study.CONTRIBUTOR, "University of Michigan");
        } else if (titlesAndAuthors.contains("Waide RB, Reagan WB (eds) (1996) The food web of a tropical rainforest. University of Chicago Press, Chicago")) {
            properties.put(Study.PUBLICATION_YEAR, "1996");
            properties.put(Study.TITLE, "Waide et. al 1996");
            properties.put(Study.DESCRIPTION, "The food web of a tropical rainforest. University of Chicago Press, Chicago");
            properties.put(Study.CONTRIBUTOR, "Waide RB, Reagan WB (eds)");
        } else if (titlesAndAuthors.contains("M. E. Blindloss, A. V. Holden, A. E. Bailey-Watts and I. R. Smith, Phytoplankton production, chemical and physical conditions in Loch Leven. Productivity Problems of Freshwaters (Eds. Z. Kajak and A. Hillbricht-Ilkowska), Polish Scientific Publishers, War")) {
            properties.put(Study.PUBLICATION_YEAR, "n/a");
            properties.put(Study.TITLE, "Blindloss");
            properties.put(Study.DESCRIPTION, "Phytoplankton production, chemical and physical conditions in Loch Leven. Productivity Problems of Freshwaters (Eds. Z. Kajak and A. Hillbricht-Ilkowska), Polish Scientific Publishers, War");
            properties.put(Study.CONTRIBUTOR, "M. E. Blindloss, A. V. Holden, A. E. Bailey-Watts and I. R. Smith");
        } else if (titlesAndAuthors.contains("K. Kuusela, Early summer ecology and community structure of the macrozoobenthos on stones in the Javajankoski rapids on the river Lestijoki, Finland, Acta Universitatis Ouluensis (Ser. A, no. 87, Oulu, Finland, 1979).")) {
            properties.put(Study.PUBLICATION_YEAR, "1979");
            properties.put(Study.TITLE, "Kuusela 1979");
            properties.put(Study.DESCRIPTION, "Early summer ecology and community structure of the macrozoobenthos on stones in the Javajankoski rapids on the river Lestijoki, Finland, Acta Universitatis Ouluensis (Ser. A, no. 87, Oulu, Finland, 1979).");
            properties.put(Study.CONTRIBUTOR, "K. Kuusela");
        } else if (titlesAndAuthors.contains("N. A. Mackintosh, A survey of antarctic biology up to 1945. In: Biologie antarctique, R. Carrick, M. Holdgate, J. Prevost, Eds. (Hermann, Paris, 1964), pp. 3-38.")) {
            properties.put(Study.PUBLICATION_YEAR, "1964");
            properties.put(Study.TITLE, "Mackintosh 1964");
            properties.put(Study.DESCRIPTION, "A survey of antarctic biology up to 1945. In: Biologie antarctique, R. Carrick, M. Holdgate, J. Prevost, Eds. (Hermann, Paris, 1964), pp. 3-38.");
            properties.put(Study.CONTRIBUTOR, "N. A. Mackintosh");
        } else if (titlesAndAuthors.contains("R. R. Askew, 1975.  The organisation of chalcid-dominated parasitoid communities centred upon endophytic hosts.  In:  Evolutionary Strategies of Parasitic Insects and Mites, P. W. Price, Ed. (Plenum Press, New York) pp. 130-153, from p. 132.")) {
            properties.put(Study.PUBLICATION_YEAR, "1975");
            properties.put(Study.TITLE, "Askew 1975");
            properties.put(Study.DESCRIPTION, "The organisation of chalcid-dominated parasitoid communities centred upon endophytic hosts.  In:  Evolutionary Strategies of Parasitic Insects and Mites, P. W. Price, Ed. (Plenum Press, New York) pp. 130-153, from p. 132.");
            properties.put(Study.CONTRIBUTOR, "R. R. Askew");
        } else if (titlesAndAuthors.contains("N. C. Collins, R. Mitchell and R. G. Wiegert, 1976.  Functional analysis of a thermal spring ecosystem, with an evaluation of the role of consumers. Ecology 57:1221-1232, from p. 1222.")) {
            properties.put(Study.PUBLICATION_YEAR, "1976");
            properties.put(Study.TITLE, "Collins et al. 1976");
            properties.put(Study.DESCRIPTION, "Functional analysis of a thermal spring ecosystem, with an evaluation of the role of consumers. Ecology 57:1221-1232, from p. 1222.");
            properties.put(Study.CONTRIBUTOR, "N. C. Collins, R. Mitchell and R. G. Wiegert");
        } else if (titlesAndAuthors.contains("R. R. Askew, 1961.  On the biology of the inhabitants of oak galls of Cynipidae (Hymenoptera) in Britain.  Trans. Soc. Brit. Entomol. 14:237-268, from p. 242.")) {
            properties.put(Study.PUBLICATION_YEAR, "1961");
            properties.put(Study.TITLE, "Askew 1961");
            properties.put(Study.DESCRIPTION, "On the biology of the inhabitants of oak galls of Cynipidae (Hymenoptera) in Britain.  Trans. Soc. Brit. Entomol. 14:237-268, from p. 242.");
            properties.put(Study.CONTRIBUTOR, "R. R. Askew");
        } else if (titlesAndAuthors.contains("K. Hogetsu, Biological productivity of some coastal regions of Japan. In: Marine Production Mechanisms, M. J. Dunbar, Ed. (International Biological Programme Series, no. 20, Cambridge Univ. Press, Cambridge, England, 1979), pp. 71-87, from p. 74.")) {
            properties.put(Study.PUBLICATION_YEAR, "1979");
            properties.put(Study.TITLE, "Hogetsu 1979");
            properties.put(Study.DESCRIPTION, "Biological productivity of some coastal regions of Japan. In: Marine Production Mechanisms, M. J. Dunbar, Ed. (International Biological Programme Series, no. 20, Cambridge Univ. Press, Cambridge, England, 1979), pp. 71-87, from p. 74.");
            properties.put(Study.CONTRIBUTOR, "K. Hogetsu");
        } else if (titlesAndAuthors.contains("J. Sarvala, Paarjarven energiatalous, Luonnon Tutkija 78:181-190, from p. 185.")) {
            properties.put(Study.PUBLICATION_YEAR, "n/a");
            properties.put(Study.TITLE, "Sarvala");
            properties.put(Study.DESCRIPTION, "Paarjarven energiatalous, Luonnon Tutkija 78:181-190, from p. 185.");
            properties.put(Study.CONTRIBUTOR, "J. Sarvala");
        } else if (titlesAndAuthors.contains("R. M. Badcock, 1949.  Studies in stream life in tributaries of the Welsh Dee. J. Anim. Ecol. 18:193-208, from pp. 202-206 and Price, P. W., 1984, Insect Ecology, 2nd ed., New York: John Wiley, p. 23")) {
            properties.put(Study.PUBLICATION_YEAR, "1949");
            properties.put(Study.TITLE, "Badcock 1949");
            properties.put(Study.DESCRIPTION, "Studies in stream life in tributaries of the Welsh Dee. J. Anim. Ecol. 18:193-208, from pp. 202-206 and Price, P. W., 1984, Insect Ecology, 2nd ed., New York: John Wiley, p. 23");
            properties.put(Study.CONTRIBUTOR, "R. M. Badcock");
        } else if (titlesAndAuthors.contains("G. E. Walsh, An ecological study of a Hawaiian mangrove swamp. In: Estuaries, G. H. Lauff, Ed. (AAAS Publication 83, Washington, DC, 1967), pp. 420-431, from p. 429.")) {
            properties.put(Study.PUBLICATION_YEAR, "1967");
            properties.put(Study.TITLE, "Walsh 1967");
            properties.put(Study.DESCRIPTION, "An ecological study of a Hawaiian mangrove swamp. In: Estuaries, G. H. Lauff, Ed. (AAAS Publication 83, Washington, DC, 1967), pp. 420-431, from p. 429.");
            properties.put(Study.CONTRIBUTOR, "G. E. Walsh");
        } else if (titlesAndAuthors.contains("B. C. Patten and 40 co-authors, Total ecosystem model for a cove in Lake Texoma. In: Systems Analysis and Simulation in Ecology, B. C. Patten, Ed. (Academic Press, New York, 1975), 3:205-421, from pp. 236, 258, 268.")) {
            properties.put(Study.PUBLICATION_YEAR, "1975");
            properties.put(Study.TITLE, "Patten et al. 1975");
            properties.put(Study.DESCRIPTION, "An ecological study of a Hawaiian mangrove swamp. In: Estuaries, G. H. Lauff, Ed. (AAAS Publication 83, Washington, DC, 1967), pp. 420-431, from p. 429.");
            properties.put(Study.CONTRIBUTOR, "B. C. Patten and 40 co-authors");
        } else {
            parseGenericTitlesAndAuthors(titlesAndAuthors, properties);
            if (properties.containsKey(Study.PUBLICATION_YEAR)) {
                try {
                    Long.parseLong(properties.get(Study.PUBLICATION_YEAR));
                } catch (NumberFormatException ex) {
                    addUnformattedFields(titlesAndAuthors, properties);
                }
            } else {
                addUnformattedFields(titlesAndAuthors, properties);
            }
        }


    }

    private static void addUnformattedFields(String titlesAndAuthors, Map<String, String> properties) {
        properties.clear();
        properties.put(Study.TITLE, titlesAndAuthors);
        properties.put(Study.DESCRIPTION, titlesAndAuthors);
    }

    private static void parseGenericTitlesAndAuthors(String titlesAndAuthors, Map<String, String> properties) throws StudyImporterException {
        String[] split = titlesAndAuthors.split(",");

        List<String> authors = new ArrayList<String>();
        List<String> other = new ArrayList<String>();
        for (String part : split) {
            if (part.matches("(( )*\\w\\. )+[\\w.-]*")) {
                authors.add(part);
            } else {
                other.add(part);
            }
        }
        if (authors.size() == 0) {
            int i = titlesAndAuthors.indexOf("(");
            int j = titlesAndAuthors.indexOf(")");
            if (i > 0 && j > i) {
                parseTitlesAndAuthors(titlesAndAuthors, properties, i, j, " ");
            } else {
                i = titlesAndAuthors.indexOf(".");
                if (i > 5 && (i + 1) < titlesAndAuthors.length()) {
                    j = titlesAndAuthors.substring(i + 1).indexOf(".");
                    if (j > 0) {
                        parseTitlesAndAuthors(titlesAndAuthors, properties, i, i + j + 1, " ");
                    } else {
                        throw new StudyImporterException("failed to import study for [" + titlesAndAuthors + "]");
                    }
                } else {
                    i = titlesAndAuthors.indexOf(",");
                    if (i > 0 && (i + 1) < titlesAndAuthors.length()) {
                        j = titlesAndAuthors.substring(i + 1).indexOf(".");
                        parseTitlesAndAuthors(titlesAndAuthors, properties, i, i + j + 1, ",");
                    }
                }
            }
        } else {
            parseTitlesAndArticlesFormatA(properties, authors, other);
        }
    }

    private static void parseTitlesAndAuthors(String titlesAndAuthors, Map<String, String> properties, int i, int j, String authorSeparator) {
        String authorString = titlesAndAuthors.substring(0, i).trim();
        String publicationYear = titlesAndAuthors.substring(i + 1, j).trim();
        String description = titlesAndAuthors.substring(j + 1).trim();
        properties.put(Study.DESCRIPTION, description);
        properties.put(Study.PUBLICATION_YEAR, publicationYear);
        properties.put(Study.CONTRIBUTOR, authorString);
        String[] split1 = authorString.split(authorSeparator);
        String title = split1.length > 0 ? split1[0] : "";
        title = title.replace(",", "").trim();
        title += split1.length > 2 ? " et al. " : " ";
        properties.put(Study.TITLE, title + publicationYear);
    }

    private static void parseTitlesAndArticlesFormatA(Map<String, String> properties, List<String> authors, List<String> other) {
        String description = StringUtils.join(other, ",");
        int i = description.lastIndexOf("(");
        int j = description.lastIndexOf(")");
        String publicationYear = i > 0 && j > i ? (description.substring(i + 1, j)) : "";

        String author = StringUtils.join(authors, ",");


        String firstAuthor = authors.get(0).replaceAll("(( )*\\w\\. )+", "");
        String title = authors.size() > 1 ? firstAuthor + " et al." : firstAuthor;
        title += " " + publicationYear;

        properties.put(Study.TITLE, title);
        properties.put(Study.CONTRIBUTOR, author);
        properties.put(Study.DESCRIPTION, description.trim());
        properties.put(Study.PUBLICATION_YEAR, publicationYear);
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

        Map<String, String> properties = new HashMap<String, String>();
        ResIterator resIterator = model.listSubjects();
        while (resIterator.hasNext()) {
            Resource resource = resIterator.next();
            properties.clear();
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
        }

        // should probably retire this . . .
        return null;
    }

    private void parseStudyInfo(Map<String, String> properties, Statement next1) throws StudyImporterException {
        StmtIterator stmtIterator1 = next1.getObject().asResource().listProperties();
        while (stmtIterator1.hasNext()) {
            Statement next2 = stmtIterator1.next();
            String localName = next2.getPredicate().getLocalName();
            if ("locality".equals(localName)) {
                parseLocalityInfo(properties, getTrimmedObject(next2));
            } else if (OF_HABITAT.equals(localName)) {
                properties.put(localName, getTrimmedObject(next1));
            } else if ("titleAndAuthors".equals(localName)) {
                parseTitlesAndAuthors(getTrimmedObject(next2), properties);
            }
        }
    }

    private void parseLocalityInfo(Map<String, String> properties, String localityString) throws StudyImporterException {
        localityString = localityString.replaceAll("\\^\\^http://www.w3.org/2001/XMLSchema#string", "");
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

    private void importTrophicLink(Map<String, String> properties) {
        if (properties.containsKey(Study.TITLE)) {
            try {
                Study study = nodeFactory.getOrCreateStudy(properties.get(Study.TITLE),
                        properties.get(Study.CONTRIBUTOR),
                        null, null, properties.get(Study.DESCRIPTION), properties.get(Study.PUBLICATION_YEAR));
                Specimen predator = createSpecimen(properties.get(PREDATOR_NAME));
                LatLng latLng = geoLookup.get(COUNTRY);
                if (latLng != null) {
                    predator.caughtIn(nodeFactory.getOrCreateLocation(latLng.getLat(), latLng.getLng(), null));
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
        return next1.getObject().toString().replaceAll("http://spire.umbc.edu/ethan/", "");
    }
}
