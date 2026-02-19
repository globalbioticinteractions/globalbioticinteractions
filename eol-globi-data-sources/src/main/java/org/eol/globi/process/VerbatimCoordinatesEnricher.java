package org.eol.globi.process;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.DatasetImporterForMetaTable;
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
        if (!hasNonNullProperty(interaction, DatasetImporterForMetaTable.LATITUDE)
                && !hasNonNullProperty(interaction, DatasetImporterForMetaTable.LONGITUDE)
                && hasNonNullProperty(interaction, DatasetImporterForMetaTable.VERBATIM_LONGITUDE)
                && hasNonNullProperty(interaction, DatasetImporterForMetaTable.VERBATIM_LATITUDE)
        ) {
            try {
                if (!hasNonNullProperty(interaction, DatasetImporterForMetaTable.VERBATIM_SRS)) {
                    LogUtil.logWarningIfPossible(interaction, "cannot interpret {verbatimLatitude,verbatimLongitude} " + "[{" + interaction.get(DatasetImporterForMetaTable.VERBATIM_LATITUDE) + "," + interaction.get(DatasetImporterForMetaTable.VERBATIM_LONGITUDE) + "}] : no spatial reference system defined using [verbatimSRS].", logger);
                } else {
                    enriched = convertCoordinates(interaction);
                }
            } catch (Proj4jException ex) {
                throw new StudyImporterException("failed to enrich coordinates", ex);
            }
        }
        emit(enriched);
    }

    private static Map<String, String> convertCoordinates(Map<String, String> interaction) throws StudyImporterException {
        Map<String, String> enriched;
        CoordinateReferenceSystem srs = getCRSOrThrow(interaction.get(DatasetImporterForMetaTable.VERBATIM_SRS));
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();

        double verbatimLongitude = parseDoubleOrThrow(interaction, DatasetImporterForMetaTable.VERBATIM_LONGITUDE);
        double verbatimLatitude = parseDoubleOrThrow(interaction, DatasetImporterForMetaTable.VERBATIM_LATITUDE);

        ProjCoordinate sourceCoordinates = new ProjCoordinate(
                verbatimLongitude,
                verbatimLatitude
        );

        CoordinateTransform transform = ctFactory.createTransform(srs, WGS_84);
        ProjCoordinate targetCoordinates = new ProjCoordinate();
        transform.transform(sourceCoordinates, targetCoordinates);
        enriched = new TreeMap<>(interaction);
        enriched.put(DatasetImporterForMetaTable.LATITUDE, Double.toString(targetCoordinates.y));
        enriched.put(DatasetImporterForMetaTable.LONGITUDE, Double.toString(targetCoordinates.x));
        enriched.put(DatasetImporterForMetaTable.GEODETIC_DATUM, "epsg:4326");
        return enriched;
    }

    private static boolean hasNonNullProperty(Map<String, String> interaction, String propertyName) {
        return interaction.containsKey(propertyName)
                && StringUtils.isNoneBlank(interaction.get(propertyName));
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
