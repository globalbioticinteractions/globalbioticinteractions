package org.trophic.graph.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;
import org.neo4j.index.lucene.ValueContext;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.Season;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import static org.trophic.graph.domain.RelTypes.IS_A;

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

    public NodeFactory(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
        this.studies = graphDb.index().forNodes("studies");
        this.seasons = graphDb.index().forNodes("seasons");
        this.locations = graphDb.index().forNodes("locations");
        this.taxons = graphDb.index().forNodes("taxons");

    }

    public Taxon createTaxon(String name, Taxon family) throws NodeFactoryException {
        String cleanedName = TAXON_NAME_NORMALIZER.normalize(name);
        String[] split = cleanedName.split(" ");
        Taxon taxon = null;

        if (split.length > 1) {
            String firstPart = split[0];
            if (cleanedName.contains("sp.") || cleanedName.contains("spp.")) {
                taxon = createFamilyOrGenus(family, firstPart);
            } else {
                if (isFamilyName(firstPart)) {
                    taxon = getOrCreateFamily(firstPart);
                } else {
                    Taxon genus = getOrCreateGenus(firstPart);
                    if (family != null) {
                        genus.createRelationshipTo(family, IS_A);
                    }
                    taxon = getOrCreateSpecies(genus, cleanedName);
                }
            }
        } else if (split.length == 1) {
            taxon = createFamilyOrGenus(family, split[0]);
        }
        return taxon;
    }

    private Taxon createFamilyOrGenus(Taxon family, String firstPart) throws NodeFactoryException {
        Taxon taxon;
        if (isFamilyName(firstPart)) {
            taxon = getOrCreateFamily(firstPart);
        } else {
            Taxon genus = getOrCreateGenus(firstPart);
            if (family != null) {
                genus.createRelationshipTo(family, IS_A);
            }
            taxon = genus;
        }
        return taxon;
    }

    private boolean isFamilyName(String firstPart) {
        return firstPart.endsWith("ae");
    }

    public Taxon getOrCreateSpecies(Taxon genus, String speciesName) throws NodeFactoryException {
        Taxon species = findTaxonOfType(speciesName);
        if (species == null) {
            species = getOrCreateTaxon(speciesName);
        }
        if (null != genus) {
            species.createRelationshipTo(genus, IS_A);
        }
        return species;
    }


    public Taxon getOrCreateGenus(String genusName) throws NodeFactoryException {
        Taxon genus = findTaxonOfType(genusName);
        if (genus == null) {
            genus = getOrCreateTaxon(genusName);

        }
        return genus;
    }

    public Taxon getOrCreateFamily(final String familyName) throws NodeFactoryException {
        Taxon family = null;
        if (familyName != null) {
            Taxon foundFamily = findTaxonOfType(familyName);
            if (foundFamily == null) {
                family = getOrCreateTaxon(familyName);
            } else {
                family = foundFamily;
            }
        }
        return family;
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
        if (matchingTaxons.hasNext()) {
            matchingTaxon = matchingTaxons.next();
        }

        if (matchingTaxons.hasNext()) {
            LOG.error("found two or more instances of taxon with name [" + taxonName + "]");
        }
        matchingTaxons.close();
        return matchingTaxon == null ? null : new Taxon(matchingTaxon);
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
        Transaction transaction = graphDb.beginTx();
        Study study;
        try {
            Node node = graphDb.createNode();
            study = new Study(node, title);
            studies.add(node, "title", title);
            transaction.success();
        } finally {
            transaction.finish();
        }

        return study;
    }

    public Study getOrCreateStudy(String title) {
        Study study = findStudy(title);
        if (null == study) {
            study = createStudy(title);
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
            Transaction transaction = graphDb.beginTx();
            try {
                taxon = createTaxonNoTransaction(name, externalId);
                transaction.success();
            } finally {
                transaction.finish();
            }
        }
        return taxon;
    }

    public Taxon createTaxonNoTransaction(String name, String externalId) {
        Taxon taxon;
        Node node = graphDb.createNode();
        taxon = new Taxon(node, TAXON_NAME_NORMALIZER.normalize(name));
        if (null != externalId) {
            taxon.setExternalId(externalId);
        }
        addTaxonToIndex(taxon, node);
        return taxon;
    }
}
