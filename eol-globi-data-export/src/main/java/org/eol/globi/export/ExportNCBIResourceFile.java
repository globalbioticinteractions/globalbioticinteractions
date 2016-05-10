package org.eol.globi.export;

import com.Ostermiller.util.CSVPrint;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.CSVUtil;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportNCBIResourceFile implements StudyExporter {

    @Override
    public void exportStudy(final Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            doExport(study, writer);
        }
    }

    protected void doExport(Study study, Writer writer) {
        String query = "START taxon = node:taxons('*:*') " +
                "MATCH taxon-[?:SAME_AS*0..1]->linkedTaxon " +
                "WHERE has(linkedTaxon.externalId) AND linkedTaxon.externalId =~ 'NCBI:.*'" +
                "RETURN distinct(linkedTaxon.externalId) as id";

        HashMap<String, Object> params = new HashMap<String, Object>();

        ExecutionResult rows = new ExecutionEngine(study.getUnderlyingNode().getGraphDatabase()).execute(query, params);
        CSVPrint printer = CSVUtil.createCSVPrint(writer);
        List<String> columns = rows.columns();
        final PrintWriter printWriter = new PrintWriter(writer);
        printWriter.print("<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE LinkSet PUBLIC \"-//NLM//DTD LinkOut 1.0//EN\"\n" +
                "\"http://www.ncbi.nlm.nih.gov/projects/linkout/doc/LinkOut.dtd\"\n" +
                "[<!ENTITY base.url \"http://www.globalbioticinteractions.org?\">]>\n" +
                "<LinkSet>");

        for (Map<String, Object> row : rows) {
            printer.println();
            for (String column : columns) {
                String taxonId = row.get(column).toString();
                String ncbiTaxonId = StringUtils.replace(taxonId, TaxonomyProvider.ID_PREFIX_NCBI, "");
                printWriter.print(String.format(" <Link>\n" +
                        "   <LinkId>%s</LinkId>\n" +
                        "   <ProviderId>%s</ProviderId>\n" +
                        "   <ObjectSelector>\n" +
                        "     <Database>Taxonomy</Database>\n" +
                        "     <ObjectList>\n" +
                        "         <ObjId>%s</ObjId>\n" +
                        "      </ObjectList>\n" +
                        "   </ObjectSelector>\n" +
                        "   <ObjectUrl>\n" +
                        "      <Base>&base.url;</Base>\n" +
                        "      <Rule>sourceTaxon=NCBI:&lo.id;</Rule>\n" +
                        "   </ObjectUrl>\n" +
                        " </Link>", taxonId, ExportNCBIIdentityFile.PROVIDER_ID, ncbiTaxonId));
            }
        }
        printWriter.append("\n</LinkSet>");
    }
}
