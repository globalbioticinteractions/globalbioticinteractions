package org.globalbioticinteractions.cache;

import java.io.File;
import java.io.IOException;

public interface LineReaderFactory {

    LineReader createLineReader(File file) throws IOException;
}
