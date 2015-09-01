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
import org.eol.globi.data.StudyImporterForTSV;
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
                final String sourceDOI = desc.has("doi") ? desc.get("doi").asText() : "";
                String format = desc.has("format") ? desc.get("format").asText() : "globi";
                if ("globi".equals(format)) {
                    importer = createTSVImporter(repo, baseUrl, parserFactory, nodeFactory, citation);
                } else if ("gomexsi".equals(format)) {
                    importer = createGoMexSIImporter(baseUrl, citation, parserFactory, nodeFactory);
                } else if ("hechinger".equals(format)) {
                    importer = createHechingerImporter(repo, desc, citation, sourceDOI, parserFactory, nodeFactory);
                } else if ("seltmann".equals(format)) {
                    importer = createSeltmannImporter(repo, desc, parserFactory, nodeFactory);
                } else {
                    throw new StudyImporterException("unsupported format [" + format + "]");
                }
            }

            return importer;
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
            final String archiveURL = desc.has("archiveURL") ? desc.get("archiveURL").asText() : "";
            if (StringUtils.isBlank(archiveURL)) {
                throw new StudyImporterException("failed to import [" + repo + "]: no [archiveURL] specified");
            } else {
                studyImporterForSeltmann.setArchiveURL(archiveURL);
            }
            return studyImporterForSeltmann;
        }

        private StudyImporterForGoMexSI createGoMexSIImporter(String baseUrl, String sourceCitation,final ParserFactory parserFactory, final NodeFactory nodeFactory) {
            StudyImporterForGoMexSI importer = new StudyImporterForGoMexSI(parserFactory, nodeFactory);
            importer.setBaseUrl(baseUrl);
            importer.setSourceCitation(sourceCitation);
            return importer;
        }

        private StudyImporterForHechinger createHechingerImporter(String repo, JsonNode desc, String sourceCitation, String sourceDOI,final ParserFactory parserFactory, final NodeFactory nodeFactory) {
            StudyImporterForHechinger importer = new StudyImporterForHechinger(parserFactory, nodeFactory);
            JsonNode resources = desc.get("resources");
            if (resources.has("links")) {
                importer.setLinkResource(resources.get("links").asText());
            }
            if (resources.has("nodes")) {
                importer.setNodeResource(resources.get("nodes").asText());
            }
            JsonNode location = desc.get("location");
            JsonNode latitude = location.get("latitude");
            JsonNode longitude = location.get("longitude");
            if (latitude != null && latitude.isDouble() && longitude != null && longitude.isDouble()) {
                importer.setLocation(new LatLng(latitude.asDouble(), longitude.asDouble()));
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

        private String getContent(String uri) throws IOException {
            try {
                return HttpUtil.getContent(uri);
            } catch (IOException ex) {
                throw new IOException("failed to find [" + uri + "]", ex);
            }
        }
    }