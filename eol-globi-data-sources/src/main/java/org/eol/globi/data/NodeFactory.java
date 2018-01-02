package org.eol.globi.data;

import org.eol.globi.domain.*;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.service.AuthorIdResolver;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.TermLookupService;

import java.util.Date;
import java.util.List;

public interface NodeFactory {
    Location findLocation(Location location) throws NodeFactoryException;

    Season createSeason(String seasonNameLower);

    Specimen createSpecimen(Interaction interaction, Taxon taxon) throws NodeFactoryException;

    Specimen createSpecimen(Study study, Taxon taxon) throws NodeFactoryException;

    Study createStudy(Study study);

    Study getOrCreateStudy(Study study) throws NodeFactoryException;

    Study findStudy(String title);

    Season findSeason(String seasonName);

    Location getOrCreateLocation(Location location) throws NodeFactoryException;

    void setUnixEpochProperty(Specimen specimen, Date date) throws NodeFactoryException;

    Date getUnixEpochProperty(Specimen specimen) throws NodeFactoryException;

    List<Environment> getOrCreateEnvironments(Location location, String externalId, String name) throws NodeFactoryException;

    List<Environment> addEnvironmentToLocation(Location location, List<Term> terms);

    Term getOrCreateBodyPart(String externalId, String name) throws NodeFactoryException;

    Term getOrCreatePhysiologicalState(String externalId, String name) throws NodeFactoryException;

    Term getOrCreateLifeStage(String externalId, String name) throws NodeFactoryException;

    TermLookupService getTermLookupService();

    EcoregionFinder getEcoregionFinder();

    AuthorIdResolver getAuthorResolver();

    Term getOrCreateBasisOfRecord(String externalId, String name) throws NodeFactoryException;

    Dataset getOrCreateDataset(Dataset dataset);

    Interaction createInteraction(Study study) throws NodeFactoryException;
}
