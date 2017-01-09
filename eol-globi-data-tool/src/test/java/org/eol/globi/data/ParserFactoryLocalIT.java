package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class ParserFactoryLocalIT {

    @Test
    public void retrieveRemoteResource() throws IOException {
        LabeledCSVParser parser = new ParserFactoryLocal().createParser("http://www.esapubs.org/archive/ecol/E095/124/PairwiseList.txt", "UTF-8");
        parser.changeDelimiter('\t');
        parser.getLine();
        assertThat(parser.getValueByLabel("PREDATOR"), is(notNullValue()));
        assertThat(parser.getLabels(), is(new String[] { "PREY", "PREDATOR", "CODE", "REFERENCE"}));
        assertThat(parser.getValueByLabel("PREY"), is(notNullValue()));
    }

}
