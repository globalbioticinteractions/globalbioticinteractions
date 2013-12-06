package org.eol.globi.data.taxon;

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
import org.neo4j.kernel.impl.util.FileUtils;

import java.io.File;
import java.io.IOException;

public class TaxonLookupServiceImpl implements TaxonImportListener, TaxonLookupService {
    private static final Log LOG = LogFactory.getLog(TaxonLookupServiceImpl.class);

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    private static final String FIELD_RANK_PATH = "rank_path";
    private static final String FIELD_RECOMMENDED_NAME = "recommended_name";

    private Directory indexDir;
    private IndexWriter indexWriter;
    private IndexSearcher indexSearcher;
    private File indexPath;

    public TaxonLookupServiceImpl() {
        this(null);
    }

    public TaxonLookupServiceImpl(Directory indexDir) {
        this.indexDir = indexDir;
    }

    @Override
    public void addTerm(TaxonTerm taxonTerm) {
         addTerm(taxonTerm.getName(), taxonTerm);
    }

    @Override
    public void addTerm(String name, TaxonTerm taxonTerm) {
        if (hasStarted()) {
                    Document doc = new Document();
                    doc.add(new Field(FIELD_NAME, name, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
                    doc.add(new Field(FIELD_RECOMMENDED_NAME, taxonTerm.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
                    doc.add(new Field(FIELD_ID, taxonTerm.getId(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
                    String rankPath = taxonTerm.getRankPath();
                    if (rankPath != null) {
                        doc.add(new Field(FIELD_RANK_PATH, rankPath, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
                    }
                    try {
                        indexWriter.addDocument(doc);
                    } catch (IOException e) {
                        throw new RuntimeException("failed to add document for term with name [" + taxonTerm.getName() + "]");
                    }
                }
    }

    @Override
    public TaxonTerm[] lookupTermsByName(String taxonName) throws IOException {
        TaxonTerm[] terms = new TaxonTerm[0];
        if (indexSearcher != null) {
            PhraseQuery query = new PhraseQuery();
            query.add(new Term(FIELD_NAME, taxonName));
            int maxHits = 3;
            TopDocs docs = indexSearcher.search(query, maxHits);

            if (docs.totalHits > 0) {
                terms = new TaxonTerm[docs.totalHits];
                for (int i = 0; i < docs.totalHits && i < maxHits; i++) {
                    ScoreDoc scoreDoc = docs.scoreDocs[i];
                    Document foundDoc = indexSearcher.doc(scoreDoc.doc);
                    TaxonTerm term = new TaxonTerm();
                    Fieldable idField = foundDoc.getFieldable(FIELD_ID);
                    if (idField != null) {
                        term.setId(idField.stringValue());
                    }
                    Fieldable rankPathField = foundDoc.getFieldable(FIELD_RANK_PATH);
                    if (rankPathField != null) {
                        term.setRankPath(rankPathField.stringValue());
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

            } finally {
                try {
                    File indexPath1 = getIndexPath();
                    if (indexPath1 != null) {
                        FileUtils.deleteRecursively(indexPath1);
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
                LOG.info("creating index directory at [" + indexPath + "]");
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
}
