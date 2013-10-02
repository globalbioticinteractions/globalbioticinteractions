package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.BodyPartLookupService;
import org.eol.globi.service.LifeStageLookupService;
import org.eol.globi.service.LookupServiceException;
import org.eol.globi.service.PhysiologicalStateLookupService;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForGoMexSI extends BaseStudyImporter {

    public static final String GOMEXSI_URL = "http://gomexsi.tamucc.edu";
    private static final Log LOG = LogFactory.getLog(StudyImporterForGoMexSI.class);
    public static final String STOMACH_COUNT_TOTAL = "stomachCountTotal";
    public static final String STOMACH_COUNT_WITH_FOOD = "stomachCountWithFood";
    public static final String STOMACH_COUNT_WITHOUT_FOOD = "stomachCountWithoutFood";

    public StudyImporterForGoMexSI(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected String getPreyResourcePath() {
        return getResourcePackage() + "/Prey.csv";
    }

    private String getResourcePackage() {
        return "gomexsi";
    }

    protected String getPredatorResourcePath() {
        return getResourcePackage() + "/Predators.csv";
    }

    protected String getReferencesResourcePath() {
        return getResourcePackage() + "/References.csv";
    }

    protected String getLocationsResourcePath() {
        return getResourcePackage() + "/Locations.csv";
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        final Map<String, Map<String, String>> predatorIdToPredatorNames = new HashMap<String, Map<String, String>>();
        final Map<String, List<Map<String, String>>> predatorIdToPreyNames = new HashMap<String, List<Map<String, String>>>();
        Map<String, Study> referenceIdToStudy = new HashMap<String, Study>();
        addSpecimen(getPredatorResourcePath(), "PRED_SCI_NAME", new ParseEventHandler() {
            @Override
            public void onSpecimen(String predatorUID, Map<String, String> properties) {
                predatorIdToPredatorNames.put(predatorUID, properties);
            }
        });
        addSpecimen(getPreyResourcePath(), "DATABASE_PREY_NAME", new ParseEventHandler() {
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
        addObservations(predatorIdToPredatorNames, referenceIdToStudy, predatorIdToPreyNames);

        // TODO figure out a way to introduce the separation of study and reference.
        return nodeFactory.getOrCreateStudy("GoMexSI",
                "James D. Simons",
                "Center for Coastal Studies, Texas A&M University - Corpus Christi, United States",
                "",
                "<a href=\"http://www.ingentaconnect.com/content/umrsmas/bullmar/2013/00000089/00000001/art00009\">Building a Fisheries Trophic Interaction Database for Management and Modeling Research in the Gulf of Mexico Large Marine Ecosystem.</a>"
                , null
                , GOMEXSI_URL);
    }

    private void addReferences(Map<String, Study> referenceIdToStudy) throws StudyImporterException {
        String referenceResource = getReferencesResourcePath();
        try {
            LabeledCSVParser parser = parserFactory.createParser(referenceResource, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String refId = getMandatoryValue(referenceResource, parser, "REF_ID");
                String lastName = getMandatoryValue(referenceResource, parser, "AUTH_L_NAME");
                String firstName = getMandatoryValue(referenceResource, parser, "AUTH_F_NAME");
                Study study = referenceIdToStudy.get(refId);
                if (study == null) {
                    addNewStudy(referenceIdToStudy, referenceResource, parser, refId, lastName, firstName);
                } else {
                    updateContributorList(lastName, firstName, study);
                }

            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to open resource [" + referenceResource + "]", e);
        }
    }

    private void updateContributorList(String lastName, String firstName, Study study) {
        Transaction transaction = nodeFactory.getGraphDb().beginTx();
        try {
            String contributor = study.getContributor();
            study.setContributor(contributor + ", " + firstName + " " + lastName);
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    private void addNewStudy(Map<String, Study> referenceIdToStudy, String referenceResource, LabeledCSVParser parser, String refId, String lastName, String firstName) throws StudyImporterException {
        Study study;
        String refTag = getMandatoryValue(referenceResource, parser, "REF_TAG");
        String externalId = getMandatoryValue(referenceResource, parser, "GAME_ID");
        String description = getMandatoryValue(referenceResource, parser, "TITLE_REF");
        String publicationYear = getMandatoryValue(referenceResource, parser, "YEAR_PUB");
        String universityName = getMandatoryValue(referenceResource, parser, "UNIV_NAME");
        String universityCity = getMandatoryValue(referenceResource, parser, "UNIV_CITY");

        String universityState = getMandatoryValue(referenceResource, parser, "UNIV_STATE");

        String universityCountry = getMandatoryValue(referenceResource, parser, "UNIV_COUNTRY");
        StringBuilder institution = new StringBuilder();
        if (StringUtils.isNotBlank(universityName)
                && StringUtils.isNotBlank(universityCity)
                && StringUtils.isNotBlank(universityState)
                && StringUtils.isNotBlank(universityCountry)) {
            institution.append(universityName);
            institution.append(", ");
            institution.append(universityCity);
            institution.append(", ");
            institution.append(universityState);
            institution.append(", ");
            institution.append(universityCountry);
        }
        study = nodeFactory.getOrCreateStudy(refTag, firstName + " " + lastName, institution.toString(), "", description
                , publicationYear
                , GOMEXSI_URL);
        Transaction transaction = nodeFactory.getGraphDb().beginTx();
        try {
            study.setPublicationYear(publicationYear);
            study.setExternalId(TaxonomyProvider.ID_PREFIX_GAME + externalId);
            transaction.success();
        } finally {
            transaction.finish();
        }
        referenceIdToStudy.put(refId, study);
    }

    private void addObservations(Map<String, Map<String, String>> predatorIdToPredatorSpecimen, Map<String, Study> refIdToStudyMap, Map<String, List<Map<String, String>>> predatorUIToPreyLists) throws StudyImporterException {
        String locationResource = getLocationsResourcePath();
        try {
            LabeledCSVParser parser = parserFactory.createParser(locationResource, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String refId = getMandatoryValue(locationResource, parser, "REF_ID");
                String specimenId = getMandatoryValue(locationResource, parser, "PRED_ID");
                Double latitude = getMandatoryDoubleValue(locationResource, parser, "LOC_CENTR_LAT");
                Double longitude = getMandatoryDoubleValue(locationResource, parser, "LOC_CENTR_LONG");
                Double depth = getMandatoryDoubleValue(locationResource, parser, "MN_DEP_SAMP");
                Location location = nodeFactory.getOrCreateLocation(latitude, longitude, depth == null ? null : -depth);
                String predatorId = refId + specimenId;
                Map<String, String> predatorProperties = predatorIdToPredatorSpecimen.get(predatorId);
                if (predatorProperties == null) {
                    LOG.warn("failed to lookup location for predator [" + refId + ":" + specimenId + "] from [" + locationResource + ":" + parser.getLastLineNumber() + "]");
                } else {
                    try {
                        Specimen predatorSpecimen = createSpecimen(predatorProperties);
                        predatorSpecimen.setExternalId(predatorId);
                        if (location == null) {
                            LOG.warn("no location for predator with id [" + predatorSpecimen.getExternalId() + "]");
                        } else {
                            predatorSpecimen.caughtIn(location);
                        }
                        Study study = refIdToStudyMap.get(refId);
                        if (study != null) {
                            study.collected(predatorSpecimen);
                        } else {
                            LOG.warn("failed to find study for ref id [" + refId + "] on related to observation location in [" + locationResource + ":" + parser.getLastLineNumber() + "]");
                        }

                        List<Map<String, String>> preyList = predatorUIToPreyLists.get(predatorId);
                        checkStomachDataConsistency(predatorId, predatorProperties, preyList);
                        if (preyList != null) {
                            for (Map<String, String> preyProperties : preyList) {
                                if (preyProperties != null) {
                                    try {
                                        predatorSpecimen.ate(createSpecimen(preyProperties));
                                    } catch (NodeFactoryException e) {
                                        throw new StudyImporterException("failed to create specimen for [" + predatorProperties + "]");
                                    }
                                }
                            }
                        }

                    } catch (NodeFactoryException e) {
                        throw new StudyImporterException("failed to create specimen for location on line [" + parser.getLastLineNumber() + "]");
                    }
                }
            }
        } catch (
                IOException e
                )

        {
            throw new StudyImporterException("failed to open resource [" + locationResource + "]", e);
        }

    }

    private void checkStomachDataConsistency(String predatorId, Map<String, String> predatorProperties, List<Map<String, String>> preyList) {
        Integer total = getValueOrNull(predatorProperties, STOMACH_COUNT_TOTAL);
        Integer withoutFood = getValueOrNull(predatorProperties, STOMACH_COUNT_WITHOUT_FOOD);
        Integer withFood = getValueOrNull(predatorProperties, STOMACH_COUNT_WITH_FOOD);
        if (total != null && withoutFood != null) {
            if (preyList == null || preyList.size() == 0) {
                if (!total.equals(withoutFood)) {
                    LOG.warn("no prey for predator with id [" + predatorId + "], but found [" + withFood + "] stomachs with food");
                }
            } else {
                if (total.equals(withoutFood)) {
                    LOG.warn("found prey for predator with id [" + predatorId + "], but found only stomachs without food");
                }
            }
        }
    }

    private Integer getValueOrNull(Map<String, String> predatorProperties, String label) {
        String stomachCountString = predatorProperties.get(label);
        return "NA".equals(stomachCountString) ? null : Integer.parseInt(stomachCountString);
    }

    private Specimen createSpecimen(Map<String, String> properties) throws NodeFactoryException, StudyImporterException {
        Specimen specimen = nodeFactory.createSpecimen(properties.get(Taxon.NAME));
        try {
            specimen.setLifeStage(new LifeStageLookupService().lookup(properties.get(Specimen.LIFE_STAGE)));
            specimen.setPhysiologicalState(new PhysiologicalStateLookupService().lookup(properties.get(Specimen.PHYSIOLOGICAL_STATE)));
            specimen.setBodyPart(new BodyPartLookupService().lookup(properties.get(Specimen.BODY_PART)));
        } catch (LookupServiceException ex) {
            throw new StudyImporterException(ex);
        }
        return specimen;
    }

    private Double getMandatoryDoubleValue(String locationResource, LabeledCSVParser parser, String label) throws StudyImporterException {
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
            while (parser.getLine() != null) {
                String refId = getMandatoryValue(datafile, parser, "REF_ID");
                String specimenId = getMandatoryValue(datafile, parser, "PRED_ID");
                String stomachCountWithoutFood = parser.getValueByLabel("TOT_WO_FD");
                String stomachCountWithFood = parser.getValueByLabel("TOT_W_FD");
                String stomachCountTotal = parser.getValueByLabel("TOT_PRED_STOM_EXAM");
                String scientificName = getMandatoryValue(datafile, parser, scientificNameLabel);
                String predatorUID = refId + specimenId;
                Map<String, String> properties = new HashMap<String, String>();
                properties.put(Taxon.NAME, scientificName);
                properties.put(Specimen.LIFE_STAGE, parser.getValueByLabel("LIFE_HIST_STAGE"));
                properties.put(Specimen.PHYSIOLOGICAL_STATE, parser.getValueByLabel("PHYSIOLOG_STATE"));
                properties.put(Specimen.BODY_PART, parser.getValueByLabel("PREY_PARTS"));
                properties.put(STOMACH_COUNT_TOTAL, stomachCountTotal);
                properties.put(STOMACH_COUNT_WITH_FOOD, stomachCountWithFood);
                properties.put(STOMACH_COUNT_WITHOUT_FOOD, stomachCountWithoutFood);
                specimenListener.onSpecimen(predatorUID, properties);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to open resource [" + datafile + "]", e);
        }
    }

    private String getMandatoryValue(String datafile, LabeledCSVParser parser, String label) throws StudyImporterException {
        String value = parser.getValueByLabel(label);
        if (value == null) {
            throw new StudyImporterException("missing mandatory column [" + label + "] in [" + datafile + "]:[" + parser.getLastLineNumber() + "]");
        }
        return "NA".equals(value) ? "" : value;
    }
}
