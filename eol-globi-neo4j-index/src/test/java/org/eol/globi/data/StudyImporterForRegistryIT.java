package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.DatasetRegistry;
import org.eol.globi.service.DatasetRegistryGitHubArchive;
import org.eol.globi.service.DatasetRegistryProxy;
import org.eol.globi.service.DatasetRegistryZenodo;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.dataset.DatasetRegistryWithCache;
import org.globalbioticinteractions.doi.DOI;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class StudyImporterForRegistryIT extends GraphDBTestCase {

    public static DatasetRegistry defaultFinder() {
        List<DatasetRegistry> finders = Arrays.asList(new DatasetRegistryZenodo(), new DatasetRegistryGitHubArchive());
        return new DatasetRegistryWithCache(new DatasetRegistryProxy(finders), dataset -> CacheUtil.cacheFor(dataset.getNamespace(), "target/datasets", inStream -> inStream));
    }

    @Test
    public void importAll() throws StudyImporterException {
        StudyImporterForRegistry importer = createImporter();
        final List<String> geoTerms = new ArrayList<String>();
        importer.setGeoNamesService(new GeoNamesService() {
            @Override
            public boolean hasTermForLocale(String locality) {
                return true;
            }

            @Override
            public LatLng findLatLng(String termsOrLocale) throws IOException {
                geoTerms.add(termsOrLocale);
                return new LatLng(10, 10);
            }
        });
        importStudy(importer);

        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        List<String> refs = new ArrayList<String>();
        List<DOI> DOIs = new ArrayList<>();
        List<String> externalIds = new ArrayList<String>();
        List<String> sources = new ArrayList<String>();
        for (Study study : allStudies) {
            DOIs.add(study.getDOI());
            externalIds.add(study.getExternalId());
            refs.add(study.getCitation());
            sources.add(study.getSource());
        }

        assertThat(refs, hasItem("Gittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1"));
        assertThat(DOIs, hasItem(new DOI("1007", "s13127-011-0039-1")));
        assertThat(DOIs, hasItem(new DOI("3354", "meps09511")));
        assertThat(DOIs, hasItem(new DOI("2307", "3544990")));
        assertThat(externalIds, hasItem("https://doi.org/10.2307/3544990"));
        assertThat(sources, hasItem(containsString("Accessed at")));
        assertThat(sources, hasItem(containsString("Miller")));
        assertThat(sources, hasItem(containsString("http://gomexsi.tamucc.edu")));
        assertThat(geoTerms, hasItem("GEONAMES:8556192"));

        assertThat(taxonIndex.findTaxonByName("Leptoconchus incycloseris"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Sandalolitha dentata"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Pterois volitans/miles"), is(notNullValue()));
    }


    @Test
    public void importWeidinger() throws StudyImporterException {
        StudyImporterForRegistry importer = createImporter();
        importer.importData("millerse/Weidinger-et-al.-2009");
        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        assertThat(allStudies.get(0).getSource(), containsString("Miller"));
        assertThat(taxonIndex.findTaxonByName("Garrulus glandarius"), is(notNullValue()));
    }

    @Test
    public void importDapstom() throws StudyImporterException {
        StudyImporterForRegistry importer = createImporter();
        importer.importData("millerse/Dapstrom-integrated-database-and-portal-for-fish-stomach-records");
        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size() > 0, is(true));
    }

    @Test
    public void importSmithsonian() throws StudyImporterException {
        StudyImporterForRegistry importer = createImporter();
        importer.importData("millerse/Smithsonian-Repository-Interactions");
        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        Map<String, String> citationDOIs = new TreeMap<String, String>();
        for (Study study : allStudies) {
            citationDOIs.put(study.getCitation(), study.getDOI().toString());
        }

        String citation = "Raboy, Becky E., and James M. Dietz. Diet, Foraging, and Use of Space in Wild Golden-headed Lion Tamarins. American Journal of Primatology, 63(1):, 2004, 1-15. Accessed April 20, 2015. http://hdl.handle.net/10088/4251.";
        assertThat(citationDOIs.keySet(), hasItem(citation));
        String doi = citationDOIs.get(citation);
        assertThat(doi, startsWith("doi:Raboy"));

        assertThat(allStudies.get(0).getSource(), containsString("Miller"));
        assertThat(taxonIndex.findTaxonByName("Leontopithecus chrysomelas"), is(notNullValue()));
    }

    @Test
    public void importSeltmann() throws StudyImporterException {
        StudyImporterForRegistry importer = createImporter();
        importer.importData("globalbioticinteractions/digital-bee-collections-network");
        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        assertThat(taxonIndex.findTaxonByName("Garrulus glandarius"), is(notNullValue()));
    }

    @Test
    public void importMillerSE() throws StudyImporterException {
        StudyImporterForRegistry importer = createImporter();
        importer.importData("millerse/Bird-Parasite");
        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        assertThat(taxonIndex.findTaxonByName("Bombycilla cedrorum"), is(notNullValue()));
    }

    @Test
    public void importMillerSECarribean() throws StudyImporterException {
        StudyImporterForRegistry importer = createImporter();
        importer.importData("millerse/Caribbean-food-web");
        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        assertThat(taxonIndex.findTaxonByName("Scarus coelestinus"), is(notNullValue()));
    }

    @Test
    public void importJSONLD() throws StudyImporterException {
        StudyImporterForRegistry importer = createImporter();
        importer.importData("globalbioticinteractions/jsonld-template-dataset");
        resolveNames();
        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        assertThat(taxonIndex.findTaxonById("NCBI:8782"), is(notNullValue()));
    }

    @Test
    public void importGoMexSI() throws StudyImporterException {
        StudyImporterForRegistry importer = createImporter();

        importer.importData("gomexsi/interaction-data");
        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        for (Study study : allStudies) {
            assertThat(study.getSource(), is(Matchers.notNullValue()));
        }
    }

    @Test
    public void importNHM() throws StudyImporterException {
        StudyImporterForRegistry importer = createImporter();
        importer.importData("globalbioticinteractions/natural-history-museum-london-interactions-bank");
        resolveNames();
        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(taxonIndex.findTaxonByName("Magnifera indica"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Hermagoras sigillatus"), is(notNullValue()));
        List<String> citations = new ArrayList<String>();
        List<DOI> dois = new ArrayList<>();
        for (Study study : allStudies) {
            assertThat(study.getSource(), is(Matchers.notNullValue()));
            assertThat(study.getCitation(), is(Matchers.notNullValue()));
            citations.add(study.getCitation());
            dois.add(study.getDOI());
        }

        assertThat(citations, hasItem("F.  Seow-Choen, A Taxonomic Guide to the Stick Insects of Borneo. Kota Kinabalu: Natural History Publications (Borneo), 2016."));
        assertThat(dois, hasItem(new DOI("2307", "3503496")));
    }

    @Test
    public void importREEM() throws StudyImporterException {
        StudyImporterForRegistry importer = createImporter();

        importer.importData("globalbioticinteractions/noaa-reem");
        List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(3));
        int specimenCount = 0;
        for (StudyNode study : allStudies) {
            specimenCount+= getSpecimenCount(study);
        }
        assertThat(specimenCount > 0, is(true));
    }

    @Test
    public void importHechingers() throws Exception {
        String datasetsUsingHechingerFormat[] = {"thieltges2011", "preston2012", "zander2011", "mouritsen2011"};

        int studyCount = 0;

        for (String dataset : datasetsUsingHechingerFormat) {
            try {
                StudyImporterForRegistry importer = createImporter();
                importer.importData(("globalbioticinteractions/" + dataset));
                List<StudyNode> allStudies = NodeUtil.findAllStudies(getGraphDb());
                assertThat(allStudies.size(), is(studyCount + 1));
                studyCount += 1;
            } catch (Exception e) {
                throw new Exception("failed to import [" + dataset + "]", e);
            }

        }
    }

    private StudyImporterForRegistry createImporter() {
        return new StudyImporterForRegistry(new ParserFactoryLocal(), nodeFactory, defaultFinder());
    }
}