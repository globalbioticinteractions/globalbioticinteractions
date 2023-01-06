package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.util.InteractUtil;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ExportFlatInteractions implements GraphExporter {

    private final ExportUtil.ValueJoiner joiner;
    private final String filename;
    private final String neo4jVersion;

    public RelTypes getTaxonRelation() {
        return taxonRelation;
    }

    private final RelTypes taxonRelation;

    private RelTypes argumentType = RelTypes.SUPPORTS;
    private String argumentTypeId = PropertyAndValueDictionary.SUPPORTS;

    public ExportFlatInteractions(ExportUtil.ValueJoiner joiner, String filename) {
        this(joiner, filename, RelTypes.CLASSIFIED_AS);
    }

    public ExportFlatInteractions(ExportUtil.ValueJoiner joiner, String filename, RelTypes taxonRelation) {
        this(joiner, filename, taxonRelation, "2");
    }

    public ExportFlatInteractions(ExportUtil.ValueJoiner joiner, String filename, RelTypes taxonRelation, String neo4jVersion) {
        this.joiner = joiner;
        this.filename = filename;
        this.taxonRelation = taxonRelation;
        this.neo4jVersion = neo4jVersion;
    }


    private List<String> createExportQueries(String prefix) {
        return Collections.singletonList(
                ExportFlatInteractions.createQuery(
                        getArgumentType(),
                        getArgumentTypeId(),
                        getTaxonRelation(),
                        prefix)
        );
    }

    private static String createQuery(RelTypes argumentTypeRel, String argumentTypeId, RelTypes taxonRelation, String prefix) {
        String argumentType = argumentTypeRel.name();
        return prefix +
                "MATCH (dataset)<-[:IN_DATASET]-(study)-[c:" + argumentType + "]->(sourceSpecimen)-[:" + taxonRelation.name() + "]->(sourceTaxon), " +
                "(sourceSpecimen)-[r:" + InteractUtil.allInteractionsCypherClause() + "]->(targetSpecimen)-[:" + taxonRelation.name() + "]->(targetTaxon) " +
                "WHERE NOT exists(r.inverted) " +
                "WITH dataset, study, c, sourceSpecimen, sourceTaxon, targetSpecimen, targetTaxon, r " +
                "OPTIONAL MATCH (sourceSpecimen)-[:COLLECTED_AT]->(loc) " +
                "RETURN " +
                "sourceTaxon.externalId as sourceTaxonId" +
                ", sourceTaxon.nameIds as sourceTaxonIds" +
                ", sourceTaxon.name as sourceTaxonName" +
                ", sourceTaxon.rank as sourceTaxonRank" +
                ", sourceTaxon.path as sourceTaxonPathNames" +
                ", sourceTaxon.pathIds as sourceTaxonPathIds" +
                ", sourceTaxon.pathNames as sourceTaxonPathRankNames" +
                ", sourceTaxon.speciesName as sourceTaxonSpeciesName" +
                ", sourceTaxon.speciesId as sourceTaxonSpeciesId" +
                ", sourceTaxon.subgenusName as sourceTaxonSubgenusName" +
                ", sourceTaxon.subgenusId as sourceTaxonSubgenusId" +
                ", sourceTaxon.genusName as sourceTaxonGenusName" +
                ", sourceTaxon.genusId as sourceTaxonGenusId" +
                ", sourceTaxon.familyName as sourceTaxonFamilyName" +
                ", sourceTaxon.familyId as sourceTaxonFamilyId" +
                ", sourceTaxon.orderName as sourceTaxonOrderName" +
                ", sourceTaxon.orderId as sourceTaxonOrderId" +
                ", sourceTaxon.className as sourceTaxonClassName" +
                ", sourceTaxon.classId as sourceTaxonClassId" +
                ", sourceTaxon.phylumName as sourceTaxonPhylumName" +
                ", sourceTaxon.phylumId as sourceTaxonPhylumId" +
                ", sourceTaxon.kingdomName as sourceTaxonKingdomName" +
                ", sourceTaxon.kingdomId as sourceTaxonKingdomId" +
                ", sourceSpecimen.externalId as sourceId" +
                ", sourceSpecimen.occurrenceID as sourceOccurrenceId" +
                ", sourceSpecimen.institutionCode as sourceInstitutionCode" +
                ", sourceSpecimen.sollectionCode as sourceCollectionCode" +
                ", sourceSpecimen.catalogNumber as sourceCatalogNumber" +
                ", sourceSpecimen.basisOfRecordId as sourceBasisOfRecordId" +
                ", sourceSpecimen.basisOfRecordLabel as sourceBasisOfRecordName" +
                ", sourceSpecimen." + SpecimenConstant.LIFE_STAGE_ID + " as sourceLifeStageId" +
                ", sourceSpecimen." + SpecimenConstant.LIFE_STAGE_LABEL + " as sourceLifeStageName" +
                ", sourceSpecimen." + SpecimenConstant.BODY_PART_ID + " as sourceBodyPartId" +
                ", sourceSpecimen." + SpecimenConstant.BODY_PART_LABEL + " as sourceBodyPartName" +
                ", sourceSpecimen." + SpecimenConstant.PHYSIOLOGICAL_STATE_ID + " as sourcePhysiologicalStateId" +
                ", sourceSpecimen." + SpecimenConstant.PHYSIOLOGICAL_STATE_LABEL + " as sourcePhysiologicalStateName" +
                ", sourceSpecimen." + SpecimenConstant.SEX_ID + " as sourceSexId" +
                ", sourceSpecimen." + SpecimenConstant.SEX_LABEL + " as sourceSexName" +
                ", r.label as interactionTypeName" +
                ", r.iri as interactionTypeId" +
                ", targetTaxon.externalId as targetTaxonId" +
                ", targetTaxon.nameIds as targetTaxonIds" +
                ", targetTaxon.name as targetTaxonName" +
                ", targetTaxon.rank as targetTaxonRank" +
                ", targetTaxon.path as targetTaxonPathNames" +
                ", targetTaxon.pathIds as targetTaxonPathIds" +
                ", targetTaxon.pathNames as targetTaxonPathRankNames" +
                ", targetTaxon.speciesName as targetTaxonSpeciesName" +
                ", targetTaxon.speciesId as targetTaxonSpeciesId" +
                ", targetTaxon.subgenusName as targetTaxonSubgenusName" +
                ", targetTaxon.subgenusId as targetTaxonSubgenusId" +
                ", targetTaxon.genusName as targetTaxonGenusName" +
                ", targetTaxon.genusId as targetTaxonGenusId" +
                ", targetTaxon.familyName as targetTaxonFamilyName" +
                ", targetTaxon.familyId as targetTaxonFamilyId" +
                ", targetTaxon.orderName as targetTaxonOrderName" +
                ", targetTaxon.orderId as targetTaxonOrderId" +
                ", targetTaxon.className as targetTaxonClassName" +
                ", targetTaxon.classId as targetTaxonClassId" +
                ", targetTaxon.phylumName as targetTaxonPhylumName" +
                ", targetTaxon.phylumId as targetTaxonPhylumId" +
                ", targetTaxon.kingdomName as targetTaxonKingdomName" +
                ", targetTaxon.kingdomId as targetTaxonKingdomId" +
                ", targetSpecimen.externalId as targetId" +
                ", targetSpecimen.occurrenceID as targetOccurrenceId" +
                ", targetSpecimen.institutionCode as targetInstitutionCode" +
                ", targetSpecimen.collectionCode as targetCollectionCode" +
                ", targetSpecimen.catalogNumber as targetCatalogNumber" +
                ", targetSpecimen.basisOfRecordId as targetBasisOfRecordId" +
                ", targetSpecimen.basisOfRecordLabel as targetBasisOfRecordName" +
                ", targetSpecimen." + SpecimenConstant.LIFE_STAGE_ID + " as targetLifeStageId" +
                ", targetSpecimen." + SpecimenConstant.LIFE_STAGE_LABEL + " as targetLifeStageName" +
                ", targetSpecimen." + SpecimenConstant.BODY_PART_ID + " as targetBodyPartId" +
                ", targetSpecimen." + SpecimenConstant.BODY_PART_LABEL + " as targetBodyPartName" +
                ", targetSpecimen." + SpecimenConstant.PHYSIOLOGICAL_STATE_ID + " as targetPhysiologicalStateId" +
                ", targetSpecimen." + SpecimenConstant.PHYSIOLOGICAL_STATE_LABEL + " as targetPhysiologicalStateName" +
                ", targetSpecimen." + SpecimenConstant.SEX_ID + " as targetSexId" +
                ", targetSpecimen." + SpecimenConstant.SEX_LABEL + " as targetSexName" +
                ", loc.latitude as decimalLatitude" +
                ", loc.longitude as decimalLongitude" +
                ", loc.localityId as localityId" +
                ", loc.locality as localityName" +
                ", c.eventDate as eventDate" +
                ", '" + argumentTypeId + "' as argumentTypeId" +
                ", study.citation as referenceCitation" +
                ", study.doi as referenceDoi" +
                ", study.externalUrl as referenceUrl" +
                ", dataset." + DatasetConstant.CITATION + " as sourceCitation" +
                ", dataset." + DatasetConstant.NAMESPACE + " as sourceNamespace" +
                ", dataset." + DatasetConstant.ARCHIVE_URI + " as sourceArchiveURI" +
                ", dataset." + DatasetConstant.DOI + " as sourceDOI" +
                ", dataset." + DatasetConstant.LAST_SEEN_AT + " as sourceLastSeenAtUnixEpoch";
    }

    @Override
    public void export(GraphDatabaseService graphService, File baseDir, String neo4jVersion) throws StudyImporterException {
        String prefix = getPrefix();
        ExportUtil.export(
                graphService,
                new File(baseDir, filename),
                createExportQueries(prefix),
                joiner);
    }

    private String getPrefix() {
        return "2".equals(neo4jVersion)
                ? "CYPHER 2.3 START dataset = node:datasets('namespace:*') "
                : "";
    }

    void export(GraphDatabaseService graphService, ExportUtil.Appender appender) throws IOException {
        String prefix = getPrefix();
        ExportUtil.export(
                appender,
                graphService,
                createExportQueries(prefix));
    }

    public RelTypes getArgumentType() {
        return argumentType;
    }


    public ExportFlatInteractions setArgumentType(RelTypes argumentType) {
        this.argumentType = argumentType;
        return this;
    }

    public ExportFlatInteractions setArgumentTypeId(String argumentTypeId) {
        this.argumentTypeId = argumentTypeId;
        return this;
    }

    public String getArgumentTypeId() {
        return argumentTypeId;
    }

}
