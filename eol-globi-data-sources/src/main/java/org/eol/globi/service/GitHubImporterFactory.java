package org.eol.globi.service;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.ParserFactory;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForArthopodEasyCapture;
import org.eol.globi.data.StudyImporterForCoetzer;
import org.eol.globi.data.StudyImporterForDunne;
import org.eol.globi.data.StudyImporterForGoMexSI2;
import org.eol.globi.data.StudyImporterForGray;
import org.eol.globi.data.StudyImporterForHechinger;
import org.eol.globi.data.StudyImporterForJSONLD;
import org.eol.globi.data.StudyImporterForMetaTable;
import org.eol.globi.data.StudyImporterForPlanque;
import org.eol.globi.data.StudyImporterForSeltmann;
import org.eol.globi.data.StudyImporterForSzoboszlai;
import org.eol.globi.data.StudyImporterForTSV;
import org.eol.globi.data.StudyImporterForWood;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.HashMap;

public class GitHubImporterFactory {

    public StudyImporter createImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws StudyImporterException {
        Class<? extends StudyImporter> anImporter = findImporterFor(dataset);
        try {
            Constructor<? extends StudyImporter> constructor = anImporter.getConstructor(ParserFactory.class, NodeFactory.class);
            StudyImporter studyImporter = constructor.newInstance(parserFactory, nodeFactory);
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

    private static Class<? extends StudyImporter> lookupImporterByFormat(Dataset dataset) throws StudyImporterException {
        final String format = dataset.getFormat();
        if (StringUtils.isBlank(format)) {
            throw new StudyImporterException("provide specify format for [" + dataset.getConfigURI() + "]");
        }

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
            }
        };
        Class<? extends StudyImporter> anImporter = supportedFormats.get(dataset.getFormat());
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