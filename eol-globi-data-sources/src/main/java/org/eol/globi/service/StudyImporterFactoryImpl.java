package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.eol.globi.data.DatasetImporterForZOVER;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.data.ParserFactory;
import org.eol.globi.data.ParserFactoryForDataset;
import org.eol.globi.data.DatasetImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.DatasetImporterForAkin;
import org.eol.globi.data.DatasetImporterForBatBase;
import org.eol.globi.data.DatasetImporterForBatPlant;
import org.eol.globi.data.DatasetImporterForDBatVir;
import org.eol.globi.data.DatasetImporterForDwCA;
import org.eol.globi.data.DatasetImporterForGlobalWebDb;
import org.eol.globi.data.DatasetImporterForMangal;
import org.eol.globi.data.DatasetImporterForPensoft;
import org.eol.globi.data.DatasetImporterForSaproxylic;
import org.eol.globi.data.DatasetImporterForRSS;
import org.eol.globi.data.DatasetImporterForBaremore;
import org.eol.globi.data.DatasetImporterForBarnes;
import org.eol.globi.data.DatasetImporterForBell;
import org.eol.globi.data.DatasetImporterForBioInfo;
import org.eol.globi.data.DatasetImporterForBlewett;
import org.eol.globi.data.DatasetImporterForBrose;
import org.eol.globi.data.DatasetImporterForByrnes;
import org.eol.globi.data.DatasetImporterForCoetzer;
import org.eol.globi.data.DatasetImporterForCook;
import org.eol.globi.data.DatasetImporterForCruaud;
import org.eol.globi.data.DatasetImporterForDunne;
import org.eol.globi.data.DatasetImporterForFishbase3;
import org.eol.globi.data.DatasetImporterForGemina;
import org.eol.globi.data.DatasetImporterForGoMexSI2;
import org.eol.globi.data.DatasetImporterForGray;
import org.eol.globi.data.DatasetImporterForHafner;
import org.eol.globi.data.DatasetImporterForHechinger;
import org.eol.globi.data.DatasetImporterForHurlbert;
import org.eol.globi.data.DatasetImporterForICES;
import org.eol.globi.data.DatasetImporterForINaturalist;
import org.eol.globi.data.DatasetImporterForJRFerrerParis;
import org.eol.globi.data.DatasetImporterForJSONLD;
import org.eol.globi.data.DatasetImporterForKelpForest;
import org.eol.globi.data.DatasetImporterForLifeWatchGreece;
import org.eol.globi.data.DatasetImporterForMetaTable;
import org.eol.globi.data.DatasetImporterForPlanque;
import org.eol.globi.data.DatasetImporterForRaymond;
import org.eol.globi.data.DatasetImporterForRobledo;
import org.eol.globi.data.DatasetImporterForRoopnarine;
import org.eol.globi.data.DatasetImporterForSIAD;
import org.eol.globi.data.DatasetImporterForSPIRE;
import org.eol.globi.data.DatasetImporterForSeltmann;
import org.eol.globi.data.DatasetImporterForSimons;
import org.eol.globi.data.DatasetImporterForStrona;
import org.eol.globi.data.DatasetImporterForSzoboszlai;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.DatasetImporterForWebOfLife;
import org.eol.globi.data.DatasetImporterForWood;
import org.eol.globi.data.DatasetImporterForWrast;
import org.eol.globi.data.DatasetImporterForZenodoMetadata;
import org.globalbioticinteractions.dataset.Dataset;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import static org.eol.globi.domain.PropertyAndValueDictionary.MIME_TYPE_DWCA;

public class StudyImporterFactoryImpl implements StudyImporterFactory {

    private final NodeFactory nodeFactory;

