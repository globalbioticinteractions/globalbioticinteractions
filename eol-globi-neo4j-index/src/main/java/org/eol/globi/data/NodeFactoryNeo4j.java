package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.EnvironmentNode;
import org.eol.globi.domain.Interaction;
import org.eol.globi.domain.InteractionNode;
import org.eol.globi.domain.Location;
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
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.globalbioticinteractions.doi.DOI;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.eol.globi.domain.LocationUtil.fromLocation;

public abstract class NodeFactoryNeo4j extends NodeFactoryAbstract {

    private static final Logger LOG = LoggerFactory.getLogger(NodeFactoryNeo4j.class);
    public static final TermImpl NO_MATCH_TERM = new TermImpl(PropertyAndValueDictionary.NO_MATCH, PropertyAndValueDictionary.NO_MATCH);

    private GraphDatabaseService graphDb;

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

    }

    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    @Override
    public SeasonNode createSeason(String seasonNameLower) {
        Node node = createSeasonNode();
        SeasonNode season = new SeasonNode(node, seasonNameLower);
        indexSeasonNode(seasonNameLower, node);
        return season;
    }

    protected abstract void indexSeasonNode(String seasonNameLower, Node node);

    protected abstract Node createSeasonNode();

    private LocationNode createLocation(final Location location) {
        Node node = createLocationNode();
        LocationNode locationNode = new LocationNode(node, fromLocation(location));
        indexLocation(location, node);
        return locationNode;
    }

    abstract protected void indexLocation(Location location, Node node);

    protected abstract Node createLocationNode();

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


    private SpecimenNode createSpecimen() {
        return new SpecimenNode(graphDb.createNode(), null);
    }


    abstract Node createStudyNode();

    abstract void indexStudyNode(StudyNode studyNode);

    @Override
    public StudyNode createStudy(Study study) {
        Node node = createStudyNode();
        StudyNode studyNode = createStudyNode(study, node);
        indexStudyNode(studyNode);
        return studyNode;
    }

    private StudyNode createStudyNode(Study study, Node node) {
        StudyNode studyNode;
        studyNode = new StudyNode(node, study.getTitle());
        studyNode.setCitation(study.getCitation());
        studyNode.setDOI(study.getDOI());
        if (study.getDOI() != null) {
            String doiString = study.getDOI().toString();
            createExternalIdRelationIfExists(node, doiString, RelTypes.HAS_DOI);
        }

        String externalId = getExternalIdOrDOI(study);
        studyNode.setExternalId(externalId);

        createExternalIdRelationIfExists(node, externalId, RelTypes.HAS_EXTERNAL_ID);

        Dataset dataset = getOrCreateDatasetNoTx(study.getOriginatingDataset());
        if (dataset instanceof DatasetNode) {
            studyNode.createRelationshipTo(dataset, RelTypes.IN_DATASET);
        }

        studyNode.getUnderlyingNode().setProperty(StudyConstant.TITLE_IN_NAMESPACE, getIdInNamespace(study));
        return studyNode;
    }

    private void createExternalIdRelationIfExists(Node node, String externalId, RelTypes hasExternalId) {
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

    protected abstract Node createDatasetNode();

    protected abstract void indexDatasetNode(Dataset dataset, Node datasetNode);

    Node createDatasetNode(Dataset dataset) {
        Node datasetNode = createDatasetNode();
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

    Node createExternalId(String externalId) {
        Node externalIdNode = createExternalIdNode();
        externalIdNode.setProperty(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
        indexExternalIdNode(externalId, externalIdNode);
        return externalIdNode;
    }


    protected abstract void indexExternalIdNode(String externalId, Node externalIdNode);

    protected abstract Node createExternalIdNode();

    @Override
    public StudyNode getOrCreateStudy(Study study) throws NodeFactoryException {
        if (StringUtils.isBlank(study.getTitle())) {
            throw new NodeFactoryException("null or empty study title");
        }

        StudyNode studyNode = findStudy(study);

        if (studyNode == null) {
            studyNode = createStudy(study);
        }

        return studyNode;
    }

    private String namespaceOrNull(Study study) {
        return study != null && study.getOriginatingDataset() != null
                ? study.getOriginatingDataset().getNamespace()
                : null;
    }

    @Override
    public abstract StudyNode findStudy(Study study);

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
    public LocationNode getOrCreateLocation(org.eol.globi.domain.Location location) throws NodeFactoryException {
        Location location1 = findLocation(location);
        if (!(location1 instanceof LocationNode)) {
            location1 = createLocation(location);
        }
        return (LocationNode) location1;
    }


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
                Node node = createEnvironmentNode();
                EnvironmentNode environmentNode = new EnvironmentNode(node, term.getId(), term.getName());
                indexEnvironmentNode(term, environmentNode);
                environment = environmentNode;
            }
            location.addEnvironment(environment);
            normalizedEnvironments.add(environment);
        }
        return normalizedEnvironments;
    }

    abstract public void indexEnvironmentNode(Term term, EnvironmentNode environmentNode);

    abstract public Node createEnvironmentNode();

    abstract public EnvironmentNode findEnvironment(String name);


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
        return getOrCreateDatasetNoTx(originatingDataset);
    }

    @Override
    public Interaction createInteraction(Study study) throws NodeFactoryException {
        InteractionNode interactionNode;
        Node node = graphDb.createNode();
        StudyNode studyNode = getOrCreateStudy(study);
        interactionNode = new InteractionNode(node);
        interactionNode.createRelationshipTo(studyNode, RelTypes.DERIVED_FROM);
        Dataset dataset = getOrCreateDatasetNoTx(study.getOriginatingDataset());
        if (dataset instanceof DatasetNode) {
            interactionNode.createRelationshipTo(dataset, RelTypes.ACCESSED_AT);
        }
        return interactionNode;
    }

    protected abstract Node getOrCreateExternalIdNoTx(String externalId);

    abstract protected Dataset getOrCreateDatasetNoTx(Dataset originatingDataset);

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

}

