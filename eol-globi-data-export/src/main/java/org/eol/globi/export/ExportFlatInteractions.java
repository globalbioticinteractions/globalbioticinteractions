package org.eol.globi.export;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.util.InteractUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

public class ExportFlatInteractions implements GraphExporter {


    public static final String CYPHER_QUERY = "START study = node:studies('*:*') " +
            "MATCH study-[c:COLLECTED]->sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon, " +
            "sourceSpecimen-[?:COLLECTED_AT]->loc, " +
            "sourceSpecimen-[r:" + InteractUtil.allInteractionsCypherClause() + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon " +
            "WHERE has(sourceTaxon.path) AND has(targetTaxon.path) AND not(has(r.inverted)) " +
            "RETURN sourceTaxon.externalId? as sourceTaxonId" +
            ", sourceTaxon.name? as sourceTaxonName" +
            ", sourceTaxon.rank? as sourceTaxonRank" +
            ", sourceTaxon.path? as sourceTaxonPathNames" +
            ", sourceTaxon.pathIds? as sourceTaxonPathIds" +
            ", sourceTaxon.pathNames? as sourceTaxonPathRankNames" +
            ", r.label as interactionTypeName" +
            ", r.iri as interactionTypeId" +
            ", targetTaxon.externalId? as targetTaxonId" +
            ", targetTaxon.name? as targetTaxonName" +
            ", targetTaxon.rank? as targetTaxonRank" +
            ", targetTaxon.path? as targetTaxonPathNames" +
            ", targetTaxon.pathIds? as targetTaxonPathIds" +
            ", targetTaxon.pathNames? as targetTaxonPathRankNames" +
            ", loc.latitude? as decimalLatitude" +
            ", loc.longitude? as decimalLongitude" +
            ", loc.locality? as locality" +
            ", c.dateInUnixEpoch? as eventDateUnixEpoch" +
            ", study.citation? as referenceCitation" +
            ", study.doi? as referenceDoi" +
            ", study.externalUrl? as referenceUrl" +
            ", study.source? as sourceCitation";

    @Override
    public void export(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        String tsvFilename = "/interactions.tsv.gz";
        ExportCitations.export(graphService, baseDir, tsvFilename, CYPHER_QUERY);
    }

    void export(GraphDatabaseService graphService, Writer writer) throws IOException {
        ExportCitations.export(graphService, writer, CYPHER_QUERY);
    }


}
