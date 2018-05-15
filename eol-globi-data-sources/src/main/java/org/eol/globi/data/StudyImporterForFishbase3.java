package org.eol.globi.data;

import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.TaxonomyProvider;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.eol.globi.data.StudyImporterForHurlbert.columnValueOrNull;

public class StudyImporterForFishbase3 extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForFishbase3.class);
    public static final String C_CODE_GLOBAL = "9999";

    public StudyImporterForFishbase3(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            String defaultNamespace = getDataset().getOrDefault("namespace", "FB");

            HashMap<String, Map<String, String>> countries = new HashMap<>();
            importCountries(countries, getDataset().getResource("countref.tsv"));
            HashMap<String, Map<String, String>> references = new HashMap<>();
            importReferences(references, getDataset().getResource("refrens.tsv"), defaultNamespace);
            HashMap<String, Map<String, String>> speciesMap = new HashMap<>();
            importSpecies(speciesMap, getDataset().getResource("species_sealifebase.tsv"), "SLB");
            importSpecies(speciesMap, getDataset().getResource("species_fishbase.tsv"), "FB");

            InteractionListener listener = new InteractionListener() {
                private final InteractionListener listener = new InteractionListenerImpl(nodeFactory, getGeoNamesService(), getLogger());

                @Override
                public void newLink(Map<String, String> properties) throws StudyImporterException {
                    listener.newLink(new HashMap<String, String>(properties) {{
                        put(StudyImporterForTSV.STUDY_SOURCE_CITATION, getSourceCitationLastAccessed());
                    }});
                }
            };

            importDiet(listener, getDataset().getResource("diet.tsv"), speciesMap, references, countries, defaultNamespace);
            importDietFoodII(listener, getDataset().getResource("diet.tsv"), speciesMap, references, countries, defaultNamespace);

            importPredators(listener, getDataset().getResource("predats.tsv"), speciesMap, references, countries, defaultNamespace);

            importFoodItemsByFoodName(listener, getDataset().getResource("fooditems.tsv"), speciesMap, references, countries, defaultNamespace);
            importFoodItemsByFoodII(listener, getDataset().getResource("fooditems.tsv"), speciesMap, references, countries, defaultNamespace);

        } catch (IOException e) {
            throw new StudyImporterException("failed to import", e);
        }

    }

    protected static void importReferences(Map<String, Map<String, String>> references, InputStream resourceAsStream2, String defaultNamespace) throws StudyImporterException {
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

            String hostname = StringUtils.equals(defaultNamespace, "SLB")
                    ? "sealifebase.org"
                    : "fishbase.org";

            reference.put(StudyImporterForTSV.REFERENCE_URL, "http://" + hostname + "/references/FBRefSummary.php?id=" + refNo);
            references.put(defaultNamespace + "_REF:" + refNo, reference);
        }, resourceAsStream2);
    }

    protected static void importSpecies(Map<String, Map<String, String>> speciesMap, InputStream resourceAsStream1, String speciesNamespace) throws StudyImporterException {
        handleTsvInputStream(record -> {
            Map<String, String> species = new HashMap<>();
            String specCode = columnValueOrNull(record, "SpecCode");
            String genus = columnValueOrNull(record, "Genus");
            String speciesName = columnValueOrNull(record, "Species");
            String scientificName = StringUtils.join(Arrays.asList(genus, speciesName).iterator(), " ");
            species.put("name", scientificName);
            species.put("rank", "species");
            speciesMap.put(idForSpecies(speciesNamespace, specCode), species);
        }, resourceAsStream1);
    }

    private static String idForSpecies(String speciesNamespace, String specCode) {
        return TaxonomyProvider.FISHBASE_CACHE.getIdPrefix() + speciesNamespace + ":SPECCODE:" + specCode;
    }

    public static void handleTsvInputStream(RecordListener listener, InputStream is) throws StudyImporterException {
        TsvParserSettings settings = new TsvParserSettings();
        settings.getFormat().setLineSeparator("\n");
        settings.setMaxCharsPerColumn(4096 * 8);
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

    protected static void importFoodItemsByFoodName(InteractionListener interactionListener, InputStream is, Map<String, Map<String, String>> speciesMap, Map<String, Map<String, String>> references, Map<String, Map<String, String>> countries, String namespace) throws StudyImporterException {
        RecordListener listener = record -> {
            Map<String, String> props = generateFoodItemInteraction(speciesMap, references, countries, namespace, record, "Foodname");
            appendTargetSpeciesInfo(record, props);
            interactionListener.newLink(props);
        };
        handleTsvInputStream(listener, is);
    }

    protected static void importFoodItemsByFoodII(InteractionListener interactionListener, InputStream is, Map<String, Map<String, String>> speciesMap, Map<String, Map<String, String>> references, Map<String, Map<String, String>> countries, String namespace) throws StudyImporterException {
        RecordListener listener = record -> {
            Map<String, String> props = generateFoodItemInteraction(speciesMap, references, countries, namespace, record, "FoodII");
            interactionListener.newLink(props);
        };
        handleTsvInputStream(listener, is);
    }

    private static Map<String, String> generateFoodItemInteraction(Map<String, Map<String, String>> speciesMap, Map<String, Map<String, String>> references, Map<String, Map<String, String>> countries, String namespace, Record record, String foodItemName) {
        Map<String, String> props = new HashMap<>();

        String predatorSpeciesCode = columnValueOrNull(record, "SpecCode");
        String sourceTaxonId = idForSpecies(namespace, predatorSpeciesCode);
        props.put(StudyImporterForTSV.SOURCE_TAXON_ID, sourceTaxonId);
        if (StringUtils.isNotBlank(predatorSpeciesCode)) {
            Map<String, String> predatorProps = speciesMap.get(sourceTaxonId);
            if (predatorProps != null) {
                props.put(StudyImporterForTSV.SOURCE_TAXON_NAME, predatorProps.get("name"));
            }
        }
        String predatorStage = columnValueOrNull(record, "PredatorStage");
        props.put(StudyImporterForTSV.SOURCE_LIFE_STAGE, predatorStage);


        String preyName = columnValueOrNull(record, foodItemName);
        props.put(StudyImporterForTSV.TARGET_TAXON_NAME, preyName);

        lookupReference(references, namespace, record, props, "FoodsRefNo");
        lookupLocality(countries, namespace, record, props);

        props.put(StudyImporterForTSV.INTERACTION_TYPE_NAME, "eats");
        props.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "http://purl.obolibrary.org/obo/RO_0002470");
        return props;
    }

    private static void appendTargetSpeciesInfo(Record record, Map<String, String> props) {
        String preySpeciesCode = columnValueOrNull(record, "PreySpecCode");
        String preySpeciesCodeSLB = columnValueOrNull(record, "PreySpecCodeSLB");
        String targetTaxonId = null;
        if (StringUtils.isNotBlank(preySpeciesCode)) {
            String preySpecCodeDB = columnValueOrNull(record, "PreySpecCodeDB");
            String prefix = StringUtils.isBlank(preySpecCodeDB) ? "FB" : "SLB";
            targetTaxonId = idForSpecies(prefix, preySpeciesCode);
        } else if (StringUtils.isNotBlank(preySpeciesCodeSLB)) {
            targetTaxonId = idForSpecies("SLB", preySpeciesCodeSLB);
        }

        props.put(StudyImporterForTSV.TARGET_TAXON_ID, targetTaxonId);

        String preyLifestage = columnValueOrNull(record, "PreyStage");
        props.put(StudyImporterForTSV.TARGET_LIFE_STAGE, preyLifestage);
    }

    private static void lookupReference(Map<String, Map<String, String>> references, String defaultNamespace, Record record, Map<String, String> props, String refColumnName) {
        String referenceCode = columnValueOrNull(record, refColumnName);
        String referenceId = defaultNamespace + "_REF:" + referenceCode;
        props.put(StudyImporterForTSV.REFERENCE_ID, referenceId);
        Map<String, String> reference = references.get(referenceId);
        if (reference != null) {
            props.putAll(reference);
        }
    }

    protected static void importPredators(InteractionListener interactionListener, InputStream is, Map<String, Map<String, String>> speciesMap, Map<String, Map<String, String>> references, Map<String, Map<String, String>> countries, String defaultNamespace) throws StudyImporterException {
        RecordListener listener1 = record -> {
            Map<String, String> props = new HashMap<>();
            String predatorSpeciesCode = columnValueOrNull(record, "predatcode");
            String speciesDB = columnValueOrNull(record, "PredatCodeDB");
            String taxonIdPrefix = StringUtils.isBlank(speciesDB) ? defaultNamespace : speciesDB;

            String sourceTaxonId = idForSpecies(taxonIdPrefix, predatorSpeciesCode);
            props.put(StudyImporterForTSV.SOURCE_TAXON_ID, sourceTaxonId);
            if (StringUtils.isNotBlank(predatorSpeciesCode)) {
                Map<String, String> predatorProps = speciesMap.get(sourceTaxonId);
                if (predatorProps != null) {
                    props.put(StudyImporterForTSV.SOURCE_TAXON_NAME, predatorProps.get("name"));
                }
            }
            String predatorStage = columnValueOrNull(record, "Predatstage");
            props.put(StudyImporterForTSV.SOURCE_LIFE_STAGE, predatorStage);


            String preySpeciesCode = columnValueOrNull(record, "SpecCode");
            String targetTaxonId = null;
            if (StringUtils.isNotBlank(preySpeciesCode)) {
                targetTaxonId = idForSpecies(defaultNamespace, preySpeciesCode);
                Map<String, String> preySpecies = speciesMap.get(targetTaxonId);
                if (preySpecies != null) {
                    props.put(StudyImporterForTSV.TARGET_TAXON_NAME, preySpecies.get("name"));
                }
            }

            props.put(StudyImporterForTSV.TARGET_TAXON_ID, targetTaxonId);


            String preyLifestage = columnValueOrNull(record, "PreyStage");
            props.put(StudyImporterForTSV.TARGET_LIFE_STAGE, preyLifestage);

            lookupReference(references, defaultNamespace, record, props, "PredatsRefNo");

            lookupLocality(countries, defaultNamespace, record, props);

            InteractType interactType = InteractType.PREYS_UPON;
            props.put(StudyImporterForTSV.INTERACTION_TYPE_NAME, interactType.getLabel());
            props.put(StudyImporterForTSV.INTERACTION_TYPE_ID, interactType.getIRI());
            interactionListener.newLink(props);
        };


        handleTsvInputStream(listener1, is);
    }

    private static void lookupLocality(Map<String, Map<String, String>> countries, String defaultNamespace, Record record, Map<String, String> props) {
        String locality = columnValueOrNull(record, "Locality");
        String countryCode = columnValueOrNull(record, "C_Code");
        Map<String, String> countryProperties = countries.get(countryCode);
        if (!StringUtils.equals(C_CODE_GLOBAL, countryCode) && countryProperties != null) {
            props.put(StudyImporterForTSV.DECIMAL_LATITUDE, countryProperties.get(StudyImporterForTSV.DECIMAL_LATITUDE));
            props.put(StudyImporterForTSV.DECIMAL_LONGITUDE, countryProperties.get(StudyImporterForTSV.DECIMAL_LONGITUDE));

            props.put(StudyImporterForTSV.LOCALITY_NAME, countryProperties.get(StudyImporterForTSV.LOCALITY_NAME) + "|" + locality);
            props.put(StudyImporterForTSV.LOCALITY_ID, defaultNamespace + "_COUNTRY:" + countryCode + "|");
        }
    }

    protected static void importDiet(InteractionListener listener, InputStream is, Map<String, Map<String, String>> speciesMap, Map<String, Map<String, String>> references, Map<String, Map<String, String>> countries, String namespace) throws StudyImporterException {
        RecordListener listener1 = record -> {
            Map<String, String> props = importPredator(speciesMap, references, countries, namespace, record);
            appendPreyInfo(speciesMap, namespace, record, props);
            listener.newLink(props);
        };


        handleTsvInputStream(listener1, is);
    }

    protected static void importDietFoodII(InteractionListener listener, InputStream is, Map<String, Map<String, String>> speciesMap, Map<String, Map<String, String>> references, Map<String, Map<String, String>> countries, String namespace) throws StudyImporterException {
        RecordListener listener1 = record -> {
            Map<String, String> props = importPredator(speciesMap, references, countries, namespace, record);
            props.put(StudyImporterForTSV.TARGET_TAXON_NAME, columnValueOrNull(record, "FoodII"));
            listener.newLink(props);
        };


        handleTsvInputStream(listener1, is);
    }

    private static void appendPreyInfo(Map<String, Map<String, String>> speciesMap, String namespace, Record record, Map<String, String> props) {
        String preySpeciesCode = columnValueOrNull(record, "DietSpeccode");
        String targetTaxonId = null;
        if (StringUtils.isNotBlank(preySpeciesCode)) {
            targetTaxonId = idForSpecies(namespace, preySpeciesCode);
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
    }

    private static Map<String, String> importPredator(Map<String, Map<String, String>> speciesMap, Map<String, Map<String, String>> references, Map<String, Map<String, String>> countries, String namespace, Record record) {
        Map<String, String> props = new HashMap<>();
        String predatorSpeciesCode = columnValueOrNull(record, "Speccode");
        String sourceTaxonId = idForSpecies(namespace, predatorSpeciesCode);
        props.put(StudyImporterForTSV.SOURCE_TAXON_ID, sourceTaxonId);
        if (StringUtils.isNotBlank(predatorSpeciesCode)) {
            Map<String, String> predatorProps = speciesMap.get(sourceTaxonId);
            if (predatorProps != null) {
                props.put(StudyImporterForTSV.SOURCE_TAXON_NAME, predatorProps.get("name"));
            }
        }
        String predatorStage = columnValueOrNull(record, "SampleStage");
        props.put(StudyImporterForTSV.SOURCE_LIFE_STAGE, predatorStage);

        lookupReference(references, namespace, record, props, "DietRefNo");
        lookupLocality(countries, namespace, record, props);

        props.put(StudyImporterForTSV.INTERACTION_TYPE_NAME, InteractType.ATE.getLabel());
        props.put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
        return props;
    }


    interface RecordListener {
        void onRecord(Record record) throws StudyImporterException;
    }
}
