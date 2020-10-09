package org.globalbioticinteractions.pensoft;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.CharsetConstant;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

public class ExpandRowSpansTest {

    @Test
    public void doExpandRows() throws IOException {
        String preppedTable = IOUtils
                .toString(getClass()
                        .getResourceAsStream("/org/eol/globi/data/pensoft/annotated-table-preprocessed.html"), CharsetConstant.UTF8);

        assertThat(preppedTable, containsString("rowspan=\"2\""));

        TableProcessor prep = new ExpandRowSpans();


        String processedString = prep.process(preppedTable);
        assertThat(processedString, not(containsString("rowspan=\"2\"")));

        assertThat(processedString,
                is(IOUtils.toString(getClass().getResourceAsStream("/org/eol/globi/data/pensoft/annotated-table-expanded-rows.html"), CharsetConstant.UTF8)));
    }


}