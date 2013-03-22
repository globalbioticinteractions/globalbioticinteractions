package org.eol.globi.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;
import org.neo4j.index.lucene.ValueContext;
import org.eol.globi.data.taxon.TaxonLookupService;
import org.eol.globi.data.taxon.TaxonNameNormalizer;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class NodeFactory {

    private static final Log LOG = LogFactory.getLog(NodeFactory.class);

    public static final TaxonNameNormalizer TAXON_NAME_NORMALIZER = new TaxonNameNormalizer();


    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    private GraphDatabaseService graphDb;
    private Index<Node> studies;
    private Index<Node> seasons;
    private Index<Node> locations;
    private Index<Node> taxons;
    private TaxonLookupService taxonLookupService;

    public NodeFactory(GraphDatabaseService graphDb, TaxonLookupService taxonLookupService) {
        this.graphDb = graphDb;
        this.studies = graphDb.index().forNodes("studies");
        this.seasons = graphDb.index().forNodes("seasons");
        this.locations = graphDb.index().forNodes("locations");
        this.taxonLookupService = taxonLookupService;
        this.taxons = graphDb.index().forNodes("taxons");
    }

    private void addTaxonToIndex(Taxon taxon, Node node) {
        taxons.add(node, Taxon.NAME, taxon.getName());
    }

    public Taxon findTaxon(String taxonName) throws NodeFactoryException {
        return findTaxonOfType(taxonName);
    }

    public Taxon findTaxonOfType(String taxonName) throws NodeFactoryException {
        String cleanedTaxonName = TAXON_NAME_NORMALIZER.normalize(taxonName);
        String query = "name:\"" + cleanedTaxonName + "\"";
        IndexHits<Node> matchingTaxons = taxons.query(query);
        Node matchingTaxon = null;
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

    public Specimen createSpecimen() {
        Transaction transaction = graphDb.beginTx();
        Specimen specimen;
        try {
            specimen = new Specimen(graphDb.createNode(), null);
            transaction.success();
        } finally {
            transaction.finish();
        }
        return specimen;
    }

    public Study createStudy(String title) {
        return createStudy(title, null, null, null, null);
    }

    public Study createStudy(String title, String contributor, String institution, String period, String description) {
        Transaction transaction = graphDb.beginTx();
        Study study;
        try {
            Node node = graphDb.createNode();
            study = new Study(node, title);
            if (contributor != null) {
                study.setContributor(contributor);
            }
            if (institution != null) {
                study.setInstitution(institution);
            }
            if (period != null) {
                study.setPeriod(period);
            }
            if (description != null) {
                study.setDescription(description);
            }

            studies.add(node, "title", title);
            transaction.success();
        } finally {
            transaction.finish();
        }

        return study;
    }

    public Study getOrCreateStudy(String title, String contributor, String institution, String period, String description) {
            Study study = findStudy(title);
            if (null == study) {
                study = createStudy(title, contributor, institution, period, description);
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
        return getOrCreateTaxon(name, null);
    }

    public Taxon getOrCreateTaxon(String name, String externalId) throws NodeFactoryException {
        Taxon taxon = findTaxon(name);
        if (taxon == null) {
            String normalizedName = TAXON_NAME_NORMALIZER.normalize(name);
            externalId = findExternalId(name, externalId, normalizedName);
            Transaction transaction = graphDb.beginTx();
            try {
                taxon = createTaxonNoTransaction(normalizedName, externalId);
                transaction.success();
            } finally {
                transaction.finish();
            }
        }
        return taxon;
    }

    private String findExternalId(String name, String externalId1, String normalizedName) throws NodeFactoryException {
        try {
            long[] longs = taxonLookupService.lookupTerms(normalizedName);
            if (longs.length > 0) {
                // TODO should put EOL dependency in taxonLookupService
                externalId1 = "EOL:" + longs[0];
                if (longs.length > 1) {
                    LOG.info("found at least one duplicate for taxon with name [" + name + "]: {" + longs[0] + "," + longs[1] + "} - using first.");
                }
            }
        } catch (IOException e) {
            throw new NodeFactoryException("failed to lookup taxon", e);
        }
        return externalId1;
    }

    public Taxon createTaxonNoTransaction(String name, String externalId) {
        Node node = graphDb.createNode();
        Taxon taxon = new Taxon(node, TAXON_NAME_NORMALIZER.normalize(name));
        if (null != externalId) {
            taxon.setExternalId(externalId);
        }
        addTaxonToIndex(taxon, node);
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
}
