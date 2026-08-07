package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.EnvironmentNode;
import org.eol.globi.domain.Interaction;
import org.eol.globi.domain.InteractionNode;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.LocationImpl;
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
import org.eol.globi.service.EnvoLookupService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.taxon.TermLookupServiceWithResource;
import org.eol.globi.taxon.UberonLookupService;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceServiceLocal;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.doi.DOI;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eol.globi.domain.LocationUtil.fromLocation;

public abstract class NodeFactoryNeo4j extends NodeFactoryAbstract {

    private static final Logger LOG = LoggerFactory.getLogger(NodeFactoryNeo4j.class);
    public static final TermImpl NO_MATCH_TERM = new TermImpl(PropertyAndValueDictionary.NO_MATCH, PropertyAndValueDictionary.NO_MATCH);

    private final GraphDatabaseService graphDb;

    private TermLookupService termLookupService;
    private TermLookupService envoLookupService;
    private final TermLookupService lifeStageLookupService;
    private final TermLookupService bodyPartLookupService;
    private AtomicBoolean shouldStartNextBatch = new AtomicBoolean(false);

    public NodeFactoryNeo4j(GraphDatabaseService graphDb) {
        this.graphDb = new GraphDatabaseServiceProxy(graphDb, shouldStartNextBatch);

        InputStreamFactory inputStreamFactory = new InputStreamFactoryNoop();
        this.termLookupService = new UberonLookupService(
                new ResourceServiceLocal(inputStreamFactory)
        );

        this.lifeStageLookupService
                = new TermLookupServiceWithResource(
                "life-stage-mapping.csv",
                new ResourceServiceLocal(inputStreamFactory)
        );

        this.bodyPartLookupService
                = new TermLookupServiceWithResource(
                "body-part-mapping.csv",
                new ResourceServiceLocal(inputStreamFactory)
        );

        this.envoLookupService = new EnvoLookupService(
                new ResourceServiceLocal(inputStreamFactory)
        );

    }

    public void shouldStartNextBatch() {
        shouldStartNextBatch.set(true);
    }

    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    @Override
    public SeasonNode createSeason(String seasonNameLower) throws NodeFactoryException {
        Node node = createSeasonNode();
        SeasonNode season = new SeasonNode(node, seasonNameLower);
        return season;
    }

    public static void initSchema(GraphDatabaseService graphDb) {
        NodeFactoryNeo4j.initConstraints(graphDb);
        NodeFactoryNeo4j.initIndexes(graphDb);
    }

    private static void initIndexes(GraphDatabaseService graphDb) {
        NodeFactoryNeo4j.createIndexIfNeeded(
                graphDb,
                NodeLabel.Location,
                LocationConstant.LATITUDE
        );
        NodeFactoryNeo4j.createIndexIfNeeded(
                graphDb,
                NodeLabel.Reference,
                StudyConstant.TITLE_IN_NAMESPACE
        );
    }

    private static void initConstraints(GraphDatabaseService graphDb) {
        NodeFactoryNeo4j.createConstraintIfNeeded(
                graphDb,
                NodeLabel.Dataset,
                DatasetConstant.NAMESPACE
        );
        NodeFactoryNeo4j.createConstraintIfNeeded(
                graphDb,
                NodeLabel.Reference,
                StudyConstant.TITLE_IN_NAMESPACE
        );
        NodeFactoryNeo4j.createConstraintIfNeeded(
                graphDb,
                NodeLabel.ExternalId,
                PropertyAndValueDictionary.EXTERNAL_ID
        );
    }

    protected Node createSeasonNode() {
        try (Transaction transaction = getGraphDb().beginTx()) {
            Node node = transaction.createNode(NodeLabel.Season);
            transaction.commit();
            return node;
        }
    }

    private LocationNode createLocationNode(Transaction transaction, final Location location) throws NodeFactoryException {
        Node node = createLocationNode(transaction);
        LocationNode locationNode = new LocationNode(node, fromLocation(location));
        indexLocation(location, node);
        return locationNode;

    }

