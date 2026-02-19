package org.eol.globi.process;

import org.apache.commons.lang.math.NumberUtils;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.StudyImporterException;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.Proj4jException;
import org.locationtech.proj4j.ProjCoordinate;
import org.locationtech.proj4j.UnknownAuthorityCodeException;

import java.util.Map;
import java.util.TreeMap;

public class VerbatimCoordinatesEnricher extends InteractionProcessorAbstract {

    public static final CRSFactory CRS_FACTORY = new CRSFactory();
    public static final CoordinateReferenceSystem WGS_84 = CRS_FACTORY.createFromName("epsg:4326");

    public VerbatimCoordinatesEnricher(InteractionListener listener, ImportLogger logger) {
        super(listener, logger);
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        Map<String, String> enriched = interaction;
        if (!interaction.containsKey("decimalLatitude")
                && !interaction.containsKey("decimalLongitude")
                && interaction.containsKey("verbatimLongitude")
                && interaction.containsKey("verbatimLatitude")
                && interaction.containsKey("verbatimSRS")
        ) {
            try {
                CoordinateReferenceSystem srs = getCRSOrThrow(interaction.get("verbatimSRS"));
                CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();

                double verbatimLongitude = parseDoubleOrThrow(interaction, "verbatimLongitude");
                double verbatimLatitude = parseDoubleOrThrow(interaction, "verbatimLatitude");

                ProjCoordinate sourceCoordinates = new ProjCoordinate(
                        verbatimLongitude,
                        verbatimLatitude
                );

                CoordinateTransform transform = ctFactory.createTransform(srs, WGS_84);
                ProjCoordinate targetCoordinates = new ProjCoordinate();
                transform.transform(sourceCoordinates, targetCoordinates);
                enriched = new TreeMap<>(interaction);
                enriched.put("decimalLatitude", Double.toString(targetCoordinates.y));
                enriched.put("decimalLongitude", Double.toString(targetCoordinates.x));
                enriched.put("geodeticDatum", "epsg:4326");
            } catch (Proj4jException ex) {
                throw new StudyImporterException("failed to enrich coordinates", ex);
            }
        }
        emit(enriched);
    }

    private static CoordinateReferenceSystem getCRSOrThrow(String spatialReferenceSystem) throws StudyImporterException {
        try {
            return CRS_FACTORY.createFromName(spatialReferenceSystem);
        } catch (UnknownAuthorityCodeException ex) {
            throw new StudyImporterException("unsupported spatial reference system [" + spatialReferenceSystem + "] found in [verbatimSRS]", ex);
        }
    }

    private static double parseDoubleOrThrow(Map<String, String> interaction, String propertyName) throws StudyImporterException {
        double verbatimLongitude;
        String verbatimLongitude1 = interaction.get(propertyName);
        if (NumberUtils.isNumber(verbatimLongitude1)) {
            verbatimLongitude = Double.parseDouble(verbatimLongitude1);
        } else {
            throw new StudyImporterException("expected a number for [" + propertyName + "], but found [" + verbatimLongitude1 + "]");
        }
        return verbatimLongitude;
    }

}
