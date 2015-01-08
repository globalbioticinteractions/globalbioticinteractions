package org.eol.globi.data;

public interface ImportFilter {
    boolean shouldImportRecord(Long recordNumber);
}
