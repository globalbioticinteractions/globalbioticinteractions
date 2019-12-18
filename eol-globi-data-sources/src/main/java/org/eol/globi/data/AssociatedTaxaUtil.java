package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.eol.globi.data.StudyImporterForTSV.ASSOCIATED_TAXA;

public final class AssociatedTaxaUtil {

    public static void expandNewLinkIfNeeded(InteractionListener interactionListener, Map<String, String> mappedLine) throws StudyImporterException {
        List<Map<String, String>> links = expandIfNeeded(mappedLine);
        for (Map<String, String> link : links) {
            interactionListener.newLink(link);
        }
    }

    public static List<Map<String, String>> expandIfNeeded(Map<String, String> properties) {
        List<Map<String, String>> expandedList = Collections.singletonList(properties);
        String associatedTaxa = properties.get(ASSOCIATED_TAXA);
        return StringUtils.isNotBlank(associatedTaxa)
                ? expand(properties, associatedTaxa)
                : expandedList;
    }

    private static List<Map<String, String>> expand(Map<String, String> properties, String associatedTaxa) {
        List<Map<String, String>> maps = StudyImporterForDwCA.parseAssociatedTaxa(associatedTaxa);
        return maps.stream().map(x -> new TreeMap<String, String>(properties) {{
            putAll(x);
        }}).collect(Collectors.toList());
    }
}