    public StudyNode findStudyNode(Transaction tx, Study study) {
        Node node = tx.findNode(
                NodeLabel.Reference,
                StudyConstant.TITLE_IN_NAMESPACE,
                getIdInNamespace(study)
        );
        return node == null
                ? null
                : new StudyNode(node);

    }


    public StudyNode getOrCreateStudyNode(Transaction tx, Study study) throws NodeFactoryException {
        StudyNode studyNode;
        studyNode = findStudyNode(tx, study);
        return studyNode == null
                ? createStudyNode(tx, study)
                : studyNode;
    }


    protected void indexLocation(Location location, Node node) throws NodeFactoryException {
        // should already be taken care of by constraints: do nothing
    }


    protected Node createLocationNode(Transaction transaction1) {
        return transaction1.createNode(NodeLabel.Location);
    }

    protected Node findFirstMatchingLocationIfAvailable(Location location, ResourceIterator<Node> matchingLocations) {
        Node matching = null;
        while (matching == null && matchingLocations.hasNext()) {
            Node node = matchingLocations.next();
            final LocationNode foundLocation = new LocationNode(node);
            if (org.eol.globi.domain.LocationUtil.isSameLocation(location, foundLocation)) {
                matching = node;
            }
        }
        return matching;
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

    public SpecimenNode createSpecimenNode(Transaction tx, Study study, Taxon taxon) throws NodeFactoryException {
        return createSpecimenNode(tx, study, taxon, RelTypes.COLLECTED, RelTypes.SUPPORTS);
    }

    @Override
    public SpecimenNode createSpecimen(Study study, Taxon taxon, RelTypes... types) throws NodeFactoryException {
        if (null == study) {
            throw new NodeFactoryException("specimen needs study, but none is specified");
        }

        if (null == types || types.length == 0) {
            throw new NodeFactoryException("specimen needs at least one study relationship type, but none is specified");
        }

        try (Transaction tx = graphDb.beginTx()) {
            SpecimenNode specimen = createSpecimenNode(tx, study, taxon, types);
            tx.commit();
            return specimen;
        }
    }

    protected SpecimenNode createSpecimenNode(Transaction tx, Study study, Taxon taxon, RelTypes... types) throws NodeFactoryException {
        StudyNode orCreateStudy = getOrCreateStudyNode(tx, study);
        SpecimenNode specimen = createSpecimenNode(tx);
        for (RelTypes type : types) {
            orCreateStudy.createRelationshipTo(specimen, type);
        }

        specimen.setOriginalTaxonNodeDescription(taxon, tx);
        if (StringUtils.isNotBlank(taxon.getName())) {
            extractTerms(taxon.getName(), specimen);
        }
        return specimen;
    }

    private void extractTerms(String taxonName, Specimen specimen) throws NodeFactoryException {
        String s = RegExUtils.replacePattern(taxonName, "[^A-Za-z]", " ");
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


    private SpecimenNode createSpecimenNode(Transaction transaction) {
        return new SpecimenNode(transaction.createNode());
    }

    @Override
    public Study createStudy(Study study) throws NodeFactoryException {
        try (Transaction transaction = getGraphDb().beginTx()) {
            StudyNode studyNode = createStudyNode(transaction, study);
            indexStudyNode(studyNode);
            Study copyOf = copyOf(studyNode);
            transaction.commit();
            return copyOf;
        }
    }

    protected static StudyImpl copyOf(StudyNode studyNode) {
        StudyImpl copyOf = new StudyImpl(studyNode.getTitle(), studyNode.getDOI(), studyNode.getCitation());
        copyOf.setExternalId(studyNode.getExternalId());
        Dataset originatingDataset = studyNode.getOriginatingDataset();
        if (originatingDataset != null) {
            copyOf.setOriginatingDataset(copyOf(studyNode.getOriginatingDataset()));
        }
        return copyOf;
    }

    protected StudyNode createStudyNode(Transaction transaction, Study study) throws NodeFactoryException {
        Node node = transaction.createNode();
        StudyNode studyNode = new StudyNode(node, study.getTitle());
        studyNode.setCitation(study.getCitation());
        studyNode.setDOI(study.getDOI());
        if (study.getDOI() != null) {
            String doiString = study.getDOI().toString();
            createExternalIdRelationIfExists(node, doiString, RelTypes.HAS_DOI);
        }

        String externalId = getExternalIdOrDOI(study);
        studyNode.setExternalId(externalId);

        createExternalIdRelationIfExists(node, externalId, RelTypes.HAS_EXTERNAL_ID);

        DatasetNode dataset = getOrCreateDatasetNode(transaction, study.getOriginatingDataset());
        if (dataset != null) {
            studyNode.createRelationshipTo(dataset, RelTypes.IN_DATASET);
        }

        studyNode.getUnderlyingNode().setProperty(StudyConstant.TITLE_IN_NAMESPACE, getIdInNamespace(study));
        return studyNode;
    }

    private void createExternalIdRelationIfExists(Node node, String externalId, RelTypes hasExternalId) throws NodeFactoryException {
        Node externalIdNode = getOrCreateExternalIdNoTx(externalId);
        if (node != null && externalIdNode != null) {
            node.createRelationshipTo(externalIdNode, NodeUtil.asNeo4j(hasExternalId));
        }
    }

    private String getExternalIdOrDOI(Study study) {
        String externalId = study.getExternalId();
        if (StringUtils.isBlank(externalId) && null != study.getDOI()) {
            externalId = study.getDOI().toURI().toString();
        }
        return externalId;
    }

    Node createDatasetNode(Transaction tx, Dataset dataset) throws NodeFactoryException {
        Node datasetNode = createDatasetNode(tx);
        datasetNode.setProperty(DatasetConstant.NAMESPACE, dataset.getNamespace());
        URI archiveURI = dataset.getArchiveURI();
        if (archiveURI != null) {
            String archiveURIString = archiveURI.toString();
            datasetNode.setProperty(DatasetConstant.ARCHIVE_URI, archiveURIString);
            createExternalIdRelationIfExists(datasetNode, archiveURIString, RelTypes.HAS_EXTERNAL_ID);
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
        DOI doi = dataset.getDOI();
        if (doi != null) {
            String doiString = doi.toString();
            datasetNode.setProperty(StudyConstant.DOI, doiString);
            createExternalIdRelationIfExists(datasetNode, doiString, RelTypes.HAS_DOI);
        }
        datasetNode.setProperty(DatasetConstant.CITATION, StringUtils.defaultIfBlank(dataset.getCitation(), "no citation"));
        datasetNode.setProperty(DatasetConstant.SHOULD_RESOLVE_REFERENCES, dataset.getOrDefault(DatasetConstant.SHOULD_RESOLVE_REFERENCES, "true"));
        datasetNode.setProperty(DatasetConstant.LAST_SEEN_AT, dataset.getOrDefault(DatasetConstant.LAST_SEEN_AT, Long.toString(System.currentTimeMillis())));
        indexDatasetNode(dataset, datasetNode);
        return datasetNode;
    }

    Node createExternalId(Transaction transaction, String externalId) throws NodeFactoryException {
        Node externalIdNode = createExternalIdNode(transaction);
        externalIdNode.setProperty(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
        indexExternalIdNode(externalId, externalIdNode);
        return externalIdNode;
    }

    @Override
    public Study getOrCreateStudy(Study study) throws NodeFactoryException {
        if (StringUtils.isBlank(study.getTitle())) {
            throw new NodeFactoryException("null or empty study title");
        }

        Study studyFoundOrCreated = findStudy(study);

        if (studyFoundOrCreated == null) {
            studyFoundOrCreated = createStudy(study);
        }

        return studyFoundOrCreated;
    }

    private String namespaceOrNull(Study study) {
        return study != null && study.getOriginatingDataset() != null
                ? study.getOriginatingDataset().getNamespace()
                : null;
    }

    @Override
    public Study findStudy(Study study) {
        try (Transaction tx = getGraphDb().beginTx()) {
            StudyNode studyNode = findStudyNode(tx, study);
            Study copyOfStudy = copyOf(studyNode);
            tx.commit();
            return copyOfStudy;
        }
    }


    String getIdInNamespace(Study study) {
        String namespace = namespaceOrNull(study);
        String externalIdOrDOI = getExternalIdOrDOI(study);
        String id = StringUtils.isBlank(externalIdOrDOI)
                ? study.getTitle()
                : externalIdOrDOI;

        return StringUtils.isBlank(namespace)
                ? id
                : "globi:" + namespace + "/" + id;
    }


    @Override
    public Location getOrCreateLocation(org.eol.globi.domain.Location location) throws NodeFactoryException {
        try (Transaction transaction = getGraphDb().beginTx()) {
            LocationNode location1 = getOrCreateLocationNode(transaction, location);
            Location copyOf = getCopyOf(location1);
            transaction.commit();
            return copyOf;

        }
    }

    public static Location getCopyOf(LocationNode location1) {
        LocationImpl location = new LocationImpl(location1.getLatitude(), location1.getLongitude(), location1.getAltitude(), location1.getFootprintWKT());
        location.setLocality(location1.getLocality());
        location.setLocalityId(location1.getLocalityId());
        return location;
    }

    protected LocationNode getOrCreateLocationNode(Transaction transaction, Location location) throws NodeFactoryException {
        LocationNode location1 = findLocationNode(transaction, location);
        if (location1 == null) {
            location1 = createLocationNode(transaction, location);
        }
        return location1;
    }

    protected abstract LocationNode findLocationNode(Transaction tx, Location location) throws NodeFactoryException;


    @Override
    public void setUnixEpochProperty(Specimen specimen, Date date) throws NodeFactoryException {
        if (specimen != null && date != null) {
            Iterable<Relationship> rels = getCollectedRel(specimen);
            for (Relationship rel : rels) {
                rel.setProperty(SpecimenConstant.EVENT_DATE, DateUtil.printDate(date));
            }
        }
    }

    public static Iterable<Relationship> getCollectedRel(Specimen specimen) throws NodeFactoryException {
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
        DateTime date = null;
        Iterable<Relationship> rels = getCollectedRel(specimen);
        if (rels.iterator().hasNext()) {
            Relationship rel = rels.iterator().next();
            if (rel.hasProperty(SpecimenConstant.EVENT_DATE)) {
                String unixEpoch = (String) rel.getProperty(SpecimenConstant.EVENT_DATE);
                date = DateUtil.parseDateUTC(unixEpoch);
            }
        }
        return date == null ? null : date.toDate();
    }

    public List<EnvironmentNode> getOrCreateEnvironmentNodes(Transaction tx, Location location, String externalId, String name) throws NodeFactoryException {
        List<Term> terms;
        try {
            terms = envoLookupService.lookupTermByName(name);
            if (terms.size() == 0) {
                terms.add(new TermImpl(externalId, name));
            }
        } catch (TermLookupServiceException e) {
            throw new NodeFactoryException("failed to lookup environment [" + name + "]", e);
        }

        return addEnvironmentNodesToLocationNodes(tx, location, terms);
    }

    public List<EnvironmentNode> addEnvironmentNodesToLocationNodes(
            Transaction tx, Location location, List<Term> terms) throws NodeFactoryException {
        List<EnvironmentNode> normalizedEnvironments = new ArrayList<>();
        for (Term term : terms) {
            Node node = createEnvironmentNode(tx);
            EnvironmentNode environment = new EnvironmentNode(node, term.getId(), term.getName());
            location.addEnvironment(environment);
            normalizedEnvironments.add(environment);
        }
        return normalizedEnvironments;
    }

    abstract public Node createEnvironmentNode(Transaction tx);

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
            return terms.isEmpty() ? NO_MATCH_TERM : terms.get(0);
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
    public Term getOrCreateBasisOfRecord(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

    @Override
    public Dataset getOrCreateDataset(Dataset originatingDataset) throws NodeFactoryException {
        try (Transaction tx = getGraphDb().beginTx()) {
            DatasetNode orCreateDatasetNode = getOrCreateDatasetNode(tx, originatingDataset);
            Dataset dataset = copyOf(orCreateDatasetNode);
            tx.commit();
            return dataset;
        }
    }

    private static Dataset copyOf(Dataset orCreateDatasetNode) {
        DatasetImpl dataset = new DatasetImpl(orCreateDatasetNode.getNamespace(), null, orCreateDatasetNode.getArchiveURI());
        dataset.setConfig(orCreateDatasetNode.getConfig());
        return dataset;
    }

    @Override
    public Interaction createInteraction(Study study) throws NodeFactoryException {
        InteractionNode interactionNode;
        try (Transaction transaction = graphDb.beginTx()) {
            interactionNode = createInteractionNode(transaction, study);
            transaction.commit();
            return interactionNode;
        }
    }

    protected InteractionNode createInteractionNode(Transaction transaction, Study study) throws NodeFactoryException {
        InteractionNode interactionNode;
        Node node = transaction.createNode();
        StudyNode studyNode = getOrCreateStudyNode(transaction, study);
        interactionNode = new InteractionNode(node);
        interactionNode.createRelationshipTo(studyNode, RelTypes.DERIVED_FROM);
        Dataset dataset = getOrCreateDatasetNode(getGraphDb().beginTx(), study.getOriginatingDataset());
        if (dataset instanceof DatasetNode) {
            interactionNode.createRelationshipTo(dataset, RelTypes.ACCESSED_AT);
        }
        return interactionNode;
    }

    protected abstract Node getOrCreateExternalIdNoTx(String externalId) throws NodeFactoryException;

    private static void createConstraintIfNeeded(GraphDatabaseService graphDb,
                                                 NodeLabel label,
                                                 String propertyName) {
        try (Transaction transaction = graphDb.beginTx()) {
            if (!transaction
                    .schema()
                    .getConstraints(label)
                    .iterator()
                    .hasNext()) {

                transaction
                        .schema()
                        .constraintFor(label)
                        .assertPropertyIsUnique(propertyName)
                        .create();
                transaction.commit();
            }
        }
    }

    private static void createIndexIfNeeded(GraphDatabaseService graphDb,
                                            NodeLabel label,
                                            String propertyName) {

        try (Transaction transaction = graphDb.beginTx()) {
            Iterable<IndexDefinition> indexes = transaction
                    .schema()
                    .getIndexes(label);

            IndexDefinition indexMatching = null;
            for (IndexDefinition index : indexes) {
                Iterator<String> keyIterator = index.getPropertyKeys().iterator();
                if (keyIterator.hasNext()) {
                    if (StringUtils.equals(keyIterator.next(), propertyName)) {
                        indexMatching = index;
                        break;
                    }
                }

            }
            if (indexMatching == null) {
                transaction
                        .schema()
                        .indexFor(NodeLabel.Location)
                        .on(propertyName)
                        .create();
            }
            transaction.commit();
        }
    }

    abstract protected DatasetNode getOrCreateDatasetNode(Transaction tx, Dataset originatingDataset) throws NodeFactoryException;

    protected void validate(Location location) throws NodeFactoryException {
        if (location.getLatitude() != null
                && !LocationUtil.isValidLatitude(location.getLatitude())) {
            throw new NodeFactoryException("found invalid latitude [" + location.getLatitude() + "]");
        }
        if (location.getLongitude() != null
                && !LocationUtil.isValidLongitude(location.getLongitude())) {
            throw new NodeFactoryException("found invalid longitude [" + location.getLongitude() + "]");
        }
    }

    void indexStudyNode(StudyNode studyNode) {
        // indexing already done via constraint: do nothing
    }

    protected Node createDatasetNode(Transaction tx) {
        return tx.createNode(NodeLabel.Dataset);
    }

    protected void indexDatasetNode(Dataset dataset, Node datasetNode) {
        // indexing already done via constraint; do nothing
    }

    protected void indexExternalIdNode(String externalId, Node externalIdNode) {
        // external ids already indexed through constraint, do nothing.
    }

    protected Node createExternalIdNode(Transaction transaction) {
        return transaction.createNode(NodeLabel.ExternalId);
    }


}

