package org.trophic.graph.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.helpers.collection.ClosableIterable;
import org.trophic.graph.domain.*;
import org.trophic.graph.repository.LocationRepository;
import org.trophic.graph.repository.SeasonRepository;
import org.trophic.graph.repository.StudyRepository;

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

    private LocationRepository locationRepository;

    private SeasonRepository seasonRepository;

    private StudyRepository studyRepository;

    private TaxonFactory taxonFactory;

    private ParserFactory parserFactory;

    public StudyImporterImpl() {

    }

    public StudyImporterImpl(ParserFactory parserFactory) {
        this.parserFactory = parserFactory;
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
            throw new StudyImporterException("failed to create study [" + studyResource + "]", e);
        }
        study.persist();
        return study;
    }

    private void addNextRecordToStudy(LabeledCSVParser csvParser, Study study, Map<String, String> columnToNormalizedTermMapper, LengthParser lengthParser) throws StudyImporterException {
        String seasonName = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(SEASON));
        Specimen prey = createAndClassifySpecimen(csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREY_SPECIES)), null);

        Location sampleLocation = getOrCreateSampleLocation(csvParser, columnToNormalizedTermMapper);
        prey.caughtIn(sampleLocation);
        prey.caughtDuring(getOrCreateSeason(seasonName));
        prey.persist();

        String speciesName = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREDATOR_SPECIES));
        String familyName = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREDATOR_FAMILY));
        Specimen predator = null;
        try {
            predator = createAndClassifySpecimen(speciesName, taxonFactory.createFamily(familyName));
        } catch (TaxonFactoryException e) {
            throw new StudyImporterException("failed to create taxon", e);
        }
        predator.setLengthInMm(lengthParser.parseLengthInMm(csvParser));

        predator.caughtIn(sampleLocation);
        predator.persist();
        predator.ate(prey);
        predator.caughtDuring(getOrCreateSeason(seasonName));
        predator.persist();
        study.getSpecimens().add(predator);

    }

    private Season getOrCreateSeason(String seasonName) {
        String seasonNameLower = seasonName.toLowerCase().trim();
        Season season = seasonRepository.findByPropertyValue("title", seasonNameLower);
        if (null == season) {
            season = new Season();
            season.setTitle(seasonNameLower);
            season.persist();
        }
        return season;
    }

    private Study getOrCreateStudy(String title) {
        Study study = studyRepository.findByPropertyValue("title", title);
        if (null == study) {
            study = new Study();
            study.setTitle(title);
            study.persist();
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
                location = new Location();
                location.setLatitude(latitude);
                location.setLongitude(longitude);
                location.setAltitude(altitude);
                location.persist();
            }
        }
        return location;
    }

    private Double parseAsDouble(LabeledCSVParser csvParser, String stringValue) {
        String valueByLabel = csvParser.getValueByLabel(stringValue);
        return valueByLabel == null ? null : Double.parseDouble(valueByLabel);
    }

    private Location findLocation(Double latitude, Double longitude, Double altitude) {
        Location foundLocation = null;
        ClosableIterable<Location> matchForLongitude = locationRepository.findAllByPropertyValue("longitude", longitude);
        for (Location location : matchForLongitude) {
            if (latitude.equals(location.getLatitude()) && altitude.equals(location.getAltitude())) {
                foundLocation = location;
                break;
            }
        }
        matchForLongitude.close();
        return foundLocation;
    }

    private Specimen createAndClassifySpecimen(final String speciesName, Family family) throws StudyImporterException {
        Specimen specimen = new Specimen().persist();
        String trimmedSpeciesName = StringUtils.trim(speciesName);
        try {
            specimen.classifyAs(taxonFactory.create(trimmedSpeciesName, family));
        } catch (TaxonFactoryException e) {
            throw new StudyImporterException("failed to classify specimen", e);
        }
        return specimen;
    }

    public void setLocationRepository(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public void setStudyRepository(StudyRepository studyRepository) {
        this.studyRepository = studyRepository;
    }

    public void setSeasonRepository(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    public TaxonFactory getTaxonFactory() {
        return taxonFactory;
    }

    public void setTaxonFactory(TaxonFactory taxonFactory) {
        this.taxonFactory = taxonFactory;
    }
}


