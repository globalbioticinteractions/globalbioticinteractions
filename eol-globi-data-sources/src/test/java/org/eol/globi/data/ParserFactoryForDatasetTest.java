package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.service.DatasetLocal;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ParserFactoryForDatasetTest {

    @Test
    public void parserWithDatasetContextLocalResource() throws IOException {
        ParserFactoryForDataset parserFactory = new ParserFactoryForDataset(new DatasetLocal(inStream -> inStream));
        LabeledCSVParser parser = parserFactory.createParser(URI.create("classpath:/org/eol/globi/data/someResource.csv"), "UTF-8");
        assertThat(parser.getLine(), is(new String[] { "valueA", "valueB"}));
    }
}