package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.translate.AggregateTranslator;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.LookupTranslator;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupServiceException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class InteractUtil {

    public static String allInteractionsCypherClause() {
        return interactionsCypherClause(InteractType.values());
    }

    public static String joinInteractTypes(Collection<InteractType> interactTypes) {
        return StringUtils.join(interactTypes, CharsetConstant.SEPARATOR_CHAR);
    }

    public static String interactionsCypherClause(InteractType... values) {
        TreeSet<InteractType> types = new TreeSet<>();
        for (InteractType value : values) {
            types.addAll(InteractType.typesOf(value));
        }
        return joinInteractTypes(types);
    }

    public static InteractTypeMapper createInteractionTypeMapper(ResourceService resourceService) throws TermLookupServiceException {
        return new InteractTypeMapperFactoryWithFallback(
                        new InteractTypeMapperFactoryImpl(resourceService),
                        new InteractTypeMapperFactoryImpl())
                .create();
    }

    public static InteractTypeMapper createInteractionTypeMapperForImporter(ResourceService dataset) throws StudyImporterException {
        try {
            return createInteractionTypeMapper(dataset);
        } catch (TermLookupServiceException e) {
            throw new StudyImporterException("failed to create interaction type mapper", e);
        }
    }

    public static void putNotBlank(Map<String, String> link, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            link.put(key, value);
        }
    }

    public static void putIfKeyNotExistsAndValueNotBlank(Map<String, String> link, String key, String value) {
        if (StringUtils.isBlank(link.get(key))) {
            putNotBlank(link, key, value);
        }
    }

    public static String normalizeInteractionNameOrId(String name) {
        final String translate = removeQuotesAndBackslashes(name);
        return StringUtils.lowerCase(StringUtils.trim(translate));
    }


    private static final CharSequenceTranslator mappingNormalizer;

    static {
        final Map<CharSequence, CharSequence> escapeJavaMap = new HashMap<CharSequence, CharSequence>() {{
            put("\"", "");
            put("\\", "");
        }};
        mappingNormalizer = new AggregateTranslator(
                new LookupTranslator(Collections.unmodifiableMap(escapeJavaMap)));
    }

    public static String removeQuotesAndBackslashes(String name) {
        return mappingNormalizer.translate(name);
    }
}
