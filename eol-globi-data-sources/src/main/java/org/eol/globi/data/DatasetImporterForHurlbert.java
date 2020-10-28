package org.eol.globi.data;

import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.geo.LatLng;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.InvalidLocationException;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.util.SpecimenUtil;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DatasetImporterForHurlbert extends NodeBasedImporter {

    public static final URI RESOURCE = URI.create("AvianDietDatabase.txt");

    private static final Log LOG = LogFactory.getLog(DatasetImporterForHurlbert.class);

    public DatasetImporterForHurlbert(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {

        ;
        try (InputStream resource = getDataset().retrieve(RESOURCE)) {
            setCurrentResource(RESOURCE);

            Set<String> regions = new HashSet<String>();
            Set<String> locales = new HashSet<String>();
            Set<String> habitats = new HashSet<String>();

            TsvParserSettings settings = new TsvParserSettings();
            settings.getFormat().setLineSeparator("\n");
            settings.setHeaderExtractionEnabled(true);
            TsvParser parser = new TsvParser(settings);
            parser.beginParsing(resource, CharsetConstant.UTF8);
            Record record;
            while ((record = parser.parseNextRecord()) != null) {
                setCurrentLine(parser.getContext().currentLine());
                String columnNameSource = "Source";
                String sourceCitation = columnValueOrNull(record, columnNameSource);
                if (StringUtils.isBlank(sourceCitation)) {
                    LOG.warn(createMsg("failed to extract source from column [" + columnNameSource + "] in [" + RESOURCE + "]"));
                } else {
                    importRecords(regions, locales, habitats, record, sourceCitation);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to access [" + RESOURCE + "]", e);
        }

    }

    private void importRecords(Set<String> regions, Set<String> locales, Set<String> habitats, Record record, String sourceCitation) throws StudyImporterException {
        String namespace = getDataset() == null ? "" : getDataset().getNamespace();
        StudyImpl study1 = new StudyImpl(namespace + sourceCitation, null, sourceCitation);
        study1.setOriginatingDataset(getDataset());
        Study study = getNodeFactory().getOrCreateStudy(study1);

        //ID,Common_Name,Scientific_Name,,,,Prey_Common_Name,Fraction_Diet_By_Wt_or_Vol,Fraction_Diet_By_Items,Fraction_Occurrence,Fraction_Diet_Unspecified,Item Sample Size,Bird Sample size,Sites,StudyNode Type,Notes,Source

        String preyLabels[] = {"Prey_Kingdom", "Prey_Phylum", "Prey_Class", "Prey_Order", "Prey_Suborder", "Prey_Family", "Prey_Genus", "Prey_Scientific_Name"};
        ArrayUtils.reverse(preyLabels);
        String preyTaxonName = null;
        for (String preyLabel : preyLabels) {
            preyTaxonName = columnValueOrNull(record, preyLabel);
            if (StringUtils.isNotBlank(preyTaxonName) && !"NA".equalsIgnoreCase(preyTaxonName)) {
                break;
            }
        }

        String predatorTaxonName = StringUtils.trim(columnValueOrNull(record, "Scientific_Name"));
        if (StringUtils.isNotBlank(StringUtils.trim(predatorTaxonName))
                && StringUtils.isNotBlank(StringUtils.trim(preyTaxonName))) {
            importInteraction(regions, locales, habitats, record, study, preyTaxonName, predatorTaxonName);
        }
    }

    public static String columnValueOrNull(Record record, String columnName) {
        String value = record.getMetaData().containsColumn(columnName)
                ? StringUtils.trim(record.getString(columnName))
                : null;
        return StringUtils.equals("null", value) || StringUtils.equalsIgnoreCase("NA", value) ? null : StringEscapeUtils.unescapeCsv(value);
    }

    protected void importInteraction(Set<String> regions, Set<String> locales, Set<String> habitats, Record record, Study study, String preyTaxonName, String predatorName) throws StudyImporterException {
        try {
            Taxon predatorTaxon = new TaxonImpl(predatorName);
            Specimen predatorSpecimen = getNodeFactory().createSpecimen(study, predatorTaxon);
            SpecimenUtil.setBasisOfRecordAsLiterature(predatorSpecimen, getNodeFactory());

            Taxon preyTaxon = new TaxonImpl(preyTaxonName);
            String preyNameId = StringUtils.trim(columnValueOrNull(record, "Prey_Name_ITIS_ID"));
            if (NumberUtils.isDigits(preyNameId)) {
                preyTaxon.setExternalId(TaxonomyProvider.ITIS.getIdPrefix() + preyNameId);
            }
            Specimen preySpecimen = getNodeFactory().createSpecimen(study, preyTaxon);
            SpecimenUtil.setBasisOfRecordAsLiterature(preySpecimen, getNodeFactory());

            String preyStage = StringUtils.trim(columnValueOrNull(record, "Prey_Stage"));
            if (StringUtils.isNotBlank(preyStage)) {
                Term lifeStage = getNodeFactory().getOrCreateLifeStage("HULBERT:" + StringUtils.replace(preyStage, " ", "_"), preyStage);
                preySpecimen.setLifeStage(lifeStage);
            }

            String preyPart = StringUtils.trim(columnValueOrNull(record, "Prey_Part"));
            if (StringUtils.isNotBlank(preyPart)) {
                Term term = getNodeFactory().getOrCreateBodyPart("HULBERT:" + StringUtils.replace(preyPart, " ", "_"), preyPart);
                preySpecimen.setBodyPart(term);
            }
            Date date = addCollectionDate(record, study);
            getNodeFactory().setUnixEpochProperty(predatorSpecimen, date);
            getNodeFactory().setUnixEpochProperty(preySpecimen, date);

            LocationImpl location = new LocationImpl(null, null, null, null);
            String longitude = columnValueOrNull(record, "Longitude_dd");
            String latitude = columnValueOrNull(record, "Latitude_dd");
            if (NumberUtils.isParsable(latitude) && NumberUtils.isCreatable(longitude)) {
                try {
                    LatLng latLng = LocationUtil.parseLatLng(latitude, longitude);
                    String altitude = columnValueOrNull(record, "Altitude_mean_m");
                    Double altitudeD = NumberUtils.isCreatable(altitude) ? Double.parseDouble(altitude) : null;
                    location = new LocationImpl(latLng.getLat(), latLng.getLng(), altitudeD, null);
                } catch (InvalidLocationException e) {
                    getLogger().warn(study, createMsg("found invalid (lat,lng) pair: (" + latitude + "," + longitude + ")"));
                }
            }

            String locationRegion = columnValueOrNull(record, "Location_Region");
            String locationSpecific = columnValueOrNull(record, "Location_Specific");
            location.setLocality(StringUtils.join(Arrays.asList(locationRegion, locationSpecific), ":"));

            Location locationNode = getNodeFactory().getOrCreateLocation(location);
            String habitat_type = columnValueOrNull(record, "Habitat_type");
            List<Term> habitatList = Arrays.stream(StringUtils.split(StringUtils.defaultIfBlank(habitat_type, ""), ";"))
                    .map(StringUtils::trim)
                    .map(habitat -> new TermImpl(idForHabitat(habitat), habitat))
                    .collect(Collectors.toList());
            getNodeFactory().addEnvironmentToLocation(locationNode, habitatList);

            preySpecimen.caughtIn(locationNode);
            predatorSpecimen.caughtIn(locationNode);

            predatorSpecimen.ate(preySpecimen);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create interaction between [" + predatorName + "] and [" + preyTaxonName + "]", e);
        }

    }

    private final static Map<String, String> HABITAT_MAPPING = new TreeMap<String, String>() {{
        put("agriculture", "ENVO:00000077");
        put("coniferous forest", "ENVO:01000240");
        put("deciduous forest", "ENVO:01000816");
        put("desert", "ENVO:00000097");
        put("forest", "ENVO:00000111");
        put("grassland", "ENVO:00000106");
        put("mangrove forest", "ENVO:01000403");
        put("mudflat", "ENVO:00000192");
        put("shrubland", "ENVO:00000300");
        put("tundra", "ENVO:00000112");
        put("urban", "ENVO:00000856");
        put("wetland", "ENVO:00000043");
        put("woodland", "ENVO:00000109");
    }};

    private String idForHabitat(String habitat) {
        String id = HABITAT_MAPPING.get(StringUtils.lowerCase(habitat));
        return StringUtils.isBlank(id) ? "HURLBERT:" + habitat : id;
    }

    private Date addCollectionDate(Record record, Study study) {
        String dateString = getDateString(record.getString("Observation_Year_Begin"), record.getString("Observation_Month_Begin"));
        if (StringUtils.isBlank(dateString)) {
            dateString = getDateString(record.getString("Observation_Year_End"), record.getString("Observation_Month_End"));
        }

        Date date = null;
        if (StringUtils.isNotBlank(dateString)) {
            try {
                DateTime dateTime = DateUtil.parseYearMonthUTC(dateString);
                date = dateTime.toDate();
            } catch (IllegalArgumentException e) {
                try {
                    date = DateUtil.parseYearUTC(dateString).toDate();
                } catch (IllegalArgumentException e1) {
                    String message = "not setting collection date, because [" + dateString + "] could not be read as date.";
                    getLogger().warn(study, createMsg(message));
                }
            }
        }
        return date;
    }

    static String getDateString(String yearString, String monthString) {
        String dateString = "";
        String year = yearString;
        if (notBlankOrNA(year)) {
            dateString = StringUtils.trim(year);
            String month = monthString;
            if (notBlankOrNA(month)) {
                String monthTrimmedAndChopped = trimAndChopMonth(month);
                dateString += "-" + monthTrimmedAndChopped;
            }
        }
        return dateString;
    }

    private static String trimAndChopMonth(String month) {
        // from https://github.com/hurlbertlab/dietdatabase/issues/104#issuecomment-421398922
        //
        // [...] I have left for the moment the decimal month entries. These were used for studies that examined
        // diets for different times within the same month. E.g., diet items for June 1-10 versus June 11-20
        // versus June 21-30. Within each of those periods, % diet info for a given bird species sums to 100%, b
        // ut with our current database structure, the only way to identify unique diet analyses
        // (set of records that sum to 100%) is to find the unique combination of Source,
        // Observation_Year_Begin, Observation_Month_Begin, Observation_Season, Bird_Sample_Size,
        // Habitat_type, Location_Region, Item_Sample_Size, Taxon, and Diet_Type.

        //  For some of these problem studies, all of the values are identical, and specifying an
        // Observation_Month of 6.3 versus 6.7 is the only way that these sets of records are distinguished
        // by some of our summary functions. [...]

        return StringUtils.replacePattern(StringUtils.trim(month), "\\.[0-9]+$", "");
    }

    private static boolean notBlankOrNA(String str) {
        return StringUtils.isNotBlank(str) && !StringUtils.equalsIgnoreCase(StringUtils.trim(str), "NA");
    }

}
