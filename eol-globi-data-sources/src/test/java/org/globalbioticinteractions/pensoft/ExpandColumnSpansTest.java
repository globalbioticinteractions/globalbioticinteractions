package org.globalbioticinteractions.pensoft;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.CharsetConstant;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;

public class ExpandColumnSpansTest {

    @Test
    public void doExpandColumnSpans() throws IOException {
        String preppedTable = IOUtils
                .toString(getClass()
                        .getResourceAsStream("/org/eol/globi/data/pensoft/table-with-colspan-zookeys.318.5693.html"), CharsetConstant.UTF8);

        assertThat(preppedTable, containsString("colspan=\"6\""));

        TableProcessor prep = new ExpandColumnSpans();

        String processedString = prep.process(preppedTable);

        assertThat(processedString, not(containsString("colspan=\"6\"")));

    }


}