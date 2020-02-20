package org.globalbioticinteractions.cache;

import org.eol.globi.service.ResourceService;

import java.net.URI;

public interface Cache extends ResourceService {
    ContentProvenance provenanceOf(URI resourceURI);
}
