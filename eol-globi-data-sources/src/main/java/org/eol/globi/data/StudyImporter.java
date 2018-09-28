package org.eol.globi.data;

import org.eol.globi.service.Dataset;
import org.eol.globi.service.GeoNamesService;

public interface StudyImporter {

    void importStudy() throws StudyImporterException;

    void setFilter(ImportFilter importFilter);

    void setLogger(ImportLogger importLogger);

    void setGeoNamesService(GeoNamesService geoNamesService);

    void setDataset(Dataset dataset);

}
