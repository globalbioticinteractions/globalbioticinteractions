package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.*;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.service.AuthorIdResolver;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.TermLookupService;

import java.util.Date;
import java.util.List;

public class NodeFactoryWithDatasetContext implements NodeFactory {

    private final NodeFactory factory;
    private final Dataset dataset;

    public NodeFactoryWithDatasetContext(NodeFactory factory, Dataset dataset) {
        this.factory = factory;
        this.dataset = dataset;
    }


    @Override
    public Location findLocation(Location location) throws NodeFactoryException {
        return factory.findLocation(location);
    }

    @Override
    public Season createSeason(String seasonNameLower) {
        return factory.createSeason(seasonNameLower);
    }

    @Override
    public Specimen createSpecimen(Interaction interaction, Taxon taxon) throws NodeFactoryException {
        return factory.createSpecimen(interaction, taxon);
    }

    @Override
    public Specimen createSpecimen(Study study, Taxon taxon) throws NodeFactoryException {
        return factory.createSpecimen(study, taxon);
    }

    @Override
    public Study createStudy(Study study) {
        return factory.createStudy(studyForDataset(study));
    }

    private Study studyForDataset(Study study) {
        StudyImpl study1 = new StudyImpl(study.getTitle(), study.getSource(), study.getDOI(), study.getCitation());
        study1.setExternalId(study.getExternalId());
        if (StringUtils.isNotBlank(dataset.getNamespace())) {
            study1.setSourceId("globi:" + StringUtils.trim(dataset.getNamespace()));
        }
        study1.setOriginatingDataset(dataset);
        return study1;
    }

    @Override
    public Study getOrCreateStudy(Study study) throws NodeFactoryException {
        return factory.getOrCreateStudy(studyForDataset(study));
    }

    @Override
    public Study findStudy(String title) {
        return factory.findStudy(title);
    }

    @Override
    public Season findSeason(String seasonName) {
        return factory.findSeason(seasonName);
    }

    @Override
    public Location getOrCreateLocation(Location location) throws NodeFactoryException {
        return factory.getOrCreateLocation(location);
    }

    @Override
    public void setUnixEpochProperty(Specimen specimen, Date date) throws NodeFactoryException {
        factory.setUnixEpochProperty(specimen, date);
    }

    @Override
    public Date getUnixEpochProperty(Specimen specimen) throws NodeFactoryException {
        return factory.getUnixEpochProperty(specimen);
    }

    @Override
    public List<Environment> getOrCreateEnvironments(Location location, String externalId, String name) throws NodeFactoryException {
        return factory.getOrCreateEnvironments(location, externalId, name);
    }

    @Override
    public List<Environment> addEnvironmentToLocation(Location location, List<Term> terms) {
        return factory.addEnvironmentToLocation(location, terms);
    }

    @Override
    public Term getOrCreateBodyPart(String externalId, String name) throws NodeFactoryException {
        return factory.getOrCreateBodyPart(externalId, name);
    }

    @Override
    public Term getOrCreatePhysiologicalState(String externalId, String name) throws NodeFactoryException {
        return factory.getOrCreatePhysiologicalState(externalId, name);
    }

    @Override
    public Term getOrCreateLifeStage(String externalId, String name) throws NodeFactoryException {
        return factory.getOrCreateLifeStage(externalId, name);
    }

    @Override
    public TermLookupService getTermLookupService() {
        return factory.getTermLookupService();
    }

    @Override
    public EcoregionFinder getEcoregionFinder() {
        return factory.getEcoregionFinder();
    }

    @Override
    public AuthorIdResolver getAuthorResolver() {
        return factory.getAuthorResolver();
    }

    @Override
    public Term getOrCreateBasisOfRecord(String externalId, String name) throws NodeFactoryException {
        return factory.getOrCreateBasisOfRecord(externalId, name);
    }

    @Override
    public Dataset getOrCreateDataset(Dataset dataset) {
        return factory.getOrCreateDataset(dataset);
    }

    @Override
    public Interaction createInteraction(Study study) throws NodeFactoryException {
        return factory.createInteraction(study);
    }

}
