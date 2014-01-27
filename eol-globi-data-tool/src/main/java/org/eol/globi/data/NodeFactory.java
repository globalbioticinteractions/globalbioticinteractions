package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.eol.globi.data.taxon.CorrectionService;
import org.eol.globi.data.taxon.TaxonNameCorrector;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.NamedNode;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.geo.EcoRegion;
import org.eol.globi.geo.EcoRegionFinder;
import org.eol.globi.geo.EcoRegionFinderException;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.EnvoLookupService;
import org.eol.globi.service.TaxonMatchValidator;
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

    private final TaxonPropertyEnricher taxonEnricher;
    private GraphDatabaseService graphDb;

    private final Index<Node> studies;
    private final Index<Node> seasons;
    private final Index<Node> locations;
    private final Index<Node> environments;
    private final Index<Node> taxons;
    private final Index<Node> taxonNameSuggestions;
    private final Index<Node> taxonPaths;
    private final Index<Node> ecoRegions;
    private final Index<Node> ecoRegionSuggestions;
    private final Index<Node> ecoRegionPaths;
    private final Index<Node> taxonCommonNames;
    public static final org.eol.globi.domain.Term NO_MATCH_TERM = new org.eol.globi.domain.Term(PropertyAndValueDictionary.NO_MATCH, PropertyAndValueDictionary.NO_MATCH);

    private TermLookupService termLookupService;
    private TermLookupService envoLookupService;
    private CorrectionService correctionService;

    private DOIResolver doiResolver;
    private EcoRegionFinder ecoRegionFinder;

    public NodeFactory(GraphDatabaseService graphDb, TaxonPropertyEnricher taxonEnricher) {
        this.graphDb = graphDb;
        this.taxonEnricher = taxonEnricher;
        this.termLookupService = new UberonLookupService();
        this.envoLookupService = new EnvoLookupService();
        this.correctionService = new TaxonNameCorrector();
        this.studies = graphDb.index().forNodes("studies");
        this.seasons = graphDb.index().forNodes("seasons");
        this.locations = graphDb.index().forNodes("locations");
        this.environments = graphDb.index().forNodes("environments");
        this.taxons = graphDb.index().forNodes("taxons");
        this.taxonNameSuggestions = graphDb.index().forNodes("taxonNameSuggestions");
        this.taxonPaths = graphDb.index().forNodes("taxonpaths", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
        this.taxonCommonNames = graphDb.index().forNodes("taxonCommonNames", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));

        this.ecoRegions = graphDb.index().forNodes("ecoRegions");
        this.ecoRegionPaths = graphDb.index().forNodes("ecoRegionPaths", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
        this.ecoRegionSuggestions = graphDb.index().forNodes("ecoRegionSuggestions");
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

    private void addToIndeces(Taxon taxon, String correctedName) {
        String canonicalName = taxon.getName();
        if (StringUtils.isNotBlank(canonicalName)) {
            taxons.add(taxon.getUnderlyingNode(), Taxon.NAME, canonicalName);
            if (!StringUtils.equals(canonicalName, correctedName)) {
                taxons.add(taxon.getUnderlyingNode(), Taxon.NAME, correctedName);
            }
            indexCommonNames(taxon);
            indexTaxonPath(taxon);
        }
    }

    private void indexTaxonPath(Taxon taxon) {
        String path = taxon.getPath();
        if (StringUtils.isNotBlank(path)) {
            taxonPaths.add(taxon.getUnderlyingNode(), Taxon.PATH, path);
            taxonCommonNames.add(taxon.getUnderlyingNode(), Taxon.PATH, path);
            String[] pathElementArray = path.split(CharsetConstant.SEPARATOR);
            for (String pathElement : pathElementArray) {
                taxonNameSuggestions.add(taxon.getUnderlyingNode(), Taxon.NAME, StringUtils.lowerCase(pathElement));
            }
        }
    }

    private void indexCommonNames(Taxon taxon) {
        String commonNames = taxon.getCommonNames();
        if (StringUtils.isNotBlank(commonNames)) {
            taxonCommonNames.add(taxon.getUnderlyingNode(), Taxon.COMMON_NAMES, commonNames);
            String[] commonNameArray = commonNames.split(CharsetConstant.SEPARATOR);
            for (String commonName : commonNameArray) {
                taxonNameSuggestions.add(taxon.getUnderlyingNode(), Taxon.NAME, StringUtils.lowerCase(commonName));
            }
        }
    }

    public Taxon findTaxon(String taxonName) throws NodeFactoryException {
        return findTaxonOfType(taxonName);
    }

    public Taxon findTaxonOfType(String taxonName) throws NodeFactoryException {
        String cleanedTaxonName = correctionService.correct(taxonName);
        String query = "name:\"" + QueryParser.escape(cleanedTaxonName) + "\"";
        IndexHits<Node> matchingTaxa = taxons.query(query);
        Node matchingTaxon;
        Taxon firstMatchingTaxon = null;
        if (matchingTaxa.hasNext()) {
            matchingTaxon = matchingTaxa.next();
            firstMatchingTaxon = new Taxon(matchingTaxon);
        }

        ArrayList<Taxon> duplicateTaxons = null;
        while (matchingTaxa.hasNext()) {
            if (duplicateTaxons == null) {
                duplicateTaxons = new ArrayList<Taxon>();
            }
            duplicateTaxons.add(new Taxon(matchingTaxa.next()));
        }
        if (duplicateTaxons != null) {
            StringBuffer buffer = new StringBuffer();
            duplicateTaxons.add(firstMatchingTaxon);
            for (Taxon duplicateTaxon : duplicateTaxons) {
                buffer.append('{');
                buffer.append(duplicateTaxon.getName());
                buffer.append(':');
                buffer.append(duplicateTaxon.getExternalId());
                buffer.append('}');
            }
            LOG.warn("found duplicates for taxon with name [" + taxonName + "], using first only: " + buffer.toString());
        }
        matchingTaxa.close();
        return firstMatchingTaxon;
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

    public Specimen createSpecimen(String specimenTaxonDescription) throws NodeFactoryException {
        return createSpecimen(specimenTaxonDescription, null);
    }

    public Specimen createSpecimen(String specimenTaxonDescription, String taxonExternalId) throws NodeFactoryException {
        Taxon taxon = getOrCreateTaxon(specimenTaxonDescription, taxonExternalId, null);
        Specimen specimen = createSpecimen(taxon);
        specimen.setOriginalTaxonDescription(specimenTaxonDescription);
        return specimen;
    }


    private Specimen createSpecimen(Taxon taxon) {
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
            studies.add(node, "title", title);
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

    public Study getOrCreateStudy(String title, String contributor, String institution, String period, String description, String publicationYear, String source) {
        return getOrCreateStudy(title, contributor, institution, period, description, publicationYear, source, null);
    }

    public Study getOrCreateStudy(String title, String contributor, String institution, String period, String description, String publicationYear, String source, String doi) {
        Study study = findStudy(title);
        if (null == study) {
            study = createStudy(title, contributor, institution, period, description, publicationYear, source, doi);
        }
        return study;
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
                    getOrCreateEcoRegions(location);
                } catch (NodeFactoryException e) {
                    LOG.error("failed to create eco region for location (" + location.getLatitude() + ", " + location.getLongitude() + ")");
                }
            }
        }
        return location;
    }

    public Taxon getOrCreateTaxon(String name) throws NodeFactoryException {
        return getOrCreateTaxon(name, null, null);
    }

    public Taxon getOrCreateTaxon(String name, String externalId, String path) throws NodeFactoryException {
        if (StringUtils.length(name) < 2) {
            throw new NodeFactoryException("taxon name [" + name + "] must contains more than 1 character");
        }
        Taxon taxon = findTaxon(name);
        if (taxon == null) {
            String correctedName = correctionService.correct(name);
            Transaction transaction = graphDb.beginTx();
            try {
                taxon = new Taxon(graphDb.createNode(), correctedName);
                taxon.setExternalId(externalId);
                taxon.setPath(path);
                boolean shouldContinue;
                do {
                    taxonEnricher.enrich(taxon);
                    shouldContinue = !TaxonMatchValidator.hasMatch(taxon);
                    if (shouldContinue) {
                        String[] nameParts = StringUtils.split(taxon.getName());
                        if (nameParts.length > 1) {
                            taxon.setName(nameParts[0]);
                        } else if (nameParts.length > 2) {
                            taxon.setName(nameParts[0].trim() + " " + nameParts[1].trim());
                        } else {
                            shouldContinue = false;
                        }
                    }

                } while (shouldContinue);

                if (!TaxonMatchValidator.hasMatch(taxon)) {
                    taxon.setName(correctedName);
                }

                addToIndeces(taxon, correctedName);
                transaction.success();
            } catch (IOException e) {
                transaction.failure();
            } finally {
                transaction.finish();
            }
        }
        return taxon;
    }

    public Taxon createTaxonNoTransaction(String name, String externalId, String path) {
        Node node = graphDb.createNode();
        Taxon taxon = new Taxon(node, correctionService.correct(name));
        if (null != externalId) {
            taxon.setExternalId(externalId);
        }
        if (null != path) {
            taxon.setPath(path);
        }
        addToIndeces(taxon, name);
        return taxon;
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

    public IndexHits<Node> findCloseMatchesForTaxonName(String taxonName) {
        return query(taxonName, Taxon.NAME, taxons);
    }

    public IndexHits<Node> findCloseMatchesForTaxonPath(String taxonPath) {
        return query(taxonPath, Taxon.PATH, taxonPaths);
    }

    private IndexHits<Node> query(String taxonName, String name, Index<Node> taxonIndex) {
        String capitalizedValue = StringUtils.capitalize(taxonName);
        List<Query> list = new ArrayList<Query>();
        addQueriesForProperty(capitalizedValue, name, list);
        BooleanQuery fuzzyAndWildcard = new BooleanQuery();
        for (Query query : list) {
            fuzzyAndWildcard.add(query, BooleanClause.Occur.SHOULD);
        }
        return taxonIndex.query(fuzzyAndWildcard);
    }

    private void addQueriesForProperty(String capitalizedValue, String propertyName, List<Query> list) {
        list.add(new FuzzyQuery(new Term(propertyName, capitalizedValue)));
        list.add(new WildcardQuery(new Term(propertyName, capitalizedValue + "*")));
    }


    public IndexHits<Node> findTaxaByPath(String wholeOrPartialPath) {
        return taxonPaths.query("path:\"" + wholeOrPartialPath + "\"");
    }

    public IndexHits<Node> findTaxaByCommonName(String wholeOrPartialName) {
        return taxonCommonNames.query("commonNames:\"" + wholeOrPartialName + "\"");
    }

    public IndexHits<Node> suggestTaxaByName(String wholeOrPartialScientificOrCommonName) {
        return taxonNameSuggestions.query("name:\"" + wholeOrPartialScientificOrCommonName + "\"");
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
                environment = new Environment(graphDb.createNode(), term.getId(), term.getName());
                environments.add(environment.getUnderlyingNode(), NamedNode.NAME, name);
                transaction.success();
                transaction.finish();
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
            ecoRegion.setName(NodeUtil.getPropertyStringValueOrNull(ecoRegionNode, NamedNode.NAME));
            ecoRegion.setId(NodeUtil.getPropertyStringValueOrNull(ecoRegionNode, NodeBacked.EXTERNAL_ID));
            ecoRegion.setPath(NodeUtil.getPropertyStringValueOrNull(ecoRegionNode, "path"));
            if (ecoRegions == null) {
                ecoRegions = new ArrayList<EcoRegion>();
            }
            ecoRegions.add(ecoRegion);
        }
        return ecoRegions;
    }

    public List<EcoRegion> getOrCreateEcoRegions(Location location) throws NodeFactoryException {
        List<EcoRegion> associatedEcoRegions = getEcoRegions(location.getUnderlyingNode());
        if (null == associatedEcoRegions) {
            associatedEcoRegions = new ArrayList<EcoRegion>();
            List<EcoRegion> ecoRegionsToBeIndexed = new ArrayList<EcoRegion>();
            try {
                EcoRegionFinder finder = getEcoRegionFinder();
                if (finder != null) {
                    Collection<EcoRegion> ecoRegions = finder.findEcoRegion(location.getLatitude(), location.getLongitude());
                    for (EcoRegion ecoRegion : ecoRegions) {
                        if (isNewEcoRegion(ecoRegion)) {
                            ecoRegionsToBeIndexed.add(ecoRegion);
                        }
                        associatedEcoRegions.add(ecoRegion);
                    }
                }
            } catch (EcoRegionFinderException e) {
                throw new NodeFactoryException("problem finding eco region for location (lat,lng):(" + location.getLatitude() + "," + location.getLongitude() + ")");
            }

            Transaction tx = graphDb.beginTx();
            try {
                for (EcoRegion ecoRegion : ecoRegionsToBeIndexed) {
                    addAndIndexEcoRegion(location, ecoRegion);
                }
                tx.success();
            } finally {
                tx.finish();
            }

        }
        return associatedEcoRegions;
    }

    private boolean isNewEcoRegion(EcoRegion ecoRegion) {
        String query = "name:\"" + ecoRegion.getName() + "\"";
        IndexHits<Node> hits = this.ecoRegions.query(query);
        boolean newEcoRegion = !hits.hasNext();
        hits.close();
        return newEcoRegion;
    }

    private void addAndIndexEcoRegion(Location location, EcoRegion ecoRegion) {
        Node node = graphDb.createNode();
        node.setProperty(NamedNode.NAME, ecoRegion.getName());
        node.setProperty(NamedNode.EXTERNAL_ID, ecoRegion.getId());
        node.setProperty("path", ecoRegion.getPath());
        node.setProperty("geometry", ecoRegion.getGeometry());
        location.getUnderlyingNode().createRelationshipTo(node, RelTypes.IN_ECO_REGION);
        ecoRegions.add(node, NamedNode.NAME, ecoRegion.getName());
        ecoRegionPaths.add(node, "path", ecoRegion.getPath());
        ecoRegionSuggestions.add(node, NamedNode.NAME, StringUtils.lowerCase(ecoRegion.getName()));
        if (StringUtils.isNotBlank(ecoRegion.getPath())) {
            for (String part : ecoRegion.getPath().split(CharsetConstant.SEPARATOR)) {
                ecoRegionSuggestions.add(node, NamedNode.NAME, StringUtils.lowerCase(part));
            }
        }
    }

    protected Environment findEnvironment(String name) {
        String query = "name:\"" + name + "\"";
        IndexHits<Node> matches = environments.query(query);
        Node matchingEnvironment;
        Environment firstMatchingEnvironment = null;
        if (matches.hasNext()) {
            matchingEnvironment = matches.next();
            firstMatchingEnvironment = new Environment(matchingEnvironment);
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
        this.correctionService = correctionService;
    }


    public void setEcoRegionFinder(EcoRegionFinder ecoRegionFinder) {
        this.ecoRegionFinder = ecoRegionFinder;
    }

    public EcoRegionFinder getEcoRegionFinder() {
        return ecoRegionFinder;
    }

    public IndexHits<Node> findCloseMatchesForEcoRegion(String ecoRegionName) {
        return query(ecoRegionName, Taxon.NAME, ecoRegions);
    }

    public IndexHits<Node> findCloseMatchesForEcoRegionPath(String ecoRegionPath) {
        return query(ecoRegionPath, Taxon.PATH, ecoRegionPaths);
    }

    public IndexHits<Node> suggestEcoRegionByName(String wholeOrPartialEcoRegionNameOrPath) {
        return ecoRegionSuggestions.query("name:\"" + wholeOrPartialEcoRegionNameOrPath + "\"");
    }


}

