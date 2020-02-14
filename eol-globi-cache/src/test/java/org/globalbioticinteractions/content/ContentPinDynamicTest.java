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
        when(resolver.query(any())).thenReturn(Stream.of(prov));


        ContentSource source = Mockito.mock(ContentSource.class);
        when(source.getContent())
                .thenReturn(Optional.of(IOUtils.toInputStream("hallo", StandardCharsets.UTF_8)));

        ContentStore store = Mockito.mock(ContentStore.class);
        when(store.retrieve(URI.create("hash://sha256/someSha")))
                .thenReturn(source);

        ContentPin contentPin = new ContentPinDynamic(resolver, store);

        URI pin = contentPin.pin(URI.create("some:uri"));
        assertThat(pin, is(URI.create("uri:local")));
    }

    public ContentProvenance getProv() {
        return new ContentProvenance("namespace", URI.create("uri:source"), URI.create("uri:local"), "someSha", "someDate");
    }

    @Test
    public void unknownPin() throws IOException {
        ContentResolver resolver = Mockito.mock(ContentResolver.class);
        when(resolver.query(any())).thenReturn(Stream.empty());

        ContentStore store = Mockito.mock(ContentStore.class);
        when(store.store(URI.create("unknown:uri")))
                .thenReturn(getProv());

        ContentSource source = Mockito.mock(ContentSource.class);
        when(source.getContent())
                .thenReturn(Optional.of(new ByteArrayInputStream("0".getBytes())));


        when(store.retrieve(URI.create("hash://sha256/someSha")))
                .thenReturn(source);

        ContentPin contentPin = new ContentPinDynamic(resolver, store);
        URI pin = contentPin.pin(URI.create("unknown:uri"));
        assertThat(pin, is(URI.create("uri:local")));
    }

    @Test(expected = IOException.class)
    public void unknownPinFailedProvideAndRegister() throws IOException {
        ContentResolver resolver = Mockito.mock(ContentResolver.class);
        when(resolver.query(any())).thenReturn(Stream.empty());

        ContentStore store = Mockito.mock(ContentStore.class);
        when(store.store(URI.create("unknown:uri")))
                .thenThrow(new IOException("kaboom!"));

        ContentSource source = Mockito.mock(ContentSource.class);
        when(source.getContent())
                .thenReturn(Optional.of(new ByteArrayInputStream("0".getBytes())));


        when(store.retrieve(URI.create("hash://sha256/someSha")))
                .thenReturn(source);



        ContentPin contentPin = new ContentPinDynamic(resolver, store);
        try {
            contentPin.pin(URI.create("unknown:uri"));
        } catch(IOException ex) {
            assertThat(ex.getMessage(), is("kaboom!"));
            throw ex;
        }

    }

}