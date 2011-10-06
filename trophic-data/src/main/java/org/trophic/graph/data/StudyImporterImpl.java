package org.trophic.graph.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.trophic.graph.domain.*;
import org.trophic.graph.repository.LocationRepository;
import org.trophic.graph.repository.SeasonRepository;
import org.trophic.graph.repository.SpeciesRepository;
import org.trophic.graph.repository.StudyRepository;

import java.io.IOException;
import java.util.Map;

@Component
public class StudyImporterImpl implements StudyImporter {

    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String DEPTH = "depth";
    public static final String SEASON = "season";
    public static final String PREY_SPECIES = "prey species";
    public static final String PREDATOR_SPECIES = "predator species";

    @Autowired
    private SpeciesRepository speciesRepository;

    @Autowired
    private LocationRepository locationRepository;


    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
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
            while (csvParser.getLine() != null) {
                addNextRecordToStudy(csvParser, study, StudyLibrary.COLUMN_MAPPERS.get(studyResource));
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to parse study [" + studyResource + "]", e);
        }
        study.persist();
        return study;
    }

    private void addNextRecordToStudy(LabeledCSVParser csvParser, Study study, Map<String, String> columnToNormalizedTermMapper) throws IOException {
        String seasonName = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(SEASON));
        Specimen prey = createSpecimenForSpecies(csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREY_SPECIES)));

        Location sampleLocation = getOrCreateSampleLocation(csvParser, columnToNormalizedTermMapper);
        prey.caughtIn(sampleLocation);
        prey.caughtDuring(getOrCreateSeason(seasonName));
        prey.persist();

        Specimen predator = createSpecimenForSpecies(csvParser.getValueByLabel(columnToNormalizedTermMapper.get(PREDATOR_SPECIES)));
        predator.caughtIn(sampleLocation);
        predator.persist();
        predator.ate(prey);
        predator.caughtDuring(getOrCreateSeason(seasonName));
        predator.persist();
        study.getSpecimens().add(predator);

    }

    private Season getOrCreateSeason(String seasonName) {
        String seasonNameLower = seasonName.toLowerCase();
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

    private Specimen createSpecimenForSpecies(String scientificName) {
        Specimen specimen = new Specimen();

        Species species = speciesRepository.findByPropertyValue("scientificName", scientificName);
        if (null == species) {
            species = new Species();
            species.setScientificName(scientificName);
            species.persist();
        }
        specimen.setSpecies(species);
        return specimen;
    }

    public void setSpeciesRepository(SpeciesRepository speciesRepository) {
        this.speciesRepository = speciesRepository;
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
}
