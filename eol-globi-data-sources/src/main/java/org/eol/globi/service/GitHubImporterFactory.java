package org.eol.globi.service;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
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
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class GitHubImporterFactory {

    public StudyImporter createImporter(String repo, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws IOException, URISyntaxException, StudyImporterException {
        try {
            Dataset dataset = new DatasetFinderGitHubRemote().datasetFor(repo);
            return createImporter(configDataset(dataset), parserFactory, nodeFactory);
        } catch (DatasetFinderException e) {
            throw new StudyImporterException("failed to locate archive url for [" + repo + "]", e);
        }
    }

    private Dataset configDataset(Dataset dataset) throws StudyImporterException {
        Dataset datasetConfigured = configureDatasetLD(dataset);
        if (datasetConfigured == null) {
            try {
                datasetConfigured = configureDataset(dataset);
            } catch (IOException e) {
                throw new StudyImporterException("failed to import [" + dataset.getNamespace() + "]", e);
            }
        }
        return configureArchiveURI(datasetConfigured);
    }

    private StudyImporter createImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws IOException, StudyImporterException {
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

    private Dataset configureDataset(Dataset dataset) throws IOException {
        Dataset datasetConfigured1 = null;
        final URI configURI = dataset.getResourceURI("/globi.json");
        if (ResourceUtil.resourceExists(configURI)) {
            datasetConfigured1 = configureDataset(dataset, configURI);
        }
        return datasetConfigured1;
    }

    private Dataset configureDatasetLD(Dataset dataset) {
        Dataset datasetConfigured = null;
        final URI jsonldResourceUrl = dataset.getResourceURI("/globi-dataset.jsonld");
        if (ResourceUtil.resourceExists(jsonldResourceUrl)) {
            datasetConfigured = new Dataset(dataset.getNamespace(), dataset.getArchiveURI());
            datasetConfigured.setConfigURI(jsonldResourceUrl);
        }
        return datasetConfigured;
    }

    public StudyImporter createImporter(String repo, String basedir, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws IOException, StudyImporterException, NodeFactoryException {
        return createImporter(new Dataset(repo, URI.create(basedir)), parserFactory, nodeFactory);
    }

    private Dataset configureDataset(Dataset dataset, URI configURI) throws IOException {
        Dataset datasetConfigured = null;
        String descriptor = ResourceUtil.getContent(configURI);
        if (StringUtils.isNotBlank(descriptor)) {
            JsonNode desc = new ObjectMapper().readTree(descriptor);
            datasetConfigured = new Dataset(dataset.getNamespace(), dataset.getArchiveURI());
            datasetConfigured.setConfig(desc);
            datasetConfigured.setConfigURI(configURI);
        }
        return datasetConfigured;
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

    private Dataset configureArchiveURI(Dataset dataset) {
        JsonNode desc = dataset.getConfig();
        String archiveURL = desc.has("archiveURL") ? desc.get("archiveURL").asText() : "";
        URI archiveURI = dataset.getResourceURI(archiveURL);
        Dataset dataset1 = new Dataset(dataset.getNamespace(), archiveURI);
        dataset1.setConfig(dataset.getConfig());
        dataset1.setConfigURI(dataset.getConfigURI());
        return dataset1;
    }

}