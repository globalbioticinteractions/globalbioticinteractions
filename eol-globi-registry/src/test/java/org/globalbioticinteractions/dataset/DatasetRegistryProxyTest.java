package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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


        List<String> namespaces = new ArrayList<>();
        registry.findNamespaces().forEach(namespaces::add);

        assertThat(namespaces.size(), is(2));
        assertThat(namespaces, CoreMatchers.hasItem("one"));
        assertThat(namespaces, CoreMatchers.hasItem("two"));

        assertThat(registry.datasetFor("one").getNamespace(), is("one"));
        assertThat(registry.datasetFor("two").getNamespace(), is("one|two"));
    }

    @Test(expected = DatasetRegistryException.class)
    public void oneFinderNoMatch() throws DatasetRegistryException {
        DatasetRegistry registry = new DatasetRegistryProxy(Collections.singletonList(
                new DatasetRegistryMock(Collections.singletonList("one"))
        )
        );

        List<String> namespaces = new ArrayList<>();
        registry.findNamespaces().forEach(namespaces::add);

        assertThat(namespaces.size(), is(1));
        assertThat(namespaces, CoreMatchers.hasItem("one"));

        registry.datasetFor("foo");
    }



    private static class DatasetRegistryMock implements DatasetRegistry {

        private final List<String> namespaces;

        DatasetRegistryMock(List<String> namespaces) {
            this.namespaces = namespaces;
        }

        @Override
        public Iterable<String> findNamespaces() throws DatasetRegistryException {
            return namespaces;
        }

        @Override
        public void findNamespaces(Consumer<String> namespaceConsumer) throws DatasetRegistryException {
            for (String namespace : findNamespaces()) {
                namespaceConsumer.accept(namespace);
            }
        }


        @Override
        public Dataset datasetFor(String namespace) throws DatasetRegistryException {
            if (!namespaces.contains(namespace)) {
                throw new DatasetRegistryException("no dataset for [" + namespace +"]");
            }
            return new DatasetWithResourceMapping(StringUtils.join(findNamespaces(), "|"), URI.create("http://example.com/" + namespaces.size()), getResourceService());
        }
    }

    private static ResourceService getResourceService() {
        return new ResourceServiceLocal(new InputStreamFactoryNoop());
    }
}