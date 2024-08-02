package org.eol.globi.data;

import org.globalbioticinteractions.dataset.Dataset;
import org.eol.globi.service.GeoNamesService;

import java.io.File;

public interface DatasetImporter {

    void importStudy() throws StudyImporterException;

    void setFilter(ImportFilter importFilter);

    void setLogger(ImportLogger importLogger);

    void setGeoNamesService(GeoNamesService geoNamesService);

    void setDataset(Dataset dataset);

    void setWorkDir(File workDir);

    File getWorkDir();

}
