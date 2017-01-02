package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.EnvironmentNode;
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
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderException;
import org.eol.globi.service.AuthorIdResolver;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.EnvoLookupService;
import org.eol.globi.service.ORCIDResolverImpl;
import org.eol.globi.service.QueryUtil;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.taxon.TermLookupServiceWithResource;
import org.eol.globi.taxon.UberonLookupService;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.eol.globi.domain.LocationUtil.fromLocation;

public class NodeFactoryImpl implements NodeFactory {

    private static final Log LOG = LogFactory.getLog(NodeFactoryImpl.class);
    public static final org.eol.globi.domain.Term NO_MATCH_TERM = new org.eol.globi.domain.Term(PropertyAndValueDictionary.NO_MATCH, PropertyAndValueDictionary.NO_MATCH);

    private GraphDatabaseService graphDb;
    private final Index<Node> studies;
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

    public NodeFactoryImpl(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;

        this.termLookupService = new UberonLookupService();
        this.lifeStageLookupService = new TermLookupServiceWithResource("life-stage-mapping.csv");
        this.bodyPartLookupService = new TermLookupServiceWithResource("body-part-mapping.csv");
        this.envoLookupService = new EnvoLookupService();
        this.studies = graphDb.index().forNodes("studies");
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
    public LocationNode findLocation(Double latitude, Double longitude, Double altitude) {
        return findLocation(new LocationImpl(latitude, longitude, altitude, null));
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
    public SpecimenNode createSpecimen(Study study, String taxonName, String taxonExternalId) throws NodeFactoryException {
        return createSpecimen(study, new TaxonImpl(taxonName, taxonExternalId));
    }

    @Override
    public SpecimenNode createSpecimen(Study study, String taxonName) throws NodeFactoryException {
        return createSpecimen(study, new TaxonImpl(taxonName, null));
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
    public StudyNode createStudy(String title) {
        return createStudy(title, null, null, null);
    }

    private StudyNode createStudy(String title, String source, String doi, String citation) {
        Transaction transaction = graphDb.beginTx();
        StudyNode study;
        try {
            Node node = graphDb.createNode();
            study = new StudyNode(node, title);
            study.setSource(source);
            study.setCitation(citation);
            if (doiResolver != null) {
                try {
                    if (StringUtils.isBlank(doi) && citationLikeString(citation)) {
                        doi = doiResolver.findDOIForReference(citation);
                    }

                    if (StringUtils.isNotBlank(doi)) {
                        study.setDOI(doi);
                        final String citationForDOI = doiResolver.findCitationForDOI(doi);
                        if (StringUtils.isNotBlank(citationForDOI)) {
                            study.setCitation(citationForDOI);
                        } else {
                            LOG.warn("failed to find citation for doi [" + doi + "], using [" + citation + "] instead.");
                        }
                    }
                } catch (IOException e) {
                    LOG.warn("failed to lookup doi for citation [" + citation + "] with id [" + title + "]", e);
                }
            }
            studies.add(node, StudyNode.TITLE, title);
            transaction.success();
        } finally {
            transaction.finish();
        }

        return study;
    }

    private boolean citationLikeString(String citation) {
        return !StringUtils.startsWith(citation, "http://");
    }

    @Override
    public StudyNode getOrCreateStudy(String title, String source, String citation) throws NodeFactoryException {
        return getOrCreateStudy(title, source, null, citation);
    }

    @Override
    public StudyNode getOrCreateStudy(String title, String source, String doi, String citation) throws NodeFactoryException {
        if (StringUtils.isBlank(title)) {
            throw new NodeFactoryException("null or empty study title");
        }
        if (StringUtils.isBlank(source)) {
            throw new NodeFactoryException("null or empty study source");
        }
        StudyNode study = findStudy(title);
        if (null == study) {
            study = createStudy(title, source, doi, citation);
        }
        return study;
    }

    @Override
    public StudyNode getOrCreateStudy2(String title, String source, String doi) throws NodeFactoryException {
        return getOrCreateStudy(title, source, doi, null);
    }

    @Override
    public StudyNode findStudy(String title) {
        final IndexHits<Node> nodes = title == null ? null : studies.get(StudyNode.TITLE, title);
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

    @Override
    public Location getOrCreateLocation(Double latitude, Double longitude, Double altitude) throws NodeFactoryException {
        return getOrCreateLocation(new LocationImpl(latitude, longitude, altitude, null));

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
            ecoregion.setGeometry(NodeUtil.getPropertyStringValueOrNull(ecoregionNode, "geometry"));
            ecoregion.setName(NodeUtil.getPropertyStringValueOrNull(ecoregionNode, PropertyAndValueDictionary.NAME));
            ecoregion.setId(NodeUtil.getPropertyStringValueOrNull(ecoregionNode, PropertyAndValueDictionary.EXTERNAL_ID));
            ecoregion.setPath(NodeUtil.getPropertyStringValueOrNull(ecoregionNode, "path"));
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
}

