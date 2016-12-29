package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.GeoNamesServiceImpl;

import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.*;

public abstract class BaseStudyImporter extends BaseImporter implements StudyImporter {
    protected ParserFactory parserFactory;
    protected ImportFilter importFilter = new ImportFilter() {
        @Override
        public boolean shouldImportRecord(Long recordNumber) {
            return true;
        }
    };

    private Dataset dataset;

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

    protected ImportFilter getFilter() {
        return this.importFilter;
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

    public String getSourceCitation() {
        return getDataset() == null ? null : getDataset().getCitation();
    }

    public String getSourceDOI() {
        return dataset == null ? null : dataset.getDOI();
    }

    protected void setBasisOfRecordAsLiterature(Specimen specimen) throws NodeFactoryException {
        specimen.setBasisOfRecord(nodeFactory.getOrCreateBasisOfRecord("http://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/BasisOfRecord.html#LITERATURE", "Literature"));
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Dataset getDataset() {
        return dataset;
    }
}
