package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.data.ParserFactory;
import org.eol.globi.data.ParserFactoryForDataset;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForAkin;
import org.eol.globi.data.StudyImporterForGlobalWebDb;
import org.eol.globi.data.StudyImporterForSaproxylic;
import org.eol.globi.data.StudyImporterForArthopodEasyCapture;
import org.eol.globi.data.StudyImporterForBaremore;
import org.eol.globi.data.StudyImporterForBarnes;
import org.eol.globi.data.StudyImporterForBell;
import org.eol.globi.data.StudyImporterForBioInfo;
import org.eol.globi.data.StudyImporterForBlewett;
import org.eol.globi.data.StudyImporterForBrose;
import org.eol.globi.data.StudyImporterForByrnes;
import org.eol.globi.data.StudyImporterForCoetzer;
import org.eol.globi.data.StudyImporterForCook;
import org.eol.globi.data.StudyImporterForCruaud;
import org.eol.globi.data.StudyImporterForDunne;
import org.eol.globi.data.StudyImporterForFishbase3;
import org.eol.globi.data.StudyImporterForGemina;
import org.eol.globi.data.StudyImporterForGoMexSI2;
import org.eol.globi.data.StudyImporterForGray;
import org.eol.globi.data.StudyImporterForHafner;
import org.eol.globi.data.StudyImporterForHechinger;
import org.eol.globi.data.StudyImporterForHurlbert;
import org.eol.globi.data.StudyImporterForICES;
import org.eol.globi.data.StudyImporterForINaturalist;
import org.eol.globi.data.StudyImporterForJRFerrerParis;
import org.eol.globi.data.StudyImporterForJSONLD;
import org.eol.globi.data.StudyImporterForKelpForest;
import org.eol.globi.data.StudyImporterForLifeWatchGreece;
import org.eol.globi.data.StudyImporterForMetaTable;
import org.eol.globi.data.StudyImporterForPlanque;
import org.eol.globi.data.StudyImporterForRaymond;
import org.eol.globi.data.StudyImporterForRobledo;
import org.eol.globi.data.StudyImporterForRoopnarine;
import org.eol.globi.data.StudyImporterForSIAD;
import org.eol.globi.data.StudyImporterForSPIRE;
import org.eol.globi.data.StudyImporterForSeltmann;
import org.eol.globi.data.StudyImporterForSimons;
import org.eol.globi.data.StudyImporterForStrona;
import org.eol.globi.data.StudyImporterForSzoboszlai;
import org.eol.globi.data.StudyImporterForTSV;
import org.eol.globi.data.StudyImporterForWebOfLife;
import org.eol.globi.data.StudyImporterForWood;
import org.eol.globi.data.StudyImporterForWrast;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class GitHubImporterFactory {

    public StudyImporter createImporter(Dataset dataset, final NodeFactory nodeFactory) throws StudyImporterException {
        Class<? extends StudyImporter> anImporter = findImporterFor(dataset);
        try {
            Constructor<? extends StudyImporter> constructor = anImporter.getConstructor(ParserFactory.class, NodeFactory.class);
            ParserFactoryForDataset parserFactory = new ParserFactoryForDataset(dataset);
            StudyImporter studyImporter = constructor.newInstance(parserFactory,
                    new NodeFactoryWithDatasetContext(nodeFactory, dataset));
            studyImporter.setDataset(dataset);
            return studyImporter;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new StudyImporterException("failed to instantiate importer for [" + dataset.getNamespace() + "]", e);
        }
    }

    private Class<? extends StudyImporter> findImporterFor(Dataset dataset) throws StudyImporterException {
        Class<? extends StudyImporter> anImporter;
        if (dataset.getConfigURI().toString().endsWith(".jsonld")) {
            anImporter = StudyImporterForJSONLD.class;
        } else if (isMetaTableImporter(dataset.getConfig())) {
            anImporter = StudyImporterForMetaTable.class;
        } else {
            anImporter = lookupImporterByFormat(dataset);
        }
        return anImporter;
    }

    static Class<? extends StudyImporter> lookupImporterByFormat(Dataset dataset) throws StudyImporterException {
        final String format = dataset.getFormat();
        if (StringUtils.isBlank(format)) {
            throw new StudyImporterException("provide specify format for [" + dataset.getConfigURI() + "]");
        }

        return importerForFormat(format);
    }

    static Class<? extends StudyImporter> importerForFormat(String format) throws StudyImporterException {
        HashMap<String, Class<? extends StudyImporter>> supportedFormats = new HashMap<String, Class<? extends StudyImporter>>() {
            {
                put("globi", StudyImporterForTSV.class);
                put("gomexsi", StudyImporterForGoMexSI2.class);
                put("hechinger", StudyImporterForHechinger.class);
                put("dunne", StudyImporterForDunne.class);
                put("seltmann", StudyImporterForSeltmann.class);
                put("arthropodEasyCapture", StudyImporterForArthopodEasyCapture.class);
                put("coetzer", StudyImporterForCoetzer.class);
                put("wood", StudyImporterForWood.class);
                put("szoboszlai", StudyImporterForSzoboszlai.class);
                put("planque", StudyImporterForPlanque.class);
                put("gray", StudyImporterForGray.class);
                put("akin", StudyImporterForAkin.class);
                put("baremore", StudyImporterForBaremore.class);
                put("barnes", StudyImporterForBarnes.class);
                put("bell", StudyImporterForBell.class);
                put("bioinfo", StudyImporterForBioInfo.class);
                put("blewett", StudyImporterForBlewett.class);
                put("brose", StudyImporterForBrose.class);
                put("byrnes", StudyImporterForByrnes.class);
                put("cook", StudyImporterForCook.class);
                put("cruaud", StudyImporterForCruaud.class);
                put("ferrer-paris", StudyImporterForJRFerrerParis.class);
                put("fishbase", StudyImporterForFishbase3.class);
                put("gemina", StudyImporterForGemina.class);
                put("hafner", StudyImporterForHafner.class);
                put("hurlbert", StudyImporterForHurlbert.class);
                put("ices", StudyImporterForICES.class);
                put("inaturalist", StudyImporterForINaturalist.class);
                put("kelpforest", StudyImporterForKelpForest.class);
                put("life-watch-greece", StudyImporterForLifeWatchGreece.class);
                put("raymond", StudyImporterForRaymond.class);
                put("robledo", StudyImporterForRobledo.class);
                put("roopnarine", StudyImporterForRoopnarine.class);
                put("simons", StudyImporterForSimons.class);
                put("siad", StudyImporterForSIAD.class);
                put("spire", StudyImporterForSPIRE.class);
                put("strona", StudyImporterForStrona.class);
                put("wrast", StudyImporterForWrast.class);
                put("web-of-life", StudyImporterForWebOfLife.class);
                put("saproxylic", StudyImporterForSaproxylic.class);
                put("globalwebdb", StudyImporterForGlobalWebDb.class);
            }
        };
        Class<? extends StudyImporter> anImporter = supportedFormats.get(format);
        if (anImporter == null) {
            throw new StudyImporterException("unsupported format [" + format + "]");
        }
        return anImporter;
    }

    private boolean isMetaTableImporter(JsonNode desc) {
        boolean isMetaTable = false;
        final JsonNode contextNode = desc.get("@context");
        if (contextNode != null) {
            if (contextNode.isArray()) {
                for (JsonNode node : contextNode) {
                    isMetaTable = isMetaTable || isMetaTable(node);
                }
            } else {
                isMetaTable = isMetaTable || isMetaTable(contextNode);
            }
        }
        return isMetaTable;
    }

    private boolean isMetaTable(JsonNode node) {
        boolean isMetaTable = false;
        if (node.isTextual()) {
            if ("http://www.w3.org/ns/csvw".equals(node.asText())) {
                isMetaTable = true;
            }
        }
        return isMetaTable;
    }

}