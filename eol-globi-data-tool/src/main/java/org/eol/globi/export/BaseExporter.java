package org.eol.globi.export;

import java.io.IOException;
import java.io.Writer;

public abstract class BaseExporter implements StudyExporter {


    protected abstract String getMetaTablePrefix();

    protected abstract String getMetaTableSuffix();

    @Override
    public void exportDarwinCoreMetaTable(Writer writer, String filename) throws IOException {
        writer.write(getMetaTablePrefix());
        writer.write(filename);
        writer.write(getMetaTableSuffix());

    }
}
