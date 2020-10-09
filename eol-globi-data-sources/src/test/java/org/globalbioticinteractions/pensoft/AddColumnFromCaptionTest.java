package org.globalbioticinteractions.pensoft;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.CharsetConstant;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AddColumnFromCaptionTest {

    @Test
    public void doExpandValueLists() throws IOException {
        String preppedTable = IOUtils
                .toString(getClass()
                        .getResourceAsStream("/org/eol/globi/data/pensoft/annotated-table-expanded-rows.html"), CharsetConstant.UTF8);

        TableProcessor prep = new AddColumnFromCaption("<caption> <p>Collection data, sex, host and host plant associations of paratypes of <italic><tp:taxon-name obkms_id=\"1E4230FC-3C67-44BA-B744-1626EC9B0B77\" obkms_process=\"TRUE\">Colastomion parotiphagus</tp:taxon-name></italic></p> </caption>");


        String processedString = prep.process(preppedTable);

        System.out.println(processedString);

        assertThat(processedString,
                is(IOUtils
                        .toString(getClass()
                                .getResourceAsStream("/org/globalbioticinteractions/pensoft/table-enriched-with-caption.html"), CharsetConstant.UTF8))
        );
    }


}