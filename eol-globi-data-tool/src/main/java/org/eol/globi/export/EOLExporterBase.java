package org.eol.globi.export;

import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public abstract class EOLExporterBase extends BaseExporter {

    protected static String getQueryForDistinctTargetTaxaForPreyBySourceTaxa(Study study) {
        return "START study = node:studies(title='" + study.getTitle() + "') " +
                "MATCH predatorTaxon<-[:CLASSIFIED_AS]-predator-[interaction:ATE]->prey-[:CLASSIFIED_AS]->preyTaxon, study-[:COLLECTED]->predator " +
                "RETURN distinct(predatorTaxon) as predatorTaxon, count(distinct(predator)), interaction, collect(distinct(preyTaxon)) as preyTaxa, count(distinct(prey)), study";
    }

    protected static void addProperty(Map<String, String> properties, PropertyContainer node, String propertyName, String fieldName) throws IOException {
        if (node != null && node.hasProperty(propertyName)) {
            properties.put(fieldName, node.getProperty(propertyName).toString());
        }
    }

    protected abstract String[] getFields();

    protected abstract void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException;

    final public void exportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        if (includeHeader) {
            writeHeader(writer);
        }
        doExportStudy(study, writer, includeHeader);
    }

    private void writeHeader(Writer writer) throws IOException {
        String[] fields = getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            int index = field.lastIndexOf("/");
            String fieldSuffix = index > 0 ? field.substring(index + 1) : field;
            writeHeaderField(writer, fields, i, fieldSuffix);
        }

        writer.write("\n");
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            writeHeaderField(writer, fields, i, field);
        }
    }

    private void writeHeaderField(Writer writer, String[] fields, int i, String field) throws IOException {
        writer.write("\"");
        writer.write(field);
        writer.write("\"");
        if (i < (fields.length - 1)) {
            writer.write(",");
        }
    }


    @Override
    protected String getMetaTableSuffix() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("</location>\n")
                .append("    </files>\n");

        String[] fields = getFields();
        for (int i = 0; i < fields.length; i++) {
            buffer.append("    <field index=\"")
                    .append(i)
                    .append("\" term=\"")
                    .append(fields[i])
                    .append("\"/>\n");
        }
        return buffer.append("</table>\n").toString();
    }

    @Override
    protected String getMetaTablePrefix() {
        return "<table encoding=\"UTF-8\" fieldsTerminatedBy=\",\" linesTerminatedBy=\"\\n\" ignoreHeaderLines=\"1\" rowType=\"http://rs.tdwg.org/dwc/terms/DarwinRecord\">\n" +
                "    <files>\n" +
                "      <location>";
    }

    protected void writeProperties(Writer writer, Map<String, String> properties) throws IOException {
        writer.write("\n");
        String[] fields = getFields();
        for (int i = 0; i < fields.length; i++) {
            if (properties.containsKey(fields[i])) {
                writer.write(properties.get(fields[i]));
            }
            if (i < (fields.length - 1)) {
                writer.write(",");
            }
        }
    }


    protected void addCollectionDate(Map<String, String> writer, Relationship collectedRelationship, String datePropertyName) throws IOException {
        Calendar instance;
        if (collectedRelationship.hasProperty(Specimen.DATE_IN_UNIX_EPOCH)) {
            Long epoch = (Long) collectedRelationship.getProperty(Specimen.DATE_IN_UNIX_EPOCH);
            Date date = new Date(epoch);
            instance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            instance.setTime(date);
            writer.put(datePropertyName, javax.xml.bind.DatatypeConverter.printDateTime(instance));
        }

    }

}
