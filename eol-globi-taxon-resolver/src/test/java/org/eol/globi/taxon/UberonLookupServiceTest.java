package org.eol.globi.taxon;

import org.eol.globi.domain.Term;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.ResourceUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class UberonLookupServiceTest {

    private UberonLookupService uberonLookupService = new UberonLookupService(new ResourceService() {

        @Override
        public InputStream retrieve(URI resourceName) throws IOException {
            return ResourceUtil.asInputStream(resourceName, is -> is);
        }
    });

    @Test
    public void bodyPartMapping() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("scales");
        assertThat(scales.size(), Is.is(1));
        assertThat(scales.get(0).getId(), Is.is("UBERON:0002542"));
        assertThat(scales.get(0).getName(), Is.is("scale"));
    }

    @Test
    public void physiologicalStateMapping() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("remains");
        assertThat(scales.size(), Is.is(1));
        assertThat(scales.get(0).getId(), Is.is("GLOBI:REMAINS"));
        assertThat(scales.get(0).getName(), Is.is("REMAINS"));
    }

    @Test
    public void lifeStageMapping() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("newborn");
        assertThat(scales.size(), Is.is(1));
        assertThat(scales.get(0).getId(), Is.is("GLOBI:NEWBORN"));
        assertThat(scales.get(0).getName(), Is.is("NEWBORN"));
    }

    @Test
    public void lifeStageMapping2() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("NAUPLII");
        assertThat(scales.size(), Is.is(1));
        assertThat(scales.get(0).getId(), Is.is("UBERON:0014406"));
        assertThat(scales.get(0).getName(), Is.is("nauplius stage"));
    }

    @Test
    public void lifeStageMappingCapitalized() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("Nauplii");
        assertThat(scales.size(), Is.is(1));
        assertThat(scales.get(0).getId(), Is.is("UBERON:0014406"));
        assertThat(scales.get(0).getName(), Is.is("nauplius stage"));
    }

    @Test
    public void donaldDuck() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("donald duck");
        assertThat(scales.size(), Is.is(1));
        assertThat(scales.get(0).getId(), Is.is("no:match"));
        assertThat(scales.get(0).getName(), Is.is("donald duck"));
    }

    @Test
    public void noHeader() throws TermLookupServiceException {
        // ensure that header is not used as value
        List<Term> scales = uberonLookupService.lookupTermByName("original_name");
        assertThat(scales.size(), Is.is(1));
        assertThat(scales.get(0).getId(), Is.is("no:match"));
        assertThat(scales.get(0).getName(), Is.is("original_name"));
    }
}
