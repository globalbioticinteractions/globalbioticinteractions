package org.trophic.graph.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.trophic.graph.domain.*;

import java.io.IOException;
import java.util.Map;

public class StudyImporterImpl implements StudyImporter {

    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String DEPTH = "depth";
    public static final String LENGTH_RANGE_IN_MM = "lengthRangeInMm";
    public static final String LENGTH_IN_MM = "lengthInMm";
    public static final String SEASON = "season";
    public static final String PREY_SPECIES = "prey species";
    public static final String PREDATOR_SPECIES = "predator species";
    public static final String PREDATOR_FAMILY = "predatorFamily";

    private NodeFactory nodeFactory;

    private ParserFactory parserFactory;

    public StudyImporterImpl() {

    }

    public StudyImporterImpl(ParserFactory parserFactory, NodeFactory nodeFactory) {
        this.parserFactory = parserFactory;
        this.nodeFactory = nodeFactory;
    }


    @Override
    public Study importStudy() throws StudyImporterException {
        return importStudy(StudyLibrary.MISSISSIPPI_ALABAMA);
    }

    @Override
    public Study importStudy(String studyResource) throws StudyImporterException {
        return createAndPopulateStudy(parserFactory, studyResource);
    }


    private Study createAndPopulateStudy(ParserFactory parserFactory, String studyResource) throws StudyImporterException {
        Map<String, String> columnMapper = StudyLibrary.COLUMN_MAPPERS.get(studyResource);
        if (null == columnMapper) {
            throw new StudyImporterException("no suitable column mapper found for [" + studyResource + "]");
        }
        return importStudy(parserFactory, studyResource);
    }

    private Study importStudy(ParserFactory parserFactory, String studyResource) throws StudyImporterException {
        Study study = getOrCreateStudy(studyResource);
        try {
            LabeledCSVParser csvParser = parserFactory.createParser(studyResource);
            LengthParser parser = new LengthParserFactory().createParser(study.getTitle());
            while (csvParser.getLine() != null) {
                addNextRecordToStudy(csvParser, study, StudyLibrary.COLUMN_MAPPERS.get(studyResource), parser);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to createTaxon study [" + studyResource + "]", e);
        }
        return study;
    }

    private void addNextRecordToStudy(LabeledCSVParser csvParser, Study study, Map<String, String> columnToNormalizedTermMapper, LengthParser lengthParser) throws StudyImporterException {
        String seasonName = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(SEASON));
        Specimen prey = createAndClassifySpecimen(csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREY_SPECIES)), null);

        Location sampleLocation = getOrCreateSampleLocation(csvParser, columnToNormalizedTermMapper);
        prey.caughtIn(sampleLocation);
        prey.caughtDuring(getOrCreateSeason(seasonName));

        String speciesName = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREDATOR_SPECIES));
        String familyName = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREDATOR_FAMILY));
        Specimen predator = null;
        try {
            predator = createAndClassifySpecimen(speciesName, nodeFactory.getOrCreateFamily(familyName));
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to createTaxon taxon", e);
        }
        predator.setLengthInMm(lengthParser.parseLengthInMm(csvParser));

        predator.caughtIn(sampleLocation);
        predator.ate(prey);
        predator.caughtDuring(getOrCreateSeason(seasonName));
        study.collected(predator);

    }

    private Season getOrCreateSeason(String seasonName) {
        String seasonNameLower = seasonName.toLowerCase().trim();
        Season season = nodeFactory.findSeason(seasonNameLower);
        if (null == season) {
            season = nodeFactory.createSeason(seasonNameLower);
        }
        return season;
    }

    private Study getOrCreateStudy(String title) {
        Study study = nodeFactory.findStudy(title);
        if (null == study) {
            study = nodeFactory.createStudy(title);
        }
        return study;
    }

    private Location getOrCreateSampleLocation(LabeledCSVParser csvParser, Map<String, String> columnToNormalizedTermMapper) {
        Double latitude = parseAsDouble(csvParser, columnToNormalizedTermMapper.get(LATITUDE));
        Double longitude = parseAsDouble(csvParser, columnToNormalizedTermMapper.get(LONGITUDE));
        Double depth = parseAsDouble(csvParser, columnToNormalizedTermMapper.get(DEPTH));
        Double altitude = depth == null ? null : -depth;

        Location location = null;
        if (latitude != null && longitude != null && altitude != null) {
            location = findLocation(latitude, longitude, altitude);
            if (null == location) {
                location = nodeFactory.createLocation(latitude, longitude, altitude);
            }
        }
        return location;
    }

    private Double parseAsDouble(LabeledCSVParser csvParser, String stringValue) {
        String valueByLabel = csvParser.getValueByLabel(stringValue);
        return valueByLabel == null ? null : Double.parseDouble(valueByLabel);
    }

    private Location findLocation(Double latitude, Double longitude, Double altitude) {
        return nodeFactory.findLocation(latitude, longitude, altitude);
    }

    private Specimen createAndClassifySpecimen(final String speciesName, Taxon family) throws StudyImporterException {
        Specimen specimen = nodeFactory.createSpecimen();
        String trimmedSpeciesName = StringUtils.trim(speciesName);
        try {
            specimen.classifyAs(nodeFactory.createTaxon(trimmedSpeciesName, family));
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to classify specimen", e);
        }
        return specimen;
    }

}


