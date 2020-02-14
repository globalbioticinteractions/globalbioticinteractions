package org.globalbioticinteractions.content;

import org.apache.commons.io.IOUtils;
import org.globalbioticinteractions.cache.ContentProvenance;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class ContentPinStaticTest {

    @Test
    public void knownPin() throws IOException {
        ContentResolver resolver = Mockito.mock(ContentResolver.class);
        ContentProvenance prov = new ContentProvenance("namespace",
                URI.create("uri:source"),
                URI.create("uri:local"),
                "someSha",
                "someDate");
        when(resolver.query(any())).thenReturn(Stream.of(prov));

        ContentStore store = Mockito.mock(ContentStore.class);

        when(store.retrieve(URI.create("hash://sha256/someSha")))
                .thenReturn(Optional.of(IOUtils.toInputStream("hallo", StandardCharsets.UTF_8)));

        ContentPinStatic contentPinStatic = new ContentPinStatic(resolver, store);

        URI pin = contentPinStatic.pin(URI.create("some:uri"));
        assertThat(pin, is(URI.create("uri:local")));
    }

    @Test(expected = IOException.class)
    public void unknownPin() throws IOException {
        ContentResolver resolver = Mockito.mock(ContentResolver.class);
        when(resolver.query(any())).thenReturn(Stream.empty());

        ContentStore store = Mockito.mock(ContentStore.class);
        when(store.retrieve(any())).thenThrow(new IOException("kaboom!"));

        ContentPinStatic contentPinStatic = new ContentPinStatic(resolver, store);
        try {
            contentPinStatic.pin(URI.create("unknown:uri"));
        } catch(IOException ex) {
            assertThat(ex.getMessage(), is("failed to pin [unknown:uri]"));
            throw ex;
        }
    }

    @Test(expected = IOException.class)
    public void uriKnownInRegistryButUnknownInStore() throws IOException {
        ContentResolver resolver = Mockito.mock(ContentResolver.class);
        ContentProvenance prov = new ContentProvenance(
                "namespace",
                URI.create("uri:source"),
                URI.create("uri:local"),
                "someSha",
                "someDate");

        when(resolver.query(any())).thenReturn(Stream.of(prov));

        ContentStore store = Mockito.mock(ContentStore.class);
        when(store.retrieve(URI.create("hash://sha256/someSha")))
                .thenReturn(Optional.empty());

        ContentPinStatic contentPinStatic = new ContentPinStatic(resolver, store);
        try {
            contentPinStatic.pin(URI.create("unknown:uri"));
        } catch(IOException ex) {
            assertThat(ex.getMessage(), is("failed to pin [unknown:uri]: failed to locate last known content uri [hash://sha256/someSha]"));
            throw ex;
        }
    }

}