package org.globalbioticinteractions.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface ContentSource {

    Optional<InputStream> getContent() throws IOException;
}
