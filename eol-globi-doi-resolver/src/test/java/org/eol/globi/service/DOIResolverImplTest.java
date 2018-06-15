package org.eol.globi.service;

import org.apache.commons.io.IOUtils;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class DOIResolverImplTest {

    @Test
    public void extractDOI() throws IOException, MalformedDOIException {
        String response = IOUtils.toString(getClass().getResourceAsStream("crossRefReply.json"));
        DOI doi = new DOIResolverImpl().extractDOI(response);
        assertThat(doi.toString(), is("10.1002/(sici)1098-2345(1997)42:1<1::aid-ajp1>3.0.co;2-0"));
    }

    @Test
    public void extractDOIBelowMinScore() throws IOException, MalformedDOIException {
        String response = IOUtils.toString(getClass().getResourceAsStream("crossRefReply.json"));
        DOIResolverImpl doiResolver = new DOIResolverImpl();
        doiResolver.setMinMatchScore(200);
        DOI doi = doiResolver.extractDOI(response);
        assertThat(doi, is(nullValue()));
    }

    @Test
    public void extractDOILowScore() throws IOException, MalformedDOIException {
        String response = IOUtils.toString(getClass().getResourceAsStream("crossRefReplyLowScore.json"));
        assertNull(new DOIResolverImpl().extractDOI(response));
    }

}