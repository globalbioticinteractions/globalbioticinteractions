package org.globalbioticinteractions.content;

import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.cache.ContentProvenance;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This pin dynamically resolves remote URIs if content is not available locally.
 */

public class ContentPinDynamic extends ContentPinStatic {

    ContentPinDynamic(ContentResolver resolver, ContentStore store, InputStreamFactory factory) {
        super(resolver, store, factory);
    }

    @Override
    public Stream<ContentProvenance> doQuery(URI knownContentIdentifier) throws IOException {
        Stream<ContentProvenance> resolve = super.doQuery(knownContentIdentifier);
        return Stream.concat(resolve,
                Stream.of(getStore().store(new ContentSource() {
                    @Override
                    public Optional<InputStream> getContent() throws IOException {
                        return Optional.of(ResourceUtil.asInputStream(knownContentIdentifier, getInputStreamFactory()));
                    }
                })));
    }
}
