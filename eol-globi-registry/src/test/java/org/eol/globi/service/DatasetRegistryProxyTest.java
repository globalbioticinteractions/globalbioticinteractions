package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetFinderException;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;

public class DatasetRegistryProxyTest {

    @Test
    public void twoFinders() throws DatasetFinderException {
        DatasetRegistry registry = new DatasetRegistryProxy(Arrays.asList(
                new DatasetRegistryMock(Arrays.asList("one")),
                new DatasetRegistryMock(Arrays.asList("one", "two"))
        )
        );

        assertThat(registry.findNamespaces().size(), is(2));
        assertThat(registry.findNamespaces(), CoreMatchers.hasItem("one"));
        assertThat(registry.findNamespaces(), CoreMatchers.hasItem("two"));

        assertThat(registry.datasetFor("one").getNamespace(), is("one"));
        assertThat(registry.datasetFor("two").getNamespace(), is("one|two"));
    }

    @Test(expected = DatasetFinderException.class)
    public void oneFinderNoMatch() throws DatasetFinderException {
        DatasetRegistry registry = new DatasetRegistryProxy(Collections.singletonList(
                new DatasetRegistryMock(Collections.singletonList("one"))
        )
        );

        assertThat(registry.findNamespaces().size(), is(1));
        assertThat(registry.findNamespaces(), CoreMatchers.hasItem("one"));

        registry.datasetFor("foo");
    }



    private static class DatasetRegistryMock implements DatasetRegistry {

        private final List<String> namespaces;

        DatasetRegistryMock(List<String> namespaces) {
            this.namespaces = namespaces;
        }

        @Override
        public Collection<String> findNamespaces() throws DatasetFinderException {
            return namespaces;
        }

        @Override
        public Dataset datasetFor(String namespace) throws DatasetFinderException {
            if (!namespaces.contains(namespace)) {
                throw new DatasetFinderException("no dataset for [" + namespace +"]");
            }
            return new DatasetImpl(StringUtils.join(findNamespaces(), "|"), URI.create("http://example.com/" + findNamespaces().size()), inStream -> inStream);
        }
    }
}