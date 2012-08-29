package org.trophic.graph.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.Season;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForLavacaBay extends BaseStudyImporter {

    public static final String LENGTH_IN_MM = "lengthInMm";
    public static final String SEASON = "season";
    public static final String PREY_SPECIES = "prey species";
    public static final String PREDATOR_SPECIES = "predator species";
    public static final String PREDATOR_FAMILY = "predatorFamily";

    static final HashMap<String, String> COLUMN_MAPPER = new HashMap<String, String>() {{
        put(SEASON, "Season");
        put(PREY_SPECIES, "Prey Item Species");
        put(PREDATOR_SPECIES, "Predator Species");
        put(LENGTH_IN_MM, "TL");
        put(PREDATOR_FAMILY, "Family");
    }};

    public static final String LAVACA_BAY_DATA_SOURCE = "lavacaBayTrophicData.csv.gz";

    public StudyImporterForLavacaBay(ParserFactory parserFactory, NodeFactory nodeFactory, StudyLibrary.Study study) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        return importStudy(LAVACA_BAY_DATA_SOURCE);
    }

    private Study importStudy(String studyResource) throws StudyImporterException {
        return createAndPopulateStudy(parserFactory, studyResource);
    }


    private Study createAndPopulateStudy(ParserFactory parserFactory, String studyResource) throws StudyImporterException {
        Map<String, String> columnMapper = COLUMN_MAPPER;
        if (null == columnMapper) {
            throw new StudyImporterException("no suitable column mapper found for [" + studyResource + "]");
        }
        return importStudy(parserFactory, studyResource);
    }

    private Study importStudy(ParserFactory parserFactory, String studyResource) throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy(studyResource);
        try {
            LabeledCSVParser csvParser = parserFactory.createParser(studyResource);
            LengthParser parser = new LengthParserImpl(COLUMN_MAPPER.get(LENGTH_IN_MM));
            while (csvParser.getLine() != null) {
                addNextRecordToStudy(csvParser, study, COLUMN_MAPPER, parser);
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

    private Location getOrCreateSampleLocation(LabeledCSVParser csvParser, Map<String, String> columnToNormalizedTermMapper) {
        return null;
    }


    private Double parseAsDouble(LabeledCSVParser csvParser, String stringValue) {
        String valueByLabel = csvParser.getValueByLabel(stringValue);
        return valueByLabel == null ? null : Double.parseDouble(valueByLabel);
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


