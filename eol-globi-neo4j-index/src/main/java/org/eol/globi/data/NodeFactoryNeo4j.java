package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryParser.QueryParser;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.EnvironmentNode;
import org.eol.globi.domain.Interaction;
import org.eol.globi.domain.InteractionNode;
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
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.AuthorIdResolver;
import org.eol.globi.service.EnvoLookupService;
import org.eol.globi.service.ORCIDResolverImpl;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.taxon.TermLookupServiceWithResource;
import org.eol.globi.taxon.UberonLookupService;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;
import org.neo4j.index.lucene.ValueContext;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.eol.globi.domain.LocationUtil.fromLocation;

public class NodeFactoryNeo4j implements NodeFactory {

    private static final Log LOG = LogFactory.getLog(NodeFactoryNeo4j.class);
    public static final TermImpl NO_MATCH_TERM = new TermImpl(PropertyAndValueDictionary.NO_MATCH, PropertyAndValueDictionary.NO_MATCH);

    private GraphDatabaseService graphDb;
    private final Index<Node> studies;
    private final Index<Node> datasets;
    private final Index<Node> seasons;
    private final Index<Node> locations;
    private final Index<Node> environments;

    private TermLookupService termLookupService;
    private TermLookupService envoLookupService;
    private final TermLookupService lifeStageLookupService;
    private final TermLookupService bodyPartLookupService;

    public NodeFactoryNeo4j(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;

        this.termLookupService = new UberonLookupService();
        this.lifeStageLookupService = new TermLookupServiceWithResource("life-stage-mapping.csv");
        this.bodyPartLookupService = new TermLookupServiceWithResource("body-part-mapping.csv");
        this.envoLookupService = new EnvoLookupService();
        this.studies = NodeUtil.forNodes(graphDb, "studies");
        this.datasets = NodeUtil.forNodes(graphDb, "datasets");
        this.seasons = NodeUtil.forNodes(graphDb, "seasons");
        this.locations = NodeUtil.forNodes(graphDb, "locations");
        this.environments = NodeUtil.forNodes(graphDb, "environments");
    }

    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    @Override
    public LocationNode findLocation(Location location) throws NodeFactoryException {
        Node matchingLocation = null;
        if (hasLatLng(location)) {
            matchingLocation = findLocationByLatitude(location);
        }
        if (matchingLocation == null) {
            matchingLocation = findLocationByLocality(location);
        }
        if (matchingLocation == null) {
            matchingLocation = findLocationByLocalityId(location);
        }
        return matchingLocation == null ? null : new LocationNode(matchingLocation);
    }

    private boolean hasLatLng(Location location) {
        return location.getLatitude() != null && location.getLongitude() != null;
    }

    private Node findLocationByLocality(Location location) throws NodeFactoryException {
        return location.getLocality() == null ? null : findLocationBy(location, LocationConstant.LOCALITY, location.getLocality());
    }

    private Node findLocationByLocalityId(Location location) throws NodeFactoryException {
        return location.getLocalityId() == null ? null : findLocationBy(location, LocationConstant.LOCALITY_ID, location.getLocalityId());
    }

    private Node findLocationBy(Location location, String key, String value) {
        Node matchingLocation = null;
        String query = key + ":\"" + QueryParser.escape(value) + "\"";
        try (Transaction transaction = getGraphDb().beginTx()) {
            IndexHits<Node> matchingLocations = locations.query(query);
            for (Node node : matchingLocations) {
                final LocationNode foundLocation = new LocationNode(node);
                if (isSameLocation(location, foundLocation)) {
                    matchingLocation = node;
                    break;
                }
            }
            matchingLocations.close();
            transaction.success();
        }
        return matchingLocation;
    }

    private boolean isSameLocation(Location location, Location foundLocation) {
        return sameLatitude(location, foundLocation)
                && sameLongitude(location, foundLocation)
                && sameAltitude(location, foundLocation)
                && sameFootprintWKT(location, foundLocation)
                && sameLocality(location, foundLocation)
                && sameLocalityId(location, foundLocation);
    }

    private boolean sameLocalityId(Location location, Location foundLocation) {
        return foundLocation.getLocalityId() == null && location.getLocalityId() == null
                || location.getLocalityId() != null && location.getLocalityId().equals(foundLocation.getLocalityId());
    }

