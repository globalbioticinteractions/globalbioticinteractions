package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.EnvironmentNode;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SeasonNode;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderException;
import org.eol.globi.service.AuthorIdResolver;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetConstant;
import org.eol.globi.service.EnvoLookupService;
import org.eol.globi.service.ORCIDResolverImpl;
import org.eol.globi.service.QueryUtil;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.taxon.TermLookupServiceWithResource;
import org.eol.globi.taxon.UberonLookupService;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.QueryContext;
import org.neo4j.index.lucene.ValueContext;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.eol.globi.domain.LocationUtil.fromLocation;

public class NodeFactoryNeo4j implements NodeFactory {

    private static final Log LOG = LogFactory.getLog(NodeFactoryNeo4j.class);
    public static final org.eol.globi.domain.Term NO_MATCH_TERM = new org.eol.globi.domain.Term(PropertyAndValueDictionary.NO_MATCH, PropertyAndValueDictionary.NO_MATCH);

    private GraphDatabaseService graphDb;
    private final Index<Node> studies;
    private final Index<Node> datasets;
    private final Index<Node> seasons;
    private final Index<Node> locations;
    private final Index<Node> environments;
    private final Index<Node> ecoregions;
    private final Index<Node> ecoregionSuggestions;
    private final Index<Node> ecoregionPaths;

    private TermLookupService termLookupService;
    private TermLookupService envoLookupService;
    private final TermLookupService lifeStageLookupService;
    private final TermLookupService bodyPartLookupService;

    private DOIResolver doiResolver;
    private EcoregionFinder ecoregionFinder;

