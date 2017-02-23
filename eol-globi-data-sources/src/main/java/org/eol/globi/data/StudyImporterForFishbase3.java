package org.eol.globi.data;

import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.StudyImporterForHurlbert.columnValueOrNull;

public class StudyImporterForFishbase3 extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForFishbase3.class);

    public StudyImporterForFishbase3(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected static void importReferences(Map<String, Map<String, String>> references, InputStream resourceAsStream2) throws StudyImporterException {
        handleTsvInputStream(record -> {
            Map<String, String> reference = new HashMap<>();
            String refNo = columnValueOrNull(record, "RefNo");
            String author = columnValueOrNull(record, "Author");
            String year = columnValueOrNull(record, "Year");
            String title = columnValueOrNull(record, "Title");
            String source = columnValueOrNull(record, "Source");
            String citation = StringUtils.join(Arrays.asList(author, year, title, source).iterator(), ". ");

            reference.put(StudyImporterForTSV.REFERENCE_CITATION, citation.replace("..", "."));
            String doi = columnValueOrNull(record, "DOI");
            reference.put(StudyImporterForTSV.REFERENCE_DOI, doi);
            reference.put(StudyImporterForTSV.REFERENCE_URL, "http://fishbase.org/references/FBRefSummary.php?id=" + refNo);

            references.put(refNo, reference);
        }, resourceAsStream2);
    }

    protected static void importSpecies(Map<String, Map<String, String>> speciesMap, InputStream resourceAsStream1) throws StudyImporterException {
        handleTsvInputStream(record -> {
            Map<String, String> species = new HashMap<>();
            String specCode = columnValueOrNull(record, "SpecCode");
            String genus = columnValueOrNull(record, "Genus");
            String speciesName = columnValueOrNull(record, "Species");
            String scientificName = StringUtils.join(Arrays.asList(genus, speciesName).iterator(), " ");
            species.put("name", scientificName);
            species.put("rank", "species");
            speciesMap.put(specCode, species);
        }, resourceAsStream1);
    }

    public static void handleTsvInputStream(RecordListener listener, InputStream is) throws StudyImporterException {
        TsvParserSettings settings = new TsvParserSettings();
        settings.getFormat().setLineSeparator("\n");
        settings.setMaxCharsPerColumn(4096 * 2);
        settings.setHeaderExtractionEnabled(true);
        TsvParser parser = new TsvParser(settings);
        parser.beginParsing(is, CharsetConstant.UTF8);
        Record record;
        while ((record = parser.parseNextRecord()) != null) {
            listener.onRecord(record);
        }
    }

    protected static void importCountries(Map<String, Map<String, String>> countries, InputStream resourceAsStream) throws StudyImporterException {
        RecordListener recordListener = record -> {
            Map<String, String> country = new HashMap<>();
            String refNo = columnValueOrNull(record, "C_Code");
            String latitude = columnValueOrNull(record, "CenterLat");
            String longitude = columnValueOrNull(record, "CenterLong");
            String name = columnValueOrNull(record, "PAESE");

            country.put(StudyImporterForTSV.LOCALITY_NAME, name);
            country.put(StudyImporterForTSV.LOCALITY_ID, "FB_COUNTRY:" + refNo);
            String doi = columnValueOrNull(record, "DOI");
            country.put(StudyImporterForTSV.REFERENCE_DOI, doi);
            String paperURL = columnValueOrNull(record, "PaperURL");
            country.put(StudyImporterForTSV.REFERENCE_URL, paperURL);
            country.put(StudyImporterForTSV.DECIMAL_LATITUDE, latitude);
            country.put(StudyImporterForTSV.DECIMAL_LONGITUDE, longitude);

            countries.put(refNo, country);
        };
        handleTsvInputStream(recordListener, resourceAsStream);
    }

    protected static void importFoodItems(Map<String, Map<String, String>> references, Map<String, Map<String, String>> speciesMap, Map<String, Map<String, String>> countries, InteractionListener interactionListener, InputStream is) throws StudyImporterException {
        RecordListener listener = record -> {
            Map<String, String> props = new HashMap<>();

            String predatorSpeciesCode = columnValueOrNull(record, "SpecCode");
            props.put(StudyImporterForTSV.SOURCE_TAXON_ID, "FB_SPECIES:" + predatorSpeciesCode);
            if (StringUtils.isNotBlank(predatorSpeciesCode)) {
                Map<String, String> predatorProps = speciesMap.get(predatorSpeciesCode);
                if (predatorProps != null) {
                    props.put(StudyImporterForTSV.SOURCE_TAXON_NAME, predatorProps.get("name"));
                }
            }
            String predatorStage = columnValueOrNull(record, "PredatorStage");
            props.put(StudyImporterForTSV.SOURCE_LIFE_STAGE, predatorStage);

            String preyName = columnValueOrNull(record, "Foodname");
            props.put(StudyImporterForTSV.TARGET_TAXON_NAME, preyName);

            String preySpeciesCode = columnValueOrNull(record, "PreySpecCode");
            String preySpeciesCodeSLB = columnValueOrNull(record, "PreySpecCodeSLB");
            String targetTaxonId = null;
            if (StringUtils.isNotBlank(preySpeciesCode)) {
                targetTaxonId = "FB_SPECIES:" + preySpeciesCode;
            } else if (StringUtils.isNotBlank(preySpeciesCodeSLB)) {
                targetTaxonId = "SLB_SPECIES:" + preySpeciesCodeSLB;
            }

            props.put(StudyImporterForTSV.TARGET_TAXON_ID, targetTaxonId);

            String preyLifestage = columnValueOrNull(record, "PreyStage");
            props.put(StudyImporterForTSV.TARGET_LIFE_STAGE, preyLifestage);

            String referenceCode = columnValueOrNull(record, "FoodsRefNo");
            props.put(StudyImporterForTSV.REFERENCE_ID, "FB_REF:" + referenceCode);
            Map<String, String> reference = references.get(referenceCode);
            if (reference != null) {
                props.putAll(reference);
            }

            String locality = columnValueOrNull(record, "Locality");
            String countryCode = columnValueOrNull(record, "C_Code");
            Map<String, String> countryProperties = countries.get(countryCode);
            if (countryProperties != null) {
                props.put(StudyImporterForTSV.DECIMAL_LATITUDE, countryProperties.get(StudyImporterForTSV.DECIMAL_LATITUDE));
                props.put(StudyImporterForTSV.DECIMAL_LONGITUDE, countryProperties.get(StudyImporterForTSV.DECIMAL_LONGITUDE));

                props.put(StudyImporterForTSV.LOCALITY_NAME, countryProperties.get(StudyImporterForTSV.LOCALITY_NAME) + "|" + locality);
                props.put(StudyImporterForTSV.LOCALITY_ID, "FB_COUNTRY:" + countryCode + "|");
            }
            props.put(StudyImporterForTSV.INTERACTION_TYPE_NAME, "eats");
            props.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "http://purl.obolibrary.org/obo/RO_0002470");
            interactionListener.newLink(props);
        };


        handleTsvInputStream(listener, is);
    }

    protected static void importPredators(InteractionListener interactionListener, InputStream is, Map<String, Map<String, String>> speciesMap, Map<String, Map<String, String>> references, Map<String, Map<String, String>> countries) throws StudyImporterException {
        RecordListener listener1 = record -> {
            Map<String, String> props = new HashMap<>();
            String predatorSpeciesCode = columnValueOrNull(record, "predatcode");
            props.put(StudyImporterForTSV.SOURCE_TAXON_ID, "FB_SPECIES:" + predatorSpeciesCode);
            if (StringUtils.isNotBlank(predatorSpeciesCode)) {
                Map<String, String> predatorProps = speciesMap.get(predatorSpeciesCode);
                if (predatorProps != null) {
                    props.put(StudyImporterForTSV.SOURCE_TAXON_NAME, predatorProps.get("name"));
                }
            }
            String predatorStage = columnValueOrNull(record, "Predatstage");
            props.put(StudyImporterForTSV.SOURCE_LIFE_STAGE, predatorStage);


            String preySpeciesCode = columnValueOrNull(record, "SpecCode");
            String targetTaxonId = null;
            if (StringUtils.isNotBlank(preySpeciesCode)) {
                targetTaxonId = "FB_SPECIES:" + preySpeciesCode;
            }

            props.put(StudyImporterForTSV.TARGET_TAXON_ID, targetTaxonId);

            Map<String, String> preySpecies = speciesMap.get(preySpeciesCode);
            if (preySpecies != null) {
                props.put(StudyImporterForTSV.TARGET_TAXON_NAME, preySpecies.get("name"));
            }

            String preyLifestage = columnValueOrNull(record, "PreyStage");
            props.put(StudyImporterForTSV.TARGET_LIFE_STAGE, preyLifestage);

            String referenceCode = columnValueOrNull(record, "PredatsRefNo");
            props.put(StudyImporterForTSV.REFERENCE_ID, "FB_REF:" + referenceCode);
            Map<String, String> reference = references.get(referenceCode);
            if (reference != null) {
                props.putAll(reference);
            }

            String locality = columnValueOrNull(record, "Locality");
            String countryCode = columnValueOrNull(record, "C_Code");
            Map<String, String> countryProperties = countries.get(countryCode);
            if (countryProperties != null) {
                props.put(StudyImporterForTSV.DECIMAL_LATITUDE, countryProperties.get(StudyImporterForTSV.DECIMAL_LATITUDE));
                props.put(StudyImporterForTSV.DECIMAL_LONGITUDE, countryProperties.get(StudyImporterForTSV.DECIMAL_LONGITUDE));

                props.put(StudyImporterForTSV.LOCALITY_NAME, countryProperties.get(StudyImporterForTSV.LOCALITY_NAME) + "|" + locality);
                props.put(StudyImporterForTSV.LOCALITY_ID, "FB_COUNTRY:" + countryCode + "|");
            }
            props.put(StudyImporterForTSV.INTERACTION_TYPE_NAME, InteractType.PREYS_UPON.getLabel());
            props.put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.PREYS_UPON.getIRI());
            interactionListener.newLink(props);
        };


        handleTsvInputStream(listener1, is);
    }

    protected static void importDiet(List<Map<String, String>> links, InputStream is, Map<String, Map<String, String>> speciesMap, Map<String, Map<String, String>> references, Map<String, Map<String, String>> countries) throws StudyImporterException {
        RecordListener listener1 = record -> {
            Map<String, String> props = new HashMap<>();
            String predatorSpeciesCode = columnValueOrNull(record, "Speccode");
            props.put(StudyImporterForTSV.SOURCE_TAXON_ID, "FB_SPECIES:" + predatorSpeciesCode);
            if (StringUtils.isNotBlank(predatorSpeciesCode)) {
                Map<String, String> predatorProps = speciesMap.get(predatorSpeciesCode);
                if (predatorProps != null) {
                    props.put(StudyImporterForTSV.SOURCE_TAXON_NAME, predatorProps.get("name"));
                }
            }
            String predatorStage = columnValueOrNull(record, "SampleStage");
            props.put(StudyImporterForTSV.SOURCE_LIFE_STAGE, predatorStage);


            String preySpeciesCode = columnValueOrNull(record, "DietSpeccode");
            String targetTaxonId = null;
            if (StringUtils.isNotBlank(preySpeciesCode)) {
                targetTaxonId = "FB_SPECIES:" + preySpeciesCode;
            }

            props.put(StudyImporterForTSV.TARGET_TAXON_ID, targetTaxonId);

            Map<String, String> preySpecies = speciesMap.get(preySpeciesCode);
            if (preySpecies != null) {
                props.put(StudyImporterForTSV.TARGET_TAXON_NAME, preySpecies.get("name"));
            } else {
                props.put(StudyImporterForTSV.TARGET_TAXON_NAME, columnValueOrNull(record, "ItemName"));
            }

            String preyLifestage = columnValueOrNull(record, "Stage");
            props.put(StudyImporterForTSV.TARGET_LIFE_STAGE, preyLifestage);

            String referenceCode = columnValueOrNull(record, "DietRefNo");
            props.put(StudyImporterForTSV.REFERENCE_ID, "FB_REF:" + referenceCode);
            Map<String, String> reference = references.get(referenceCode);
            if (reference != null) {
                props.putAll(reference);
            }

            String locality = columnValueOrNull(record, "Locality");
            String countryCode = columnValueOrNull(record, "C_Code");
            Map<String, String> countryProperties = countries.get(countryCode);
            if (countryProperties != null) {
                props.put(StudyImporterForTSV.DECIMAL_LATITUDE, countryProperties.get(StudyImporterForTSV.DECIMAL_LATITUDE));
                props.put(StudyImporterForTSV.DECIMAL_LONGITUDE, countryProperties.get(StudyImporterForTSV.DECIMAL_LONGITUDE));

                props.put(StudyImporterForTSV.LOCALITY_NAME, countryProperties.get(StudyImporterForTSV.LOCALITY_NAME) + "|" + locality);
                props.put(StudyImporterForTSV.LOCALITY_ID, "FB_COUNTRY:" + countryCode + "|");
            }
            props.put(StudyImporterForTSV.INTERACTION_TYPE_NAME, InteractType.ATE.getLabel());
            props.put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
            ((InteractionListener) links::add).newLink(props);
        };


        handleTsvInputStream(listener1, is);
    }

    @Override
    public void importStudy() throws StudyImporterException {

    }

    interface RecordListener {
        void onRecord(Record record) throws StudyImporterException;
    }
}
