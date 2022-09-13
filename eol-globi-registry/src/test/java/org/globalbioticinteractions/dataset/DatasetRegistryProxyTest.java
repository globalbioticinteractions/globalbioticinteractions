package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetRegistryProxyTest {

    @Test
    public void twoFinders() throws DatasetRegistryException {
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

    @Test(expected = DatasetRegistryException.class)
    public void oneFinderNoMatch() throws DatasetRegistryException {
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
        public Collection<String> findNamespaces() throws DatasetRegistryException {
            return namespaces;
        }

        @Override
        public Dataset datasetFor(String namespace) throws DatasetRegistryException {
            if (!namespaces.contains(namespace)) {
                throw new DatasetRegistryException("no dataset for [" + namespace +"]");
            }
            return new DatasetWithResourceMapping(StringUtils.join(findNamespaces(), "|"), URI.create("http://example.com/" + findNamespaces().size()), new ResourceServiceLocalAndRemote(inStream -> inStream));
        }
    }
}