package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.util.NodeUtil;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForGitHubDataIT extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporterForGitHubData importer = new StudyImporterForGitHubData(new ParserFactoryImpl(), nodeFactory);
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
        importer.importStudy();

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        List<String> refs = new ArrayList<String>();
        List<String> DOIs = new ArrayList<String>();
        List<String> externalIds = new ArrayList<String>();
        List<String> sources = new ArrayList<String>();
        for (Study study : allStudies) {
            DOIs.add(study.getDOI());
            externalIds.add(study.getExternalId());
            refs.add(study.getCitation());
            sources.add(study.getSource());
        }

        assertThat(refs, hasItem("Gittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1"));
        assertThat(DOIs, hasItem("doi:10.1007/s13127-011-0039-1"));
        assertThat(DOIs, hasItem("doi:10.3354/meps09511"));
        assertThat(DOIs, hasItem("doi:10.2307/3544990"));
        assertThat(externalIds, hasItem("http://dx.doi.org/10.2307/3544990"));
        assertThat(sources, hasItem(containsString("Accessed at")));
        assertThat(sources, hasItem(containsString("Miller")));
        assertThat(sources, hasItem(containsString("http://gomexsi.tamucc.edu")));
        assertThat(geoTerms, hasItem("GEONAMES:8556192"));

        assertThat(nodeFactory.findTaxonByName("Leptoconchus incycloseris"), is(notNullValue()));
        assertThat(nodeFactory.findTaxonByName("Sandalolitha dentata"), is(notNullValue()));
        assertThat(nodeFactory.findTaxonByName("Pterois volitans/miles"), is(notNullValue()));

    }

    @Test
    public void importWeidinger() throws StudyImporterException, NodeFactoryException {
        StudyImporterForGitHubData importer = new StudyImporterForGitHubData(new ParserFactoryImpl(), nodeFactory);
        importer.importData("millerse/Weidinger-et-al.-2009");
        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        assertThat(allStudies.get(0).getSource(), containsString("Miller"));
        assertThat(nodeFactory.findTaxonByName("Garrulus glandarius"), is(notNullValue()));
    }

    @Test
    public void importSeltmann() throws StudyImporterException, NodeFactoryException {
        StudyImporterForGitHubData importer = new StudyImporterForGitHubData(new ParserFactoryImpl(), nodeFactory);
        importer.importData("globalbioticinteractions/digital-bee-collections-network");
        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        assertThat(nodeFactory.findTaxonByName("Garrulus glandarius"), is(notNullValue()));
    }

    @Test
    public void importMillerSE() throws StudyImporterException, NodeFactoryException {
        StudyImporterForGitHubData importer = new StudyImporterForGitHubData(new ParserFactoryImpl(), nodeFactory);
        importer.importData("millerse/Bird-Parasite");
        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        assertThat(nodeFactory.findTaxonByName("Bombycilla cedrorum"), is(notNullValue()));
    }

    @Test
    public void importMillerSECarribean() throws StudyImporterException, NodeFactoryException {
        StudyImporterForGitHubData importer = new StudyImporterForGitHubData(new ParserFactoryImpl(), nodeFactory);
        importer.importData("millerse/Caribbean-food-web");
        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        assertThat(nodeFactory.findTaxonByName("Scarus coelestinus"), is(notNullValue()));
    }

    @Test
    public void importJSONLD() throws StudyImporterException, NodeFactoryException {
        StudyImporterForGitHubData importer = new StudyImporterForGitHubData(new ParserFactoryImpl(), nodeFactory);
        importer.importData("globalbioticinteractions/jsonld-template-dataset");
        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));
        assertThat(nodeFactory.findTaxonById("NCBI:8782"), is(notNullValue()));
    }

    @Test
    public void importGoMexSI() throws StudyImporterException, NodeFactoryException {
        StudyImporterForGitHubData importer = new StudyImporterForGitHubData(new ParserFactoryImpl(), nodeFactory);

        importer.importData("gomexsi/interaction-data");
        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        for (Study study : allStudies) {
            assertThat(study.getSource(), is(Matchers.notNullValue()));
        }
    }

    @Test
    public void importHechingers() throws Exception {
        String datasetsUsingHechingerFormat[] = {"thieltges2011", "preston2012", "zander2011", "mouritsen2011"};

        int studyCount = 0;

        for (String dataset : datasetsUsingHechingerFormat) {
            try {
                StudyImporterForGitHubData importer = new StudyImporterForGitHubData(new ParserFactoryImpl(), nodeFactory);
                importer.importData("globalbioticinteractions/" + dataset);
                List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
                assertThat(allStudies.size(), is(studyCount + 1));
                studyCount += 1;
            } catch (Exception e) {
                throw new Exception("failed to import [" + dataset + "]", e);
            }

        }

    }

}