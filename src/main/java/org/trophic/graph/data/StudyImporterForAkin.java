package org.trophic.graph.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;

public class StudyImporterForAkin extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForAkin.class);
    public static final String AKIN_MAD_ISLAND = "akinMadIsland";

    public StudyImporterForAkin(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        importStudy("Senol akin (mad island) data edited-sheet1-WINTER.csv.gz");
        importStudy("Senol akin (mad island) data edited-sheet2-SUMMER.csv.gz");
        importStudy("Senol akin (mad island) data edited-sheet3-winter2.csv.gz");
        return importStudy("Senol akin (mad island) data edited-sheet4-summer2.csv.gz");
    }

    private Study importAkinStudyFile(String[][] siteInfos, String studyResource, Study study) throws IOException, StudyImporterException, NodeFactoryException {
        LabeledCSVParser parser = parserFactory.createParser(studyResource);
        String[] header = parser.getLabels();
        String[] line = null;

        while ((line = parser.getLine()) != null) {
            if (isValid(line, header, parser, siteInfos)) {
                parseLine(siteInfos, studyResource, study, parser, header, line);
            } else {
                LOG.warn("[" + studyResource + "," + parser.lastLineNumber() + "]: skipping line, missing mandatory date or site field in line [" + StringUtils.join(line, ",") + "]");
            }
        }
        return study;
    }

    private boolean isValid(String[] line, String[] header, LabeledCSVParser parser, String[][] siteInfos) throws StudyImporterException {
        int dateIndex = findIndexForColumnWithName("Date ", header);
        String dateString = line[dateIndex];
        String[] siteInfo = findSiteInfo(header, line, siteInfos, parser);
        return !StringUtils.isBlank(dateString) && siteInfo != null;
    }


    private void parseLine(String[][] siteInfos, String studyResource, Study study, LabeledCSVParser parser, String[] header, String[] line) throws NodeFactoryException, IOException {
        try {
            Specimen specimen = addSpecimen(study, parser, header, line);
            if (specimen != null) {
                addPrey(parser, header, line, specimen);
                Location location = parseLocation(findSiteInfo(header, line, siteInfos, parser));
                specimen.caughtIn(location);
            }
        } catch (StudyImporterException ex) {
            System.out.println("[" + studyResource + "]: " + ex.getMessage());
        }
    }

    private String[][] loadSampleSiteLocations() throws IOException {
        CSVParser csvParser = new CSVParser(getClass().getResourceAsStream("Akin2002Locations.csv"));
        return csvParser.getAllValues();
    }

    private String[] findSiteInfo(String[] header, String[] line, String[][] siteInfos, LabeledCSVParser parser) throws StudyImporterException {
        int siteIndex = findIndexForColumnWithName("Site ", header);
        String siteString = line[siteIndex];
        String[] siteInfo = null;
        for (int i = 0; i < siteInfos.length; i++) {
            if (siteString.equals(siteInfos[i][0])) {
                siteInfo = siteInfos[i];
            }
        }

        return siteInfo;
    }

    private Location parseLocation(String[] siteInfo) throws StudyImporterException, IOException {


        Double longitude = null;
        Double latitude = null;
        // TODO note that this study was taken in shallow water ~ 0.7m, probably better to include a depth range?
        Double altitude = 0d;

        String latitudeString = siteInfo[7];
        try {
            latitude = Double.parseDouble(latitudeString);
        } catch (NumberFormatException ex) {
            throw new StudyImporterException("failed to parse latitude [" + latitudeString + "]");
        }
        String longitudeString = siteInfo[8];
        try {
            longitude = Double.parseDouble(longitudeString);
        } catch (NumberFormatException ex) {
            throw new StudyImporterException("failed to parse longitude [" + longitudeString + "]");
        }

        return nodeFactory.createLocation(latitude, longitude, altitude);
    }

    private void addPrey(LabeledCSVParser parser, String[] header, String[] line, Specimen specimen) throws StudyImporterException, NodeFactoryException {
        int firstPreyIndex = findIndexForColumnWithName("Detritus", header);
        for (int i = firstPreyIndex; i < line.length; i++) {
            String preyVolumeString = line[i];
            String preySpeciesName = header[i];
            try {
                if (preyVolumeString.trim().length() > 0) {
                    double volume = Double.parseDouble(preyVolumeString);
                    if (volume > 0) {
                        Specimen prey = nodeFactory.createSpecimen();
                        prey.classifyAs(nodeFactory.getOrCreateSpecies(null, preySpeciesName));
                        specimen.ate(prey);
                    }
                }
            } catch (NumberFormatException ex) {
                throw new StudyImporterException("failed to parse volume of prey [" + preySpeciesName + "] in stomach [" + preyVolumeString + "] on line [" + parser.getLastLineNumber() + "]");
            }
        }
    }

    private Specimen addSpecimen(Study study, LabeledCSVParser parser, String[] header, String[] line) throws StudyImporterException, NodeFactoryException {
        Specimen specimen = null;
        int speciesIndex = findIndexForColumnWithName("Fish Species", header);
        String speciesName = line[speciesIndex];
        if (speciesName != null && speciesName.length() > 0) {
            Taxon species = nodeFactory.getOrCreateSpecies(null, speciesName);
            specimen = nodeFactory.createSpecimen();
            specimen.classifyAs(species);
            int lengthIndex = findIndexForColumnWithName("SL(mm)", header);
            String lengthInMm = line[lengthIndex];
            if (!StringUtils.isBlank(lengthInMm)) {
                try {
                    specimen.setLengthInMm(Double.parseDouble(lengthInMm));
                } catch (NumberFormatException ex) {
                    LOG.warn("not setting specimen length, because [" + lengthInMm + "] on line [" + parser.getLastLineNumber() + "] is not a number.");
                }
            }
            study.collected(specimen);
        }
        return specimen;
    }

    private int findIndexForColumnWithName(String name, String[] header) throws StudyImporterException {
        int index = -1;
        for (int i = 0; i < header.length; i++) {
            if (header[i].trim().equals(name.trim())) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            throw new StudyImporterException("failed to find column with name [" + name + "]");
        }
        return index;
    }

    private Study importStudy(String studyResource) throws StudyImporterException {
        Study study = null;
        try {
            study = nodeFactory.getOrCreateStudy(AKIN_MAD_ISLAND);
            String[][] siteInfo = loadSampleSiteLocations();
            importAkinStudyFile(siteInfo, studyResource, study);
        } catch (IOException e) {
            throw new StudyImporterException("failed to find resource [" + studyResource + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to parse resource [" + studyResource + "]", e);
        }

        return study;
    }
}
