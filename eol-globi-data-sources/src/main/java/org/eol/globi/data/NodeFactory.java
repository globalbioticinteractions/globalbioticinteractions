package org.eol.globi.data;

import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.SeasonNode;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.service.AuthorIdResolver;
import org.eol.globi.service.TermLookupService;

import java.util.Date;
import java.util.List;

public interface NodeFactory {
    LocationNode findLocation(Location location);

    LocationNode findLocation(Double latitude, Double longitude, Double altitude);

    SeasonNode createSeason(String seasonNameLower);

    SpecimenNode createSpecimen(Study study, Taxon taxon) throws NodeFactoryException;

    SpecimenNode createSpecimen(Study study, String taxonName) throws NodeFactoryException;

    SpecimenNode createSpecimen(Study study, String taxonName, String taxonExternalId) throws NodeFactoryException;

    StudyNode createStudy(String title);

    StudyNode getOrCreateStudy(String title, String source, String citation) throws NodeFactoryException;

    StudyNode getOrCreateStudy(String title, String source, String doi, String citation) throws NodeFactoryException;

    StudyNode getOrCreateStudy2(String title, String source, String doi) throws NodeFactoryException;

    StudyNode findStudy(String title);

    SeasonNode findSeason(String seasonName);

    Location getOrCreateLocation(Location location) throws NodeFactoryException;

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

    AuthorIdResolver getAuthorResolver();

    Term getOrCreateBasisOfRecord(String externalId, String name) throws NodeFactoryException;
}
