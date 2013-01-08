package org.trophic.graph.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.junit.Test;
import org.trophic.graph.obo.OboTermListener;
import org.trophic.graph.obo.TaxonParser;
import org.trophic.graph.obo.TaxonTerm;
import org.trophic.graph.service.EOLTaxonImageService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class EOLTaxonomyImporterTest {

    @Test
    public void readLine() throws IOException {
        TaxonReaderFactory taxonReaderFactory = new TaxonReaderFactory() {

            @Override
            public BufferedReader createReader() throws IOException {
                InputStream resourceAsStream = getClass().getResourceAsStream("eol/taxon.tab.gz");
                GZIPInputStream gzipInputStream = new GZIPInputStream(resourceAsStream);
                return new BufferedReader(new InputStreamReader(gzipInputStream));
            }
        };

        assertThat(taxonReaderFactory.createReader(), is(notNullValue()));


        TaxonParser taxonParser = new TaxonParser() {
            @Override
            public void parse(BufferedReader reader, OboTermListener listener) throws IOException {
                LabeledCSVParser labeledCSVParser = new LabeledCSVParser(new CSVParser(reader));
                labeledCSVParser.changeDelimiter('\t');
                int count = 0;
                while (labeledCSVParser.getLine() != null) {
                    TaxonTerm term = new TaxonTerm();
                    term.setId(EOLTaxonImageService.EOL_LSID_PREFIX + labeledCSVParser.getValueByLabel("taxonID"));
                    term.setName(labeledCSVParser.getValueByLabel("scientificName"));
                    term.setRank(labeledCSVParser.getValueByLabel("taxonRank"));
                    listener.notifyTermWithRank(term);
                    count++;
                }
                assertThat(count, is(2474168));
            }
        };


        final List<TaxonTerm> terms = new ArrayList<TaxonTerm>();

        taxonParser.parse(taxonReaderFactory.createReader(), new OboTermListener() {
            @Override
            public void notifyTermWithRank(TaxonTerm term) {
                if (terms.size() < 10) {
                    terms.add(term);
                }
            }
        });

        TaxonTerm taxonTerm = terms.get(0);
        assertThat(taxonTerm.getId(), is("EOL:1"));
        assertThat(taxonTerm.getRank(), is("kingdom"));
        assertThat(taxonTerm.getName(), is("Animalia"));
        assertThat(terms.size(), is(10));

    }
}
