package org.eol.globi.taxon;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;

import java.io.File;
import java.io.IOException;

public class TaxonLookupServiceImpl implements TaxonImportListener, TaxonLookupService {
    private static final Log LOG = LogFactory.getLog(TaxonLookupServiceImpl.class);

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_COMMON_NAMES = "common_names";
    private static final String FIELD_RANK_PATH = "rank_path";
    private static final String FIELD_RANK_PATH_IDS = "rank_path_ids";
    private static final String FIELD_RANK_PATH_NAMES = "rank_path_names";
    private static final String FIELD_RECOMMENDED_NAME = "recommended_name";

    private Directory indexDir;
    private IndexWriter indexWriter;
    private IndexSearcher indexSearcher;
    private File indexPath;
    private int maxHits = Integer.MAX_VALUE;

    public TaxonLookupServiceImpl() {
        this(null);
    }

    public TaxonLookupServiceImpl(Directory indexDir) {
        this.indexDir = indexDir;
    }

    @Override
    public void addTerm(Taxon taxonTerm) {
        addTerm(taxonTerm.getName(), taxonTerm);
    }

    @Override
    public void addTerm(String key, Taxon value) {
        if (hasStarted()) {
            Document doc = new Document();
            doc.add(new Field(FIELD_NAME, key, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            if (value.getName() != null) {
                doc.add(new Field(FIELD_RECOMMENDED_NAME, value.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            }
            doc.add(new Field(FIELD_ID, value.getExternalId(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            String rankPath = value.getPath();
            if (rankPath != null) {
                doc.add(new Field(FIELD_RANK_PATH, rankPath, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            }
            String rankPathIds = value.getPathIds();
            if (rankPathIds != null) {
                doc.add(new Field(FIELD_RANK_PATH_IDS, rankPathIds, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            }
            String rankPathNames = value.getPathNames();
            if (rankPathNames != null) {
                doc.add(new Field(FIELD_RANK_PATH_NAMES, rankPathNames, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            }
            String commonNames = value.getCommonNames();
            if (commonNames != null) {
                doc.add(new Field(FIELD_COMMON_NAMES, commonNames, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            }
            try {
                indexWriter.addDocument(doc);
            } catch (IOException e) {
                throw new RuntimeException("failed to add document for term with name [" + value.getName() + "]");
            }
        }
    }

    @Override
    public Taxon[] lookupTermsByName(String taxonName) throws IOException {
        return findTaxon(FIELD_NAME, taxonName);
    }

    @Override
    public Taxon[] lookupTermsById(String taxonId) throws IOException {
        return findTaxon(FIELD_ID, taxonId);
    }

    private Taxon[] findTaxon(String fieldName1, String fieldValue) throws IOException {
        Taxon[] terms = new TaxonImpl[0];
        if (StringUtils.isNotBlank(fieldValue) && indexSearcher != null) {
            PhraseQuery query = new PhraseQuery();
            query.add(new Term(fieldName1, fieldValue));
            TopDocs docs = indexSearcher.search(query, getMaxHits());

            if (docs.totalHits > 0) {
                int maxResults = Math.min(docs.totalHits, getMaxHits());
                terms = new TaxonImpl[maxResults];
                for (int i = 0; i < maxResults; i++) {
                    ScoreDoc scoreDoc = docs.scoreDocs[i];
                    Document foundDoc = indexSearcher.doc(scoreDoc.doc);
                    Taxon term = new TaxonImpl();
                    Fieldable idField = foundDoc.getFieldable(FIELD_ID);
                    if (idField != null) {
                        term.setExternalId(idField.stringValue());
                    }
                    Fieldable rankPathField = foundDoc.getFieldable(FIELD_RANK_PATH);
                    if (rankPathField != null) {
                        term.setPath(rankPathField.stringValue());
                    }
                    Fieldable rankPathIdsField = foundDoc.getFieldable(FIELD_RANK_PATH_IDS);
                    if (rankPathIdsField != null) {
                        term.setPathIds(rankPathIdsField.stringValue());
                    }
                    Fieldable rankPathNamesField = foundDoc.getFieldable(FIELD_RANK_PATH_NAMES);
                    if (rankPathNamesField != null) {
                        term.setPathNames(rankPathNamesField.stringValue());
                    }
                    Fieldable commonNamesFields = foundDoc.getFieldable(FIELD_COMMON_NAMES);
                    if (commonNamesFields != null) {
                        term.setCommonNames(commonNamesFields.stringValue());
                    }
                    Fieldable fieldName = foundDoc.getFieldable(FIELD_RECOMMENDED_NAME);
                    if (fieldName != null) {
                        term.setName(fieldName.stringValue());
                    }
                    terms[i] = term;
                }
            }
        }
        return terms;
    }

    @Override
    public void destroy() {
        if (indexDir != null) {
            try {
                indexDir.close();
            } catch (IOException e) {
                //
            } finally {
                try {
                    File indexPath1 = getIndexPath();
                    if (indexPath1 != null) {
                        FileUtils.deleteDirectory(indexPath1);
                        LOG.info("index directory at [" + indexPath + "] deleted.");
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private boolean hasStarted() {
        return indexWriter != null && indexDir != null;
    }

    @Override
    public void start() {
        try {
            if (indexDir == null) {
                indexPath = new File(System.getProperty("java.io.tmpdir") + "/taxon" + System.currentTimeMillis());
                LOG.info("index directory at [" + indexPath + "] created.");
                //FileUtils.forceDeleteOnExit(indexPath);
                indexDir = new SimpleFSDirectory(indexPath);
            }
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
                indexSearcher = new IndexSearcher(IndexReader.open(indexDir));
            } catch (IOException e) {
                throw new RuntimeException("failed to successfully finish taxon import", e);
            }
        }

    }

    public File getIndexPath() {
        return indexPath;
    }

    public int getMaxHits() {
        return maxHits;
    }

    public void setMaxHits(int maxHits) {
        this.maxHits = maxHits;
    }
}
