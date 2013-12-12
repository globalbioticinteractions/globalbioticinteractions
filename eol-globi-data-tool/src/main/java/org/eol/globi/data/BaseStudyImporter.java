package org.eol.globi.data;

import org.eol.globi.domain.Study;

public abstract class BaseStudyImporter extends BaseImporter implements StudyImporter {
    protected ParserFactory parserFactory;
    protected ImportFilter importFilter = new ImportFilter() {
        @Override
        public boolean shouldImportRecord(Long recordNumber) {
            return true;
        }
    };
    private ImportLogger importLogger = new ImportLogger() {
        @Override
        public void warn(Study study, String message) {

        }

        @Override
        public void info(Study study, String message) {

        }

        @Override
        public void severe(Study study, String message) {

        }
    };

    public BaseStudyImporter(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(nodeFactory);
        this.parserFactory = parserFactory;
    }

    @Override
    public void setFilter(ImportFilter importFilter) {
        this.importFilter = importFilter;
    }

    @Override
    public void setLogger(ImportLogger importLogger) {
        this.importLogger = importLogger;
    }

    public ImportLogger getLogger() {
        return this.importLogger;
    }
}
