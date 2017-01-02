package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForFishbaseTest extends GraphDBTestCase {

    @Test
    public void importFewLines() throws StudyImporterException, NodeFactoryException, IOException {
        String aFewLines = "prey species code\tfood III\tfood item name\tfood item genus\tfood item species\tfood item of\tconsumer species code\tconsumer genus\tconsumer species\tauthor\tyear\ttitle\tlocality\tcountryCode\tlatitude\tlongitude\n" +
                "NULL\tdebris\t< 1 mm organic debris\tNULL\tNULL\tfood item of\t2\tOreochromis\tniloticus\tHickley, P. and R.G. Bailey\t1987\tFood and feeding relationships of fish in the Sudd swamps (River Nile, southern Sudan).\tSudd swamps, River Nile.\tSD\t13.8871414568\t30.0899425353\n" +
                "NULL\tdebris\t> 1 mm organic debris\tNULL\tNULL\tfood item of\t2\tOreochromis\tniloticus\tHickley, P. and R.G. Bailey\t1987\tFood and feeding relationships of fish in the Sudd swamps (River Nile, southern Sudan).\tSudd swamps, River Nile.\tSD\t13.8871414568\t30.0899425353\n" +
                "NULL\tn.a./others\tunidentified\tNULL\tNULL\tfood item of\t2\tOreochromis\tniloticus\tRainboth, W.J.1996\tFishes of the Cambodian Mekong.\tMekong.\tNULL\tNULL\tNULL\n" +
                "NULL\tn.a./others\tSpirillum\tNULL\tNULL\tfood item of\t2\tOreochromis\tniloticus\tTrewavas, E.\t1983\tTilapiine fishes of the genera <i>Sarotherodon</i>, <i>Oreochromis</i> and <i>Danakilia</i>.\tNULL\tNULL\tNULL\tNULL\n" +
                "NULL\tbenthic algae/weeds\tunidentified\tNULL\tNULL\tfood item of\t2\tOreochromis\tniloticus\tHickley, P. and R.G. Bailey\t1987\tFood and feeding relationships of fish in the Sudd swamps (River Nile, southern Sudan).\tSudd swamps, River Nile.\tSD\t13.8871414568\t30.0899425353\n" +
                "NULL\tbivalves\tModiolus sp.\r\\nodiolus sp.\tNULL\tNULL\tfood item of\t308\tGadus\tmacrocephalus\tJewett, S.C.\t1978\tSummer food of the pacific cod, <i>Gadus macrocephalus</i>, near Kodiak Island, Alaska.\tNULL\tNULL\tNULL\tNULL\n" +
                "1345\tbony fish\tPomatoschistus minutus\tPomatoschistus\tminutus\tfood item of\t29\tMerlangius\tmerlangus\tICES\t2012\tStomach DatasetImpl.\tNorth Sea\tGB\t52.8763053517\t-1.69182449421\n";

        StudyImporterForFishbase studyImporter = new StudyImporterForFishbase(new TestParserFactory(aFewLines), nodeFactory);

        studyImporter.importStudy(IOUtils.toInputStream(aFewLines));
        resolveNames();

        List<Study> studies = NodeUtil.findAllStudies(getGraphDb());
        List<String> sources = new ArrayList<String>();
        List<String> citations = new ArrayList<String>();
        for (Study study : studies) {
            sources.add(study.getSource());
            citations.add(study.getCitation());
        }

        assertThat(sources, hasItem("Database export shared by http://fishbase.org in December 2013. For use by Brian Hayden and Jorrit Poelen only."));
        assertThat(citations, hasItem("citation:doi:Hickley, P. and R.G. Bailey. 1987. Food and feeding relationships of fish in the Sudd swamps (River Nile, southern Sudan)."));
        assertThat(citations, hasItem("citation:doi:Rainboth, W.J.1996. Fishes of the Cambodian Mekong.. Mekong."));
        assertThat(citations, hasItem("citation:doi:Trewavas, E.. 1983. Tilapiine fishes of the genera <i>Sarotherodon</i>, <i>Oreochromis</i> and <i>Danakilia</i>."));

        assertThat(citations, hasItem("citation:doi:ICES. 2012. Stomach DatasetImpl."));

        assertThat(taxonIndex.findTaxonByName("n.a./others"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("benthic algae/weeds"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("bony fish"), is(nullValue()));
        assertThat(taxonIndex.findTaxonByName("debris"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Pomatoschistus minutus"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Oreochromis niloticus"), is(notNullValue()));

        assertThat(nodeFactory.findLocation(new LocationImpl(52.8763053517, -1.69182449421, null, null)), is(notNullValue()));
    }
}
