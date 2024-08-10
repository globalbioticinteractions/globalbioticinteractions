package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.GeoNamesServiceImpl;
import org.eol.globi.tool.NullImportLogger;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.doi.DOI;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

public abstract class BaseDatasetImporter implements DatasetImporter {
    protected ImportFilter importFilter = recordNumber -> true;
    private AtomicLong currentLine = null;
    private URI currentResource = null;

    private Dataset dataset;

    private GeoNamesService geoNamesService = new GeoNamesServiceImpl(new ResourceServiceLocal(new InputStreamFactoryNoop()));

    private ImportLogger importLogger = new NullImportLogger();
    private String sourceCitationLastAccessed;
    private File workDir;

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

    public ImportLogger getLogger() {
        return this.importLogger;
    }

    @Override
    public void setGeoNamesService(GeoNamesService geoNamesService) {
        this.geoNamesService = geoNamesService;
    }

    public GeoNamesService getGeoNamesService() {
        return geoNamesService;
    }

    public String getSourceCitation() {
        return getDataset() == null ? null : getDataset().getCitation();
    }

    public DOI getSourceDOI() {
        return dataset == null ? null : dataset.getDOI();
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Dataset getDataset() {
        return dataset;
    }

    String getSourceCitationLastAccessed() {
        if (StringUtils.isBlank(sourceCitationLastAccessed)) {
            sourceCitationLastAccessed = CitationUtil.sourceCitationLastAccessed(getDataset());
        }
        return sourceCitationLastAccessed;
    }

    @Override
    public void setWorkDir(File workDir) {
        this.workDir = workDir;
    }

    @Override
    public File getWorkDir() {
        return createParentDirsIfNeeded(workDir == null
                ? Paths.get(".").toFile()
                : workDir);
    }

    private static File createParentDirsIfNeeded(File file) {
        file.mkdirs();
        return file;
    }


    protected void setCurrentResource(URI currentResource) {
        this.currentResource = currentResource;
    }

    protected URI getCurrentResource() {
        return this.currentResource;
    }

    protected void setCurrentLine(long currentLine) {
        if (this.currentLine == null) {
            this.currentLine = new AtomicLong();
        }
        this.currentLine.set(currentLine);
    }

    protected long getCurrentLine() {
        return this.currentLine.get();
    }

    protected String createMsg(String message) {
        StringBuilder builder = new StringBuilder();
        if (currentLine != null) {
            builder.append("[");
            if (getCurrentResource() != null) {
                builder.append(getCurrentResource());
            }
            builder.append(":");
            builder.append(getCurrentLine());
            builder.append("] ");
        }
        return builder.append(message).toString();
    }

}
