package org.globalbioticinteractions.pensoft;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.CharsetConstant;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AddColumnsForOpenBiodivTermsTest {

    private String resourcePrefix;

    @Test
    public void doExpandValueLists() throws IOException {
        resourcePrefix = "/org/eol/globi/data/pensoft/";
        String preppedTable = IOUtils
                .toString(getClass()
                        .getResourceAsStream(resourcePrefix + "annotated-table-expanded-rows.html"), CharsetConstant.UTF8);

        TableProcessor prep = new AddColumnsForOpenBiodivTerms();


        String processedString = prep.process(preppedTable);

        assertThat(processedString,
                is(IOUtils
                        .toString(getClass()
                                .getResourceAsStream("/org/eol/globi/data/pensoft/" + "annotated-table-expanded-row-values.html"), CharsetConstant.UTF8))
        );
    }


}