    private boolean sameLocality(Location location, Location foundLocation) {
        return foundLocation.getLocality() == null && location.getLocality() == null
                || location.getLocality() != null && location.getLocality().equals(foundLocation.getLocality());
    }

    private boolean sameFootprintWKT(Location location, Location foundLocation) {
        return foundLocation.getFootprintWKT() == null && location.getFootprintWKT() == null
                || location.getFootprintWKT() != null && location.getFootprintWKT().equals(foundLocation.getFootprintWKT());
    }

    private boolean sameAltitude(Location location, Location foundLocation) {
        return foundLocation.getAltitude() == null && location.getAltitude() == null
                || location.getAltitude() != null && location.getAltitude().equals(foundLocation.getAltitude());
    }

    private boolean sameLatitude(Location location, Location foundLocation) {
        return foundLocation.getLatitude() == null && location.getLatitude() == null
                || location.getLatitude() != null && location.getLatitude().equals(foundLocation.getLatitude());
    }

    private boolean sameLongitude(Location location, Location foundLocation) {
        return foundLocation.getLongitude() == null && location.getLongitude() == null
                || location.getLongitude() != null && location.getLongitude().equals(foundLocation.getLongitude());
    }


    private Node findLocationByLatitude(Location location) throws NodeFactoryException {
        Node matchingLocation = null;
        validate(location);
        QueryContext queryOrQueryObject = QueryContext.numericRange(LocationConstant.LATITUDE, location.getLatitude(), location.getLatitude());
        try (Transaction transaction = getGraphDb().beginTx()) {
            IndexHits<Node> matchingLocations = locations.query(queryOrQueryObject);
            for (Node node : matchingLocations) {
                final LocationNode foundLocation = new LocationNode(node);
                if (isSameLocation(location, foundLocation)) {
                    matchingLocation = node;
                    break;
                }
            }
            matchingLocations.close();
            transaction.success();
        }
        return matchingLocation;
    }

    @Override
    public SeasonNode createSeason(String seasonNameLower) {
        SeasonNode season;
        try (Transaction transaction = graphDb.beginTx()) {
            Node node = graphDb.createNode();
            season = new SeasonNode(node, seasonNameLower);
            seasons.add(node, SeasonNode.TITLE, seasonNameLower);
            transaction.success();
        }
        return season;
    }

    private LocationNode createLocation(final Location location) {
        LocationNode locationNode;

        try (Transaction transaction = graphDb.beginTx()) {
            Node node = graphDb.createNode();
            locationNode = new LocationNode(node, fromLocation(location));
            if (location.getLatitude() != null) {
                locations.add(node, LocationConstant.LATITUDE, ValueContext.numeric(location.getLatitude()));
            }
            if (location.getLongitude() != null) {
                locations.add(node, LocationConstant.LONGITUDE, ValueContext.numeric(location.getLongitude()));
            }
            if (location.getAltitude() != null) {
                locations.add(node, LocationConstant.ALTITUDE, ValueContext.numeric(location.getAltitude()));
            }
            if (StringUtils.isNotBlank(location.getFootprintWKT())) {
                locations.add(node, LocationConstant.FOOTPRINT_WKT, location.getFootprintWKT());
            }
            if (StringUtils.isNotBlank(location.getLocality())) {
                locations.add(node, LocationConstant.LOCALITY, location.getLocality());
            }
            if (StringUtils.isNotBlank(location.getLocalityId())) {
                locations.add(node, LocationConstant.LOCALITY_ID, location.getLocalityId());
            }
            transaction.success();
        }
        return locationNode;
    }

    @Override
    public SpecimenNode createSpecimen(Interaction interaction, Taxon taxon) throws NodeFactoryException {
        SpecimenNode specimen = createSpecimen(interaction.getStudy(), taxon);
        ((InteractionNode) interaction).createRelationshipTo(specimen, RelTypes.HAS_PARTICIPANT);
        return specimen;
    }

    @Override
    public SpecimenNode createSpecimen(Study study, Taxon taxon) throws NodeFactoryException {
        return createSpecimen(study, taxon, RelTypes.COLLECTED, RelTypes.SUPPORTS);
    }

