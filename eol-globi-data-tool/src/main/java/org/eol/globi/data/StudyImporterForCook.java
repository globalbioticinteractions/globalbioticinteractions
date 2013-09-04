package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.Relationship;

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
    public Study importStudy() throws StudyImporterException {
        LabeledCSVParser parser;
        try {
            parser = parserFactory.createParser(DATASET_RESOURCE_NAME, CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource", e);
        }

        Study study = nodeFactory.getOrCreateStudy("Cook 2012", "Colt W. Cook", "Department of Marine Science, The University of Texas at Austin, USA", "", "<a href=\"http://repositories.lib.utexas.edu/handle/2152/ETD-UT-2012-08-6285\">The early life history and reproductive biology of Cymothoa excisa, a marine isopod parasitizing Atlantic croaker, (Micropogonias undulatus), along the Texas coast.</a>", "2012", "Colt W. Cook");

        try {

            Double latitude = LocationUtil.parseDegrees("27º51'N");
            Double longitude = LocationUtil.parseDegrees("97º8'W");

            Location sampleLocation = nodeFactory.getOrCreateLocation(latitude, longitude, -3.0);

            try {
                while (parser.getLine() != null) {
                    Specimen host = nodeFactory.createSpecimen("Micropogonias undulatus");
                    host.setLengthInMm(Double.parseDouble(parser.getValueByLabel("Fish Length")) * 10.0);
                    Relationship collected = study.collected(host);

                    String dateString = parser.getValueByLabel("Date");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                    Date collectionDate = dateFormat.parse(dateString);
                    nodeFactory.setUnixEpochProperty(collected, collectionDate);
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
        return study;
    }

    private void addParasites(LabeledCSVParser parser, Study study, Location sampleLocation, Specimen host, Date collectionDate, String isoCol) throws NodeFactoryException {
        try {
            String valueByLabel = parser.getValueByLabel(isoCol);
            boolean parasiteDetected = !"0".equals(valueByLabel);
            boolean lengthAvailable = parasiteDetected && !"NA".equals(valueByLabel);

            if (parasiteDetected) {
                Specimen parasite = nodeFactory.createSpecimen("Cymothoa excisa");
                parasite.caughtIn(sampleLocation);
                if (lengthAvailable) {
                    double parasiteLengthCm = Double.parseDouble(valueByLabel);
                    parasite.setLengthInMm(parasiteLengthCm * 10.0);
                }
                parasite.createRelationshipTo(host, InteractType.PARASITE_OF);
                Relationship collected1 = study.collected(parasite);
                nodeFactory.setUnixEpochProperty(collected1, collectionDate);
            }
        } catch (NumberFormatException ex) {
            // ignore
        }
    }
}
