package org.eol.globi.service;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpHead;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.BaseStudyImporter;
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
import org.eol.globi.domain.Term;
import org.eol.globi.geo.LatLng;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class GitHubImporterFactory {

    public StudyImporter createImporter(String repo, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws IOException, URISyntaxException, StudyImporterException {
        try {
            Dataset dataset = new DatasetFinderGitHubRemote().datasetFor(repo);
            return createImporter(dataset, parserFactory, nodeFactory);
        } catch (DatasetFinderException e) {
            throw new StudyImporterException("failed to locate archive url for [" + repo + "]", e);
        }
    }

    public StudyImporter createImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws IOException, StudyImporterException {
        StudyImporter importer = null;
        final URI resourceUrl = dataset.getResourceURI("/globi.json");
        if (resourceExists(resourceUrl)) {
            importer = createImporter(dataset, getContent(resourceUrl), parserFactory, nodeFactory);
        } else {
            final URI jsonldResourceUrl = dataset.getResourceURI("/globi-dataset.jsonld");
            if (resourceExists(jsonldResourceUrl)) {
                importer = new StudyImporterForJSONLD(parserFactory, nodeFactory) {
                    {
                        setResourceURI(jsonldResourceUrl);
                    }
                };

            }
        }
        return importer;
    }

    private boolean resourceExists(URI descriptor) {
        boolean exists = false;
        try {
            HttpResponse resp = HttpUtil.getHttpClient().execute(new HttpHead(descriptor));
            exists = resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (IOException e) {
            // ignore
        }
        return exists;
    }

    public StudyImporter createImporter(String repo, String basedir, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws IOException, StudyImporterException, NodeFactoryException {
        return createImporter(new Dataset(repo, URI.create(basedir)), parserFactory, nodeFactory);
    }

    protected StudyImporter createImporter(Dataset dataset, final String descriptor, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws IOException, StudyImporterException, NodeFactoryException {
        StudyImporter importer = null;
        if (StringUtils.isNotBlank(descriptor)) {
            JsonNode desc = new ObjectMapper().readTree(descriptor);
            Dataset dataset1 = new Dataset(dataset.getNamespace(), dataset.getArchiveURI());
            dataset1.setConfig(desc);

            if (isMetaTableImporter(desc)) {
                final StudyImporterForMetaTable studyImporterForMetaTable = new StudyImporterForMetaTable(parserFactory, nodeFactory);
                studyImporterForMetaTable.setDataset(dataset1);
                importer = studyImporterForMetaTable;
            } else {
                importer = createImporterForFormat(dataset1, parserFactory, nodeFactory);
            }
        }

        return importer;
    }

    private StudyImporter createImporterForFormat(Dataset dataset, ParserFactory parserFactory, NodeFactory nodeFactory) throws StudyImporterException {
        StudyImporter importer;

        JsonNode desc = dataset.getConfig();
        final String format = dataset.getFormat();
        if ("globi".equals(format)) {
            importer = createTSVImporter(dataset, parserFactory, nodeFactory);
        } else if ("gomexsi".equals(format)) {
            importer = createGoMexSIImporter(dataset, parserFactory, nodeFactory);
        } else if ("hechinger".equals(format)) {
            importer = createHechingerImporter(dataset, parserFactory, nodeFactory);
        } else if ("dunne".equals(format)) {
            importer = createDunneImporter(dataset, parserFactory, nodeFactory);
        } else if ("seltmann".equals(format)) {
            importer = createSeltmannImporter(dataset, parserFactory, nodeFactory);
        } else if ("arthropodEasyCapture".equals(format)) {
            importer = createArthropodEasyCaptureImporter(dataset, parserFactory, nodeFactory);
        } else if ("coetzer".equals(format)) {
            importer = createCoetzerImporter(dataset, parserFactory, nodeFactory);
        } else if ("wood".equals(format)) {
            importer = createWoodImporter(dataset, parserFactory, nodeFactory);
        } else if ("szoboszlai".equals(format)) {
            importer = createSzoboszlaiImporter(dataset, parserFactory, nodeFactory);
        } else if ("planque".equals(format)) {
            importer = createPlanqueImporter(dataset, parserFactory, nodeFactory);
        } else if ("gray".equals(format)) {
            importer = createGrayImporter(dataset, parserFactory, nodeFactory);
        } else {
            throw new StudyImporterException("unsupported format [" + format + "]");
        }
        return importer;
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

    private StudyImporter createTSVImporter(final Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) {
        return new StudyImporterForTSV(parserFactory, nodeFactory) {
            {
                setDataset(dataset);
            }
        };
    }

    private StudyImporterForSeltmann createSeltmannImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws StudyImporterException {
        StudyImporterForSeltmann studyImporterForSeltmann = new StudyImporterForSeltmann(parserFactory, nodeFactory);
        setDatasetWithArchiveURL(dataset, studyImporterForSeltmann);
        return studyImporterForSeltmann;
    }

    private StudyImporter createArthropodEasyCaptureImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws StudyImporterException {
        StudyImporterForArthopodEasyCapture importer = new StudyImporterForArthopodEasyCapture(parserFactory, nodeFactory);
        importer.setDataset(dataset);
        return importer;
    }

    private StudyImporterForCoetzer createCoetzerImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws StudyImporterException {
        StudyImporterForCoetzer studyImporter = new StudyImporterForCoetzer(parserFactory, nodeFactory);
        setDatasetWithArchiveURL(dataset, studyImporter);
        return studyImporter;
    }

    private void setDatasetWithArchiveURL(Dataset dataset, BaseStudyImporter studyImporter) throws StudyImporterException {
        JsonNode desc = dataset.getConfig();
        String archiveURL = desc.has("archiveURL") ? desc.get("archiveURL").asText() : "";
        if (StringUtils.isBlank(archiveURL)) {
            throw new StudyImporterException("dataset with name [" + dataset.getNamespace() + "] has no archiveURI");
        }
        Dataset dataset1 = new Dataset(dataset.getNamespace(), URI.create(archiveURL));
        dataset1.setConfig(dataset.getConfig());
        studyImporter.setDataset(dataset1);
    }

    private StudyImporter createWoodImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws StudyImporterException {
        StudyImporterForWood studyImporter = new StudyImporterForWood(parserFactory, nodeFactory);
        JsonNode desc = dataset.getConfig();
        if (desc.has("doi")) {
            studyImporter.setSourceDOI(desc.get("doi").asText());
        }
        if (desc.has("citation")) {
            studyImporter.setSourceCitation(desc.get("citation").asText());
        }
        studyImporter.setLocation(parseLocation(desc));
        studyImporter.setLocality(parseLocality(desc));
        if (desc.has("resources")) {
            JsonNode resources = desc.get("resources");
            if (resources.has("links")) {
                studyImporter.setLinksURL(resources.get("links").asText());
            }
        }
        return studyImporter;
    }

    private StudyImporter createGrayImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws StudyImporterException {
        StudyImporterForGray studyImporter = new StudyImporterForGray(parserFactory, nodeFactory);

        JsonNode desc = dataset.getConfig();
        if (desc.has("doi")) {
            studyImporter.setSourceDOI(desc.get("doi").asText());
        }
        if (desc.has("citation")) {
            studyImporter.setSourceCitation(desc.get("citation").asText());
        }
        if (desc.has("resources")) {
            JsonNode resources = desc.get("resources");
            if (resources.has("links")) {
                studyImporter.setLinksURL(resources.get("links").asText());
            }
        }
        return studyImporter;
    }

    private StudyImporter createPlanqueImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws StudyImporterException {
        StudyImporterForPlanque studyImporter = new StudyImporterForPlanque(parserFactory, nodeFactory);

        JsonNode desc = dataset.getConfig();
        if (desc.has("citation")) {
            studyImporter.setSourceCitation(desc.get("citation").asText());
        }
        if (desc.has("resources")) {
            JsonNode resources = desc.get("resources");
            if (resources.has("links")) {
                studyImporter.setLinks(resources.get("links").asText());
            }
            if (resources.has("references")) {
                studyImporter.setReferences(resources.get("references").asText());
            }
            if (resources.has("referencesForLinks")) {
                studyImporter.setReferencesForLinks(resources.get("referencesForLinks").asText());
            }
        }
        return studyImporter;
    }

    private StudyImporter createSzoboszlaiImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws StudyImporterException {
        StudyImporterForSzoboszlai studyImporter = new StudyImporterForSzoboszlai(parserFactory, nodeFactory);
        studyImporter.setDataset(dataset);
        return studyImporter;
    }

    private StudyImporterForGoMexSI2 createGoMexSIImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) {
        return new StudyImporterForGoMexSI2(parserFactory, nodeFactory) {{
            setBaseUrl(dataset.getArchiveURI().toString());
            setSourceCitation(dataset.getCitation());
        }};
    }

    private StudyImporterForHechinger createHechingerImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) {
        StudyImporterForHechinger importer = new StudyImporterForHechinger(parserFactory, nodeFactory);
        importer.setDataset(dataset);
        return importer;
    }

    private StudyImporterForDunne createDunneImporter(Dataset dataset, final ParserFactory parserFactory, final NodeFactory nodeFactory) {
        StudyImporterForDunne importer = new StudyImporterForDunne(parserFactory, nodeFactory);
        importer.setDataset(dataset);
        return importer;
    }

    public static LatLng parseLocation(JsonNode desc) {
        LatLng loc = null;
        JsonNode location = desc.get("location");
        JsonNode latitude = location.get("latitude");
        JsonNode longitude = location.get("longitude");
        if (latitude != null && latitude.isNumber() && longitude != null && longitude.isNumber()) {
            loc = new LatLng(latitude.asDouble(), longitude.asDouble());

        }
        return loc;
    }

    public static Term parseLocality(JsonNode desc) {
        Term locality = null;
        JsonNode location = desc.get("location");
        if (location != null) {
            JsonNode locale = location.get("locality");
            if (locale != null) {
                JsonNode id = locale.get("id");
                JsonNode name = locale.get("name");
                if (id != null && name != null) {
                    locality = new Term(id.asText(), name.asText());
                }
            }
        }
        return locality;
    }

    private String getContent(URI uri) throws IOException {
        try {
            return HttpUtil.getContent(uri);
        } catch (IOException ex) {
            throw new IOException("failed to findNamespaces [" + uri + "]", ex);
        }
    }
}