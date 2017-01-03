package org.eol.globi.tool;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.ParserFactory;
import org.eol.globi.data.ParserFactoryImpl;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForGitHubData;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderException;
import org.eol.globi.service.AuthorIdResolver;
import org.eol.globi.service.DatasetFinder;
import org.eol.globi.service.DatasetFinderCaching;
import org.eol.globi.service.DatasetFinderException;
import org.eol.globi.service.DatasetFinderGitHubArchive;
import org.eol.globi.service.DatasetFinderProxy;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GitHubRepoCheck {
    private final static Log LOG = LogFactory.getLog(GitHubRepoCheck.class);

    public static void main(final String[] args) throws IOException, StudyImporterException, DatasetFinderException {
        final String repoName = args[0];
        NodeFactoryLogging nodeFactory = new NodeFactoryLogging();
        List<DatasetFinder> finders = Collections.singletonList(new DatasetFinderGitHubArchive());
        DatasetFinderCaching finder = new DatasetFinderCaching(new DatasetFinderProxy(finders));
        StudyImporterForGitHubData studyImporterForGitHubData = new StudyImporterForGitHubData(new ParserFactoryImpl(), nodeFactory);
        studyImporterForGitHubData.setFinder(finder);
        studyImporterForGitHubData.importData(repoName);
        LOG.info("found [" + nodeFactory.counter + "] interactions in [" + repoName + "]");
    }

    private static class NodeFactoryLogging implements NodeFactory {
        int counter = 0;

        @Override
        public Location findLocation(Location location) {
            return location;
        }

        @Override
        public Season createSeason(String seasonName) {
            return new Season() {
                @Override
                public String getTitle() {
                    return seasonName;
                }
            };
        }

        @Override
        public Specimen createSpecimen(Study study, Taxon taxon) throws NodeFactoryException {
            return new Specimen() {
                @Override
                public Location getSampleLocation() {
                    return null;
                }

                @Override
                public void ate(Specimen specimen) {
                    interactsWith(specimen, InteractType.ATE);
                }

                @Override
                public void caughtIn(Location sampleLocation) {

                }

                @Override
                public Season getSeason() {
                    return null;
                }

                @Override
                public void caughtDuring(Season season) {

                }

                @Override
                public Double getLengthInMm() {
                    return null;
                }

                @Override
                public void classifyAs(Taxon taxon) {

                }

                @Override
                public void setLengthInMm(Double lengthInMm) {

                }

                @Override
                public void setVolumeInMilliLiter(Double volumeInMm3) {

                }

                @Override
                public void setStomachVolumeInMilliLiter(Double volumeInMilliLiter) {

                }

                @Override
                public void interactsWith(Specimen target, InteractType type, Location centroid) {
                    counter++;
                    System.out.print(".");
                }

                @Override
                public void interactsWith(Specimen recipientSpecimen, InteractType relType) {
                    interactsWith(recipientSpecimen, relType, null);
                }

                @Override
                public void setOriginalTaxonDescription(Taxon taxon) {

                }

                @Override
                public void setLifeStage(List<Term> lifeStages) {

                }

                @Override
                public void setLifeStage(Term lifeStage) {

                }

                @Override
                public void setPhysiologicalState(Term physiologicalState) {

                }

                @Override
                public void setBodyPart(List<Term> bodyParts) {

                }

                @Override
                public void setBodyPart(Term bodyPart) {

                }

                @Override
                public void setBasisOfRecord(Term basisOfRecord) {

                }

                @Override
                public Term getBasisOfRecord() {
                    return null;
                }

                @Override
                public void setFrequencyOfOccurrence(Double frequencyOfOccurrence) {

                }

                @Override
                public void setTotalCount(Integer totalCount) {

                }

                @Override
                public void setTotalVolumeInMl(Double totalVolumeInMl) {

                }

                @Override
                public Term getLifeStage() {
                    return null;
                }

                @Override
                public Term getBodyPart() {
                    return null;
                }

                @Override
                public void setProperty(String name, Object value) {

                }

                @Override
                public void setExternalId(String externalId) {

                }

                @Override
                public String getExternalId() {
                    return null;
                }
            };
        }

        @Override
        public Study createStudy(Study study) {
            return study;
        }

        @Override
        public Study getOrCreateStudy(Study study) throws NodeFactoryException {
            return study;
        }

        @Override
        public Study findStudy(String title) {
            return null;
        }

        @Override
        public Season findSeason(String seasonName) {
            return null;
        }

        @Override
        public Location getOrCreateLocation(Location location) throws NodeFactoryException {
            return location;
        }

        @Override
        public void setUnixEpochProperty(Specimen specimen, Date date) throws NodeFactoryException {

        }

        @Override
        public Date getUnixEpochProperty(Specimen specimen) throws NodeFactoryException {
            return null;
        }

        @Override
        public List<Environment> getOrCreateEnvironments(Location location, String externalId, String name) throws NodeFactoryException {
            return Collections.emptyList();
        }

        @Override
        public List<Environment> addEnvironmentToLocation(Location location, List<Term> terms) {
            return Collections.emptyList();
        }

        @Override
        public Term getOrCreateBodyPart(String externalId, String name) throws NodeFactoryException {
            return new Term(externalId, name);
        }

        @Override
        public Term getOrCreatePhysiologicalState(String externalId, String name) throws NodeFactoryException {
            return new Term(externalId, name);
        }

        @Override
        public Term getOrCreateLifeStage(String externalId, String name) throws NodeFactoryException {
            return new Term(externalId, name);
        }

        @Override
        public TermLookupService getTermLookupService() {
            return new TermLookupService() {
                @Override
                public List<Term> lookupTermByName(String name) throws TermLookupServiceException {
                    return Arrays.asList(new Term(name, null));
                }
            };
        }

        @Override
        public EcoregionFinder getEcoregionFinder() {
            return new EcoregionFinder() {
                @Override
                public Collection<Ecoregion> findEcoregion(double lat, double lng) throws EcoregionFinderException {
                    return Collections.emptyList();
                }

                @Override
                public void shutdown() {

                }
            };
        }

        @Override
        public AuthorIdResolver getAuthorResolver() {
            return null;
        }

        @Override
        public Term getOrCreateBasisOfRecord(String externalId, String name) throws NodeFactoryException {
            return new Term(externalId, name);
        }
    }
}
