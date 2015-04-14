package org.eol.globi.data;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpHead;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Study;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.GitHubUtil;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForGitHubData extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForGitHubData.class);
    private static final Map<String, InteractType> INTERACT_ID_TO_TYPE = new HashMap<String, InteractType>() {{
        put("RO:0002434", InteractType.INTERACTS_WITH);
        put("RO:0002439", InteractType.PREYS_UPON);
        put("RO:0002440", InteractType.SYMBIONT_OF);
        put("RO:0002444", InteractType.PARASITE_OF);
        put("RO:0002453", InteractType.HOST_OF);
        put("RO:0002455", InteractType.POLLINATES);
        put("RO:0002556", InteractType.PATHOGEN_OF);
        put("RO:0002456", InteractType.POLLINATED_BY);
        put("RO:0002458", InteractType.PREYED_UPON_BY);
        put("RO:0002470", InteractType.ATE);
    }};

    public StudyImporterForGitHubData(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        for (String repository : discoverDataRepositories()) {
            try {
                LOG.info("importing github repo [" + repository + "]...");
                importData(repository);
                LOG.info("importing github repo [" + repository + "] done.");
            } catch (StudyImporterException ex) {
                LOG.error("failed to import data from repo [" + repository + "]", ex);
            }
        }
        return null;
    }

    protected List<String> discoverDataRepositories() throws StudyImporterException {
        List<String> repositories;
        try {
            repositories = GitHubUtil.find();
        } catch (IOException e) {
            throw new StudyImporterException("failed to discover github data repositories", e);
        } catch (URISyntaxException e) {
            throw new StudyImporterException("failed to discover github data repositories", e);
        }
        return repositories;
    }

    protected void importData(String repo) throws StudyImporterException {
        try {
            final String baseUrl = GitHubUtil.getBaseUrlLastCommit(repo);
            final String resourceUrl = baseUrl + "/globi.json";
            if (resourceExists(resourceUrl)) {
                StudyImporter importer = importUsingDescriptor(repo, baseUrl, getContent(resourceUrl));
                importer.importStudy();
            } else {
                final String jsonldResourceUrl = baseUrl + "/globi-dataset.jsonld";
                if (resourceExists(jsonldResourceUrl)) {
                    StudyImporter importer = new StudyImporterForJSONLD(parserFactory, nodeFactory) {
                        {
                            setResourceUrl(jsonldResourceUrl);
                        }
                    };
                    importer.importStudy();
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to import repo [" + repo + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import repo [" + repo + "]", e);
        } catch (URISyntaxException e) {
            throw new StudyImporterException("failed to import repo [" + repo + "]", e);
        }
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

    protected StudyImporter importUsingDescriptor(final String repo, final String baseUrl, final String descriptor) throws IOException, StudyImporterException, NodeFactoryException {
        StudyImporter importer = null;
        if (StringUtils.isNotBlank(descriptor)) {
            JsonNode desc = new ObjectMapper().readTree(descriptor);
            final String sourceCitation = desc.has("citation") ? desc.get("citation").asText() : baseUrl;
            final String sourceDOI = desc.has("doi") ? desc.get("doi").asText() : "";
            String format = desc.has("format") ? desc.get("format").asText() : "globi";
            if ("globi".equals(format)) {
                importer = new StudyImporterForTSV(parserFactory, nodeFactory) {
                    {
                        setBaseUrl(baseUrl);
                        setSourceCitation(sourceCitation);
                        setRepositoryName(repo);
                    }
                };
            } else if ("gomexsi".equals(format)) {
                importer = createGoMexSIImporter(baseUrl, sourceCitation);
            } else if ("hechinger".equals(format)) {
                importer = createHechingerImporter(repo, desc, sourceCitation, sourceDOI);
            } else if ("seltmann".equals(format)) {
                importer = getStudyImporterForSeltmann(repo, desc);
            } else {
                throw new StudyImporterException("unsupported format [" + format + "]");
            }
        }

        return importer;
    }

    private StudyImporterForSeltmann getStudyImporterForSeltmann(String repo, JsonNode desc) throws StudyImporterException {
        StudyImporterForSeltmann studyImporterForSeltmann = new StudyImporterForSeltmann(parserFactory, nodeFactory);
        final String archiveURL = desc.has("archiveURL") ? desc.get("archiveURL").asText() : "";
        if (StringUtils.isBlank(archiveURL)) {
            throw new StudyImporterException("failed to import [" + repo + "]: no [archiveURL] specified");
        } else {
            studyImporterForSeltmann.setArchiveURL(archiveURL);
        }
        return studyImporterForSeltmann;
    }

    private StudyImporterForGoMexSI createGoMexSIImporter(String baseUrl, String sourceCitation) {
        StudyImporterForGoMexSI importer = new StudyImporterForGoMexSI(parserFactory, nodeFactory);
        importer.setBaseUrl(baseUrl);
        importer.setSourceCitation(sourceCitation);
        if (getLogger() != null) {
            importer.setLogger(getLogger());
        }
        return importer;
    }

    private StudyImporterForHechinger createHechingerImporter(String repo, JsonNode desc, String sourceCitation, String sourceDOI) {
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

        if (getLogger() != null) {
            importer.setLogger(getLogger());
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
