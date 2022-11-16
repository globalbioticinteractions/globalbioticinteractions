package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.ResourceServiceLocal;

import java.io.File;

public class CmdInterpretTaxa implements Cmd {

    private final GraphServiceFactory graphServiceFactory;
    private final File cacheDir;
    private String taxonCachePath;
    private String taxonMapPath;

    public CmdInterpretTaxa(GraphServiceFactory graphServiceFactory,
                            String taxonCachePath,
                            String taxonMapPath,
                            File cacheDir) {
        this.graphServiceFactory = graphServiceFactory;
        this.taxonCachePath = taxonCachePath;
        this.taxonMapPath = taxonMapPath;
        this.cacheDir = cacheDir;
    }

    @Override
    public void run() throws StudyImporterException {
        final TaxonCacheService taxonCacheService = new TaxonCacheService(
                taxonCachePath,
                taxonMapPath,
                new ResourceServiceLocal()
        );
        taxonCacheService.setCacheDir(cacheDir);
        IndexerNeo4j taxonIndexer = new IndexerTaxa(taxonCacheService, graphServiceFactory);
        taxonIndexer.index();
    }
}
