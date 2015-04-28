package org.eol.globi.data;

import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.GeoNamesServiceImpl;

public abstract class BaseStudyImporter extends BaseImporter implements StudyImporter {
    protected ParserFactory parserFactory;
    protected ImportFilter importFilter = new ImportFilter() {
        @Override
        public boolean shouldImportRecord(Long recordNumber) {
            return true;
        }
    };
    protected String sourceCitation;

    protected String sourceDOI;

    private GeoNamesService geoNamesService = new GeoNamesServiceImpl();

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

    @Override
    public boolean shouldCrossCheckReference() {
        return true;
    }

    public ImportLogger getLogger() {
        return this.importLogger;
    }

    public void setGeoNamesService(GeoNamesService geoNamesService) {
        this.geoNamesService = geoNamesService;
    }

    public GeoNamesService getGeoNamesService() {
        return geoNamesService;
    }

    public void setSourceCitation(String sourceCitation) {
        this.sourceCitation = sourceCitation;
    }

    public String getSourceCitation() {
        return sourceCitation;
    }

    public String getSourceDOI() {
        return sourceDOI;
    }

    public void setSourceDOI(String sourceDOI) {
        this.sourceDOI = sourceDOI;
    }

    protected void setBasisOfRecordAsLiterature(Specimen specimen) throws NodeFactoryException {
        specimen.setBasisOfRecord(nodeFactory.getOrCreateBasisOfRecord("http://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/BasisOfRecord.html#LITERATURE", "Literature"));
    }
}
