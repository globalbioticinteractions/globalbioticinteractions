package org.trophic.graph.data;

interface ImportFilter {

    boolean shouldImportRecord(Long recordNumber);
}
