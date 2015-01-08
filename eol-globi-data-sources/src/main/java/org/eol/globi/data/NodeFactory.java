package org.eol.globi.data;

import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.service.TermLookupService;

import java.util.Date;
import java.util.List;

public interface NodeFactory {
    TaxonNode findTaxonByName(String taxonName) throws NodeFactoryException;

    TaxonNode getOrCreateTaxon(String name) throws NodeFactoryException;

    TaxonNode getOrCreateTaxon(String name, String externalId, String path) throws NodeFactoryException;

    Location findLocation(Double latitude, Double longitude, Double altitude);

    Season createSeason(String seasonNameLower);

    Specimen createSpecimen(Study study, String taxonName) throws NodeFactoryException;

    Specimen createSpecimen(Study study, String taxonName, String taxonExternalId) throws NodeFactoryException;

    Study createStudy(String title);

    @Deprecated
    Study getOrCreateStudy(String title, String contributor, String institution, String period, String description, String publicationYear, String source);

    @Deprecated
    Study getOrCreateStudy(String title, String contributor, String institution, String period, String description, String publicationYear, String source, String doi);

    Study getOrCreateStudy(String title, String source, String doi);

    Study findStudy(String title);

    Season findSeason(String seasonName);

    Location getOrCreateLocation(Double latitude, Double longitude, Double altitude) throws NodeFactoryException;

    void setUnixEpochProperty(Specimen specimen, Date date) throws NodeFactoryException;

    Date getUnixEpochProperty(Specimen specimen) throws NodeFactoryException;

    List<Environment> getOrCreateEnvironments(Location location, String externalId, String name) throws NodeFactoryException;

    List<Environment> addEnvironmentToLocation(Location location, List<Term> terms);

    Term getOrCreateBodyPart(String externalId, String name) throws NodeFactoryException;

    Term getOrCreatePhysiologicalState(String externalId, String name) throws NodeFactoryException;

    Term getOrCreateLifeStage(String externalId, String name) throws NodeFactoryException;

    TermLookupService getTermLookupService();

    EcoregionFinder getEcoregionFinder();

    TaxonIndex getTaxonIndex();
}
