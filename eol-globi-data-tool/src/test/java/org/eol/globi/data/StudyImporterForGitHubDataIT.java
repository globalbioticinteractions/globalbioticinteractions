package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForGitHubDataIT extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporter importer = new StudyImporterForGitHubData(new ParserFactoryImpl(), nodeFactory);
        importer.importStudy();

        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        List<String> refs = new ArrayList<String>();
        List<String> DOIs = new ArrayList<String>();
        List<String> sources = new ArrayList<String>();
        for (Study study : allStudies) {
            DOIs.add(study.getDOI());
            refs.add(study.getCitation());
            sources.add(study.getSource());
        }

        assertThat(refs, hasItem("Gittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1"));
        assertThat(DOIs, hasItem("doi:10.1007/s13127-011-0039-1"));
        assertThat(DOIs, hasItem("doi:10.3354/meps09511"));
        assertThat(sources, hasItem(containsString("Accessed at")));
        assertThat(sources, hasItem(containsString("Miller")));
        assertThat(sources, hasItem(containsString("http://gomexsi.tamucc.edu")));

        assertThat(nodeFactory.findTaxonByName("Leptoconchus incycloseris"), is(notNullValue()));
        assertThat(nodeFactory.findTaxonByName("Sandalolitha dentata"), is(notNullValue()));
        assertThat(nodeFactory.findTaxonByName("Pterois volitans/miles"), is(notNullValue()));


    }

}