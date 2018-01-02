package org.eol.globi.taxon;

import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.TermLookupServiceException;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class UberonLookupServiceTest {

    private UberonLookupService uberonLookupService = new UberonLookupService();

    @Test
    public void bodyPartMapping() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("scales");
        Assert.assertThat(scales.size(), Is.is(1));
        Assert.assertThat(scales.get(0).getId(), Is.is("UBERON:0002542"));
        Assert.assertThat(scales.get(0).getName(), Is.is("scale"));
    }

    @Test
    public void physiologicalStateMapping() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("remains");
        Assert.assertThat(scales.size(), Is.is(1));
        Assert.assertThat(scales.get(0).getId(), Is.is("GLOBI:REMAINS"));
        Assert.assertThat(scales.get(0).getName(), Is.is("REMAINS"));
    }

    @Test
    public void lifeStageMapping() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("newborn");
        Assert.assertThat(scales.size(), Is.is(1));
        Assert.assertThat(scales.get(0).getId(), Is.is("GLOBI:NEWBORN"));
        Assert.assertThat(scales.get(0).getName(), Is.is("NEWBORN"));
    }

    @Test
    public void donaldDuck() throws TermLookupServiceException {
        List<Term> scales = uberonLookupService.lookupTermByName("donald duck");
        Assert.assertThat(scales.size(), Is.is(1));
        Assert.assertThat(scales.get(0).getId(), Is.is("no:match"));
        Assert.assertThat(scales.get(0).getName(), Is.is("donald duck"));
    }

    @Test
    public void noHeader() throws TermLookupServiceException {
        // ensure that header is not used as value
        List<Term> scales = uberonLookupService.lookupTermByName("original_name");
        Assert.assertThat(scales.size(), Is.is(1));
        Assert.assertThat(scales.get(0).getId(), Is.is("no:match"));
        Assert.assertThat(scales.get(0).getName(), Is.is("original_name"));
    }
}
