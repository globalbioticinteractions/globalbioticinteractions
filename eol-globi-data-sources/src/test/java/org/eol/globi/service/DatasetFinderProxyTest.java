package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class DatasetFinderProxyTest {

    @Test
    public void twoFinders() throws DatasetFinderException {
        DatasetFinder finder = new DatasetFinderProxy(Arrays.asList(
                new DatasetFinderMock(Arrays.asList("one")),
                new DatasetFinderMock(Arrays.asList("one", "two"))
        )
        );

        assertThat(finder.findNamespaces().size(), is(2));
        assertThat(finder.findNamespaces(), hasItem("one"));
        assertThat(finder.findNamespaces(), hasItem("two"));

        assertThat(finder.datasetFor("one").getNamespace(), is("one"));
        assertThat(finder.datasetFor("two").getNamespace(), is("one|two"));
    }

    @Test(expected = DatasetFinderException.class)
    public void oneFinderNoMatch() throws DatasetFinderException {
        DatasetFinder finder = new DatasetFinderProxy(Collections.singletonList(
                new DatasetFinderMock(Collections.singletonList("one"))
        )
        );

        assertThat(finder.findNamespaces().size(), is(1));
        assertThat(finder.findNamespaces(), hasItem("one"));

        finder.datasetFor("foo");
    }



    private static class DatasetFinderMock implements DatasetFinder {

        private final List<String> namespaces;

        DatasetFinderMock(List<String> namespaces) {
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