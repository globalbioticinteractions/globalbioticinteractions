package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class DatasetRegistryProxyTest {

    @Test
    public void twoFinders() throws DatasetFinderException {
        DatasetRegistry finder = new DatasetRegistryProxy(Arrays.asList(
                new DatasetRegistryMock(Arrays.asList("one")),
                new DatasetRegistryMock(Arrays.asList("one", "two"))
        )
        );

        assertThat(finder.findNamespaces().size(), is(2));
        assertThat(finder.findNamespaces(), CoreMatchers.hasItem("one"));
        assertThat(finder.findNamespaces(), CoreMatchers.hasItem("two"));

        assertThat(finder.datasetFor("one").getNamespace(), is("one"));
        assertThat(finder.datasetFor("two").getNamespace(), is("one|two"));
    }

    @Test(expected = DatasetFinderException.class)
    public void oneFinderNoMatch() throws DatasetFinderException {
        DatasetRegistry finder = new DatasetRegistryProxy(Collections.singletonList(
                new DatasetRegistryMock(Collections.singletonList("one"))
        )
        );

        assertThat(finder.findNamespaces().size(), is(1));
        assertThat(finder.findNamespaces(), CoreMatchers.hasItem("one"));

        finder.datasetFor("foo");
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
            return new DatasetImpl(StringUtils.join(findNamespaces(), "|"), URI.create("http://example.com/" + findNamespaces().size()));
        }
    }
}