package org.eol.globi.export;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.TaxonomyProvider;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportNCBIResourceFile implements GraphExporter {

    private int linksPerResourceFile = 5000;

    interface OutputStreamFactory {
        OutputStream create(int i) throws IOException;
    }


    @Override
    public void export(GraphDatabaseService graphService, final String baseDir) throws StudyImporterException {
        OutputStreamFactory fileFactory = new OutputStreamFactory() {
            @Override
            public OutputStream create(int i) throws IOException {
                ExportUtil.mkdirIfNeeded(baseDir);
                return new FileOutputStream(nameForResourceFile(baseDir, i));
            }
        };

        export(graphService, fileFactory);

    }

    protected void export(GraphDatabaseService graphService, OutputStreamFactory fileFactory) throws StudyImporterException {
        String query = "START taxon = node:taxons('*:*') " +
                "MATCH taxon-[?:SAME_AS*0..1]->linkedTaxon " +
                "WHERE has(linkedTaxon.externalId) AND linkedTaxon.externalId =~ 'NCBI:.*'" +
                "RETURN distinct(linkedTaxon.externalId) as id";

        ExecutionResult rows = new ExecutionEngine(graphService).execute(query, new HashMap<String, Object>());

        int rowCount = 0;
        OutputStream os = null;
        try {
            List<String> columns = rows.columns();
            for (Map<String, Object> row : rows) {
                if (rowCount % getLinksPerResourceFile() == 0) {
                    close(os);
                    os = null;
                }

                for (String column : columns) {
                    String taxonId = row.get(column).toString();
                    String ncbiTaxonId = StringUtils.replace(taxonId, TaxonomyProvider.ID_PREFIX_NCBI, "");
                    String aLink = String.format(
                            "         <ObjId>%s</ObjId>\n"
                            , ncbiTaxonId);

                    IOUtils.write(aLink, os == null ? (os = open(fileFactory, rowCount)) : os);
                }
                rowCount++;
            }
            close(os);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export ncbi resources", e);
        }
    }

    private OutputStream open(OutputStreamFactory fileFactory, int rowCount) throws IOException {
        OutputStream os;
        os = fileFactory.create(linkBatch(rowCount));
        IOUtils.write(String.format("<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE LinkSet PUBLIC \"-//NLM//DTD LinkOut 1.0//EN\"\n" +
                "\"http://www.ncbi.nlm.nih.gov/projects/linkout/doc/LinkOut.dtd\"\n" +
                "[<!ENTITY base.url \"http://www.globalbioticinteractions.org?\">]>\n" +
                "<LinkSet>\n" +
                " <Link>\n" +
                "   <LinkId>%d</LinkId>\n" +
                "   <ProviderId>%s</ProviderId>\n" +
                "   <ObjectSelector>\n" +
                "     <Database>Taxonomy</Database>\n" +
                "     <ObjectList>\n", linkBatch(rowCount), ExportNCBIIdentityFile.PROVIDER_ID), os);
        return os;
    }

    private int linkBatch(int rowCount) {
        return rowCount / getLinksPerResourceFile();
    }

    private void close(OutputStream os) throws IOException {
        if (os != null) {
            IOUtils.write("      </ObjectList>\n" +
                    "   </ObjectSelector>\n" +
                    "   <ObjectUrl>\n" +
                    "      <Base>&base.url;</Base>\n" +
                    "      <Rule>sourceTaxon=NCBI:&lo.id;</Rule>\n" +
                    "      <UrlName>Show Biotic Interactions</UrlName>\n" +
                    "   </ObjectUrl>\n" +
                    " </Link>", os);
            IOUtils.write("\n</LinkSet>", os);
            IOUtils.closeQuietly(os);
        }
    }

    protected String nameForResourceFile(String baseDir, int i) {
        return baseDir + "resources_" + i + ".xml";
    }

    public int getLinksPerResourceFile() {
        return linksPerResourceFile;
    }

    public void setLinksPerResourceFile(int linksPerResourceFile) {
        this.linksPerResourceFile = linksPerResourceFile;
    }


}
