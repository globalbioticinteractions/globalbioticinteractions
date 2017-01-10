package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.ExternalIdUtil;
import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForSimons extends BaseStudyImporter {

    public static final String NORTHING = "northing";
    public static final String EASTING = "easting";
    public static final String DEPTH = "depth";
    public static final String LENGTH_RANGE_IN_MM = "lengthRangeInMm";
    public static final String LENGTH_IN_MM = "lengthInMm";
    public static final String SEASON = "season";
    public static final String PREY_SPECIES = "prey species";
    public static final String PREDATOR_SPECIES = "predator species";

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

    protected static final String MISSISSIPPI_ALABAMA_DATA_SOURCE = "simons/mississippiAlabamaFishDiet.csv";

    private final HashMap<String, Specimen> predatorSpecimenMap = new HashMap<String, Specimen>();
    ;

    public StudyImporterForSimons(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        importStudy(MISSISSIPPI_ALABAMA_DATA_SOURCE);
    }

    private Study importStudy(String studyResource) throws StudyImporterException {
        return createAndPopulateStudy(parserFactory, studyResource);
    }


    private Study createAndPopulateStudy(ParserFactory parserFactory, String studyResource) throws StudyImporterException {
        getPredatorSpecimenMap().clear();
        return importStudy(parserFactory, studyResource);
    }

    private Study importStudy(ParserFactory parserFactory, String studyResource) throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy(
                new StudyImpl("Simons 1997", StudyImporterForGoMexSI2.GOMEXI_SOURCE_DESCRIPTION, null, ExternalIdUtil.toCitation("James D. Simons", "Food habits and trophic structure of the demersal fish assemblages on the Mississippi-Alabama continental shelf.", "1997")));
        try {
            LabeledCSVParser csvParser = parserFactory.createParser(studyResource, CharsetConstant.UTF8);
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
        Specimen prey = createAndClassifySpecimen(csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREY_SPECIES)), study);

        Location sampleLocation = getOrCreateSampleLocation(csvParser, columnToNormalizedTermMapper);
        prey.caughtIn(sampleLocation);
        prey.caughtDuring(getOrCreateSeason(seasonName));

        String speciesName = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREDATOR_SPECIES));

        // see https://github.com/jhpoelen/gomexsi/issues/41
        String occurrenceId = csvParser.getValueByLabel("spcode")
                + csvParser.getValueByLabel("sizecl")
                + csvParser.getValueByLabel("cruise")
                + csvParser.getValueByLabel("stcode");
        Map<String, Specimen> predatorMap = getPredatorSpecimenMap();
        Specimen predator = predatorMap.get(occurrenceId);
        if (predator == null) {
            predator = createAndClassifySpecimen(speciesName, study);
            predator.setLengthInMm(lengthParser.parseLengthInMm(csvParser));
            predator.caughtDuring(getOrCreateSeason(seasonName));
            predator.caughtIn(sampleLocation);
            predatorMap.put(occurrenceId, predator);
        }

        predator.ate(prey);

    }

    private HashMap<String, Specimen> getPredatorSpecimenMap() {
        return predatorSpecimenMap;
    }

    private Season getOrCreateSeason(String seasonName) {
        String seasonNameLower = seasonName.toLowerCase().trim();
        Season season = nodeFactory.findSeason(seasonNameLower);
        if (null == season) {
            season = nodeFactory.createSeason(seasonNameLower);
        }
        return season;
    }

    private Location getOrCreateSampleLocation(LabeledCSVParser csvParser, Map<String, String> columnToNormalizedTermMapper) throws StudyImporterException {
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
        try {
            return nodeFactory.getOrCreateLocation(new LocationImpl(latitude, longitude, altitude, null));
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create location", e);
        }
    }


    private Double parseAsDouble(LabeledCSVParser csvParser, String stringValue) {
        String valueByLabel = csvParser.getValueByLabel(stringValue);
        return valueByLabel == null ? null : Double.parseDouble(valueByLabel);
    }

    private Specimen createAndClassifySpecimen(final String speciesName, Study study) throws StudyImporterException {
        try {
            return nodeFactory.createSpecimen(study, new TaxonImpl(speciesName, null));
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to classify specimen", e);
        }
    }

}


