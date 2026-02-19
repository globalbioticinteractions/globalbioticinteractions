package org.eol.globi.process;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.DatasetImporterForMetaTable;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.StudyImporterException;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.Proj4jException;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eol.globi.data.DatasetImporterForMetaTable.EVENT_DATE;

public class EventDateEnricher extends InteractionProcessorAbstract {

    public static final CRSFactory CRS_FACTORY = new CRSFactory();
    public static final CoordinateReferenceSystem WGS_84 = CRS_FACTORY.createFromName("epsg:4326");
    public static final Pattern TRAILING_HYPHEN_PATTERN = Pattern.compile("-+$");

    public EventDateEnricher(InteractionListener listener, ImportLogger logger) {
        super(listener, logger);
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        Map<String, String> enriched = interaction;
        if (!hasNonBlankProperty(interaction, "eventDate")
                && !hasNonBlankProperty(interaction, "http://rs.tdwg.org/dwc/terms/eventDate")
                && hasNonBlankProperty(interaction, DatasetImporterForMetaTable.EVENT_DATE_YEAR)
        ) {
            try {
                enriched = new TreeMap<>(interaction);
                Stream<String> yearMonthDay = hasNonBlankProperty(interaction, DatasetImporterForMetaTable.EVENT_DATE_DAY_OF_YEAR)
                        ? yearDayOfYear(interaction)
                        : yearMondayDay(interaction)
                ;

                String eventDate = yearMonthDay.map(part -> StringUtils.defaultIfBlank(part, "")).collect(Collectors.joining("-"));

                String eventDateTrimmed = TRAILING_HYPHEN_PATTERN.matcher(eventDate).replaceFirst("");
                enriched.put(EVENT_DATE, eventDateTrimmed);
            } catch (Proj4jException ex) {
                throw new StudyImporterException("failed to enrich coordinates", ex);
            }
        }
        emit(enriched);
    }

    private static Stream<String> yearDayOfYear(Map<String, String> interaction) {
        return Stream.of(interaction.get(DatasetImporterForMetaTable.EVENT_DATE_YEAR),
                StringUtils.leftPad(interaction.get(DatasetImporterForMetaTable.EVENT_DATE_DAY_OF_YEAR), 3, "0"));
    }

    private static Stream<String> yearMondayDay(Map<String, String> interaction) {
        return Stream.of(interaction.get(DatasetImporterForMetaTable.EVENT_DATE_YEAR),
                interaction.getOrDefault(DatasetImporterForMetaTable.EVENT_DATE_MONTH, ""),
                interaction.getOrDefault(DatasetImporterForMetaTable.EVENT_DATE_DAY, ""));
    }


    private static boolean hasNonBlankProperty(Map<String, String> interaction, String propertyName) {
        return interaction.containsKey(propertyName)
                && StringUtils.isNoneBlank(interaction.get(propertyName));
    }
}
