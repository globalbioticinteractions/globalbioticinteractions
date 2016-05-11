package org.eol.globi.export;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ExportNCBIIdentityFile implements GraphExporter {

    public static final String PROVIDER_ID = "9426";

    @Override
    public void export(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        try {
            streamTo(new FileOutputStream(baseDir + "providerinfo.xml"));
        } catch (IOException e) {
            throw new StudyImporterException(e);
        }

    }

    protected void streamTo(OutputStream os) throws IOException {
        IOUtils.write(String.format("<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE Provider PUBLIC \"-//NLM//DTD LinkOut 1.0//EN\"\n" +
                "\"http://www.ncbi.nlm.nih.gov/projects/linkout/doc/LinkOut.dtd\">\n" +
                "<Provider>\n" +
                "    <ProviderId>%s</ProviderId>\n" +
                "    <Name>Global Biotic Interactions</Name>\n" +
                "    <NameAbbr>GloBI</NameAbbr>\n" +
                "    <SubjectType>taxonomy/phylogenetic</SubjectType>\n" +
                "    <Url>http://www.globalbioticinteractions.org</Url>\n" +
                "    <Brief>helps access existing species interaction datasets</Brief>\n" +
                "</Provider>\n", PROVIDER_ID), os);
    }


}
