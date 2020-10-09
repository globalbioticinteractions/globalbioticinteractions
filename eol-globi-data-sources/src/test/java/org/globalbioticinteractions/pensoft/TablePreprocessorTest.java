package org.globalbioticinteractions.pensoft;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.CharsetConstant;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TablePreprocessorTest {

    @Test
    public void prepTable() throws IOException {
        String inputString = IOUtils.toString(getClass().getResourceAsStream("/org/eol/globi/data/pensoft/annotated-table-provided.html"), CharsetConstant.UTF8);
        TableProcessor prep = new TablePreprocessor();
        String processedString = prep.process(inputString);
        assertThat(processedString,
                is(IOUtils.toString(getClass().getResourceAsStream("/org/eol/globi/data/pensoft/annotated-table-preprocessed.html"), CharsetConstant.UTF8)));
    }


}