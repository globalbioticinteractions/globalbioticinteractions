package org.eol.globi.service;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ORCIDResolverImplIT {

    @Test
    public void lookupName() throws IOException {
        ORCIDResolverImpl orcidResolver = new ORCIDResolverImpl();
        orcidResolver.setBaseUrl("http://pub.sandbox.orcid.org/v1.2/");
        String name = orcidResolver.findFullName("http://orcid.org/0000-0002-2389-8429");
        assertThat(name, is("Sofia Hernandez"));
    }

    @Test
    public void lookupNameProductionFailsOnSandbox() throws IOException {
        String fullName = new ORCIDResolverImpl().findFullName("http://orcid.org/0000-0002-6601-2165");
        assertThat(fullName, is("Christopher Mungall"));
    }

}