    public NodeFactoryNeo4j(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;

        this.termLookupService = new UberonLookupService();
        this.lifeStageLookupService = new TermLookupServiceWithResource("life-stage-mapping.csv");
        this.bodyPartLookupService = new TermLookupServiceWithResource("body-part-mapping.csv");
        this.envoLookupService = new EnvoLookupService();
        this.studies = graphDb.index().forNodes("studies");
        this.datasets = graphDb.index().forNodes("datasets");
        this.seasons = graphDb.index().forNodes("seasons");
        this.locations = graphDb.index().forNodes("locations");
        this.environments = graphDb.index().forNodes("environments");

        this.ecoregions = graphDb.index().forNodes("ecoregions");
        this.ecoregionPaths = graphDb.index().forNodes("ecoregionPaths", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
        this.ecoregionSuggestions = graphDb.index().forNodes("ecoregionSuggestions");
    }

    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    @Override
    public LocationNode findLocation(Location location) {
        QueryContext queryOrQueryObject = QueryContext.numericRange(LocationConstant.LATITUDE, location.getLatitude(), location.getLatitude());
        IndexHits<Node> matchingLocations = locations.query(queryOrQueryObject);
        Node matchingLocation = null;
        for (Node node : matchingLocations) {
            final LocationNode foundLocation = new LocationNode(node);

            boolean altitudeMatches = foundLocation.getAltitude() == null && location.getAltitude() == null
                || location.getAltitude() != null && location.getAltitude().equals(foundLocation.getAltitude());

            boolean footprintWKTMatches = foundLocation.getFootprintWKT() == null && location.getFootprintWKT() == null
                || location.getFootprintWKT() != null && location.getFootprintWKT().equals(foundLocation.getFootprintWKT());

            boolean localityMatches = foundLocation.getLocality() == null && location.getLocality() == null
                || location.getLocality() != null && location.getLocality().equals(foundLocation.getLocality());

            if (location.getLongitude().equals(foundLocation.getLongitude())
                && altitudeMatches
                && footprintWKTMatches
                && localityMatches) {
                matchingLocation = node;
                break;
            }

        }
        matchingLocations.close();
        return matchingLocation == null ? null : new LocationNode(matchingLocation);
    }

    @Override
    public SeasonNode createSeason(String seasonNameLower) {
        Transaction transaction = graphDb.beginTx();
        SeasonNode season;
        try {
            Node node = graphDb.createNode();
            season = new SeasonNode(node, seasonNameLower);
            seasons.add(node, SeasonNode.TITLE, seasonNameLower);
            transaction.success();
        } finally {
            transaction.finish();
        }
        return season;
    }

    private LocationNode createLocation(final Location location) {
        Transaction transaction = graphDb.beginTx();
        LocationNode locationNode;
        try {
            Node node = graphDb.createNode();
            locationNode = new LocationNode(node, fromLocation(location));
            locations.add(node, LocationConstant.LATITUDE, ValueContext.numeric(location.getLatitude()));
            locations.add(node, LocationConstant.LONGITUDE, ValueContext.numeric(location.getLongitude()));
            if (location.getAltitude() != null) {
                locations.add(node, LocationConstant.ALTITUDE, ValueContext.numeric(location.getAltitude()));
            }
            if (StringUtils.isNotBlank(location.getFootprintWKT())) {
                locations.add(node, LocationConstant.FOOTPRINT_WKT, location.getFootprintWKT());
            }
            if (StringUtils.isNotBlank(location.getLocality())) {
                locations.add(node, LocationConstant.LOCALITY, location.getLocality());
            }
            transaction.success();
        } finally {
            transaction.finish();
        }
        return locationNode;
    }

    @Override
    public SpecimenNode createSpecimen(Study study, Taxon taxon) throws NodeFactoryException {
        if (null == study) {
            throw new NodeFactoryException("specimen needs study, but none is specified");
        }

        SpecimenNode specimen = createSpecimen();
        ((StudyNode) study).createRelationshipTo(specimen, RelTypes.COLLECTED);

        specimen.setOriginalTaxonDescription(taxon);
        if (StringUtils.isNotBlank(taxon.getName())) {
            extractTerms(taxon.getName(), specimen);
        }
        return specimen;
    }

    private void extractTerms(String taxonName, Specimen specimen) throws NodeFactoryException {
        String[] nameParts = StringUtils.split(taxonName);
        for (String part : nameParts) {
            extractLifeStage(specimen, part);
            extractBodyPart(specimen, part);
        }
    }

    private void extractLifeStage(Specimen specimen, String part) throws NodeFactoryException {
        try {
            List<Term> terms = lifeStageLookupService.lookupTermByName(part);
            for (Term term : terms) {
                if (!StringUtils.equals(term.getId(), PropertyAndValueDictionary.NO_MATCH)) {
                    specimen.setLifeStage(terms.get(0));
                    break;
                }
            }
        } catch (TermLookupServiceException e) {
            throw new NodeFactoryException("failed to map term [" + part + "]", e);
        }
    }

    private void extractBodyPart(Specimen specimen, String part) throws NodeFactoryException {
        try {
            List<Term> terms = bodyPartLookupService.lookupTermByName(part);
            for (Term term : terms) {
                if (!StringUtils.equals(term.getId(), PropertyAndValueDictionary.NO_MATCH)) {
                    specimen.setBodyPart(terms.get(0));
                    break;
                }
            }
        } catch (TermLookupServiceException e) {
            throw new NodeFactoryException("failed to map term [" + part + "]", e);
        }
    }


    private SpecimenNode createSpecimen() {
        Transaction transaction = graphDb.beginTx();
        SpecimenNode specimen;
        try {
            specimen = new SpecimenNode(graphDb.createNode(), null);
            transaction.success();
        } finally {
            transaction.finish();
        }
        return specimen;
    }


    @Override
    public StudyNode createStudy(Study study) {
        Transaction transaction = graphDb.beginTx();
        StudyNode studyNode;
        try {
            Node node = graphDb.createNode();
            studyNode = new StudyNode(node, study.getTitle());
            studyNode.setSource(study.getSource());
            studyNode.setCitation(study.getCitation());
            studyNode.setDOI(study.getDOI());
            if (StringUtils.isBlank(study.getExternalId()) && StringUtils.isNotBlank(study.getDOI())) {
                studyNode.setExternalId(ExternalIdUtil.urlForExternalId(study.getDOI()));
            } else {
                studyNode.setExternalId(study.getExternalId());
            }
            studyNode.setSourceId(study.getSourceId());

            Dataset dataset = getOrCreateDatasetNoTx(study.getOriginatingDataset());
            if (dataset != null && dataset instanceof DatasetNode) {
                studyNode.getUnderlyingNode().createRelationshipTo((
                        (DatasetNode) dataset).getUnderlyingNode(),
                    NodeUtil.asNeo4j(RelTypes.IN_DATASET));
            }

            studies.add(node, StudyConstant.TITLE, study.getTitle());
            transaction.success();
        } finally {
            transaction.finish();
        }

        return studyNode;
    }

    private Node createDatasetNode(Dataset dataset) {
        Node datasetNode = graphDb.createNode();
        datasetNode.setProperty(DatasetConstant.NAMESPACE, dataset.getNamespace());
        URI archiveURI = dataset.getArchiveURI();
        if (archiveURI != null) {
            datasetNode.setProperty(DatasetConstant.ARCHIVE_URI, archiveURI.toString());
        }
        URI configURI = dataset.getConfigURI();
        if (configURI != null) {
            datasetNode.setProperty(DatasetConstant.CONFIG_URI, configURI.toString());
        }
        JsonNode config = dataset.getConfig();
        if (config != null) {
            try {
                datasetNode.setProperty(DatasetConstant.CONFIG, new ObjectMapper().writeValueAsString(config));
            } catch (IOException e) {
                LOG.warn("failed to serialize dataset config");
            }
        }
        datasetNode.setProperty(StudyConstant.FORMAT, dataset.getFormat());
        datasetNode.setProperty(StudyConstant.DOI, dataset.getDOI());
        datasetNode.setProperty(DatasetConstant.CITATION, dataset.getOrDefault(DatasetConstant.CITATION, "no citation"));
        datasetNode.setProperty(DatasetConstant.SHOULD_RESOLVE_REFERENCES, dataset.getOrDefault(DatasetConstant.SHOULD_RESOLVE_REFERENCES, "true"));
        datasetNode.setProperty(DatasetConstant.LAST_SEEN_AT, dataset.getOrDefault(DatasetConstant.LAST_SEEN_AT, Long.toString(System.currentTimeMillis())));
        datasets.add(datasetNode, DatasetConstant.NAMESPACE, dataset.getNamespace());
        return datasetNode;
    }

    @Override
    public StudyNode getOrCreateStudy(Study study) throws NodeFactoryException {
        if (StringUtils.isBlank(study.getTitle())) {
            throw new NodeFactoryException("null or empty study title");
        }
        if (StringUtils.isBlank(study.getSource())) {
            throw new NodeFactoryException("null or empty study source");
        }
        StudyNode studyNode = findStudy(study.getTitle());
        return null == studyNode ? createStudy(study) : studyNode;
    }

    @Override
    public StudyNode findStudy(String title) {
        final IndexHits<Node> nodes = title == null ? null : studies.get(StudyConstant.TITLE, title);
        Node foundStudyNode = nodes != null ? nodes.getSingle() : null;
        return foundStudyNode == null ? null : new StudyNode(foundStudyNode);
    }

    @Override
    public SeasonNode findSeason(String seasonName) {
        IndexHits<Node> nodeIndexHits = seasons.get(SeasonNode.TITLE, seasonName);
        Node seasonHit = nodeIndexHits.getSingle();
        nodeIndexHits.close();
        return seasonHit == null ? null : new SeasonNode(seasonHit);
    }

    @Override
    public LocationNode getOrCreateLocation(org.eol.globi.domain.Location location) throws NodeFactoryException {
        LocationNode locationNode = null;
        final Double latitude = location.getLatitude();
        final Double longitude = location.getLongitude();
        if (location.getLatitude() != null && location.getLongitude() != null) {
            validate(location);
            locationNode = findLocation(location);
            if (null == locationNode) {
                locationNode = createLocation(location);
                try {
                    enrichLocationWithEcoRegions(locationNode);
                } catch (NodeFactoryException e) {
                    LOG.error("failed to create eco region for location (" + latitude + ", " + longitude + ")", e);
                }
            }
        }
        return locationNode;
    }

    private void validate(Location location) throws NodeFactoryException {
        if (!LocationUtil.isValidLatitude(location.getLatitude())) {
            throw new NodeFactoryException("found invalid latitude [" + location.getLatitude() + "]");
        }
        if (!LocationUtil.isValidLongitude(location.getLongitude())) {
            throw new NodeFactoryException("found invalid longitude [" + location.getLongitude() + "]");
        }
    }

    @Override
    public void setUnixEpochProperty(Specimen specimen, Date date) throws NodeFactoryException {
        if (specimen != null && date != null) {
            Relationship rel = getCollectedRel(specimen);
            Transaction tx = rel.getGraphDatabase().beginTx();
            try {
                rel.setProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH, date.getTime());
                tx.success();
            } finally {
                tx.finish();
            }
        }
    }