    @Override
    public SpecimenNode createSpecimen(Study study, Taxon taxon, RelTypes... types) throws NodeFactoryException {
        if (null == study) {
            throw new NodeFactoryException("specimen needs study, but none is specified");
        }

        if (null == types || types.length == 0) {
            throw new NodeFactoryException("specimen needs at least one study relationship type, but none is specified");
        }

        SpecimenNode specimen = createSpecimen();
        for (RelTypes type : types) {
            ((StudyNode) study).createRelationshipTo(specimen, type);
        }

        specimen.setOriginalTaxonDescription(taxon);
        if (StringUtils.isNotBlank(taxon.getName())) {
            extractTerms(taxon.getName(), specimen);
        }
        return specimen;
    }

    private void extractTerms(String taxonName, Specimen specimen) throws NodeFactoryException {
        String s = StringUtils.replacePattern(taxonName, "[^A-Za-z]", " ");
        String[] nameParts = StringUtils.split(s);
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
        SpecimenNode specimen;
        try (Transaction transaction = graphDb.beginTx()) {
            specimen = new SpecimenNode(graphDb.createNode(), null);
            transaction.success();
        }
        return specimen;
    }


    @Override
    public StudyNode createStudy(Study study) {
        StudyNode studyNode;

        try (Transaction transaction = graphDb.beginTx()) {
            Node node = graphDb.createNode();
            studyNode = new StudyNode(node, study.getTitle());
            studyNode.setCitation(study.getCitation());
            studyNode.setDOI(study.getDOI());
            if (StringUtils.isBlank(study.getExternalId()) && null != study.getDOI()) {
                studyNode.setExternalId(study.getDOI().toURI().toString());
            } else {
                studyNode.setExternalId(study.getExternalId());
            }

            Dataset dataset = getOrCreateDatasetNoTx(study.getOriginatingDataset());
            if (dataset instanceof DatasetNode) {
                studyNode.createRelationshipTo(dataset, RelTypes.IN_DATASET);
            }

            studies.add(node, StudyConstant.TITLE, study.getTitle());
            studies.add(node, StudyConstant.TITLE_IN_NAMESPACE, getTitleInNamespace(study));
            transaction.success();
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
        if (dataset.getDOI() != null) {
            datasetNode.setProperty(StudyConstant.DOI, dataset.getDOI().toString());
        }
        datasetNode.setProperty(DatasetConstant.CITATION, StringUtils.defaultIfBlank(dataset.getCitation(), "no citation"));
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

        StudyNode studyNode;
        try (Transaction transaction = getGraphDb().beginTx()) {
            studyNode = findStudy(study);
            transaction.success();
        }

        return studyNode == null
                ? createStudy(study)
                : studyNode;
    }

    private String namespaceOrNull(Study study) {
        return study != null && study.getOriginatingDataset() != null
                ? study.getOriginatingDataset().getNamespace()
                : null;
    }

    @Deprecated
    @Override
    public StudyNode findStudy(String title) {
        try (Transaction transaction = getGraphDb().beginTx()) {
            StudyNode study = findStudy(new StudyImpl(title));
            transaction.success();
            return study;
        }
    }

    private StudyNode findStudy(Study study) {
        String titleNS = getTitleInNamespace(study);
        final IndexHits<Node> nodes = studies.get(StudyConstant.TITLE_IN_NAMESPACE, titleNS);
        Node foundStudyNode = nodes != null ? nodes.getSingle() : null;
        return foundStudyNode == null ? null : new StudyNode(foundStudyNode);
    }

    private String getTitleInNamespace(Study study) {
        String namespace = namespaceOrNull(study);
        return StringUtils.isBlank(namespace)
                ? study.getTitle()
                : "globi:" + namespace + "/" + study.getTitle();
    }

    @Override
    public SeasonNode findSeason(String seasonName) {
        Node seasonHit;
        try (Transaction transaction = getGraphDb().beginTx()) {
            IndexHits<Node> nodeIndexHits = seasons.get(SeasonNode.TITLE, seasonName);
            seasonHit = nodeIndexHits.getSingle();
            nodeIndexHits.close();
            transaction.success();
        }
        return seasonHit == null ? null : new SeasonNode(seasonHit);
    }

    @Override
    public LocationNode getOrCreateLocation(org.eol.globi.domain.Location location) throws NodeFactoryException {
        LocationNode locationNode = findLocation(location);
        if (null == locationNode) {
            locationNode = createLocation(location);
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
            try (Transaction tx = getGraphDb().beginTx()) {
                Iterable<Relationship> rels = getCollectedRel(specimen);
                for (Relationship rel : rels) {
                    rel.setProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH, date.getTime());
                }
                tx.success();
            }
        }
    }

