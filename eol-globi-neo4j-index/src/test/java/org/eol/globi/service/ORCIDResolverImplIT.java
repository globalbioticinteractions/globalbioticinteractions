package org.eol.globi.service;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ORCIDResolverImplIT {

    @Test
    public void lookupName() throws IOException {
        ORCIDResolverImpl orcidResolver = new ORCIDResolverImpl();
        orcidResolver.setBaseUrl("https://pub.sandbox.orcid.org/v2.0/");
        String name = orcidResolver.findFullName("http://orcid.org/0000-0002-2389-8429");
        assertThat(name, is("Sofia Hernandez"));
    }

    @Test
    public void lookupNameProductionFailsOnSandbox() throws IOException {
        String fullName = new ORCIDResolverImpl().findFullName("http://orcid.org/0000-0002-6601-2165");
        assertThat(fullName, is("Christopher Mungall"));
    }

    @Test
    public void lookupNameProductionFailsOnSandboxHttps() throws IOException {
        String fullName = new ORCIDResolverImpl().findFullName("https://orcid.org/0000-0002-6601-2165");
        assertThat(fullName, is("Christopher Mungall"));
    }

}
