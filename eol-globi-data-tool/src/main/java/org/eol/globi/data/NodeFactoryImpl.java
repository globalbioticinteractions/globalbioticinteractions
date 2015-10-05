package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
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
import org.eol.globi.service.UberonLookupService;
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
    private TaxonIndex taxonIndex;

    public NodeFactoryImpl(GraphDatabaseService graphDb, TaxonIndex taxonIndex) {
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

        this.taxonIndex = taxonIndex;

    }

    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    @Override
    public TaxonNode findTaxonByName(String taxonName) throws NodeFactoryException {
        return getTaxonIndex().findTaxonByName(taxonName);
    }

    @Override
    public TaxonNode findTaxonById(String externalId) throws NodeFactoryException {
        return getTaxonIndex().findTaxonById(externalId);
    }

    @Override
    public TaxonNode getOrCreateTaxon(String name) throws NodeFactoryException {
        return getOrCreateTaxon(new TaxonImpl(name));
    }

    @Override
    public TaxonNode getOrCreateTaxon(String name, String externalId, String path) throws NodeFactoryException {
        Taxon taxon = new TaxonImpl(name, externalId);
        taxon.setPath(path);
        return getOrCreateTaxon(taxon);
    }

    @Override
    public TaxonNode getOrCreateTaxon(Taxon taxon) throws NodeFactoryException {
        return getTaxonIndex().getOrCreateTaxon(taxon);
    }

    @Override
    public Location findLocation(Double latitude, Double longitude, Double altitude) {
        QueryContext queryOrQueryObject = QueryContext.numericRange(Location.LATITUDE, latitude, latitude);
        IndexHits<Node> matchingLocations = locations.query(queryOrQueryObject);
        Node matchingLocation = null;
        for (Node node : matchingLocations) {
            Double foundLongitude = (Double) node.getProperty(Location.LONGITUDE);

            boolean altitudeMatches = false;
            if (node.hasProperty(Location.ALTITUDE)) {
                Double foundAltitude = (Double) node.getProperty(Location.ALTITUDE);
                altitudeMatches = altitude != null && altitude.equals(foundAltitude);
            } else if (null == altitude) {
                // explicit null value matches
                altitudeMatches = true;
            }

            if (longitude.equals(foundLongitude) && altitudeMatches) {
                matchingLocation = node;
                break;
            }

        }
        matchingLocations.close();
        return matchingLocation == null ? null : new Location(matchingLocation);
    }

    @Override
    public Season createSeason(String seasonNameLower) {
        Transaction transaction = graphDb.beginTx();
        Season season;
        try {
            Node node = graphDb.createNode();
            season = new Season(node, seasonNameLower);
            seasons.add(node, Season.TITLE, seasonNameLower);
            transaction.success();
        } finally {
            transaction.finish();
        }
        return season;
    }

    private Location createLocation(Double latitude, Double longitude, Double altitude) {
        Transaction transaction = graphDb.beginTx();
        Location location;
        try {
            Node node = graphDb.createNode();
            location = new Location(node, latitude, longitude, altitude);
            locations.add(node, Location.LATITUDE, ValueContext.numeric(latitude));
            locations.add(node, Location.LONGITUDE, ValueContext.numeric(longitude));
            if (altitude != null) {
                locations.add(node, Location.ALTITUDE, ValueContext.numeric(altitude));
            }
            transaction.success();
        } finally {
            transaction.finish();
        }
        return location;
    }


    @Override
    public Specimen createSpecimen(Study study, String taxonName, String taxonExternalId) throws NodeFactoryException {
        return createSpecimen(study, new TaxonImpl(taxonName, taxonExternalId));
    }

    @Override
    public Specimen createSpecimen(Study study, String taxonName) throws NodeFactoryException {
        return createSpecimen(study, new TaxonImpl(taxonName, null));
    }

    @Override
    public Specimen createSpecimen(Study study, Taxon origTaxon) throws NodeFactoryException {
        if (null == study) {
            throw new NodeFactoryException("specimen needs study, but none is specified");
        }

        TaxonNode taxonNode = getOrCreateTaxon(origTaxon);
        Specimen specimen = createSpecimen(taxonNode);
        study.createRelationshipTo(specimen, RelTypes.COLLECTED);

        specimen.setOriginalTaxonDescription(origTaxon.getName(), origTaxon.getExternalId());
        if (StringUtils.isNotBlank(origTaxon.getName())) {
            extractTerms(origTaxon.getName(), specimen);
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


    private Specimen createSpecimen(TaxonNode taxon) {
        Transaction transaction = graphDb.beginTx();
        Specimen specimen;
        try {
            specimen = new Specimen(graphDb.createNode(), null);
            if (taxon != null) {
                specimen.classifyAs(taxon);
            }
            transaction.success();
        } finally {
            transaction.finish();
        }
        return specimen;
    }


    @Override
    public Study createStudy(String title) {
        return createStudy(title, null, null, null);
    }

    private Study createStudy(String title, String source, String doi, String citation) {
        Transaction transaction = graphDb.beginTx();
        Study study;
        try {
            Node node = graphDb.createNode();
            study = new Study(node, title);
            study.setSource(source);
            study.setCitation(citation);
            if (doiResolver != null) {
                try {
                    if (StringUtils.isBlank(doi)) {
                        doi = doiResolver.findDOIForReference(citation);
                    }

                    if (StringUtils.isNotBlank(doi)) {
                        study.setDOI(doi);
                        study.setCitation(doiResolver.findCitationForDOI(doi));
                    }
                } catch (IOException e) {
                    LOG.warn("failed to lookup doi for [" + title + "]", e);
                }
            }
            studies.add(node, Study.TITLE, title);
            transaction.success();
        } finally {
            transaction.finish();
        }

        return study;
    }

    @Override
    public Study getOrCreateStudy(String title, String source, String citation) {
        return getOrCreateStudy(title, source, null, citation);
    }

    @Override
    public Study getOrCreateStudy(String title, String source, String doi, String citation) {
        Study study = findStudy(title);
        if (null == study) {
            study = createStudy(title, source, doi, citation);
        }
        return study;
    }

    @Override
    public Study getOrCreateStudy2(String title, String source, String doi) {
        return getOrCreateStudy(title, source, doi, null);
    }

    @Override
    public Study findStudy(String title) {
        Node foundStudyNode = studies.get(Study.TITLE, title).getSingle();
        return foundStudyNode == null ? null : new Study(foundStudyNode);
    }

    @Override
    public Season findSeason(String seasonName) {
        IndexHits<Node> nodeIndexHits = seasons.get(Season.TITLE, seasonName);
        Node seasonHit = nodeIndexHits.getSingle();
        nodeIndexHits.close();
        return seasonHit == null ? null : new Season(seasonHit);
    }

    @Override
    public Location getOrCreateLocation(Double latitude, Double longitude, Double altitude) throws NodeFactoryException {
        Location location = null;
        if (latitude != null && longitude != null) {
            validate(latitude, longitude);
            location = findLocation(latitude, longitude, altitude);
            if (null == location) {
                location = createLocation(latitude, longitude, altitude);
                try {
                    enrichLocationWithEcoRegions(location);
                } catch (NodeFactoryException e) {
                    LOG.error("failed to create eco region for location (" + location.getLatitude() + ", " + location.getLongitude() + ")", e);
                }
            }
        }
        return location;
    }

    private void validate(Double latitude, Double longitude) throws NodeFactoryException {

        if (!LocationUtil.isValidLatitude(latitude)) {
            throw new NodeFactoryException("found invalid latitude [" + latitude + "]");
        }
        if (!LocationUtil.isValidLongitude(longitude)) {
            throw new NodeFactoryException("found invalid longitude [" + longitude + "]");
        }
    }

    @Override
    public void setUnixEpochProperty(Specimen specimen, Date date) throws NodeFactoryException {
        if (specimen != null && date != null) {
            Relationship rel = getCollectedRel(specimen);
            Transaction tx = rel.getGraphDatabase().beginTx();
            try {
                rel.setProperty(Specimen.DATE_IN_UNIX_EPOCH, date.getTime());
                tx.success();
            } finally {
                tx.finish();
            }
        }
    }

    protected Relationship getCollectedRel(Specimen specimen) throws NodeFactoryException {
        Relationship rel = specimen.getUnderlyingNode().getSingleRelationship(RelTypes.COLLECTED, Direction.INCOMING);
        if (null == rel) {
            throw new NodeFactoryException("specimen not associated with study");
        }
        return rel;
    }

    @Override
    public Date getUnixEpochProperty(Specimen specimen) throws NodeFactoryException {
        Date date = null;
        Relationship rel = getCollectedRel(specimen);
        if (rel.hasProperty(Specimen.DATE_IN_UNIX_EPOCH)) {
            Long unixEpoch = (Long) rel.getProperty(Specimen.DATE_IN_UNIX_EPOCH);
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
                    environment = new Environment(graphDb.createNode(), term.getId(), term.getName());
                    environments.add(environment.getUnderlyingNode(), PropertyAndValueDictionary.NAME, term.getName());
                    transaction.success();
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
        Iterable<Relationship> relationships = locationNode.getRelationships(RelTypes.IN_ECOREGION, Direction.OUTGOING);
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
        List<Ecoregion> associatedEcoregions = getEcoRegions(location.getUnderlyingNode());
        return associatedEcoregions == null ? associateWithEcoRegions(location) : associatedEcoregions;
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
            location.getUnderlyingNode().createRelationshipTo(ecoregionNode, RelTypes.IN_ECOREGION);
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

    protected Environment findEnvironment(String name) {
        String query = "name:\"" + name + "\"";
        IndexHits<Node> matches = environments.query(query);
        Environment firstMatchingEnvironment = null;
        if (matches.hasNext()) {
            firstMatchingEnvironment = new Environment(matches.next());
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

    public void setTaxonIndex(TaxonIndex taxonIndex) {
        this.taxonIndex = taxonIndex;
    }

    @Override
    public TaxonIndex getTaxonIndex() {
        return taxonIndex;
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

