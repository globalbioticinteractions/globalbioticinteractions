package org.eol.globi.export;

import org.eol.globi.data.NodeFactoryException;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExportNCBIIdentityFileTest {

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ExportNCBIIdentityFile().streamTo(baos);
        assertThat(baos.toString(), is("<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE Provider PUBLIC \"-//NLM//DTD LinkOut 1.0//EN\"\n" +
                "\"http://www.ncbi.nlm.nih.gov/projects/linkout/doc/LinkOut.dtd\">\n" +
                "<Provider>\n" +
                "    <ProviderId>9426</ProviderId>\n" +
                "    <Name>Global Biotic Interactions</Name>\n" +
                "    <NameAbbr>GloBI</NameAbbr>\n" +
                "    <SubjectType>taxonomy/phylogenetic</SubjectType>\n" +
                "    <Url>http://www.globalbioticinteractions.org</Url>\n" +
                "    <Brief>helps access existing species interaction datasets</Brief>\n" +
                "</Provider>\n"));
    }

}