package org.eol.globi.export;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.service.DatasetConstant;
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


    public static final String CYPHER_QUERY = "START dataset = node:datasets('namespace:*') " +
            "MATCH dataset<-[:IN_DATASET]-study-[c:COLLECTED]->sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon, " +
            "sourceSpecimen-[?:COLLECTED_AT]->loc, " +
            "sourceSpecimen-[r:" + InteractUtil.allInteractionsCypherClause() + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon " +
            "WHERE not(has(r.inverted)) " +
            "RETURN sourceTaxon.externalId? as sourceTaxonId" +
            ", sourceTaxon.nameIds? as sourceTaxonIds" +
            ", sourceTaxon.name? as sourceTaxonName" +
            ", sourceTaxon.rank? as sourceTaxonRank" +
            ", sourceTaxon.path? as sourceTaxonPathNames" +
            ", sourceTaxon.pathIds? as sourceTaxonPathIds" +
            ", sourceTaxon.pathNames? as sourceTaxonPathRankNames" +
            ", sourceSpecimen.externalId? as sourceId" +
            ", sourceSpecimen.occurrenceID? as sourceOccurrenceId" +
            ", sourceSpecimen.catalogNumber? as sourceCatalogNumber" +
            ", sourceSpecimen.basisOfRecordId? as sourceBasisOfRecordId" +
            ", sourceSpecimen.basisOfRecordLabel? as sourceBasisOfRecordName" +
            ", sourceSpecimen." + SpecimenConstant.LIFE_STAGE_ID + "? as sourceLifeStageId" +
            ", sourceSpecimen." + SpecimenConstant.LIFE_STAGE_LABEL + "? as sourceLifeStageName" +
            ", sourceSpecimen." + SpecimenConstant.BODY_PART_ID + "? as sourceBodyPartId" +
            ", sourceSpecimen." + SpecimenConstant.BODY_PART_LABEL + "? as sourceBodyPartName" +
            ", sourceSpecimen." + SpecimenConstant.PHYSIOLOGICAL_STATE_ID + "? as sourcePhysiologicalStateId" +
            ", sourceSpecimen." + SpecimenConstant.PHYSIOLOGICAL_STATE_LABEL + "? as sourcePhysiologicalStateName" +
            ", r.label as interactionTypeName" +
            ", r.iri as interactionTypeId" +
            ", targetTaxon.externalId? as targetTaxonId" +
            ", targetTaxon.nameIds? as targetTaxonIds" +
            ", targetTaxon.name? as targetTaxonName" +
            ", targetTaxon.rank? as targetTaxonRank" +
            ", targetTaxon.path? as targetTaxonPathNames" +
            ", targetTaxon.pathIds? as targetTaxonPathIds" +
            ", targetTaxon.pathNames? as targetTaxonPathRankNames" +
            ", targetSpecimen.externalId? as targetId" +
            ", targetSpecimen.occurrenceID? as targetOccurrenceId" +
            ", targetSpecimen.catalogNumber? as targetCatalogNumber" +
            ", targetSpecimen.basisOfRecordId? as targetBasisOfRecordId" +
            ", targetSpecimen.basisOfRecordLabel? as targetBasisOfRecordName" +
            ", targetSpecimen." + SpecimenConstant.LIFE_STAGE_ID + "? as targetLifeStageId" +
            ", targetSpecimen." + SpecimenConstant.LIFE_STAGE_LABEL + "? as targetLifeStageName" +
            ", targetSpecimen." + SpecimenConstant.BODY_PART_ID + "? as targetBodyPartId" +
            ", targetSpecimen." + SpecimenConstant.BODY_PART_LABEL + "? as targetBodyPartName" +
            ", targetSpecimen." + SpecimenConstant.PHYSIOLOGICAL_STATE_ID + "? as targetPhysiologicalStateId" +
            ", targetSpecimen." + SpecimenConstant.PHYSIOLOGICAL_STATE_LABEL + "? as targetPhysiologicalStateName" +
            ", loc.latitude? as decimalLatitude" +
            ", loc.longitude? as decimalLongitude" +
            ", loc.localityId? as localityId" +
            ", loc.locality? as localityName" +
            ", c.dateInUnixEpoch? as eventDateUnixEpoch" +
            ", study.citation? as referenceCitation" +
            ", study.doi? as referenceDoi" +
            ", study.externalUrl? as referenceUrl" +
            ", dataset." + DatasetConstant.CITATION + "? as sourceCitation" +
            ", dataset." + DatasetConstant.NAMESPACE + "? as sourceNamespace" +
            ", dataset." + DatasetConstant.ARCHIVE_URI + "? as sourceArchiveURI" +
            ", dataset." + DatasetConstant.DOI + "? as sourceDOI" +
            ", dataset." + DatasetConstant.LAST_SEEN_AT + "? as sourceLastSeenAtUnixEpoch";

    @Override
    public void export(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        String tsvFilename = "/interactions.tsv.gz";
        ExportCitations.export(graphService, baseDir, tsvFilename, CYPHER_QUERY);
    }

    void export(GraphDatabaseService graphService, Writer writer) throws IOException {
        ExportCitations.export(graphService, writer, CYPHER_QUERY);
    }


}
