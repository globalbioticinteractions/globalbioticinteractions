package org.globalbioticinteractions.content;

import java.io.IOException;
import java.net.URI;

/**
 * ContentPin: Get local access to content.
 *
 * Inspired by http://pins.rstudio.com/ .
 */

public interface ContentPin {

    URI pin(URI knownContentIdentifier) throws IOException;

}