    protected Relationship getCollectedRel(Specimen specimen) throws NodeFactoryException {
        Relationship rel = ((NodeBacked) specimen).getUnderlyingNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.COLLECTED), Direction.INCOMING);
        if (null == rel) {
            throw new NodeFactoryException("specimen not associated with study");
        }
        return rel;
    }

    @Override
    public Date getUnixEpochProperty(Specimen specimen) throws NodeFactoryException {
        Date date = null;
        Relationship rel = getCollectedRel(specimen);
        if (rel.hasProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH)) {
            Long unixEpoch = (Long) rel.getProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH);
            date = new Date(unixEpoch);
        }

        return date;
    }

    @Override
    public List<Environment> getOrCreateEnvironments(Location location, String externalId, String name) throws NodeFactoryException {
        List<org.eol.globi.domain.Term> terms;
        try {
            terms = envoLookupService.lookupTermByName(name);
            if (terms.size() == 0) {
                terms.add(new org.eol.globi.domain.Term(externalId, name));
            }
        } catch (TermLookupServiceException e) {
            throw new NodeFactoryException("failed to lookup environment [" + name + "]", e);
        }

        return addEnvironmentToLocation(location, terms);
    }

    @Override
    public List<Environment> addEnvironmentToLocation(Location location, List<Term> terms) {
        List<Environment> normalizedEnvironments = new ArrayList<Environment>();
        for (Term term : terms) {
            Environment environment = findEnvironment(term.getName());
            if (environment == null) {
                Transaction transaction = graphDb.beginTx();
                try {
                    EnvironmentNode environmentNode = new EnvironmentNode(graphDb.createNode(), term.getId(), term.getName());
                    environments.add(environmentNode.getUnderlyingNode(), PropertyAndValueDictionary.NAME, term.getName());
                    transaction.success();
                    environment = environmentNode;
                } finally {
                    transaction.finish();
                }
            }
            location.addEnvironment(environment);
            normalizedEnvironments.add(environment);
        }
        return normalizedEnvironments;
    }

    private List<Ecoregion> getEcoRegions(Node locationNode) {
        Iterable<Relationship> relationships = locationNode.getRelationships(NodeUtil.asNeo4j(RelTypes.IN_ECOREGION), Direction.OUTGOING);
        List<Ecoregion> ecoregions = null;
        for (Relationship relationship : relationships) {
            Node ecoregionNode = relationship.getEndNode();
            Ecoregion ecoregion = new Ecoregion();
            ecoregion.setGeometry(NodeUtil.getPropertyStringValueOrDefault(ecoregionNode, "geometry", null));
            ecoregion.setName(NodeUtil.getPropertyStringValueOrDefault(ecoregionNode, PropertyAndValueDictionary.NAME, null));
            ecoregion.setId(NodeUtil.getPropertyStringValueOrDefault(ecoregionNode, PropertyAndValueDictionary.EXTERNAL_ID, null));
            ecoregion.setPath(NodeUtil.getPropertyStringValueOrDefault(ecoregionNode, "path", null));
            if (ecoregions == null) {
                ecoregions = new ArrayList<Ecoregion>();
            }
            ecoregions.add(ecoregion);
        }
        return ecoregions;
    }

    public List<Ecoregion> enrichLocationWithEcoRegions(Location location) throws NodeFactoryException {
        LocationNode locationNode = (LocationNode) location;
        List<Ecoregion> associatedEcoregions = getEcoRegions(locationNode.getUnderlyingNode());
        return associatedEcoregions == null ? associateWithEcoRegions(locationNode) : associatedEcoregions;
    }

    private List<Ecoregion> associateWithEcoRegions(Location location) throws NodeFactoryException {
        List<Ecoregion> associatedEcoregions = new ArrayList<Ecoregion>();
        try {
            EcoregionFinder finder = getEcoregionFinder();
            if (finder != null) {
                Collection<Ecoregion> ecoregions = finder.findEcoregion(location.getLatitude(), location.getLongitude());
                for (Ecoregion ecoregion : ecoregions) {
                    associateLocationWithEcoRegion(location, ecoregion);
                    associatedEcoregions.add(ecoregion);
                }
            }
        } catch (EcoregionFinderException e) {
            throw new NodeFactoryException("problem finding eco region for location (lat,lng):(" + location.getLatitude() + "," + location.getLongitude() + ")");
        }
        return associatedEcoregions;
    }

    private void associateLocationWithEcoRegion(Location location, Ecoregion ecoregion) {
        Node ecoregionNode = findEcoRegion(ecoregion);
        Transaction tx = graphDb.beginTx();
        try {
            if (ecoregionNode == null) {
                ecoregionNode = addAndIndexEcoRegion(ecoregion);
            }
            ((NodeBacked) location).getUnderlyingNode().createRelationshipTo(ecoregionNode, NodeUtil.asNeo4j(RelTypes.IN_ECOREGION));
            tx.success();
        } finally {
            tx.finish();
        }
    }

    private Node findEcoRegion(Ecoregion ecoregion) {
        String query = "name:\"" + ecoregion.getName() + "\"";
        IndexHits<Node> hits = this.ecoregions.query(query);
        try {
            return hits.hasNext() ? hits.next() : null;
        } finally {
            hits.close();
        }
    }

    private Node addAndIndexEcoRegion(Ecoregion ecoregion) {
        Node node = graphDb.createNode();
        node.setProperty(PropertyAndValueDictionary.NAME, ecoregion.getName());
        node.setProperty(PropertyAndValueDictionary.EXTERNAL_ID, ecoregion.getId());
        node.setProperty("path", ecoregion.getPath());
        node.setProperty("geometry", ecoregion.getGeometry());
        ecoregions.add(node, PropertyAndValueDictionary.NAME, ecoregion.getName());
        ecoregionPaths.add(node, "path", ecoregion.getPath());
        ecoregionSuggestions.add(node, PropertyAndValueDictionary.NAME, StringUtils.lowerCase(ecoregion.getName()));
        if (StringUtils.isNotBlank(ecoregion.getPath())) {
            for (String part : ecoregion.getPath().split(CharsetConstant.SEPARATOR)) {
                ecoregionSuggestions.add(node, PropertyAndValueDictionary.NAME, StringUtils.lowerCase(part));
            }
        }
        return node;
    }

    protected EnvironmentNode findEnvironment(String name) {
        String query = "name:\"" + name + "\"";
        IndexHits<Node> matches = environments.query(query);
        EnvironmentNode firstMatchingEnvironment = null;
        if (matches.hasNext()) {
            firstMatchingEnvironment = new EnvironmentNode(matches.next());
        }
        matches.close();
        return firstMatchingEnvironment;
    }

    @Override
    public org.eol.globi.domain.Term getOrCreateBodyPart(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

    @Override
    public org.eol.globi.domain.Term getOrCreatePhysiologicalState(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

    @Override
    public org.eol.globi.domain.Term getOrCreateLifeStage(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

    private org.eol.globi.domain.Term matchTerm(String externalId, String name) throws NodeFactoryException {
        try {
            List<org.eol.globi.domain.Term> terms = getTermLookupService().lookupTermByName(name);
            return terms.size() == 0 ? NO_MATCH_TERM : terms.get(0);
        } catch (TermLookupServiceException e) {
            throw new NodeFactoryException("failed to lookup term [" + externalId + "]:[" + name + "]");
        }
    }

    @Override
    public TermLookupService getTermLookupService() {
        return termLookupService;
    }

    public void setEnvoLookupService(TermLookupService envoLookupService) {
        this.envoLookupService = envoLookupService;
    }

    public void setTermLookupService(TermLookupService termLookupService) {
        this.termLookupService = termLookupService;
    }

    public void setDoiResolver(DOIResolver doiResolver) {
        this.doiResolver = doiResolver;
    }

    public void setEcoregionFinder(EcoregionFinder ecoregionFinder) {
        this.ecoregionFinder = ecoregionFinder;
    }

    @Override
    public EcoregionFinder getEcoregionFinder() {
        return ecoregionFinder;
    }

    public IndexHits<Node> findCloseMatchesForEcoregion(String ecoregionName) {
        return QueryUtil.query(ecoregionName, PropertyAndValueDictionary.NAME, ecoregions);
    }

    public IndexHits<Node> findCloseMatchesForEcoregionPath(String ecoregionPath) {
        return QueryUtil.query(ecoregionPath, PropertyAndValueDictionary.PATH, ecoregionPaths);
    }

    public IndexHits<Node> suggestEcoregionByName(String wholeOrPartialEcoregionNameOrPath) {
        return ecoregionSuggestions.query("name:\"" + wholeOrPartialEcoregionNameOrPath + "\"");
    }

    @Override
    public AuthorIdResolver getAuthorResolver() {
        return new ORCIDResolverImpl();
    }

    @Override
    public Term getOrCreateBasisOfRecord(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

    @Override
    public Dataset getOrCreateDataset(Dataset originatingDataset) {
        Transaction transaction = graphDb.beginTx();
        try {
            Dataset datasetCreated = getOrCreateDatasetNoTx(originatingDataset);
            transaction.success();
            return datasetCreated;
        } finally {
            transaction.finish();
        }
    }

    public Dataset getOrCreateDatasetNoTx(Dataset originatingDataset) {
        Dataset datasetCreated = null;
        if (originatingDataset != null && StringUtils.isNotBlank(originatingDataset.getNamespace())) {
            IndexHits<Node> datasetHits = datasets.get(DatasetConstant.NAMESPACE, originatingDataset.getNamespace());
            Node datasetNode = datasetHits.hasNext() ? datasetHits.next() : createDatasetNode(originatingDataset);
            datasetCreated = new DatasetNode(datasetNode);
        }
        return datasetCreated;
    }
}

