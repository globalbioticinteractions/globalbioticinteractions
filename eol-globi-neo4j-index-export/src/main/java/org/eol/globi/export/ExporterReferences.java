package org.eol.globi.export;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.util.DOIUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class ExporterReferences extends ExporterBase {

    public static final String IDENTIFIER = "http://purl.org/dc/terms/identifier";
    public static final String FULL_REFERENCE = "http://eol.org/schema/reference/full_reference";
    public static final String DOI = "http://purl.org/ontology/bibo/doi";
    public static final String URI = "http://purl.org/ontology/bibo/uri";

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
                URI,
                DOI,
                "http://schemas.talis.com/2005/address/schema#localityName"};
    }

    @Override
    protected String getRowType() {
        return "http://eol.org/schema/reference/Reference";
    }

    @Override
    protected void doExportStudy(Study study, Writer writer, boolean includeHeader) throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put(IDENTIFIER, referenceIdForStudy(study));
        properties.put(FULL_REFERENCE, referenceForStudy(study));
        properties.put(DOI, DOIUtil.urlForDOI(study.getDOI()));
        properties.put(URI, ExternalIdUtil.urlForExternalId(study.getExternalId()));
        writeProperties(writer, properties);
    }

    public static String referenceIdForStudy(Study study) {
        return "globi:ref:" + referenceId((StudyNode) study);
    }

    private String referenceForStudy(Study study) {
        return StringUtils.isBlank(study.getCitation()) ? study.getTitle() : study.getCitation();
    }


}
