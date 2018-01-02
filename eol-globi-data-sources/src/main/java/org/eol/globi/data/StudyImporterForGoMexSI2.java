package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForGoMexSI2 extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForGoMexSI2.class);
    public static final String GOMEXI_SOURCE_DESCRIPTION = "http://gomexsi.tamucc.edu";
    public static final String STOMACH_COUNT_TOTAL = "stomachCountTotal";
    public static final String STOMACH_COUNT_WITH_FOOD = "stomachCountWithFood";
    public static final String STOMACH_COUNT_WITHOUT_FOOD = "stomachCountWithoutFood";
    public static final String GOMEXSI_NAMESPACE = "GOMEXSI:";

    private static final Collection KNOWN_INVALID_DOUBLE_STRINGS = new ArrayList<String>() {{
        add("na");
        add("> .001");
        add("tr");
        add("< 2");
        add("> .005");
        add("< .005");
        add("< 0.0001");
        add("*");
        add("< 0.01");
        add("< 0.05");
    }};

    private static final Collection KNOWN_INVALID_INTEGER_STRINGS = new ArrayList<String>() {{
        add("na");
        add("numerous");
        add("a few");
        add("several");
    }};

    public StudyImporterForGoMexSI2(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected String getPreyResourcePath() {
        return getResourcePath("/Prey.csv");
    }

    private String getResourcePath(String resourceName) {
        return getDataset().getResourceURI(resourceName).toString();
    }

    protected String getPredatorResourcePath() {
        return getResourcePath("/Predators.csv");
    }

    protected String getReferencesResourcePath() {
        return getResourcePath("/References.csv");
    }

    protected String getLocationsResourcePath() {
        return getResourcePath("/Locations.csv");
    }

    @Override
    public void importStudy() throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy(
                new StudyImpl("GoMexSI", GOMEXI_SOURCE_DESCRIPTION, null, ExternalIdUtil.toCitation("James D. Simons", "<a href=\"http://www.ingentaconnect.com/content/umrsmas/bullmar/2013/00000089/00000001/art00009\">Building a Fisheries Trophic Interaction Database for Management and Modeling Research in the Gulf of Mexico Large Marine Ecosystem.</a>", null)));
        final Map<String, Map<String, String>> predatorIdToPredatorNames = new HashMap<String, Map<String, String>>();
        final Map<String, List<Map<String, String>>> predatorIdToPreyNames = new HashMap<String, List<Map<String, String>>>();
        Map<String, Study> referenceIdToStudy = new HashMap<String, Study>();
        addSpecimen(getPredatorResourcePath(), "PRED_", new ParseEventHandler() {
            @Override
            public void onSpecimen(String predatorUID, Map<String, String> properties) {
                predatorIdToPredatorNames.put(predatorUID, properties);
            }
        });
        addSpecimen(getPreyResourcePath(), "PREY_", new ParseEventHandler() {
            @Override
            public void onSpecimen(String predatorUID, Map<String, String> properties) {
                List<Map<String, String>> preyList = predatorIdToPreyNames.get(predatorUID);
                if (preyList == null) {
                    preyList = new ArrayList<Map<String, String>>();
                    predatorIdToPreyNames.put(predatorUID, preyList);
                }
                preyList.add(properties);
            }
        });
        addReferences(referenceIdToStudy);
        addObservations(predatorIdToPredatorNames, referenceIdToStudy, predatorIdToPreyNames, study);
    }

    protected void addReferences(Map<String, Study> referenceIdToStudy) throws StudyImporterException {
        String referenceResource = getReferencesResourcePath();

        try {
            LabeledCSVParser parser = parserFactory.createParser(referenceResource, CharsetConstant.UTF8);
            Map<String, String> studyContributorMap = collectContributors(referenceResource, parser);

            parser = parserFactory.createParser(referenceResource, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String refId = getMandatoryValue(referenceResource, parser, "DATA_ID");
                Study study = referenceIdToStudy.get(refId);
                if (study == null) {
                    addNewStudy(referenceIdToStudy, referenceResource, parser, refId, studyContributorMap.get(refId));
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to open resource [" + referenceResource + "]", e);
        }

    }

    protected static Map<String, String> collectContributors(String referenceResource, LabeledCSVParser parser) throws IOException, StudyImporterException {
        Map<String, String> studyContributorMap = new HashMap<String, String>();
        while (parser.getLine() != null) {
            String refId = getMandatoryValue(referenceResource, parser, "DATA_ID");
            String lastName = getMandatoryValue(referenceResource, parser, "AUTH_L_NAME");
            String firstName = getMandatoryValue(referenceResource, parser, "AUTH_F_NAME");
            String contributors = studyContributorMap.get(refId);
            studyContributorMap.put(refId, updateContributorList(lastName, firstName, contributors == null ? "" : contributors));
        }
        return studyContributorMap;
    }

    private static String updateContributorList(String lastName, String firstName, String contributor) {
        String name = firstName + " " + lastName;
        return StringUtils.isBlank(contributor) ? name : (contributor + ", " + name);
    }

    private void addNewStudy(Map<String, Study> referenceIdToStudy, String referenceResource, LabeledCSVParser parser, String refId, String contributors) throws StudyImporterException {
        Study study;
        String refTag = getMandatoryValue(referenceResource, parser, "REF_TAG");
        String externalId = getMandatoryValue(referenceResource, parser, "GAME_ID");
        String description = getMandatoryValue(referenceResource, parser, "TITLE_REF");
        String publicationYear = getMandatoryValue(referenceResource, parser, "YEAR_PUB");

        study = nodeFactory.getOrCreateStudy(
                new StudyImpl(refTag, getSourceCitation(), null, ExternalIdUtil.toCitation(contributors, description, publicationYear)));
        if (StringUtils.isNotBlank(externalId)) {
            study.setExternalId(ExternalIdUtil.urlForExternalId(TaxonomyProvider.ID_PREFIX_GAME + externalId));
        }
        referenceIdToStudy.put(refId, study);
    }

    private void addObservations(Map<String, Map<String, String>> predatorIdToPredatorSpecimen, Map<String, Study> refIdToStudyMap, Map<String, List<Map<String, String>>> predatorUIToPreyLists, Study metaStudy) throws StudyImporterException {
        String locationResource = getLocationsResourcePath();
        try {
            TermLookupService cmecsService = new CMECSService();
            LabeledCSVParser parser = parserFactory.createParser(locationResource, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String refId = getMandatoryValue(locationResource, parser, "DATA_ID");
                if (!refIdToStudyMap.containsKey(refId)) {
                    getLogger().warn(metaStudy, "failed to find study for ref id [" + refId + "] on related to observation location in [" + locationResource + ":" + parser.getLastLineNumber() + "]");
                } else {
                    Study study = refIdToStudyMap.get(refId);
                    String specimenId = getMandatoryValue(locationResource, parser, "PRED_ID");

                    Location location = parseLocation(locationResource, parser);

                    Location locationNode = nodeFactory.getOrCreateLocation(location);

                    enrichLocation(metaStudy, locationResource, cmecsService, parser, locationNode);

                    String predatorId = refId + specimenId;
                    Map<String, String> predatorProperties = predatorIdToPredatorSpecimen.get(predatorId);
                    if (predatorProperties == null) {
                        getLogger().warn(study, "failed to lookup predator [" + refId + ":" + specimenId + "] for location at [" + locationResource + ":" + (parser.getLastLineNumber() + 1) + "]");
                    } else {
                        addObservation(predatorUIToPreyLists, parser, study, locationNode, predatorId, predatorProperties);
                    }
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to open resource [" + locationResource + "]", e);
        }

    }

    private void enrichLocation(Study metaStudy, String locationResource, TermLookupService cmecsService, LabeledCSVParser parser, Location location) throws StudyImporterException {
        String habitatSystem = getMandatoryValue(locationResource, parser, "HAB_SYSTEM");
        String habitatSubsystem = getMandatoryValue(locationResource, parser, "HAB_SUBSYSTEM");
        String habitatTidalZone = getMandatoryValue(locationResource, parser, "TIDAL_ZONE");
        enrichLocation(metaStudy, locationResource, cmecsService, parser, location, habitatSystem, habitatSubsystem, habitatTidalZone);
    }

    protected static Location parseLocation(String locationResource, LabeledCSVParser parser) throws StudyImporterException {
        Double latitude = getMandatoryDoubleValue(locationResource, parser, "LOC_CENTR_LAT");
        Double longitude = getMandatoryDoubleValue(locationResource, parser, "LOC_CENTR_LONG");
        Double depth = getMandatoryDoubleValue(locationResource, parser, "MN_DEP_SAMP");
        final String polyCoords = parser.getValueByLabel("LOC_POLY_COORDS");
        String footprintWKT = polyCoordsToWKT(polyCoords);
        final LocationImpl location = new LocationImpl(latitude, longitude
                , depth == null ? null : -depth
                , StringUtils.isBlank(footprintWKT) ? null : StringUtils.trim(footprintWKT));

        final String locality = parser.getValueByLabel("LOCALE_NAME");
        if (StringUtils.isNotBlank(locality)) {
            location.setLocality(locality);
        }
        return location;
    }

    protected static String polyCoordsToWKT(String polyCoords) {
        String footprintWKT = null;
        if (StringUtils.isNotBlank(polyCoords)) {
            footprintWKT = "POLYGON" + polyCoords.replace(" ", "").replace(",", " ").replace(") (", ",");
        }
        return footprintWKT;
    }

    private Location enrichLocation(Study metaStudy, String locationResource, TermLookupService cmecsService, LabeledCSVParser parser, Location location, String habitatSystem, String habitatSubsystem, String habitatTidalZone) {
        if (location != null) {
            List<Term> terms;
            String cmecsLabel = habitatSystem + " " + habitatSubsystem + " " + habitatTidalZone;
            String msg = "failed to map CMECS habitat [" + cmecsLabel + "] on line [" + parser.lastLineNumber() + "] of image [" + locationResource + "]";
            try {
                terms = cmecsService.lookupTermByName(cmecsLabel);
                if (terms.size() == 0) {
                    getLogger().warn(metaStudy, msg);
                }
                nodeFactory.addEnvironmentToLocation(location, terms);
            } catch (TermLookupServiceException e) {
                getLogger().warn(metaStudy, msg);
            }

        }
        return location;
    }

    private void addObservation(Map<String, List<Map<String, String>>> predatorUIToPreyLists, LabeledCSVParser parser, Study study, Location location, String predatorId, Map<String, String> predatorProperties) throws StudyImporterException {
        try {
            Specimen predatorSpecimen = createSpecimen(study, predatorProperties);
            setBasisOfRecordAsLiterature(predatorSpecimen);
            predatorSpecimen.setExternalId(predatorId);
            if (location == null) {
                getLogger().warn(study, "no location for predator with id [" + predatorSpecimen.getExternalId() + "]");
            } else {
                predatorSpecimen.caughtIn(location);
            }
            List<Map<String, String>> preyList = predatorUIToPreyLists.get(predatorId);
            checkStomachDataConsistency(predatorId, predatorProperties, preyList, study);
            if (preyList != null) {
                for (Map<String, String> preyProperties : preyList) {
                    if (preyProperties != null) {
                        try {
                            Specimen prey = createSpecimen(study, preyProperties);
                            setBasisOfRecordAsLiterature(prey);
                            prey.caughtIn(location);
                            predatorSpecimen.ate(prey);
                        } catch (NodeFactoryException e) {
                            getLogger().warn(study, "failed to add prey [" + preyProperties + "] for predator with id + [" + predatorId + "]: [" + predatorProperties + "]: [" + e.getMessage() + "]");
                        }
                    }
                }
            }
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create specimen for location on line [" + parser.getLastLineNumber() + "]", e);
        }
    }

    private void checkStomachDataConsistency(String predatorId, Map<String, String> predatorProperties, List<Map<String, String>> preyList, Study study) throws StudyImporterException {
        Integer total = integerValueOrNull(predatorProperties, STOMACH_COUNT_TOTAL);
        Integer withoutFood = integerValueOrNull(predatorProperties, STOMACH_COUNT_WITHOUT_FOOD);
        Integer withFood = integerValueOrNull(predatorProperties, STOMACH_COUNT_WITH_FOOD);
        if (total != null && withoutFood != null) {
            if (preyList == null || preyList.size() == 0) {
                if (!total.equals(withoutFood)) {
                    getLogger().warn(study, "no prey for predator with id [" + predatorId + "], but found [" + withFood + "] stomachs with food");
                }
            } else {
                if (total.equals(withoutFood)) {
                    getLogger().warn(study, "found prey for predator with id [" + predatorId + "], but found only stomachs without food");
                }
            }
        }
    }

    private Integer integerValueOrNull(Map<String, String> props, String key) throws StudyImporterException {
        String value = props.get(key);
        try {
            return StringUtils.isBlank(value) || KNOWN_INVALID_INTEGER_STRINGS.contains(StringUtils.lowerCase(value)) ? null : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new StudyImporterException(errMsg(props, key, value), ex);
        }
    }

    private Double doubleValueOrNull(Map<String, String> props, String key) throws StudyImporterException {
        String value = props.get(key);
        try {
            return StringUtils.isBlank(value) || KNOWN_INVALID_DOUBLE_STRINGS.contains(StringUtils.lowerCase(value)) ? null : Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            final String msg = errMsg(props, key, value);
            throw new StudyImporterException(msg, ex);
        }
    }

    private String errMsg(Map<String, String> props, String key, String value) {
        return "failed to parse key [" + key + "] with value [" + value + "] in properties [" + props + "]";
    }

    private Specimen createSpecimen(Study study, Map<String, String> properties) throws StudyImporterException {
        Specimen specimen = nodeFactory.createSpecimen(study, new TaxonImpl(properties.get(PropertyAndValueDictionary.NAME), null));
        specimen.setLengthInMm(doubleValueOrNull(properties, SpecimenConstant.LENGTH_IN_MM));
        specimen.setFrequencyOfOccurrence(doubleValueOrNull(properties, SpecimenConstant.FREQUENCY_OF_OCCURRENCE));
        setSpecimenProperty(specimen, SpecimenConstant.FREQUENCY_OF_OCCURRENCE_PERCENT, properties);
        specimen.setTotalCount(integerValueOrNull(properties, SpecimenConstant.TOTAL_COUNT));
        setSpecimenProperty(specimen, SpecimenConstant.TOTAL_COUNT_PERCENT, properties);
        specimen.setTotalVolumeInMl(doubleValueOrNull(properties, SpecimenConstant.TOTAL_VOLUME_IN_ML));
        setSpecimenProperty(specimen, SpecimenConstant.TOTAL_VOLUME_PERCENT, properties);
        addLifeStage(properties, specimen);
        addPhysiologicalState(properties, specimen);
        addBodyPart(properties, specimen);

        // add all original GoMexSI properties for completeness
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getKey().startsWith(GOMEXSI_NAMESPACE)) {
                specimen.setProperty(entry.getKey(), entry.getValue());
            }
        }
        return specimen;
    }

    private void setSpecimenProperty(Specimen specimen, String name, Map<String, String> properties) throws StudyImporterException {
        specimen.setProperty(name, doubleValueOrNull(properties, name));
    }

    private void addLifeStage(Map<String, String> properties, Specimen specimen) throws StudyImporterException {
        try {
            String lifeStageName = properties.get(SpecimenConstant.LIFE_STAGE_LABEL);
            Term term = nodeFactory.getOrCreateLifeStage(GOMEXSI_NAMESPACE + lifeStageName, lifeStageName);
            specimen.setLifeStage(term);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to map life stage", e);
        }
    }

    private void addPhysiologicalState(Map<String, String> properties, Specimen specimen) throws StudyImporterException {
        try {
            String name = properties.get(SpecimenConstant.PHYSIOLOGICAL_STATE_LABEL);
            Term term = nodeFactory.getOrCreatePhysiologicalState(GOMEXSI_NAMESPACE + name, name);
            specimen.setPhysiologicalState(term);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to map life stage", e);
        }
    }

    private void addBodyPart(Map<String, String> properties, Specimen specimen) throws StudyImporterException {
        try {
            String name = properties.get(SpecimenConstant.BODY_PART_LABEL);
            Term term = nodeFactory.getOrCreateBodyPart(GOMEXSI_NAMESPACE + name, name);
            specimen.setBodyPart(term);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to map body part", e);
        }
    }

    private static Double getMandatoryDoubleValue(String locationResource, LabeledCSVParser parser, String label) throws StudyImporterException {
        String value = getMandatoryValue(locationResource, parser, label);
        try {
            return "NA".equals(value) || value == null || value.trim().length() == 0 ? null : Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new StudyImporterException("failed to parse [" + label + "] value [" + value + "] at line [" + parser.getLastLineNumber() + "]", ex);
        }
    }

    private void addSpecimen(String datafile, String scientificNameLabel, ParseEventHandler specimenListener) throws StudyImporterException {
        try {
            LabeledCSVParser parser = parserFactory.createParser(datafile, CharsetConstant.UTF8);
            parseSpecimen(datafile, scientificNameLabel, specimenListener, parser);
        } catch (IOException e) {
            throw new StudyImporterException("failed to open resource [" + datafile + "]", e);
        }
    }

    protected static void parseSpecimen(String datafile, String columnNamePrefix, ParseEventHandler specimenListener, LabeledCSVParser parser) throws IOException, StudyImporterException {
        while (parser.getLine() != null) {
            Map<String, String> properties = new HashMap<String, String>();
            addOptionalProperty(parser, "TOT_WO_FD", STOMACH_COUNT_WITHOUT_FOOD, properties);
            addOptionalProperty(parser, "TOT_W_FD", STOMACH_COUNT_WITH_FOOD, properties);
            addOptionalProperty(parser, "TOT_PRED_STOM_EXAM", STOMACH_COUNT_TOTAL, properties);
            addOptionalProperty(parser, columnNamePrefix + "MN_LEN", SpecimenConstant.LENGTH_IN_MM, properties);
            addOptionalProperty(parser, columnNamePrefix + "LIFE_HIST_STAGE", SpecimenConstant.LIFE_STAGE_LABEL, properties);
            addOptionalProperty(parser, "PHYSIOLOG_STATE", SpecimenConstant.PHYSIOLOGICAL_STATE_LABEL, properties);
            addOptionalProperty(parser, columnNamePrefix + "PARTS", SpecimenConstant.BODY_PART_LABEL, properties);
            addOptionalProperty(parser, "N_CONS", SpecimenConstant.TOTAL_COUNT, properties);
            addOptionalProperty(parser, "PCT_N_CONS", SpecimenConstant.TOTAL_COUNT_PERCENT, properties);
            addOptionalProperty(parser, "VOL_CONS", SpecimenConstant.TOTAL_VOLUME_IN_ML, properties);
            addOptionalProperty(parser, "PCT_VOL_CONS", SpecimenConstant.TOTAL_VOLUME_PERCENT, properties);
            addOptionalProperty(parser, "FREQ_OCC", SpecimenConstant.FREQUENCY_OF_OCCURRENCE, properties);
            addOptionalProperty(parser, "PCT_FREQ_OCC", SpecimenConstant.FREQUENCY_OF_OCCURRENCE_PERCENT, properties);
            String taxonName = getMandatoryValue(datafile, parser, columnNamePrefix + "DATABASE_NAME");
            if (StringUtils.isBlank(taxonName)) {
                taxonName = getMandatoryValue(datafile, parser, columnNamePrefix + "SOURCE_NAME");
            }
            properties.put(PropertyAndValueDictionary.NAME, taxonName);

            String refId = getMandatoryValue(datafile, parser, "DATA_ID");
            String specimenId = getMandatoryValue(datafile, parser, "PRED_ID");
            String predatorUID = refId + specimenId;

            // add all original data in GoMexSI namespace
            String[] labels = parser.getLabels();
            for (String label : labels) {
                properties.put(GOMEXSI_NAMESPACE + label, parser.getValueByLabel(label));
            }

            specimenListener.onSpecimen(predatorUID, properties);
        }
    }

    private static void addOptionalProperty(LabeledCSVParser parser, String label, String normalizedName, Map<String, String> properties) {
        String value = parser.getValueByLabel(label);
        value = value == null || "NA".equalsIgnoreCase(value) ? null : value;
        if (value != null) {
            properties.put(normalizedName, value);
        }
    }

    private static String getMandatoryValue(String datafile, LabeledCSVParser parser, String label) throws StudyImporterException {
        String value = parser.getValueByLabel(label);
        if (value == null) {
            throw new StudyImporterException("missing mandatory column [" + label + "] in [" + datafile + "]:[" + parser.getLastLineNumber() + "]");
        }
        return "NA".equals(value) ? "" : value;
    }

}
