package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.eol.globi.domain.Taxon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TaxonLookupBuilder implements TaxonImportListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TaxonLookupBuilder.class);

    private IndexWriter indexWriter;
    private Directory indexDir;

    public TaxonLookupBuilder(Directory indexDir) {
        this.indexDir = indexDir;
    }

    @Override
    public void addTerm(Taxon taxonTerm) {
        addTerm(taxonTerm.getName(), taxonTerm);
    }

    @Override
    public void addTerm(String key, Taxon taxon) {
        if (hasStarted()) {
            Document doc = new Document();
            doc.add(new Field(TaxonLookupServiceConstants.FIELD_NAME, key, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            doc.add(new Field(TaxonLookupServiceConstants.FIELD_ID, taxon.getExternalId(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));

            addIfNotBlank(doc, TaxonLookupServiceConstants.FIELD_RECOMMENDED_NAME, taxon.getName());
            addIfNotBlank(doc, TaxonLookupServiceConstants.FIELD_RANK, taxon.getRank());
            addIfNotBlank(doc, TaxonLookupServiceConstants.FIELD_RANK_PATH, taxon.getPath());
            addIfNotBlank(doc, TaxonLookupServiceConstants.FIELD_RANK_PATH_IDS, taxon.getPathIds());
            addIfNotBlank(doc, TaxonLookupServiceConstants.FIELD_RANK_PATH_NAMES, taxon.getPathNames());
            addIfNotBlank(doc, TaxonLookupServiceConstants.FIELD_COMMON_NAMES, taxon.getCommonNames());

            try {
                indexWriter.addDocument(doc);
            } catch (IOException e) {
                throw new RuntimeException("failed to add document for term with name [" + taxon.getName() + "]");
            }
        }
    }

    private void addIfNotBlank(Document doc, String fieldName, String fieldValue) {
        if (StringUtils.isNotBlank(fieldValue)) {
            doc.add(new Field(fieldName, fieldValue, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
        }
    }

    private boolean hasStarted() {
        return indexWriter != null && indexDir != null;
    }

    @Override
    public void start() {
        try {
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_35, null);
            indexWriter = new IndexWriter(indexDir, config);
        } catch (IOException e) {
            throw new RuntimeException("failed to create indexWriter, cannot continue", e);
        }
    }

    @Override
    public void finish() {
        if (hasStarted()) {
            try {
                indexWriter.close();
                indexWriter = null;
            } catch (IOException e) {
                throw new RuntimeException("failed to successfully finish taxon import", e);
            }
        }

    }

    @Override
    public void close() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
            indexWriter = null;
        }
        if (indexDir != null) {
            indexDir.close();
            indexDir = null;
        }
    }
}
