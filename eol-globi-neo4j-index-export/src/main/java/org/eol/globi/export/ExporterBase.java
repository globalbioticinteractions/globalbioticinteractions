package org.eol.globi.export;

import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

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

    protected abstract void doExportStudy(Study study, ExportUtil.Appender appender, boolean includeHeader) throws IOException;

    final public void exportStudy(Study study, ExportUtil.Appender appender, boolean includeHeader) throws IOException {
        if (includeHeader) {
            writeHeader(appender);
        }
        doExportStudy(study, appender, includeHeader);
    }

    private void writeHeader(ExportUtil.Appender appender) throws IOException {
        String[] fields = getFields();
        String[] headers = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            int index = field.lastIndexOf("/");
            String fieldSuffix = index > 0 ? field.substring(index + 1) : field;
            headers[i] = fieldSuffix;
        }
        appender.append(Stream.of(headers));
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

    protected void writeProperties(ExportUtil.Appender writer, Map<String, String> properties) throws IOException {
        writer.append("\n");
        String[] fields = getFields();
        ExportUtil.writeProperties(writer, new ExportUtil.TsvValueJoiner(), properties, fields);
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
