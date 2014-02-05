package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.taxon.CorrectionService;
import org.eol.globi.data.taxon.TaxonNameCorrector;
import org.eol.globi.data.taxon.TaxonServiceImpl;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.geo.EcoRegion;
import org.eol.globi.geo.EcoRegionFinder;
import org.eol.globi.geo.EcoRegionFinderException;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.EnvoLookupService;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
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

public class NodeFactory {

    private static final Log LOG = LogFactory.getLog(NodeFactory.class);
    public static final org.eol.globi.domain.Term NO_MATCH_TERM = new org.eol.globi.domain.Term(PropertyAndValueDictionary.NO_MATCH, PropertyAndValueDictionary.NO_MATCH);

    private GraphDatabaseService graphDb;
    private final Index<Node> studies;
    private final Index<Node> seasons;
    private final Index<Node> locations;
    private final Index<Node> environments;
    private final Index<Node> ecoRegions;
    private final Index<Node> ecoRegionSuggestions;
    private final Index<Node> ecoRegionPaths;

    private TermLookupService termLookupService;
    private TermLookupService envoLookupService;

    private DOIResolver doiResolver;
    private EcoRegionFinder ecoRegionFinder;
    private TaxonServiceImpl taxonService;

    public NodeFactory(GraphDatabaseService graphDb, TaxonPropertyEnricher taxonEnricher) {
        this.graphDb = graphDb;

        this.termLookupService = new UberonLookupService();
        this.envoLookupService = new EnvoLookupService();
        this.studies = graphDb.index().forNodes("studies");
        this.seasons = graphDb.index().forNodes("seasons");
        this.locations = graphDb.index().forNodes("locations");
        this.environments = graphDb.index().forNodes("environments");

        this.ecoRegions = graphDb.index().forNodes("ecoRegions");
        this.ecoRegionPaths = graphDb.index().forNodes("ecoRegionPaths", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
        this.ecoRegionSuggestions = graphDb.index().forNodes("ecoRegionSuggestions");

        this.taxonService = new TaxonServiceImpl(taxonEnricher, new TaxonNameCorrector(), getGraphDb());

    }

    public static List<Study> findAllStudies(GraphDatabaseService graphService) {
        List<Study> studies = new ArrayList<Study>();
        Index<Node> studyIndex = graphService.index().forNodes("studies");
        IndexHits<Node> hits = studyIndex.query("title", "*");
        for (Node hit : hits) {
            studies.add(new Study(hit));
        }
        return studies;
    }

    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    public TaxonNode findTaxon(String taxonName) throws NodeFactoryException {
        return findTaxonOfType(taxonName);
    }

    public TaxonNode findTaxonOfType(String taxonName) throws NodeFactoryException {
        return this.taxonService.findTaxon(taxonName);
    }

    public TaxonNode getOrCreateTaxon(String name) throws NodeFactoryException {
        return getOrCreateTaxon(name, null, null);
    }

    public TaxonNode getOrCreateTaxon(String name, String externalId, String path) throws NodeFactoryException {
        return taxonService.getOrCreateTaxon(name, externalId, path);
    }

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

    public Specimen createSpecimen(String taxonName) throws NodeFactoryException {
        return createSpecimen(taxonName, null);
    }

    public Specimen createSpecimen(String taxonName, String taxonExternalId) throws NodeFactoryException {
        TaxonNode taxon = getOrCreateTaxon(taxonName, taxonExternalId, null);
        Specimen specimen = createSpecimen(taxon);
        specimen.setOriginalTaxonDescription(taxonName);
        return specimen;
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


    public Study createStudy(String title) {
        return createStudy(title, null, null, null, null, null, null, null);
    }

    private Study createStudy(String title, String contributor, String institution, String period, String description, String publicationYear, String source, String doi) {
        Transaction transaction = graphDb.beginTx();
        Study study;
        try {
            Node node = graphDb.createNode();
            study = new Study(node, title);
            study.setSource(source);
            study.setContributor(contributor);
            study.setInstitution(institution);
            study.setPeriod(period);
            study.setDescription(description);
            study.setPublicationYear(publicationYear);
            if (doiResolver != null) {
                try {
                    if (StringUtils.isBlank(doi)) {
                        doi = findDOIUsingReference(contributor, description, publicationYear);
                    }

                    if (StringUtils.isNotBlank(doi)) {
                        study.setDOI(doi);
                        study.setCitation(doiResolver.findCitationForDOI(doi));
                    }
                } catch (IOException e) {
                    LOG.warn("failed to lookup doi for [" + title + "]");
                }
            }
            studies.add(node, Study.TITLE, title);
            transaction.success();
        } finally {
            transaction.finish();
        }

        return study;
    }

    private String findDOIUsingReference(String contributor, String description, String publicationYear) throws IOException {
        String doi;
        String prefix = StringUtils.isBlank(contributor) ? "" : (contributor + " ");
        String reference = StringUtils.isBlank(description) ? "" : (prefix + description);
        if (StringUtils.isNotBlank(publicationYear)) {
            reference = reference + " " + publicationYear;
        }
        doi = doiResolver.findDOIForReference(reference);
        return doi;
    }

    @Deprecated
    public Study getOrCreateStudy(String title, String contributor, String institution, String period, String description, String publicationYear, String source) {
        return getOrCreateStudy(title, contributor, institution, period, description, publicationYear, source, null);
    }

    @Deprecated
    public Study getOrCreateStudy(String title, String contributor, String institution, String period, String description, String publicationYear, String source, String doi) {
        Study study = findStudy(title);
        if (null == study) {
            study = createStudy(title, contributor, institution, period, description, publicationYear, source, doi);
        }
        return study;
    }

    public Study getOrCreateStudy(String title, String source, String doi) {
        return getOrCreateStudy(title, null, null, null, null, null, source, doi);
    }

    public Study findStudy(String title) {
        Node foundStudyNode = studies.get(Study.TITLE, title).getSingle();
        return foundStudyNode == null ? null : new Study(foundStudyNode);
    }

    public Season findSeason(String seasonName) {
        IndexHits<Node> nodeIndexHits = seasons.get(Season.TITLE, seasonName);
        Node seasonHit = nodeIndexHits.getSingle();
        nodeIndexHits.close();
        return seasonHit == null ? null : new Season(seasonHit);
    }

    public Location getOrCreateLocation(Double latitude, Double longitude, Double altitude) {
        Location location = null;
        if (latitude != null && longitude != null) {
            location = findLocation(latitude, longitude, altitude);
            if (null == location) {
                location = createLocation(latitude, longitude, altitude);
                try {
                    enrichLocationWithEcoRegions(location);
                } catch (NodeFactoryException e) {
                    LOG.error("failed to create eco region for location (" + location.getLatitude() + ", " + location.getLongitude() + ")");
                }
            }
        }
        return location;
    }

    public void setUnixEpochProperty(Relationship rel, Date date) {
        if (date != null) {
            Transaction tx = rel.getGraphDatabase().beginTx();
            try {
                rel.setProperty(Specimen.DATE_IN_UNIX_EPOCH, date.getTime());
                tx.success();
            } finally {
                tx.finish();
            }
        }
    }

    public Date getUnixEpochProperty(Relationship rel) {
        Date date = null;
        if (rel != null) {
            if (rel.hasProperty(Specimen.DATE_IN_UNIX_EPOCH)) {
                Long unixEpoch = (Long) rel.getProperty(Specimen.DATE_IN_UNIX_EPOCH);
                date = new Date(unixEpoch);
            }

        }
        return date;
    }

    public List<Environment> getOrCreateEnvironments(Location location, String externalId, String name) throws NodeFactoryException {
        List<org.eol.globi.domain.Term> terms;
        try {
            terms = envoLookupService.lookupTermByName(name);
            if (terms.size() == 0) {
                terms.add(new org.eol.globi.domain.Term(externalId, name));
            }
        } catch (TermLookupServiceException e) {
            throw new NodeFactoryException("failed to lookup environment [" + name + "]");
        }

        List<Environment> normalizedEnvironments = new ArrayList<Environment>();
        for (org.eol.globi.domain.Term term : terms) {
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

    private List<EcoRegion> getEcoRegions(Node locationNode) {
        Iterable<Relationship> relationships = locationNode.getRelationships(RelTypes.IN_ECO_REGION, Direction.OUTGOING);
        List<EcoRegion> ecoRegions = null;
        for (Relationship relationship : relationships) {
            Node ecoRegionNode = relationship.getEndNode();
            EcoRegion ecoRegion = new EcoRegion();
            ecoRegion.setGeometry(NodeUtil.getPropertyStringValueOrNull(ecoRegionNode, "geometry"));
            ecoRegion.setName(NodeUtil.getPropertyStringValueOrNull(ecoRegionNode, PropertyAndValueDictionary.NAME));
            ecoRegion.setId(NodeUtil.getPropertyStringValueOrNull(ecoRegionNode, PropertyAndValueDictionary.EXTERNAL_ID));
            ecoRegion.setPath(NodeUtil.getPropertyStringValueOrNull(ecoRegionNode, "path"));
            if (ecoRegions == null) {
                ecoRegions = new ArrayList<EcoRegion>();
            }
            ecoRegions.add(ecoRegion);
        }
        return ecoRegions;
    }

    public List<EcoRegion> enrichLocationWithEcoRegions(Location location) throws NodeFactoryException {
        List<EcoRegion> associatedEcoRegions = getEcoRegions(location.getUnderlyingNode());
        return associatedEcoRegions == null ? associateWithEcoRegions(location) : associatedEcoRegions;
    }

    private List<EcoRegion> associateWithEcoRegions(Location location) throws NodeFactoryException {
        List<EcoRegion> associatedEcoRegions = new ArrayList<EcoRegion>();
        try {
            EcoRegionFinder finder = getEcoRegionFinder();
            if (finder != null) {
                Collection<EcoRegion> ecoRegions = finder.findEcoRegion(location.getLatitude(), location.getLongitude());
                for (EcoRegion ecoRegion : ecoRegions) {
                    associateLocationWithEcoRegion(location, ecoRegion);
                    associatedEcoRegions.add(ecoRegion);
                }
            }
        } catch (EcoRegionFinderException e) {
            throw new NodeFactoryException("problem finding eco region for location (lat,lng):(" + location.getLatitude() + "," + location.getLongitude() + ")");
        }
        return associatedEcoRegions;
    }

    private void associateLocationWithEcoRegion(Location location, EcoRegion ecoRegion) {
        Node ecoRegionNode = findEcoRegion(ecoRegion);
        Transaction tx = graphDb.beginTx();
        try {
            if (ecoRegionNode == null) {
                ecoRegionNode = addAndIndexEcoRegion(ecoRegion);
            }
            location.getUnderlyingNode().createRelationshipTo(ecoRegionNode, RelTypes.IN_ECO_REGION);
            tx.success();
        } finally {
            tx.finish();
        }
    }

    private Node findEcoRegion(EcoRegion ecoRegion) {
        String query = "name:\"" + ecoRegion.getName() + "\"";
        IndexHits<Node> hits = this.ecoRegions.query(query);
        try {
            return hits.hasNext() ? hits.next() : null;
        } finally {
            hits.close();
        }
    }

    private Node addAndIndexEcoRegion(EcoRegion ecoRegion) {
        Node node = graphDb.createNode();
        node.setProperty(PropertyAndValueDictionary.NAME, ecoRegion.getName());
        node.setProperty(PropertyAndValueDictionary.EXTERNAL_ID, ecoRegion.getId());
        node.setProperty("path", ecoRegion.getPath());
        node.setProperty("geometry", ecoRegion.getGeometry());
        ecoRegions.add(node, PropertyAndValueDictionary.NAME, ecoRegion.getName());
        ecoRegionPaths.add(node, "path", ecoRegion.getPath());
        ecoRegionSuggestions.add(node, PropertyAndValueDictionary.NAME, StringUtils.lowerCase(ecoRegion.getName()));
        if (StringUtils.isNotBlank(ecoRegion.getPath())) {
            for (String part : ecoRegion.getPath().split(CharsetConstant.SEPARATOR)) {
                ecoRegionSuggestions.add(node, PropertyAndValueDictionary.NAME, StringUtils.lowerCase(part));
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

    public org.eol.globi.domain.Term getOrCreateBodyPart(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

    public org.eol.globi.domain.Term getOrCreatePhysiologicalState(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

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

    public void setCorrectionService(CorrectionService correctionService) {
        taxonService.setCorrector(correctionService);
    }


    public void setEcoRegionFinder(EcoRegionFinder ecoRegionFinder) {
        this.ecoRegionFinder = ecoRegionFinder;
    }

    public EcoRegionFinder getEcoRegionFinder() {
        return ecoRegionFinder;
    }

    public IndexHits<Node> findCloseMatchesForEcoRegion(String ecoRegionName) {
        return NodeUtil.query(ecoRegionName, PropertyAndValueDictionary.NAME, ecoRegions);
    }

    public IndexHits<Node> findCloseMatchesForEcoRegionPath(String ecoRegionPath) {
        return NodeUtil.query(ecoRegionPath, PropertyAndValueDictionary.PATH, ecoRegionPaths);
    }

    public IndexHits<Node> suggestEcoRegionByName(String wholeOrPartialEcoRegionNameOrPath) {
        return ecoRegionSuggestions.query("name:\"" + wholeOrPartialEcoRegionNameOrPath + "\"");
    }

}

