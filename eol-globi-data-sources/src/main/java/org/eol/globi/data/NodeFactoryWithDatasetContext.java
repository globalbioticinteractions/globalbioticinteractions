package org.eol.globi.data;

import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Interaction;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupService;
import org.globalbioticinteractions.dataset.Dataset;

import java.util.Date;
import java.util.List;

public class NodeFactoryWithDatasetContext extends NodeFactoryAbstract {

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
    public Season createSeason(String seasonNameLower) throws NodeFactoryException {
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
    public Specimen createSpecimen(Study study, Taxon taxon, RelTypes... relTypes) throws NodeFactoryException {
        return factory.createSpecimen(study, taxon, relTypes);
    }

    @Override
    public Study createStudy(Study study) throws NodeFactoryException {
        return factory.createStudy(studyForDataset(study));
    }

    private Study studyForDataset(Study study) {

        StudyImpl study1 = new StudyImpl(
                study.getTitle(),
                study.getDOI(),
                study.getCitation());

        study1.setExternalId(study.getExternalId());
        study1.setOriginatingDataset(dataset);
        return study1;
    }

    @Override
    public Study getOrCreateStudy(Study study) throws NodeFactoryException {
        return factory.getOrCreateStudy(studyForDataset(study));
    }

    @Override
    public Study findStudy(Study study) {
        return factory.findStudy(study);
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
    public List<Environment> addEnvironmentToLocation(Location location, List<Term> terms) throws NodeFactoryException {
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
    public Term getOrCreateBasisOfRecord(String externalId, String name) throws NodeFactoryException {
        return factory.getOrCreateBasisOfRecord(externalId, name);
    }

    @Override
    public Dataset getOrCreateDataset(Dataset dataset) throws NodeFactoryException {
        return factory.getOrCreateDataset(dataset);
    }

    @Override
    public Interaction createInteraction(Study study) throws NodeFactoryException {
        return factory.createInteraction(study);
    }

    public Dataset getDataset() {
        return dataset;
    }

}
