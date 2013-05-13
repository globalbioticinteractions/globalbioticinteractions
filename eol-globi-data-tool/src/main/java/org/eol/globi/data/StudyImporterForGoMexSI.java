package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForGoMexSI extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForGoMexSI.class);

    public StudyImporterForGoMexSI(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Map<String, List<String>> predatorIdToPredatorNames = new HashMap<String, List<String>>();
        Map<String, List<String>> predatorIdToPreyNames = new HashMap<String, List<String>>();
        Map<String, Study> referenceIdToStudy = new HashMap<String, Study>();
        addSpecimen(predatorIdToPredatorNames, "gomexsi/Predators.csv", "PRED_SCI_NAME");
        addSpecimen(predatorIdToPreyNames, "gomexsi/Prey.csv", "DATABASE_PREY_NAME");
        addReferences(referenceIdToStudy);
        addObservations(predatorIdToPredatorNames, referenceIdToStudy, predatorIdToPreyNames);

        // TODO figure out a way to introduce the separation of study and reference.
        return nodeFactory.createStudy("GoMexSI",
                "James D. Simons",
                "Center for Coastal Studies, Texas A&M University - Corpus Christi, United States",
                "",
                "<a href=\"http://www.ingentaconnect.com/content/umrsmas/bullmar/2013/00000089/00000001/art00009\">Building a Fisheries Trophic Interaction Database for Management and Modeling Research in the Gulf of Mexico Large Marine Ecosystem.</a>", null);
    }

    private void addReferences(Map<String, Study> referenceIdToStudy) throws StudyImporterException {
        String referenceResource = "gomexsi/References.csv";
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
        String description = getMandatoryValue(referenceResource, parser, "TITLE_REF");
        String publicationYear = getMandatoryValue(referenceResource, parser, "YEAR_PUB");
        String institution = getMandatoryValue(referenceResource, parser, "UNIV_NAME");
        institution += getMandatoryValue(referenceResource, parser, "UNIV_CITY");
        institution += getMandatoryValue(referenceResource, parser, "UNIV_STATE");
        institution = getMandatoryValue(referenceResource, parser, "UNIV_COUNTRY");
        study = nodeFactory.getOrCreateStudy(refTag, firstName + " " + lastName, institution, "", description, null);
        Transaction transaction = nodeFactory.getGraphDb().beginTx();
        try {
            study.setPublicationYear(publicationYear);
            transaction.success();
        } finally {
            transaction.finish();
        }
        referenceIdToStudy.put(refId, study);
    }

    private void addObservations(Map<String, List<String>> predatorIdToPredatorSpecimen, Map<String, Study> refIdToStudyMap, Map<String, List<String>> predatorIdToPreySpecimen) throws StudyImporterException {
        String locationResource = "gomexsi/Locations.csv";
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
                List<String> predatorNames = predatorIdToPredatorSpecimen.get(predatorId);
                if (predatorNames == null) {
                    LOG.warn("failed to lookup location for predator [" + refId + ":" + specimenId + "] from [" + locationResource + ":" + parser.getLastLineNumber() + "]");
                } else {
                    for (String predatorName : predatorNames) {
                        Specimen predatorSpecimen = null;
                        try {
                            predatorSpecimen = nodeFactory.createSpecimen(predatorName);
                            predatorSpecimen.setExternalId(predatorId);
                            predatorSpecimen.caughtIn(location);
                            Study study = refIdToStudyMap.get(refId);
                            if (study != null) {
                                study.collected(predatorSpecimen);
                            } else {
                                LOG.warn("failed to find study for ref id [" + refId + "] on related to observation location in [" + locationResource + ":" + parser.getLastLineNumber() + "]");
                            }
                        } catch (NodeFactoryException e) {
                            throw new StudyImporterException("failed to create specimen for [" + predatorName + "]");
                        }
                        List<String> preyNames = predatorIdToPreySpecimen.get(predatorId);
                        if (preyNames != null) {
                            for (String preyName : preyNames) {
                                try {
                                    predatorSpecimen.ate(nodeFactory.createSpecimen(preyName));
                                } catch (NodeFactoryException e) {
                                    throw new StudyImporterException("failed to create specimen for [" + preyName + "]");
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to open resource [" + locationResource + "]", e);
        }
    }

    private Double getMandatoryDoubleValue(String locationResource, LabeledCSVParser parser, String label) throws StudyImporterException {
        String lat = getMandatoryValue(locationResource, parser, label);
        try {
            return "NA".equals(lat) ? null : Double.parseDouble(lat);
        } catch (NumberFormatException ex) {
            throw new StudyImporterException("failed to parse [" + label + "]", ex);
        }
    }

    private void addSpecimen(Map<String, List<String>> specimenMap, String datafile, String scientificNameLabel) throws StudyImporterException {
        try {
            LabeledCSVParser parser = parserFactory.createParser(datafile, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String refId = getMandatoryValue(datafile, parser, "REF_ID");
                String specimenId = getMandatoryValue(datafile, parser, "PRED_ID");
                String scientificName = getMandatoryValue(datafile, parser, scientificNameLabel);
                LifeStage stage = getLifeStage(parser);
                String predatorUID = refId + specimenId;
                List<String> nameLists = specimenMap.get(predatorUID);
                if (nameLists == null) {
                    nameLists = new ArrayList<String>();
                }
                nameLists.add(scientificName);
                specimenMap.put(predatorUID, nameLists);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to open resource [" + datafile + "]", e);
        }
    }

    private LifeStage getLifeStage(LabeledCSVParser parser) throws StudyImporterException {
        LifeStage lifeStage = null;
        String lifeStageString = parser.getValueByLabel("LIFE_HIST_STAGE");
        Map<String, LifeStage> lifeStageMap = new HashMap<String, LifeStage>() {
            {
                put("adult", LifeStage.ADULT);
                put("nd", LifeStage.UNKNOWN);
                put("na", LifeStage.UNKNOWN);
                put("NA", LifeStage.UNKNOWN);
                put("zoea", LifeStage.ZOEA);
                put("larvae", LifeStage.LARVA);
                put("eggs", LifeStage.EGG);
            }
        };
        if (lifeStageString != null && lifeStageString.trim().length() > 0) {
            lifeStage = lifeStageMap.get(lifeStageString);
            if (lifeStage == null) {
                throw new StudyImporterException("failed to parse life history stage, found unsupported stage [" + lifeStageString + "]");
            }
        }
        return lifeStage;
    }

    private String getMandatoryValue(String datafile, LabeledCSVParser parser, String label) throws StudyImporterException {
        String value = parser.getValueByLabel(label);
        if (value == null) {
            throw new StudyImporterException("missing mandatory column [" + label + "] in [" + datafile + "]:[" + parser.getLastLineNumber() + "]");
        }
        return value;
    }
}
