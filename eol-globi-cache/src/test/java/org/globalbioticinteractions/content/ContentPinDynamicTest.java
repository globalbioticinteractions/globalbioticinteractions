package org.globalbioticinteractions.content;

import org.apache.commons.io.IOUtils;
import org.globalbioticinteractions.cache.ContentProvenance;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class ContentPinDynamicTest {

    @Test
    public void knownPin() throws IOException {
        ContentResolver resolver = Mockito.mock(ContentResolver.class);
        ContentProvenance prov = getProv();
        when(resolver.resolve(any())).thenReturn(Stream.of(prov));

        ContentStore store = Mockito.mock(ContentStore.class);

        when(store.retrieve(URI.create("hash://sha256/someSha")))
                .thenReturn(Optional.of(IOUtils.toInputStream("hallo", StandardCharsets.UTF_8)));

        ContentPin contentPin = new ContentPinDynamic(resolver, store);

        URI pin = contentPin.pin(URI.create("some:uri"));
        assertThat(pin, is(URI.create("hash://sha256/someSha")));
    }

    public ContentProvenance getProv() {
        return new ContentProvenance("namespace", URI.create("uri:source"), URI.create("uri:local"), "someSha", "someDate");
    }

    @Test
    public void unknownPin() throws IOException {
        ContentResolver resolver = Mockito.mock(ContentResolver.class);
        when(resolver.resolve(any())).thenReturn(Stream.empty());

        ContentStore store = Mockito.mock(ContentStore.class);
        when(store.provideAndRegister(URI.create("unknown:uri")))
                .thenReturn(getProv());

        when(store.retrieve(URI.create("hash://sha256/someSha")))
                .thenReturn(Optional.of(new ByteArrayInputStream("0".getBytes())));

        ContentPin contentPin = new ContentPinDynamic(resolver, store);
        URI pin = contentPin.pin(URI.create("unknown:uri"));
        assertThat(pin, is(URI.create("hash://sha256/someSha")));
    }

    @Test(expected = IOException.class)
    public void unknownPinFailedProvideAndRegister() throws IOException {
        ContentResolver resolver = Mockito.mock(ContentResolver.class);
        when(resolver.resolve(any())).thenReturn(Stream.empty());

        ContentStore store = Mockito.mock(ContentStore.class);
        when(store.provideAndRegister(URI.create("unknown:uri")))
                .thenThrow(new IOException("kaboom!"));

        when(store.retrieve(URI.create("hash://sha256/someSha")))
                .thenReturn(Optional.of(new ByteArrayInputStream("0".getBytes())));

        ContentPin contentPin = new ContentPinDynamic(resolver, store);
        try {
            contentPin.pin(URI.create("unknown:uri"));
        } catch(IOException ex) {
            assertThat(ex.getMessage(), is("kaboom!"));
            throw ex;
        }

    }

}