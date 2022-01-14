package org.eol.globi.export;

import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.DateUtil;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
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

    protected abstract void doExportStudy(StudyNode study, ExportUtil.Appender appender, boolean includeHeader) throws IOException;

    final public void exportStudy(StudyNode study, ExportUtil.Appender appender, boolean includeHeader) throws IOException {
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

    protected void writeProperties(ExportUtil.Appender appender, Map<String, String> properties) throws IOException {
        String[] fields = getFields();
        ExportUtil.writeProperties(appender, properties, fields);
    }


    protected void addCollectionDate(Map<String, String> writer, Relationship collectedRelationship, String datePropertyName) throws IOException {
        if (collectedRelationship.hasProperty(SpecimenConstant.EVENT_DATE)) {
            String dateString = (String) collectedRelationship.getProperty(SpecimenConstant.EVENT_DATE);
            writer.put(datePropertyName, dateString);
        }

    }

}
