package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StudyImporterForCook extends BaseStudyImporter {
    private static final String DATASET_RESOURCE_NAME = "cook/cook_atlantic_croaker_data.csv";

    public StudyImporterForCook(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        LabeledCSVParser parser;
        try {
            parser = parserFactory.createParser(DATASET_RESOURCE_NAME, CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource", e);
        }

        String citation = "Cook CW. The Early Life History and Reproductive Biology of Cymothoa excisa, a Marine Isopod Parasitizing Atlantic Croaker, (Micropogonias undulatus), along the Texas Coast. 2012. Master Thesis. Available from http://repositories.lib.utexas.edu/handle/2152/ETD-UT-2012-08-6285.";
        StudyImpl study1 = new StudyImpl("Cook 2012",
                "Data provided by Colt W. Cook. Also available from  http://repositories.lib.utexas.edu/handle/2152/ETD-UT-2012-08-6285.",
                null,
                citation);
        study1.setExternalId("http://repositories.lib.utexas.edu/handle/2152/ETD-UT-2012-08-6285");
        Study study = nodeFactory.getOrCreateStudy(study1);

        try {

            Double latitude = LocationUtil.parseDegrees("27º51'N");
            Double longitude = LocationUtil.parseDegrees("97º8'W");

            Location sampleLocation = nodeFactory.getOrCreateLocation(new LocationImpl(latitude, longitude, -3.0, null));

            try {
                while (parser.getLine() != null) {
                    Specimen host = nodeFactory.createSpecimen(study, new TaxonImpl("Micropogonias undulatus", null));
                    host.setLengthInMm(Double.parseDouble(parser.getValueByLabel("Fish Length")) * 10.0);

                    String dateString = parser.getValueByLabel("Date");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                    Date collectionDate = dateFormat.parse(dateString);
                    nodeFactory.setUnixEpochProperty(host, collectionDate);
                    host.caughtIn(sampleLocation);

                    String[] isoCols = {"Iso 1", "Iso 2", "Iso 3", "Iso 4 ", "Iso 5"};
                    for (String isoCol : isoCols) {
                        addParasites(parser, study, sampleLocation, host, collectionDate, isoCol);
                    }
                }
            } catch (IOException e) {
                throw new StudyImporterException("failed to parse [" + DATASET_RESOURCE_NAME + "]", e);
            } catch (ParseException e) {
                throw new StudyImporterException("failed to parse date", e);
            }

        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create host and parasite taxons", e);
        }
    }

    private void addParasites(LabeledCSVParser parser, Study study, Location sampleLocation, Specimen host, Date collectionDate, String isoCol) throws NodeFactoryException {
        try {
            String valueByLabel = parser.getValueByLabel(isoCol);
            boolean parasiteDetected = !"0".equals(valueByLabel);
            boolean lengthAvailable = parasiteDetected && !"NA".equals(valueByLabel);

            if (parasiteDetected) {
                Specimen parasite = nodeFactory.createSpecimen(study, new TaxonImpl("Cymothoa excisa", null));
                parasite.caughtIn(sampleLocation);
                if (lengthAvailable) {
                    double parasiteLengthCm = Double.parseDouble(valueByLabel);
                    parasite.setLengthInMm(parasiteLengthCm * 10.0);
                }
                parasite.interactsWith(host, InteractType.PARASITE_OF);
                nodeFactory.setUnixEpochProperty(parasite, collectionDate);
            }
        } catch (NumberFormatException ex) {
            // ignore
        }
    }
}
