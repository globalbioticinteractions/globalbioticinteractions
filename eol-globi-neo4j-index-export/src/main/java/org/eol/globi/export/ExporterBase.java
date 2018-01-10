package org.eol.globi.export;

import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public abstract class ExporterBase extends DarwinCoreExporter {

    protected static void addProperty(Map<String, String> properties, PropertyContainer node, String propertyName, String fieldName) throws IOException {
        if (node != null && node.hasProperty(propertyName)) {
            properties.put(fieldName, node.getProperty(propertyName).toString());
        }
    }

    public static String referenceId(NodeBacked node) {
        return Long.toString(node.getUnderlyingNode().getId());
    }

    protected abstract String[] getFields();

    protected abstract String getRowType();

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
    }

    private void writeHeaderField(Writer writer, String[] fields, int i, String field) throws IOException {
        writer.write(field);
        if (i < (fields.length - 1)) {
            writer.write("\t");
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
        return "<table encoding=\"UTF-8\" fieldsTerminatedBy=\"\\t\" linesTerminatedBy=\"\\n\" fieldsEnclosedBy=\"\" ignoreHeaderLines=\"1\" rowType=\"" + getRowType() + "\">\n" +
                "    <files>\n" +
                "      <location>";
    }

    protected void writeProperties(Writer writer, Map<String, String> properties) throws IOException {
        writer.write("\n");
        String[] fields = getFields();
        ExportUtil.writeProperties(writer, properties, fields);
    }


    protected void addCollectionDate(Map<String, String> writer, Relationship collectedRelationship, String datePropertyName) throws IOException {
        Calendar instance;
        if (collectedRelationship.hasProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH)) {
            Long epoch = (Long) collectedRelationship.getProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH);
            Date date = new Date(epoch);
            instance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            instance.setTime(date);
            writer.put(datePropertyName, javax.xml.bind.DatatypeConverter.printDateTime(instance));
        }

    }

}
