package org.globalbioticinteractions.pensoft;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.CharsetConstant;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

public class TableRectifierTest {

    @Test
    public void rectifyTable() throws IOException {
        TableProcessor rectifier = new TableRectifier();
        String inputString = IOUtils.toString(getClass().getResourceAsStream("/org/eol/globi/data/pensoft/annotated-table-provided.html"), CharsetConstant.UTF8);
        String processed = rectifier.process(inputString);

        assertThat(processed, is(IOUtils.toString(getClass().getResourceAsStream("/org/eol/globi/data/pensoft/annotated-table-expanded-row-values.html"), CharsetConstant.UTF8)));
    }

    @Test
    public void rectifyTableWithColumnSpans() throws IOException {
        String preppedTable = IOUtils
                .toString(getClass()
                                .getResourceAsStream(
                                        "/org/eol/globi/data/pensoft/table-with-colspan-zookeys.318.5693.html"),
                        CharsetConstant.UTF8);

        assertThat(preppedTable, containsString("colspan=\"6\""));

        TableProcessor prep = new TableRectifier();

        String processed = prep.process(preppedTable);

        assertThat(processed, not(containsString("colspan=\"6\"")));

        System.out.println(processed);

        String resourcePrefix = "/org/eol/globi/data/pensoft/";
        assertThat(processed,
                is(IOUtils.toString(getClass()
                                .getResourceAsStream(resourcePrefix +
                                        "table-with-colspan-zookeys.318.5693-colspan-expanded.html"),
                        CharsetConstant.UTF8)));

    }


}