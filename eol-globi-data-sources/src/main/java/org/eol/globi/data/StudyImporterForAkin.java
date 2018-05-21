package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class StudyImporterForAkin extends BaseStudyImporter {

    public StudyImporterForAkin(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        importStudy("akin/Senol-akin-(mad-island)-data-edited-sheet3-winter2.csv");
        importStudy("akin/Senol-akin-(mad-island)-data-edited-sheet4-summer2.csv");
    }

    private Study importAkinStudyFile(String[][] siteInfos, String studyResource, Study study) throws IOException, StudyImporterException, NodeFactoryException {
        LabeledCSVParser parser = parserFactory.createParser(studyResource, CharsetConstant.UTF8);
        String[] header = parser.getLabels();
        String[] line;

        while ((line = parser.getLine()) != null) {
            if (isValid(line, header, parser, siteInfos)) {
                parseLine(siteInfos, studyResource, study, parser, header, line);
            } else {
                getLogger().warn(study, "[" + studyResource + "," + parser.lastLineNumber() + "]: skipping line, missing mandatory date or site field in line [" + StringUtils.join(line, ",") + "]");
            }
        }
        return study;
    }

    private boolean isValid(String[] line, String[] header, LabeledCSVParser parser, String[][] siteInfos) throws StudyImporterException {
        int dateIndex = findIndexForColumnWithNameThrowOnMissing("Date ", header);
        String dateString = line[dateIndex];
        String[] siteInfo = findSiteInfo(header, line, siteInfos, parser);
        return StringUtils.isNotBlank(dateString) && siteInfo != null;
    }


    private void parseLine(String[][] siteInfos, String studyResource, Study study, LabeledCSVParser parser, String[] header, String[] line) throws NodeFactoryException, IOException {
        try {
            Specimen specimen = addSpecimen(study, parser, header, line);
            if (specimen != null) {
                Location location = parseLocation(findSiteInfo(header, line, siteInfos, parser));
                specimen.caughtIn(location);
                addPrey(study, parser, header, line, specimen, location);
            }
        } catch (StudyImporterException ex) {
            getLogger().warn(study, "[" + studyResource + "]:" + ex.getMessage());
        }
    }

    private String[][] loadSampleSiteLocations() throws IOException {
        CSVParser csvParser = new CSVParser(getDataset().getResource("akin/Akin2002Locations.csv"));
        return csvParser.getAllValues();
    }

    private String[] findSiteInfo(String[] header, String[] line, String[][] siteInfos, LabeledCSVParser parser) throws StudyImporterException {
        int siteIndex = findIndexForColumnWithNameThrowOnMissing("Site ", header);
        String siteString = line[siteIndex];
        String[] siteInfo = null;
        for (String[] siteInfo1 : siteInfos) {
            if (siteString.equals(siteInfo1[0])) {
                siteInfo = siteInfo1;
            }
        }

        return siteInfo;
    }

    private Location parseLocation(String[] siteInfo) throws StudyImporterException, IOException, NodeFactoryException {
        Double longitude;
        Double latitude;
        // TODO note that this study was taken in shallow water ~ 0.7m, probably better to include a depth range?
        Double altitude = -0.7d;

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

        return nodeFactory.getOrCreateLocation(new LocationImpl(latitude, longitude, altitude, null));
    }

    private void addPrey(Study study, LabeledCSVParser parser, String[] header, String[] line, Specimen specimen, Location location) throws StudyImporterException {
        int firstPreyIndex = findIndexForColumnWithNameThrowOnMissing("Detritus", header);
        for (int i = firstPreyIndex; i < line.length; i++) {
            String preySpeciesName = header[i];
            if (StringUtils.isNotBlank(preySpeciesName)) {
                String preyVolumeString = line[i];
                try {
                    if (StringUtils.isNotBlank(preyVolumeString)) {
                        double volume = Double.parseDouble(preyVolumeString);
                        if (volume > 0) {
                            Specimen prey = nodeFactory.createSpecimen(study, new TaxonImpl(preySpeciesName, null));
                            prey.setLifeStage(parseLifeStage(nodeFactory.getTermLookupService(), preySpeciesName));
                            prey.setVolumeInMilliLiter(volume);
                            prey.caughtIn(location);
                            specimen.ate(prey);
                        }
                    }
                } catch (NumberFormatException ex) {
                    throw new StudyImporterException("failed to parse volume of prey [" + preySpeciesName + "] in stomach [" + preyVolumeString + "] on line [" + parser.getLastLineNumber() + "]");
                } catch (TermLookupServiceException e) {
                    throw new StudyImporterException("failed to parse life stage of prey [" + preySpeciesName + "] in stomach [" + preyVolumeString + "] on line [" + parser.getLastLineNumber() + "]");
                }
            }
        }
    }

    protected static List<Term> parseLifeStage(TermLookupService service, String preySpeciesName) throws TermLookupServiceException {
        List<Term> terms = Collections.emptyList();
        if (preySpeciesName.contains(" larvae")) {
            terms = service.lookupTermByName("larvae");
        } else if (preySpeciesName.contains(" egg")) {
            terms = service.lookupTermByName("egg");
        } else if (preySpeciesName.contains(" zoea")) {
            terms = service.lookupTermByName("zoea");
        }
        return terms;
    }

    private Specimen addSpecimen(Study study, LabeledCSVParser parser, String[] header, String[] line) throws StudyImporterException, NodeFactoryException {
        Specimen specimen = null;
        int speciesIndex = findIndexForColumnWithNameThrowOnMissing("Fish Species", header);
        String speciesName = line[speciesIndex];
        if (StringUtils.isNotBlank(speciesName)) {
            specimen = nodeFactory.createSpecimen(study, new TaxonImpl(speciesName, null));
            addSpecimenLength(parser, header, line, specimen, study);
            addStomachVolume(parser, header, line, specimen, study);
            addCollectionDate(study, parser, header, line, specimen);
        } else {
            getLogger().warn(study, "found blank species name on line [" + parser.lastLineNumber() + "]");
        }
        return specimen;
    }

    private void addCollectionDate(Study study, LabeledCSVParser parser, String[] header, String[] line, Specimen specimen) throws StudyImporterException {
        int dateIndex = findIndexForColumnWithNameThrowOnMissing("Date", header);
        String dateString = line[dateIndex];
        if (!StringUtils.isBlank(dateString)) {
            try {
                Date date = DateUtil.parsePatternUTC(dateString, "MM.dd.yy").toDate();
                nodeFactory.setUnixEpochProperty(specimen, date);
            } catch (IllegalArgumentException e) {
                getLogger().warn(study, "not setting collection date, because [" + dateString + "] on line [" + parser.getLastLineNumber() + "] could not be read as date.");
            } catch (NodeFactoryException e) {
                throw new StudyImporterException("failed to set collection time", e);
            }
        }
    }


    private void addSpecimenLength(LabeledCSVParser parser, String[] header, String[] line, Specimen specimen, Study study) throws StudyImporterException {
        int lengthIndex = findIndexForColumnWithNameThrowOnMissing("SL(mm)", header);
        String lengthInMm = line[lengthIndex];
        if (!StringUtils.isBlank(lengthInMm)) {
            try {
                specimen.setLengthInMm(Double.parseDouble(lengthInMm));
            } catch (NumberFormatException ex) {
                getLogger().warn(study, "not setting specimen length, because [" + lengthInMm + "] on line [" + parser.getLastLineNumber() + "] is not a number.");
            }
        }
    }

    private void addStomachVolume(LabeledCSVParser parser, String[] header, String[] line, Specimen specimen, Study study) throws StudyImporterException {
        int stomachVolumeMilliLiterIndex = findIndexForColumnWithName("Stomach volume", header);
        if (stomachVolumeMilliLiterIndex == -1) {
            stomachVolumeMilliLiterIndex = findIndexForColumnWithNameThrowOnMissing("volume of stomach", header);
        }
        String stomachVolumeString = line[stomachVolumeMilliLiterIndex];
        if (!StringUtils.isBlank(stomachVolumeString)) {
            try {
                specimen.setStomachVolumeInMilliLiter(Double.parseDouble(stomachVolumeString));
            } catch (NumberFormatException ex) {
                getLogger().warn(study, "not setting specimen stomach volume, because [" + stomachVolumeString + "] on line [" + parser.getLastLineNumber() + "] is not a number.");
            }
        }
    }

    private int findIndexForColumnWithNameThrowOnMissing(String name, String[] header) throws StudyImporterException {
        int index = findIndexForColumnWithName(name, header);
        if (index == -1) {
            throw new StudyImporterException("failed to find column with name [" + name + "]");
        }
        return index;
    }

    private int findIndexForColumnWithName(String name, String[] header) {
        int index = -1;
        for (int i = 0; i < header.length; i++) {
            if (header[i].trim().equals(name.trim())) {
                index = i;
                break;
            }
        }
        return index;
    }

    private Study importStudy(String studyResource) throws StudyImporterException {
        Study study;
        try {
            String doi = "https://doi.org/10.1007/bf02784282";
            StudyImpl study1 = new StudyImpl("Akin et al 2006", StudyImporterForGoMexSI2.GOMEXI_SOURCE_DESCRIPTION, doi, ExternalIdUtil.toCitation("Senol Akin", "S. Akin, K. O. Winemiller, Seasonal variation in food web composition and structure in a temperate tidal estuary, Estuaries and Coasts" +
                    "; August 2006, Volume 29, Issue 4, pp 552-567", "2006"));
            study = nodeFactory.getOrCreateStudy(
                    study1);
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
