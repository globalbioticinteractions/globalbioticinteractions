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

@Component
public class StudyImporterImpl implements StudyImporter {

    public static final String PREDATOR = "predator";
    public static final String PREY = "prey";
    public static final String DEFAULT_STUDY_TITLE = "mississippi alabama fish study";

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
    public Study importStudy() throws IOException {
        return createAndPopulateStudy(parserFactory, DEFAULT_STUDY_TITLE);
    }

    private Study createAndPopulateStudy(ParserFactory parserFactory, String title) throws IOException {
        Study study = getOrCreateStudy(title);
        LabeledCSVParser csvParser = parserFactory.createParser();
        while (csvParser.getLine() != null) {
            addNextRecordToStudy(csvParser, study);
        }
        study.persist();
        return study;
    }

    private void addNextRecordToStudy(LabeledCSVParser csvParser, Study study) throws IOException {
        Double latitude = Double.parseDouble(csvParser.getValueByLabel("lat"));
        Double longitude = Double.parseDouble(csvParser.getValueByLabel("long"));
        Double altitude = -Double.parseDouble(csvParser.getValueByLabel("depth"));
        String seasonName = csvParser.getValueByLabel("season");
        Specimen prey = createSpecimenForSpecies(csvParser.getValueByLabel(PREY));
        setWithExistingOrNewLocation(prey, latitude, longitude, altitude);
        prey.caughtDuring(getOrCreateSeason(seasonName));
        prey.persist();

        Specimen predator = createSpecimenForSpecies(csvParser.getValueByLabel(PREDATOR));
        setWithExistingOrNewLocation(predator, latitude, longitude, altitude);
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

    private void setWithExistingOrNewLocation(Specimen specimen, Double lat, Double longitude, Double altitude) {
        Location location = findLocation(lat, longitude, altitude);
        if (null == location) {
            location = new Location();
            location.setLatitude(lat);
            location.setLongitude(longitude);
            location.setAltitude(altitude);
            location.persist();
        }

        specimen.caughtIn(location);
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
