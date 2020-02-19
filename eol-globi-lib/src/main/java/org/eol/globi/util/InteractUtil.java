package org.eol.globi.util;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.FileUtils;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class InteractUtil {

    public static final URI TYPE_IGNORED_URI_DEFAULT = URI.create("interaction_types_ignored.csv");
    public static final URI TYPE_MAP_URI_DEFAULT = URI.create("interaction_types.csv");
    private static final Set<String> UNLIKELY_INTERACTION_TYPE_NAMES
            = UnmodifiableSet.unmodifiableSet(new TreeSet<String>() {{
        add("(collected with)");
        add("collector number");
        add("(littermate or nestmate of)");
        add("(mate of)");
        add("mixed species flock");
        add("mosses");
        add("(offspring of)");
        add("(parent of)");
        add("(same individual as)");
        add("same litter");
        add("(same lot as)");
        add("(sibling of)");
    }});

    private static final Map<String, InteractType> INTERACTION_TYPE_NAME_MAP =
            UnmodifiableMap.unmodifiableMap(new HashMap<String, InteractType>() {{
                put("associated with", InteractType.RELATED_TO);
                put("ex", InteractType.HAS_HOST);
                put("ex.", InteractType.HAS_HOST);
                put("reared ex", InteractType.HAS_HOST);
                put("reared ex.", InteractType.HAS_HOST);
                put("host to", InteractType.HOST_OF);
                put("host", InteractType.HAS_HOST);
                put("h", InteractType.HAS_HOST);
                put("larval foodplant", InteractType.ATE);
                put("ectoparasite of", InteractType.ECTOPARASITE_OF);
                put("parasite of", InteractType.PARASITE_OF);
                put("stomach contents of", InteractType.EATEN_BY);
                put("stomach contents", InteractType.ATE);
                put("eaten by", InteractType.EATEN_BY);
                put("(ate)", InteractType.ATE);
                put("(eaten by)", InteractType.EATEN_BY);
                put("(parasite of)", InteractType.PARASITE_OF);
                put("(host of)", InteractType.HOST_OF);
                put("(in amplexus with)", InteractType.INTERACTS_WITH);
                put("consumption", InteractType.ATE);
                put("flower predator", InteractType.ATE);
                put("flower visitor", InteractType.VISITS_FLOWERS_OF);
                put("folivory", InteractType.ATE);
                put("fruit thief", InteractType.ATE);
                put("ingestion", InteractType.ATE);
                put("pollinator", InteractType.POLLINATES);
                put("seed disperser", InteractType.DISPERSAL_VECTOR_OF);
                put("seed predator", InteractType.ATE);
                put("n/a", null);
                put("neutral", null);
                put("unknown", null);
                put("vector of", InteractType.VECTOR_OF);
                put("found on", InteractType.INTERACTS_WITH);
                put("visitsflowersof", InteractType.VISITS_FLOWERS_OF);
                put("collected on", InteractType.INTERACTS_WITH);
                put("reared from", InteractType.INTERACTS_WITH);
                put("emerged from", InteractType.INTERACTS_WITH);
                put("collected in", InteractType.INTERACTS_WITH);
                put("hyperparasitoid of", InteractType.HYPERPARASITE_OF);
                put("on", InteractType.INTERACTS_WITH);
                put("under", InteractType.INTERACTS_WITH);
                put("inside", InteractType.INTERACTS_WITH);
                put("in", InteractType.INTERACTS_WITH);
            }});

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

    public static boolean ignoredInteractionTypeName(String interactionTypeNameCandidate) {
        return StringUtils.isBlank(interactionTypeNameCandidate)
                ? false
                : UNLIKELY_INTERACTION_TYPE_NAMES
                .contains(StringUtils.lowerCase(interactionTypeNameCandidate));

    }

    public static InteractType getInteractTypeForName(String interactionName) {
        InteractType interactType = InteractType.typeOf(interactionName);
        return interactType != null
                ? interactType
                : INTERACTION_TYPE_NAME_MAP.get(interactionName);
    }

    public static Map<String, InteractType> buildTypeMap(LabeledCSVParser labeledCSVParser) throws IOException {
        Map<String, InteractType> typeMap = new TreeMap<>();
        while (labeledCSVParser.getLine() != null) {
            String inatIdString = labeledCSVParser.getValueByLabel("observation_field_id");
            String inatId = StringUtils.trim(inatIdString);

            if (StringUtils.isBlank(inatId)) {
                throw new IOException("failed to map interaction type [" + inatIdString + "] on line [" + labeledCSVParser.lastLineNumber() + "]");
            } else {
                String interactionTypeId = labeledCSVParser.getValueByLabel("interaction_type_id");
                InteractType interactType = InteractType.typeOf(interactionTypeId);
                if (interactType == null) {
                    throw new IOException("failed to map interaction type [" + interactionTypeId + "] on line [" + labeledCSVParser.lastLineNumber() + "]");
                } else {
                    typeMap.put(inatId, interactType);
                }
            }
        }
        return typeMap;
    }

    public static List<String> buildTypesIgnored(LabeledCSVParser labeledCSVParser) throws IOException {
        List<String> typeMap1 = new ArrayList<>();
        while (labeledCSVParser.getLine() != null) {
            String inatIdString = labeledCSVParser.getValueByLabel("observation_field_id");
            typeMap1.add(StringUtils.trim(inatIdString));
        }
        return typeMap1;
    }

    public static TermLookupService getTermLookupService(ResourceService<URI> dataset) throws StudyImporterException {
        List<String> typesIgnored;
        try {
            LabeledCSVParser labeledCSVParser = parserFor(TYPE_IGNORED_URI_DEFAULT, dataset);
            typesIgnored = buildTypesIgnored(labeledCSVParser);
        } catch (IOException e) {
            throw new StudyImporterException("failed to load ignored interaction types from [" + TYPE_IGNORED_URI_DEFAULT + "]");
        }
        Map<String, InteractType> typeMap;
        try {
            LabeledCSVParser labeledCSVParser = parserFor(TYPE_MAP_URI_DEFAULT, dataset);
            typeMap = buildTypeMap(labeledCSVParser);
        } catch (IOException e) {
            throw new StudyImporterException("failed to load interaction mapping from [" + TYPE_MAP_URI_DEFAULT + "]", e);
        }
        return getTermLookupService(typesIgnored, typeMap);
    }

    public static LabeledCSVParser parserFor(URI typeIgnoredURI, ResourceService<URI> resourceService) throws IOException {
        InputStream is = resourceService.retrieve(typeIgnoredURI);
        return CSVTSVUtil.createLabeledCSVParser(FileUtils.getUncompressedBufferedReader(is, CharsetConstant.UTF8));
    }

    public static TermLookupService getTermLookupService(List<String> typesIgnored, Map<String, InteractType> typeMap) {
        return new TermLookupService() {


            @Override
            public List<Term> lookupTermByName(String name) throws TermLookupServiceException {
                List<Term> matchingTerms = Collections.emptyList();
                if (!typesIgnored.contains(name)) {
                    InteractType interactType = typeMap.get(name);
                    if (interactType != null) {
                        matchingTerms = new ArrayList<Term>() {{
                           add(new TermImpl(interactType.getIRI(), interactType.getLabel()));
                        }};
                    }
                }
                return matchingTerms;
            }
        };
    }
}
