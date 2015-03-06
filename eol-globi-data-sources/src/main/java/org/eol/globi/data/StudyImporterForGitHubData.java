package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.GitHubUtil;
import org.eol.globi.util.HttpUtil;
import org.eol.globi.util.InvalidLocationException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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

        List<String> repositories = discoverDataRepositories();

        for (String repository : repositories) {
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
            String baseUrl = GitHubUtil.getBaseUrlLastCommit(repo);
            String descriptor = baseUrl + "/globi.json";
            importUsingDescriptor(repo, baseUrl, getContent(descriptor));
        } catch (IOException e) {
            throw new StudyImporterException("failed to import repo [" + repo + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import repo [" + repo + "]", e);
        } catch (URISyntaxException e) {
            throw new StudyImporterException("failed to import repo [" + repo + "]", e);
        }
    }

    protected void importUsingDescriptor(String repo, String baseUrl, String descriptor) throws IOException, StudyImporterException, NodeFactoryException {
        if (StringUtils.isNotBlank(descriptor)) {
            JsonNode desc = new ObjectMapper().readTree(descriptor);
            String sourceCitation = desc.has("citation") ? desc.get("citation").asText() : baseUrl;
            String format = desc.has("format") ? desc.get("format").asText() : "globi";
            if ("globi".equals(format)) {
                importRepository(repo, sourceCitation, baseUrl);
            } else if ("gomexsi".equals(format)) {
                StudyImporterForGoMexSI importer = new StudyImporterForGoMexSI(parserFactory, nodeFactory);
                importer.setBaseUrl(baseUrl);
                importer.setSourceCitation(sourceCitation);
                if (getLogger() != null) {
                    importer.setLogger(getLogger());
                }
                importer.importStudy();
            } else if ("hechinger".equals(format)) {
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
                if (desc.has("delimiter")) {
                    String delimiter = desc.get("delimiter").asText();
                    if (delimiter.length() > 0) {
                        importer.setDelimiter(StringUtils.trim(delimiter).charAt(0));
                    }
                }

                if (getLogger() != null) {
                    importer.setLogger(getLogger());
                }
                importer.importStudy();
            } else {
                throw new StudyImporterException("unsupported format [" + format + "]");
            }
        }
    }

    private String getContent(String uri) throws IOException {
        return HttpUtil.getContent(uri);
    }

    private void importRepository(String repo, String sourceCitation, String baseUrl) throws IOException, NodeFactoryException, StudyImporterException {
        String dataUrl = baseUrl + "/interactions.tsv";
        LabeledCSVParser parser = parserFactory.createParser(dataUrl, "UTF-8");
        parser.changeDelimiter('\t');
        while (parser.getLine() != null) {
            String referenceCitation = parser.getValueByLabel("referenceCitation");
            String referenceDoi = StringUtils.replace(parser.getValueByLabel("referenceDoi"), " ", "");
            Study study = nodeFactory.getOrCreateStudy2(repo + referenceCitation, sourceCitation + " " + ReferenceUtil.createLastAccessedString(dataUrl), referenceDoi);
            study.setCitationWithTx(referenceCitation);

            String sourceTaxonId = StringUtils.trimToNull(parser.getValueByLabel("sourceTaxonId"));
            String sourceTaxonName = StringUtils.trim(parser.getValueByLabel("sourceTaxonName"));

            String targetTaxonId = StringUtils.trimToNull(parser.getValueByLabel("targetTaxonId"));
            String targetTaxonName = StringUtils.trim(parser.getValueByLabel("targetTaxonName"));

            String interactionTypeId = StringUtils.trim(parser.getValueByLabel("interactionTypeId"));
            if (StringUtils.isNotBlank(targetTaxonName)
                    && StringUtils.isNotBlank(sourceTaxonName)) {
                InteractType type = INTERACT_ID_TO_TYPE.get(interactionTypeId);
                if (type == null) {
                    study.appendLogMessage("unsupported interaction type id [" + interactionTypeId + "]", Level.WARNING);
                } else {
                    Specimen source = nodeFactory.createSpecimen(study, sourceTaxonName, sourceTaxonId);
                    Specimen target = nodeFactory.createSpecimen(study, targetTaxonName, targetTaxonId);


                    LatLng centroid = null;
                    String latitude = StringUtils.trim(parser.getValueByLabel("decimalLatitude"));
                    String longitude = StringUtils.trim(parser.getValueByLabel("decimalLongitude"));
                    if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
                        try {
                            centroid = LocationUtil.parseLatLng(latitude, longitude);
                        } catch (InvalidLocationException e) {
                            getLogger().warn(study, "found invalid location: [" + e.getMessage() + "]");
                        }
                    }
                    if (centroid == null) {
                        String localityId = StringUtils.trim(parser.getValueByLabel("localityId"));
                        if (StringUtils.isNotBlank(localityId)) {
                            centroid = getGeoNamesService().findLatLng(localityId);
                        }
                    }
                    Location location = centroid == null ? null : nodeFactory.getOrCreateLocation(centroid.getLat(), centroid.getLng(), null);
                    source.interactsWith(target, type, location);
                }
            }
        }
    }

}
