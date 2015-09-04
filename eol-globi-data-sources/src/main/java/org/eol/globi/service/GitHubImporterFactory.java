package org.eol.globi.service;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpHead;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.ParserFactory;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForGoMexSI;
import org.eol.globi.data.StudyImporterForHechinger;
import org.eol.globi.data.StudyImporterForJSONLD;
import org.eol.globi.data.StudyImporterForSeltmann;
import org.eol.globi.data.StudyImporterForSzoboszlai;
import org.eol.globi.data.StudyImporterForTSV;
import org.eol.globi.data.StudyImporterForWood;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.LatLng;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URISyntaxException;

public class GitHubImporterFactory {

    public StudyImporter createImporter(String repo, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws IOException, URISyntaxException, StudyImporterException, NodeFactoryException {
        StudyImporter importer = null;
        final String baseUrl = GitHubUtil.getBaseUrlLastCommit(repo);
        final String resourceUrl = baseUrl + "/globi.json";
        if (resourceExists(resourceUrl)) {
            importer = createImporter(repo, baseUrl, getContent(resourceUrl), parserFactory, nodeFactory);
        } else {
            final String jsonldResourceUrl = baseUrl + "/globi-dataset.jsonld";
            if (resourceExists(jsonldResourceUrl)) {
                importer = new StudyImporterForJSONLD(parserFactory, nodeFactory) {
                    {
                        setResourceUrl(jsonldResourceUrl);
                    }
                };

            }
        }
        return importer;
    }

    private boolean resourceExists(String descriptor) {
        boolean exists = false;
        try {
            HttpResponse resp = HttpUtil.getHttpClient().execute(new HttpHead(descriptor));
            exists = resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (IOException e) {
            // ignore
        }
        return exists;
    }

    protected StudyImporter createImporter(final String repo, final String baseUrl, final String descriptor, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws IOException, StudyImporterException, NodeFactoryException {
        StudyImporter importer = null;
        if (StringUtils.isNotBlank(descriptor)) {
            JsonNode desc = new ObjectMapper().readTree(descriptor);
            final String citation = desc.has("citation") ? desc.get("citation").asText() : baseUrl;
            final String sourceDOI = parseDOI(desc);
            String format = desc.has("format") ? desc.get("format").asText() : "globi";
            if ("globi".equals(format)) {
                importer = createTSVImporter(repo, baseUrl, parserFactory, nodeFactory, citation);
            } else if ("gomexsi".equals(format)) {
                importer = createGoMexSIImporter(baseUrl, citation, parserFactory, nodeFactory);
            } else if ("hechinger".equals(format)) {
                importer = createHechingerImporter(repo, desc, citation, sourceDOI, parserFactory, nodeFactory);
            } else if ("seltmann".equals(format)) {
                importer = createSeltmannImporter(repo, desc, parserFactory, nodeFactory);
            } else if ("wood".equals(format)) {
                importer = createWoodImporter(desc, parserFactory, nodeFactory);
            } else if ("szoboszlai".equals(format)) {
                importer = createSzoboszlaiImporter(desc, parserFactory, nodeFactory);
            } else {
                throw new StudyImporterException("unsupported format [" + format + "]");
            }
        }

        return importer;
    }

    private String parseDOI(JsonNode desc) {
        return desc.has("doi") ? desc.get("doi").asText() : "";
    }

    private StudyImporter createTSVImporter(final String repo, final String baseUrl, final ParserFactory parserFactory, final NodeFactory nodeFactory, final String citation) {
        StudyImporter importer;
        importer = new StudyImporterForTSV(parserFactory, nodeFactory) {
            {
                setBaseUrl(baseUrl);
                setSourceCitation(citation);
                setRepositoryName(repo);
            }
        };
        return importer;
    }

    private StudyImporterForSeltmann createSeltmannImporter(String repo, JsonNode desc, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws StudyImporterException {
        StudyImporterForSeltmann studyImporterForSeltmann = new StudyImporterForSeltmann(parserFactory, nodeFactory);
        final String archiveURL = parseArchiveURL(desc);
        if (StringUtils.isBlank(archiveURL)) {
            throw new StudyImporterException("failed to import [" + repo + "]: no [archiveURL] specified");
        } else {
            studyImporterForSeltmann.setArchiveURL(archiveURL);
        }
        return studyImporterForSeltmann;
    }

    private StudyImporter createWoodImporter(JsonNode desc, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws StudyImporterException {
        StudyImporterForWood studyImporter = new StudyImporterForWood(parserFactory, nodeFactory);
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

    private StudyImporter createSzoboszlaiImporter(JsonNode desc, final ParserFactory parserFactory, final NodeFactory nodeFactory) throws StudyImporterException {
        StudyImporterForSzoboszlai studyImporter = new StudyImporterForSzoboszlai(parserFactory, nodeFactory);
        if (desc.has("doi")) {
            studyImporter.setSourceDOI(desc.get("doi").asText());
        }
        if (desc.has("citation")) {
            studyImporter.setSourceCitation(desc.get("citation").asText());
        }
        if (desc.has("resources")) {
            JsonNode resources = desc.get("resources");
            if (resources.has("links")) {
                studyImporter.setLinkArchiveURL(resources.get("links").asText());
            }
            if (resources.has("shapes")) {
                studyImporter.setShapeArchiveURL(resources.get("shapes").asText());
            }
        }
        return studyImporter;
    }

    private String parseArchiveURL(JsonNode desc) {
        return desc.has("archiveURL") ? desc.get("archiveURL").asText() : "";
    }

    private StudyImporterForGoMexSI createGoMexSIImporter(String baseUrl, String sourceCitation, final ParserFactory parserFactory, final NodeFactory nodeFactory) {
        StudyImporterForGoMexSI importer = new StudyImporterForGoMexSI(parserFactory, nodeFactory);
        importer.setBaseUrl(baseUrl);
        importer.setSourceCitation(sourceCitation);
        return importer;
    }

    private StudyImporterForHechinger createHechingerImporter(String repo, JsonNode desc, String sourceCitation, String sourceDOI, final ParserFactory parserFactory, final NodeFactory nodeFactory) {
        StudyImporterForHechinger importer = new StudyImporterForHechinger(parserFactory, nodeFactory);
        JsonNode resources = desc.get("resources");
        if (resources.has("links")) {
            importer.setLinkResource(resources.get("links").asText());
        }
        if (resources.has("nodes")) {
            importer.setNodeResource(resources.get("nodes").asText());
        }
        LatLng loc = parseLocation(desc);
        if (loc != null) {
            importer.setLocation(loc);
        }

        importer.setNamespace(repo);
        importer.setSourceCitation(sourceCitation);
        importer.setSourceDOI(sourceDOI);
        if (desc.has("delimiter")) {
            String delimiter = desc.get("delimiter").asText();
            if (delimiter.length() > 0) {
                importer.setDelimiter(StringUtils.trim(delimiter).charAt(0));
            }
        }
        return importer;
    }

    private LatLng parseLocation(JsonNode desc) {
        LatLng loc = null;
        JsonNode location = desc.get("location");
        JsonNode latitude = location.get("latitude");
        JsonNode longitude = location.get("longitude");
        if (latitude != null && latitude.isDouble() && longitude != null && longitude.isDouble()) {
            loc = new LatLng(latitude.asDouble(), longitude.asDouble());

        }
        return loc;
    }

    private Term parseLocality(JsonNode desc) {
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

    private String getContent(String uri) throws IOException {
        try {
            return HttpUtil.getContent(uri);
        } catch (IOException ex) {
            throw new IOException("failed to find [" + uri + "]", ex);
        }
    }
}