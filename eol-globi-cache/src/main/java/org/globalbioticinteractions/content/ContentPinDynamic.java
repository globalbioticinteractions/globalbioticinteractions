package org.globalbioticinteractions.content;

import org.globalbioticinteractions.cache.ContentProvenance;

import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

/**
 * This pin dynamically resolves remote URIs if content is not available locally.
 */

public class ContentPinDynamic extends ContentPinStatic {

    ContentPinDynamic(ContentResolver resolver, ContentStore store) {
        super(resolver, store);
    }

    @Override
    public Stream<ContentProvenance> doResolve(URI knownContentIdentifier) throws IOException {
        Stream<ContentProvenance> resolve = super.doResolve(knownContentIdentifier);
        return Stream.concat(resolve,
                Stream.of(getStore().store(knownContentIdentifier)));
    }
}
