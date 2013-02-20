package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.*;
import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForMississippiAlabama extends BaseStudyImporter {

    public static final String NORTHING = "northing";
    public static final String EASTING = "easting";
    public static final String DEPTH = "depth";
    public static final String LENGTH_RANGE_IN_MM = "lengthRangeInMm";
    public static final String LENGTH_IN_MM = "lengthInMm";
    public static final String SEASON = "season";
    public static final String PREY_SPECIES = "prey species";
    public static final String PREDATOR_SPECIES = "predator species";
    public static final String PREDATOR_FAMILY = "predatorFamily";

    static final HashMap<String, String> COLUMN_MAPPER = new HashMap<String, String>() {{
        // note that lat / long combination is source data are northing/ easting UTM coordinates in
        // latZone 'R' and longZone 16
        put(NORTHING, "lat");
        put(EASTING, "long");
        put(DEPTH, "depth");
        put(SEASON, "season");
        put(PREY_SPECIES, "prey");
        put(PREDATOR_SPECIES, "predator");
        put(LENGTH_RANGE_IN_MM, "sizeclass");
    }};

    public static final String MISSISSIPPI_ALABAMA_DATA_SOURCE = "simons/mississippiAlabamaFishDiet.csv.gz";

    public StudyImporterForMississippiAlabama(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        return importStudy(MISSISSIPPI_ALABAMA_DATA_SOURCE);
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
        Study study = nodeFactory.getOrCreateStudy(studyResource,
                "James D. Simons",
                "Center for Coastal Studies, Texas A&M University - Corpus Christi",
                "1987- 1990",
                "Food habits and trophic structure of the demersal fish assemblages on the Mississippi-Alabama continental shelf.");
        try {
            LabeledCSVParser csvParser = parserFactory.createParser(studyResource);
            Map<String, String> columnMapper = COLUMN_MAPPER;
            LengthParser parser = new LengthRangeParserImpl(columnMapper.get(LENGTH_RANGE_IN_MM));
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
        Double northing = parseAsDouble(csvParser, columnToNormalizedTermMapper.get(NORTHING));
        Double easting = parseAsDouble(csvParser, columnToNormalizedTermMapper.get(EASTING));

        Double latitude = null;
        Double longitude = null;
        if (easting != null && northing != null) {
            UTMRef utmRef = new UTMRef(easting, northing, 'R', 16);
            LatLng latLng = utmRef.toLatLng();
            latitude = latLng.getLat();
            longitude = latLng.getLng();
        }

        Double depth = parseAsDouble(csvParser, columnToNormalizedTermMapper.get(DEPTH));
        Double altitude = depth == null ? null : -depth;
        return nodeFactory.getOrCreateLocation(latitude, longitude, altitude);
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