    private Iterable<Relationship> getCollectedRel(Specimen specimen) throws NodeFactoryException {
        Iterable<Relationship> rel = ((NodeBacked) specimen).getUnderlyingNode().getRelationships(Direction.INCOMING,
                NodeUtil.asNeo4j(RelTypes.COLLECTED),
                NodeUtil.asNeo4j(RelTypes.SUPPORTS),
                NodeUtil.asNeo4j(RelTypes.REFUTES)
        );
        if (!rel.iterator().hasNext()) {
            throw new NodeFactoryException("specimen not associated with study");
        }
        return rel;
    }

    @Override
    public Date getUnixEpochProperty(Specimen specimen) throws NodeFactoryException {
        Date date = null;
        try (Transaction tx = getGraphDb().beginTx()) {
            Iterable<Relationship> rels = getCollectedRel(specimen);
            if (rels.iterator().hasNext()) {
                Relationship rel = rels.iterator().next();
                if (rel.hasProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH)) {
                    Long unixEpoch = (Long) rel.getProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH);
                    date = new Date(unixEpoch);
                }
            }
            tx.success();
        }

        return date;
    }

    @Override
    public List<Environment> getOrCreateEnvironments(Location location, String externalId, String name) throws NodeFactoryException {
        List<Term> terms;
        try {
            terms = envoLookupService.lookupTermByName(name);
            if (terms.size() == 0) {
                terms.add(new TermImpl(externalId, name));
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
                try (Transaction transaction = graphDb.beginTx()) {
                    EnvironmentNode environmentNode = new EnvironmentNode(graphDb.createNode(), term.getId(), term.getName());
                    environments.add(environmentNode.getUnderlyingNode(), PropertyAndValueDictionary.NAME, term.getName());
                    transaction.success();
                    environment = environmentNode;
                }
            }
            try (Transaction transaction = graphDb.beginTx()) {
                location.addEnvironment(environment);
                normalizedEnvironments.add(environment);
                transaction.success();
            }
        }
        return normalizedEnvironments;
    }

    protected EnvironmentNode findEnvironment(String name) {
        EnvironmentNode firstMatchingEnvironment = null;
        String query = "name:\"" + name + "\"";
        try (Transaction transaction = getGraphDb().beginTx()) {
            IndexHits<Node> matches = environments.query(query);
            if (matches.hasNext()) {
                firstMatchingEnvironment = new EnvironmentNode(matches.next());
            }
            matches.close();
            transaction.success();
        }
        return firstMatchingEnvironment;
    }

    @Override
    public Term getOrCreateBodyPart(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

    @Override
    public Term getOrCreatePhysiologicalState(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

    @Override
    public Term getOrCreateLifeStage(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

    private Term matchTerm(String externalId, String name) throws NodeFactoryException {
        try {
            List<Term> terms = getTermLookupService().lookupTermByName(name);
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
        try (Transaction transaction = graphDb.beginTx()) {
            Dataset datasetCreated = getOrCreateDatasetNoTx(originatingDataset);
            transaction.success();
            return datasetCreated;
        }
    }

    @Override
    public Interaction createInteraction(Study study) throws NodeFactoryException {
        InteractionNode interactionNode;
        try (Transaction transaction = graphDb.beginTx()) {
            Node node = graphDb.createNode();
            StudyNode studyNode = getOrCreateStudy(study);
            interactionNode = new InteractionNode(node);
            interactionNode.createRelationshipTo(studyNode, RelTypes.DERIVED_FROM);
            Dataset dataset = getOrCreateDatasetNoTx(study.getOriginatingDataset());
            if (dataset instanceof DatasetNode) {
                interactionNode.createRelationshipTo(dataset, RelTypes.ACCESSED_AT);
            }
            transaction.success();
        }
        return interactionNode;
    }

    private Dataset getOrCreateDatasetNoTx(Dataset originatingDataset) {
        Dataset datasetCreated = null;
        if (originatingDataset != null && StringUtils.isNotBlank(originatingDataset.getNamespace())) {
            IndexHits<Node> datasetHits = datasets.get(DatasetConstant.NAMESPACE, originatingDataset.getNamespace());
            Node datasetNode = datasetHits.hasNext() ? datasetHits.next() : createDatasetNode(originatingDataset);
            datasetCreated = new DatasetNode(datasetNode);
        }
        return datasetCreated;
    }
}

