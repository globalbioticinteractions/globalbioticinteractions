package org.eol.globi.service;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UberonLookupServiceTest {

    private UberonLookupService uberonLookupService = new UberonLookupService();

    @Test
    public void bodyPartMapping() throws EnvoServiceException {
        List<EnvoTerm> scales = uberonLookupService.lookupTermByName("scales");
        assertThat(scales.size(), is(1));
        assertThat(scales.get(0).getId(), is("GLOBI:SCALES"));
        assertThat(scales.get(0).getName(), is("SCALES"));
    }

    @Test
    public void physiologicalStateMapping() throws EnvoServiceException {
        List<EnvoTerm> scales = uberonLookupService.lookupTermByName("remains");
        assertThat(scales.size(), is(1));
        assertThat(scales.get(0).getId(), is("GLOBI:REMAINS"));
        assertThat(scales.get(0).getName(), is("REMAINS"));
    }

    @Test
    public void lifeStageMapping() throws EnvoServiceException {
        List<EnvoTerm> scales = uberonLookupService.lookupTermByName("newborn");
        assertThat(scales.size(), is(1));
        assertThat(scales.get(0).getId(), is("GLOBI:NEWBORN"));
        assertThat(scales.get(0).getName(), is("NEWBORN"));
    }
}
