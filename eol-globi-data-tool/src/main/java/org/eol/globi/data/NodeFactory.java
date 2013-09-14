package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.eol.globi.data.taxon.TaxonNameNormalizer;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.NamedNode;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonPropertyEnricher;
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
import java.util.Date;
import java.util.List;

public class NodeFactory {

    private static final Log LOG = LogFactory.getLog(NodeFactory.class);

    public static final TaxonNameNormalizer TAXON_NAME_NORMALIZER = new TaxonNameNormalizer();
    private final TaxonPropertyEnricher taxonEnricher;
    private GraphDatabaseService graphDb;

    private final Index<Node> studies;
    private final Index<Node> seasons;
    private final Index<Node> locations;
    private final Index<Node> environments;
    private final Index<Node> taxons;
    private final Index<Node> taxonNameSuggestions;
    private final Index<Node> taxonPaths;
    private final Index<Node> taxonCommonNames;

    public NodeFactory(GraphDatabaseService graphDb, TaxonPropertyEnricher taxonEnricher) {
        this.graphDb = graphDb;
        this.taxonEnricher = taxonEnricher;

        this.studies = graphDb.index().forNodes("studies");
        this.seasons = graphDb.index().forNodes("seasons");
        this.locations = graphDb.index().forNodes("locations");
        this.environments = graphDb.index().forNodes("environments");
        this.taxons = graphDb.index().forNodes("taxons");
        this.taxonNameSuggestions = graphDb.index().forNodes("taxonNameSuggestions");
        this.taxonPaths = graphDb.index().forNodes("taxonpaths", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
        this.taxonCommonNames = graphDb.index().forNodes("taxonCommonNames", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
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

    private void addTaxonToIndex(Taxon taxon) {
        taxons.add(taxon.getUnderlyingNode(), Taxon.NAME, taxon.getName());
        taxonPaths.add(taxon.getUnderlyingNode(), Taxon.PATH, taxon.getPath());

        String commonNames = taxon.getCommonNames();
        if (StringUtils.isNotBlank(commonNames)) {
            taxonCommonNames.add(taxon.getUnderlyingNode(), Taxon.COMMON_NAMES, commonNames);
            String[] commonNameArray = commonNames.split(CharsetConstant.SEPARATOR);
            for (String commonName : commonNameArray) {
                taxonNameSuggestions.add(taxon.getUnderlyingNode(), Taxon.NAME, commonName);
            }
        }

        String path = taxon.getPath();
        if (StringUtils.isNotBlank(path)) {
            taxonCommonNames.add(taxon.getUnderlyingNode(), Taxon.PATH, path);
            String[] pathElementArray = path.split(CharsetConstant.SEPARATOR);
            for (String pathElement : pathElementArray) {
                taxonNameSuggestions.add(taxon.getUnderlyingNode(), Taxon.NAME, pathElement);
            }
        }

        taxonNameSuggestions.add(taxon.getUnderlyingNode(), Taxon.NAME, taxon.getCommonNames());
    }

    public Taxon findTaxon(String taxonName) throws NodeFactoryException {
        return findTaxonOfType(taxonName);
    }

    public Taxon findTaxonOfType(String taxonName) throws NodeFactoryException {
        String cleanedTaxonName = TAXON_NAME_NORMALIZER.normalize(taxonName);
        String query = "name:\"" + cleanedTaxonName + "\"";
        IndexHits<Node> matchingTaxons = taxons.query(query);
        Node matchingTaxon;
        Taxon firstMatchingTaxon = null;
        if (matchingTaxons.hasNext()) {
            matchingTaxon = matchingTaxons.next();
            firstMatchingTaxon = new Taxon(matchingTaxon);
        }

        ArrayList<Taxon> duplicateTaxons = null;
        while (matchingTaxons.hasNext()) {
            if (duplicateTaxons == null) {
                duplicateTaxons = new ArrayList<Taxon>();
            }
            duplicateTaxons.add(new Taxon(matchingTaxons.next()));
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
        matchingTaxons.close();
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
        return createStudy(title, null, null, null, null, null, null);
    }

    private Study createStudy(String title, String contributor, String institution, String period, String description, String publicationYear, String source) {
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
            studies.add(node, "title", title);
            transaction.success();
        } finally {
            transaction.finish();
        }

        return study;
    }

    public Study getOrCreateStudy(String title, String contributor, String institution, String period, String description, String publicationYear, String source) {
        Study study = findStudy(title);
        if (null == study) {
            study = createStudy(title, contributor, institution, period, description, publicationYear, source);
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
            }
        }
        return location;
    }

    public Taxon getOrCreateTaxon(String name) throws NodeFactoryException {
        return getOrCreateTaxon(name, null, null);
    }

    public Taxon getOrCreateTaxon(String name, String externalId, String path) throws NodeFactoryException {
        Taxon taxon = findTaxon(name);
        if (taxon == null) {
            String normalizedName = TAXON_NAME_NORMALIZER.normalize(name);
            Transaction transaction = graphDb.beginTx();
            try {
                taxon = new Taxon(graphDb.createNode(), normalizedName);
                taxon.setExternalId(externalId);
                taxon.setPath(path);
                taxonEnricher.enrich(taxon);
                addTaxonToIndex(taxon);
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
        Taxon taxon = new Taxon(node, TAXON_NAME_NORMALIZER.normalize(name));
        if (null != externalId) {
            taxon.setExternalId(externalId);
        }
        if (null != path) {
            taxon.setPath(path);
        }
        addTaxonToIndex(taxon);
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

    public Environment getOrCreateEnvironment(String externalId, String name) {
        Environment environment = findEnvironment(name);
        if (environment == null) {
            Transaction transaction = graphDb.beginTx();
            environment = new Environment(graphDb.createNode(), name, externalId);
            environments.add(environment.getUnderlyingNode(), NamedNode.NAME, name);
            transaction.success();
            transaction.finish();
        }
        return environment;
    }

    public Environment findEnvironment(String name) {
        String query = "name:\"" + name + "\"";
        IndexHits<Node> matches = environments.query(query);
        Node matchingTaxon;
        Environment firstMatchingEnvironment = null;
        if (matches.hasNext()) {
            matchingTaxon = matches.next();
            firstMatchingEnvironment = new Environment(matchingTaxon);
        }

        matches.close();
        return firstMatchingEnvironment;
    }
}

