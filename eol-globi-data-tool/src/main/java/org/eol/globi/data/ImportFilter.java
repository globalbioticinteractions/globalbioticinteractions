package org.eol.globi.data;

interface ImportFilter {
    boolean shouldImportRecord(Long recordNumber);
}
