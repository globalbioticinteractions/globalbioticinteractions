package org.eol.globi.export;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Study;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class ExporterReferences extends ExporterBase {

    public static final String IDENTIFIER = "http://purl.org/dc/terms/identifier";
    public static final String FULL_REFERENCE = "http://eol.org/schema/reference/full_reference";

    @Override
    protected String[] getFields() {
        return new String[]{
                IDENTIFIER,
                "http://eol.org/schema/reference/publicationType",
                FULL_REFERENCE,
                "http://eol.org/schema/reference/primaryTitle",
                "http://purl.org/dc/terms/title",
                "http://purl.org/ontology/bibo/pages",
                "http://purl.org/ontology/bibo/pageStart",
                "http://purl.org/ontology/bibo/pageEnd",
                "http://purl.org/ontology/bibo/volume",
                "http://purl.org/ontology/bibo/edition",
                "http://purl.org/dc/terms/publisher",
                "http://purl.org/ontology/bibo/authorList",
                "http://purl.org/ontology/bibo/editorList",
                "http://purl.org/dc/terms/created",
                "http://purl.org/dc/terms/language",
                "http://purl.org/ontology/bibo/uri",
                "http://purl.org/ontology/bibo/doi",
                "http://schemas.talis.com/2005/address/schema#localityName"};
    }

    @Override
    protected void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(IDENTIFIER, "globi:ref:" + study.getNodeID());
        properties.put(FULL_REFERENCE, compileReference(study));
        writeProperties(writer, properties);
    }

    private String compileReference(Study study) {
        StringBuilder reference = new StringBuilder();
        String contributor = study.getContributor();
        reference.append(StringUtils.isBlank(contributor) ? "" : contributor + " ");
        String publicationYear = study.getPublicationYear();
        reference.append(StringUtils.isBlank(publicationYear) ? "" : ("(" + publicationYear + ") "));
        String description = study.getDescription();
        reference.append(description);
        return reference.toString();
    }


}
