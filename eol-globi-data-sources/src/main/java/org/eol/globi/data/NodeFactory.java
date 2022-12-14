package org.eol.globi.data;

import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Interaction;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupService;
import org.globalbioticinteractions.dataset.Dataset;

import java.util.Date;
import java.util.List;

public interface NodeFactory extends AutoCloseable {
    Location findLocation(Location location) throws NodeFactoryException;

    Season createSeason(String seasonNameLower) throws NodeFactoryException;

    Specimen createSpecimen(Interaction interaction, Taxon taxon) throws NodeFactoryException;

    Specimen createSpecimen(Study study, Taxon taxon) throws NodeFactoryException;

    Specimen createSpecimen(Study study, Taxon taxon, RelTypes... types) throws NodeFactoryException;

    Study createStudy(Study study) throws NodeFactoryException;

    Study getOrCreateStudy(Study study) throws NodeFactoryException;

    Study findStudy(Study study);

    Season findSeason(String seasonName);

    Location getOrCreateLocation(Location location) throws NodeFactoryException;

    void setUnixEpochProperty(Specimen specimen, Date date) throws NodeFactoryException;

    Date getUnixEpochProperty(Specimen specimen) throws NodeFactoryException;

    List<Environment> getOrCreateEnvironments(Location location, String externalId, String name) throws NodeFactoryException;

    List<Environment> addEnvironmentToLocation(Location location, List<Term> terms) throws NodeFactoryException;

    Term getOrCreateBodyPart(String externalId, String name) throws NodeFactoryException;

    Term getOrCreatePhysiologicalState(String externalId, String name) throws NodeFactoryException;

    Term getOrCreateLifeStage(String externalId, String name) throws NodeFactoryException;

    TermLookupService getTermLookupService();

    Term getOrCreateBasisOfRecord(String externalId, String name) throws NodeFactoryException;

    Dataset getOrCreateDataset(Dataset dataset) throws NodeFactoryException;

    Interaction createInteraction(Study study) throws NodeFactoryException;
}
