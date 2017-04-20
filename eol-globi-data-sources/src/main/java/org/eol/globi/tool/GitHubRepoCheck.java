package org.eol.globi.tool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.Version;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.ParserFactoryLocal;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForGitHubData;
import org.eol.globi.domain.*;
import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderException;
import org.eol.globi.service.AuthorIdResolver;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetFinder;
import org.eol.globi.service.DatasetFinderCaching;
import org.eol.globi.service.DatasetFinderException;
import org.eol.globi.service.DatasetFinderGitHubArchiveMaster;
import org.eol.globi.service.DatasetFinderProxy;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public class GitHubRepoCheck {
    private final static Log LOG = LogFactory.getLog(GitHubRepoCheck.class);

    public static void main(final String[] args) throws IOException, StudyImporterException, DatasetFinderException {
        if (args.length == 0) {
            throw new StudyImporterException("please provide at least one github repository short name (e.g. globalbioticinteractions/template-dataset) as an argument");
        }
        LOG.info(Version.getVersionInfo(GitHubRepoCheck.class));
        final String repoName = args[0];
        final Set<String> infos = Collections.synchronizedSortedSet(new TreeSet<String>());
        final Set<String> warnings = Collections.synchronizedSortedSet(new TreeSet<String>());
        final Set<String> errors = Collections.synchronizedSortedSet(new TreeSet<String>());

        NodeFactoryLogging nodeFactory = new NodeFactoryLogging();
        List<DatasetFinder> finders = Collections.singletonList(new DatasetFinderGitHubArchiveMaster(Arrays.asList(args)));
        DatasetFinderCaching finder = new DatasetFinderCaching(new DatasetFinderProxy(finders));
        ParserFactoryLocal parserFactory = new ParserFactoryLocal();
        StudyImporterForGitHubData studyImporterForGitHubData = new StudyImporterForGitHubData(parserFactory, nodeFactory);
        studyImporterForGitHubData.setLogger(new ImportLogger() {
            @Override
            public void info(LogContext study, String message) {
                addUntilFull(message, infos);
            }

            @Override
            public void warn(LogContext study, String message) {
                addUntilFull(message, warnings);
            }

            @Override
            public void severe(LogContext study, String message) {
                addUntilFull(message, errors);
            }

            private void addUntilFull(String message, Set<String> msgs) {
                if (msgs.size() == 500) {
                    msgs.add(">= 500 unique messages, turning off logging.");
                } else if (msgs.size() < 500){
                    msgs.add(msgForRepo(message));
                }
            }

            String msgForRepo(String message) {
                return "[" + repoName + "]: [" + message + "]";
            }

        });

        try {
            studyImporterForGitHubData.setFinder(finder);
            studyImporterForGitHubData.importData(repoName);
            if (warnings.size() > 0 || errors.size() > 0 || NodeFactoryLogging.counter.get() == 0) {
                throw new StudyImporterException(getResultMsg(repoName, warnings, errors) + ", please check your log.");
            }
        } finally {
            infos.forEach(LOG::info);
            warnings.forEach(LOG::warn);
            errors.forEach(LOG::error);
            LOG.info(getResultMsg(repoName, warnings, errors));
        }
    }

    public static String getResultMsg(String repoName, Set<String> warnings, Set<String> errors) {
        return "found [" + NodeFactoryLogging.counter.get() + "] interactions in [" + repoName + "]"
                    + " and encountered [" + warnings.size() + "] warnings and [" + errors.size() + "] errors";
    }

    private static class NodeFactoryLogging implements NodeFactory {
        final static AtomicInteger counter = new AtomicInteger(0);

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
        public Specimen createSpecimen(Interaction interaction, Taxon taxon) throws NodeFactoryException {
            return null;
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
                    if (counter.get() > 0 && counter.get() % 1000 == 0) {
                        System.out.println();
                    }
                    if (counter.get() % 10 == 0) {
                        System.out.print(".");
                    }
                    counter.getAndIncrement();
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
            return new AuthorIdResolver() {
                @Override
                public String findFullName(String authorURI) throws IOException {
                    return "echo-[" + authorURI + "]";
                }
            };
        }

        @Override
        public Term getOrCreateBasisOfRecord(String externalId, String name) throws NodeFactoryException {
            return new Term(externalId, name);
        }

        @Override
        public Dataset getOrCreateDataset(Dataset dataset) {
            return dataset;
        }

        @Override
        public Interaction createInteraction(Study study) {
            return null;
        }
    }
}
