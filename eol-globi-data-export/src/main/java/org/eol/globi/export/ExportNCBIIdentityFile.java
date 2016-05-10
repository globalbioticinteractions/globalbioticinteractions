package org.eol.globi.export;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

public class ExportNCBIIdentityFile implements StudyExporter {

    public static final String providerId = "XXXX";

    @Override
    public void exportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            doExport(study, writer);
        }
    }

    protected void doExport(Study study, Writer writer) {
        final PrintWriter printWriter = new PrintWriter(writer);
        printWriter.print(String.format("<?xml version=\"1.0\"?>\n" +
                        "<!DOCTYPE Provider PUBLIC \"-//NLM//DTD LinkOut 1.0//EN\"\n" +
                        "\"http://www.ncbi.nlm.nih.gov/projects/linkout/doc/LinkOut.dtd\">\n" +
                        "<Provider>\n" +
                        "    <ProviderId>%s</ProviderId>\n" +
                        "    <Name>Global Biotic Interactions</Name>\n" +
                        "    <NameAbbr>GloBI</NameAbbr>\n" +
                        "    <SubjectType>taxonomy/phylogenetic</SubjectType>\n" +
                        "    <Attribute>registration required</Attribute>\n" +
                        "    <Url>http://www.globalbioticinteractions.org</Url>\n" +
                        "    <Brief>helps access to existing species interaction datasets</Brief>\n" +
                        "</Provider>\n", providerId));

        printWriter.flush();
    }
}