    public StudyImporterFactoryImpl(NodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public static DatasetImporter createImporter(Dataset dataset, final NodeFactory nodeFactory) throws StudyImporterException {
        Class<? extends DatasetImporter> anImporter = findImporterFor(dataset);
        try {
            Constructor<? extends DatasetImporter> constructor = anImporter.getConstructor(ParserFactory.class, NodeFactory.class);
            ParserFactoryForDataset parserFactory = new ParserFactoryForDataset(dataset);
            DatasetImporter datasetImporter = constructor.newInstance(parserFactory,
                    new NodeFactoryWithDatasetContext(nodeFactory, dataset));
            datasetImporter.setDataset(dataset);
            return datasetImporter;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new StudyImporterException("failed to instantiate importer for [" + dataset.getNamespace() + "]", e);
        }
    }

    private static Class<? extends DatasetImporter> findImporterFor(Dataset dataset) throws StudyImporterException {
        Class<? extends DatasetImporter> anImporter;
        if (dataset.getConfigURI() != null && dataset.getConfigURI().toString().endsWith(".jsonld")) {
            anImporter = DatasetImporterForJSONLD.class;
        } else if (isMetaTableImporter(dataset.getConfig())) {
            anImporter = DatasetImporterForMetaTable.class;
        } else {
            anImporter = lookupImporterByFormat(dataset);
        }
        return anImporter;
    }

    private static Class<? extends DatasetImporter> lookupImporterByFormat(Dataset dataset) throws StudyImporterException {
        final String format = dataset.getFormat();
        if (StringUtils.isBlank(format)) {
            throw new StudyImporterException("provide specify format for [" + dataset.getConfigURI() + "]");
        }

        return importerForFormat(format);
    }

    static Class<? extends DatasetImporter> importerForFormat(String format) throws StudyImporterException {
        HashMap<String, Class<? extends DatasetImporter>> supportedFormats = new HashMap<String, Class<? extends DatasetImporter>>() {
            {
                put("globi", DatasetImporterForTSV.class);
                put("gomexsi", DatasetImporterForGoMexSI2.class);
                put("hechinger", DatasetImporterForHechinger.class);
                put("dunne", DatasetImporterForDunne.class);
                put("seltmann", DatasetImporterForSeltmann.class);
                put("arthropodEasyCapture", DatasetImporterForRSS.class);
                put("coetzer", DatasetImporterForCoetzer.class);
                put("wood", DatasetImporterForWood.class);
                put("szoboszlai", DatasetImporterForSzoboszlai.class);
                put("planque", DatasetImporterForPlanque.class);
                put("gray", DatasetImporterForGray.class);
                put("akin", DatasetImporterForAkin.class);
                put("baremore", DatasetImporterForBaremore.class);
                put("barnes", DatasetImporterForBarnes.class);
                put("bell", DatasetImporterForBell.class);
                put("bioinfo", DatasetImporterForBioInfo.class);
                put("blewett", DatasetImporterForBlewett.class);
                put("brose", DatasetImporterForBrose.class);
                put("byrnes", DatasetImporterForByrnes.class);
                put("cook", DatasetImporterForCook.class);
                put("cruaud", DatasetImporterForCruaud.class);
                put("ferrer-paris", DatasetImporterForJRFerrerParis.class);
                put("fishbase", DatasetImporterForFishbase3.class);
                put("gemina", DatasetImporterForGemina.class);
                put("hafner", DatasetImporterForHafner.class);
                put("hurlbert", DatasetImporterForHurlbert.class);
                put("ices", DatasetImporterForICES.class);
                put("inaturalist", DatasetImporterForINaturalist.class);
                put("kelpforest", DatasetImporterForKelpForest.class);
                put("life-watch-greece", DatasetImporterForLifeWatchGreece.class);
                put("raymond", DatasetImporterForRaymond.class);
                put("robledo", DatasetImporterForRobledo.class);
                put("roopnarine", DatasetImporterForRoopnarine.class);
                put("simons", DatasetImporterForSimons.class);
                put("siad", DatasetImporterForSIAD.class);
                put("spire", DatasetImporterForSPIRE.class);
                put("strona", DatasetImporterForStrona.class);
                put("wrast", DatasetImporterForWrast.class);
                put("web-of-life", DatasetImporterForWebOfLife.class);
                put("saproxylic", DatasetImporterForSaproxylic.class);
                put("globalwebdb", DatasetImporterForGlobalWebDb.class);
                put(MIME_TYPE_DWCA, DatasetImporterForDwCA.class);
                put("dwca", DatasetImporterForDwCA.class);
                put("rss", DatasetImporterForRSS.class);
                put("mangal", DatasetImporterForMangal.class);
                put("batplant", DatasetImporterForBatPlant.class);
                put("batbase", DatasetImporterForBatBase.class);
                put("dbatvir", DatasetImporterForDBatVir.class);
                put("pensoft", DatasetImporterForPensoft.class);
                put("zenodo", DatasetImporterForZenodoMetadata.class);
                put("zover", DatasetImporterForZOVER.class);
            }
        };
        Class<? extends DatasetImporter> anImporter = supportedFormats.get(format);
        if (anImporter == null) {
            throw new StudyImporterException("unsupported format [" + format + "]");
        }
        return anImporter;
    }

    private static boolean isMetaTableImporter(JsonNode desc) {
        boolean isMetaTable = false;

        final JsonNode contextNode =
                desc == null ? null : desc.get("@context");

        if (contextNode != null) {
            if (contextNode.isArray()) {
                for (JsonNode node : contextNode) {
                    isMetaTable = isMetaTable || isMetaTable(node);
                }
            } else {
                isMetaTable = isMetaTable(contextNode);
            }
        }
        return isMetaTable;
    }

    private static boolean isMetaTable(JsonNode node) {
        boolean isMetaTable = false;
        if (node.isTextual()) {
            if ("http://www.w3.org/ns/csvw".equals(node.asText())) {
                isMetaTable = true;
            }
        }
        return isMetaTable;
    }

    @Override
    public DatasetImporter createImporter(Dataset dataset) throws StudyImporterException {
        return createImporter(dataset, this.nodeFactory);
    }
}