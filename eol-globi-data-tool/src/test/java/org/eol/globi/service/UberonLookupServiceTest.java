package org.eol.globi.service;

import org.eol.globi.domain.Term;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UberonLookupServiceTest {

    private UberonLookupService uberonLookupService = new UberonLookupService();

    @Test
    public void bodyPartMapping() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("scales");
        assertThat(scales.size(), is(1));
        assertThat(scales.get(0).getId(), is("UBERON:0002542"));
        assertThat(scales.get(0).getName(), is("scale"));
    }

    @Test
    public void physiologicalStateMapping() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("remains");
        assertThat(scales.size(), is(1));
        assertThat(scales.get(0).getId(), is("GLOBI:REMAINS"));
        assertThat(scales.get(0).getName(), is("REMAINS"));
    }

    @Test
    public void lifeStageMapping() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("newborn");
        assertThat(scales.size(), is(1));
        assertThat(scales.get(0).getId(), is("GLOBI:NEWBORN"));
        assertThat(scales.get(0).getName(), is("NEWBORN"));
    }

    @Test
    public void donaldDuck() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("donald duck");
        assertThat(scales.size(), is(1));
        assertThat(scales.get(0).getId(), is("no:match"));
        assertThat(scales.get(0).getName(), is("donald duck"));
    }

    @Test
    public void noHeader() throws TermLookupServiceException {
        // ensure that header is not used as value
        List<Term> scales = uberonLookupService.lookupTermByName("original_name");
        assertThat(scales.size(), is(1));
        assertThat(scales.get(0).getId(), is("no:match"));
        assertThat(scales.get(0).getName(), is("original_name"));
    }
}
