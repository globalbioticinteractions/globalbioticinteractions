package org.trophic.graph.data;

public abstract class BaseStudyImporter extends BaseImporter implements StudyImporter {
    protected ParserFactory parserFactory;
    protected ImportFilter importFilter = new ImportFilter() {
        @Override
        public boolean shouldImportRecord(Long recordNumber) {
            return true;
        }
    };

    public BaseStudyImporter(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(nodeFactory);
        this.parserFactory = parserFactory;
    }

    @Override
    public void setImportFilter(ImportFilter importFilter) {
        this.importFilter = importFilter;
    }
}
