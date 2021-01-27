package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.util.InteractUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static org.eol.globi.data.DatasetImporterForTSV.ASSOCIATED_TAXA;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;

public final class AssociatedTaxaUtil {

    public static void expandNewLinkIfNeeded(InteractionListener interactionListener, Map<String, String> mappedLine) throws StudyImporterException {
        List<Map<String, String>> links = expandIfNeeded(mappedLine);
        for (Map<String, String> link : links) {
            interactionListener.on(link);
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
        List<Map<String, String>> maps = parseAssociatedTaxa(associatedTaxa);
        return maps.stream().map(x -> new TreeMap<String, String>(properties) {{
            putAll(x);
        }}).collect(Collectors.toList());
    }

    static List<Map<String, String>> parseAssociatedTaxa(String s) {
        List<Map<String, String>> properties = new ArrayList<>();
        String[] parts = StringUtils.split(s, "|;,");
        for (String part : parts) {
            String trimmedPart = StringUtils.trim(part);
            Matcher matcher = DatasetImporterForDwCA.PATTERN_ASSOCIATED_TAXA_IDEA.matcher(trimmedPart);
            if (matcher.find()) {
                String genus = StringUtils.trim(matcher.group(1));
                String specificEpithet = StringUtils.trim(matcher.group(2));
                addDefaultInteractionForAssociatedTaxon(properties, genus + " " + specificEpithet);
            } else {
                Matcher matcher1 = DatasetImporterForDwCA.PATTERN_ASSOCIATED_TAXA_EAE.matcher(trimmedPart);
                if (matcher1.find()) {
                    String genus = StringUtils.trim(matcher1.group(2));
                    String specificEpithet = StringUtils.trim(matcher1.group(3));
                    addDefaultInteractionForAssociatedTaxon(properties, genus + " " + specificEpithet);
                } else {
                    String[] verbTaxon = StringUtils.splitByWholeSeparator(trimmedPart, ":", 2);
                    if (verbTaxon.length == 2) {
                        addSpecificInteractionForAssociatedTaxon(properties, verbTaxon);
                    } else {
                        addDefaultInteractionForAssociatedTaxon(properties, trimmedPart);
                    }

                }

            }


        }
        return properties;
    }

    private static void addSpecificInteractionForAssociatedTaxon(List<Map<String, String>> properties, String[] verbTaxon) {
        HashMap<String, String> e = new HashMap<>();
        String interactionTypeName = trimAndRemoveQuotes(verbTaxon[0]);
        e.put(INTERACTION_TYPE_NAME, interactionTypeName);
        e.put(TARGET_TAXON_NAME, trimAndRemoveQuotes(verbTaxon[1]));
        properties.add(e);
    }

    private static String trimAndRemoveQuotes(String verbatimTerm) {
        return StringUtils.trim(InteractUtil.removeQuotesAndBackslashes(verbatimTerm));
    }

    private static void addDefaultInteractionForAssociatedTaxon(List<Map<String, String>> properties, String part) {
        if (StringUtils.isNotBlank(part)) {
            if (DatasetImporterForDwCA.EX_NOTATION.matcher(StringUtils.trim(part)).matches()) {
                properties.add(new HashMap<String, String>() {{
                    put(TARGET_TAXON_NAME, part);
                    put(INTERACTION_TYPE_NAME, "ex");
                }});
            } else if (DatasetImporterForDwCA.REARED_EX_NOTATION.matcher(StringUtils.trim(part)).matches()) {
                properties.add(new HashMap<String, String>() {{
                    put(TARGET_TAXON_NAME, part);
                    put(INTERACTION_TYPE_NAME, "reared ex");
                }});
            } else {
                properties.add(new HashMap<String, String>() {{
                    put(TARGET_TAXON_NAME, part);
                    put(INTERACTION_TYPE_NAME, "");
                }});
            }
        }
    }
}
