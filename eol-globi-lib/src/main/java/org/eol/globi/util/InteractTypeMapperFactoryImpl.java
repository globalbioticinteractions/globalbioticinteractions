package org.eol.globi.util;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.FileUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceConfigurationException;
import org.eol.globi.service.TermLookupServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class InteractTypeMapperFactoryImpl implements InteractTypeMapperFactory {

    public static final URI TYPE_IGNORED_URI_DEFAULT = URI.create("interaction_types_ignored.csv");
    public static final URI TYPE_MAP_URI_DEFAULT = URI.create("interaction_types_mapping.csv");
    private final ResourceService resourceService;

    public InteractTypeMapperFactoryImpl(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public static ResourceService getResourceServiceForDefaultInteractionTypeMapping(ResourceService service) {
        return new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                URI supportedURI = getSupportedURI(resourceName);
                return supportedURI == null || service == null
                        ? null
                        : service.retrieve(supportedURI);
            }

            URI getSupportedURI(URI resourceName) {
                List<URI> supportedResources = Arrays.asList(TYPE_IGNORED_URI_DEFAULT, TYPE_MAP_URI_DEFAULT);
                URI resource = URI.create("classpath:/org/globalbioticinteractions/" + resourceName);
                return supportedResources.contains(resourceName) ? resource : null;

            }
        };
    }

    public static TermLookupService getIgnoredTermService(ResourceService resourceService, String ignoredInteractionTypeColumnName, URI ignoredTypeListURI) throws TermLookupServiceException {
        final List<String> typesIgnored = getAndParseIgnoredTypeList(resourceService,
                ignoredInteractionTypeColumnName,
                ignoredTypeListURI);

        return name -> {
            final String nameNorm = InteractUtil.normalizeInteractionName(name);
            return StringUtils.isNotBlank(nameNorm) &&
                    (typesIgnored.contains(nameNorm) || typesIgnored.contains(StringUtils.trim(name)))
                    ? Collections.singletonList(new TermImpl(name, name))
                    : Collections.emptyList();
        };
    }

    @Override
    public InteractTypeMapper create() throws TermLookupServiceException {
        return new InteractTypeMapperWithFallbackImpl(
                mapperForResourceService(resourceService),
                mapperForResourceService(getResourceServiceForDefaultInteractionTypeMapping(resourceService)),
                new InteractTypeMapperFactoryForRO(resourceService).create()
        );
    }

    public InteractTypeMapperImpl mapperForResourceService(ResourceService resourceService) throws TermLookupServiceException {
        final TermLookupService ignoredTermLookupService
                = getIgnoredTermService(resourceService,
                "interaction_type_ignored",
                TYPE_IGNORED_URI_DEFAULT);

        final TermLookupService termMappingService
                = getTermLookupService(
                ignoredTermLookupService,
                resourceService,
                "provided_interaction_type_id",
                "provided_interaction_type_label",
                "mapped_to_interaction_type_id",
                TYPE_MAP_URI_DEFAULT);


        return new InteractTypeMapperImpl(ignoredTermLookupService, termMappingService);
    }

    public static Map<String, InteractType> buildTypeMap(LabeledCSVParser labeledCSVParser,
                                                         String providedInteractionTypeIdColumnName,
                                                         String providedInteractionTypeNameColumnName,
                                                         String mappedInteractionTypeIdColumnName)
            throws TermLookupServiceException, IOException {
        Map<String, InteractType> typeMap = new TreeMap<>();

        List<String> columnNames = Arrays.asList(labeledCSVParser.getLabels());
        assertColumnNamePresent(columnNames, providedInteractionTypeNameColumnName);
        assertColumnNamePresent(columnNames, providedInteractionTypeIdColumnName);
        assertColumnNamePresent(columnNames, mappedInteractionTypeIdColumnName);

        while (labeledCSVParser.getLine() != null) {
            String provideInteractionIdString = labeledCSVParser.getValueByLabel(providedInteractionTypeIdColumnName);
            String providedInteractionId = StringUtils.trim(provideInteractionIdString);

            String provideInteractionNameString = labeledCSVParser.getValueByLabel(providedInteractionTypeNameColumnName);
            String providedInteractionName = InteractUtil.normalizeInteractionName(StringUtils.lowerCase(provideInteractionNameString));

            String interactionTypeId = StringUtils.trim(labeledCSVParser.getValueByLabel(mappedInteractionTypeIdColumnName));
            InteractType interactType = InteractType.typeOf(interactionTypeId);
            if (interactType == null) {
                throw new TermLookupServiceConfigurationException("failed to map interaction type to [" + interactionTypeId + "] on line [" + labeledCSVParser.lastLineNumber() + "]: interaction type unknown to GloBI");
            } else {
                if (StringUtils.isBlank(providedInteractionId) && StringUtils.isBlank(providedInteractionName)) {
                    if (typeMap.containsKey("")) {
                        throw new TermLookupServiceConfigurationException("only one default/blank interaction type can be defined, but found duplicate on line [" + labeledCSVParser.lastLineNumber() + "]: [" + StringUtils.join(labeledCSVParser.getLine(), "|") + "]");
                    }
                    typeMap.put("", interactType);
                } else {
                    if (StringUtils.isNotBlank(providedInteractionId)) {
                        setOrThrow(typeMap, providedInteractionId, interactType);
                    }

                    if (StringUtils.isNotBlank(providedInteractionName)) {
                        setOrThrow(typeMap, providedInteractionId, providedInteractionName, interactType);
                    }
                }
            }
        }
        return typeMap;
    }

    private static void setOrThrow(Map<String, InteractType> typeMap, String providedInteractionId, String providedInteractionName, InteractType interactType) throws TermLookupServiceException {
        InteractType mappedInteractionType = typeMap.get(providedInteractionName);
        if (mappedInteractionType == null) {
            typeMap.put(providedInteractionName, interactType);
        } else {
            if (StringUtils.isBlank(providedInteractionId)) {
                throwAmbiguousMappingException(providedInteractionName, "name");
            }
        }
    }

    private static void throwAmbiguousMappingException(String providedValue, String providedKey) throws TermLookupServiceConfigurationException {
        throw new TermLookupServiceConfigurationException("multiple mappings for [" + providedKey + "]: [" + providedValue + "] were found, but only one unambiguous mapping is allowed");
    }

    private static void setOrThrow(Map<String, InteractType> typeMap, String providedInteractionId, InteractType interactType) throws TermLookupServiceException {
        if (typeMap.containsKey(providedInteractionId)) {
            throwAmbiguousMappingException(providedInteractionId, "id");
        }
        typeMap.put(providedInteractionId, interactType);
    }

    private static void assertColumnNamePresent(List<String> columnNames, String columnNameToCheck) throws TermLookupServiceException {
        if (!columnNames.contains(columnNameToCheck)) {
            throw new TermLookupServiceConfigurationException("missing column [" + columnNameToCheck + "] in mapping file");
        }
    }

    public static List<String> buildTypesIgnored(LabeledCSVParser labeledCSVParser, String ignoredInteractionTypeColumnName) throws IOException {
        List<String> typeMap = new ArrayList<>();
        while (labeledCSVParser.getLine() != null) {
            String ignoredIdString = InteractUtil.normalizeInteractionName(labeledCSVParser.getValueByLabel(ignoredInteractionTypeColumnName));
            if (StringUtils.isNotBlank(ignoredIdString)) {
                typeMap.add(ignoredIdString);
            }
        }
        return typeMap;
    }

    public static TermLookupService getTermLookupService(TermLookupService ignoredTermService,
                                                         ResourceService resourceService,
                                                         String providedInteractionTypeIdColumnName,
                                                         String providedInteractionTypeNameColumnName,
                                                         String resolvedInteractionTypeIdColumnName,
                                                         URI interactionTypeMapURI) throws TermLookupServiceException {

        Map<String, InteractType> typeMap = getAndParseTypeMap(resourceService,
                providedInteractionTypeIdColumnName,
                providedInteractionTypeNameColumnName,
                resolvedInteractionTypeIdColumnName,
                interactionTypeMapURI);

        return getTermLookupService(ignoredTermService, typeMap);
    }

    public static Map<String, InteractType> getAndParseTypeMap(ResourceService resourceService,
                                                               String providedInteractionTypeIdColumnName,
                                                               String providedInteractionTypeNameColumnName,
                                                               String resolvedInteractionTypeIdColumnName,
                                                               URI interactionTypeMapURI) throws TermLookupServiceException {
        Map<String, InteractType> typeMap = Collections.emptyMap();
        try (InputStream typeMapInputStream = resourceService.retrieve(interactionTypeMapURI)) {
            if (typeMapInputStream != null) {
                try {
                    LabeledCSVParser labeledCSVParser = parserFor(typeMapInputStream);
                    typeMap = buildTypeMap(labeledCSVParser, providedInteractionTypeIdColumnName, providedInteractionTypeNameColumnName, resolvedInteractionTypeIdColumnName);
                    labeledCSVParser.close();
                } catch (IOException e) {
                    throw new TermLookupServiceException("failed to load interaction mapping from [" + TYPE_MAP_URI_DEFAULT + "]", e);
                }
            }
        } catch (IOException e) {
            // silently fail on missing resource
        }
        return typeMap;
    }

    private static List<String> getAndParseIgnoredTypeList(ResourceService resourceService,
                                                           String ignoredInteractionTypeColumnName, URI ignoredTypeListURI) throws TermLookupServiceException {
        List<String> typesIgnored = Collections.emptyList();
        try (InputStream retrieve = resourceService.retrieve(ignoredTypeListURI)) {
            if (retrieve != null) {
                try {
                    LabeledCSVParser labeledCSVParser = parserFor(retrieve);
                    typesIgnored = buildTypesIgnored(labeledCSVParser, ignoredInteractionTypeColumnName);
                    labeledCSVParser.close();
                } catch (IOException e) {
                    throw new TermLookupServiceException("failed to load ignored interaction types from [" + TYPE_IGNORED_URI_DEFAULT + "]", e);
                }
            }
        } catch (IOException e) {
            // ignored failed retrieve from
        }
        return typesIgnored;
    }

    private static LabeledCSVParser parserFor(InputStream is) throws IOException {
        return CSVTSVUtil.createLabeledCSVParser(FileUtils.getUncompressedBufferedReader(is, CharsetConstant.UTF8));
    }

    public static TermLookupService getTermLookupService
            (TermLookupService ignoredTermLookup, Map<String, InteractType> typeMap) {
        return name -> {
            List<Term> matchingTerms = Collections.emptyList();
            List<Term> ignoredTerms = ignoredTermLookup.lookupTermByName(name);
            if (isNullOrEmpty(ignoredTerms)) {
                String nameNormalized = InteractUtil.normalizeInteractionName(name);
                InteractType interactType = typeMap.get(nameNormalized);
                interactType = interactType == null ? typeMap.get(name) : interactType;
                if (interactType != null) {
                    matchingTerms = getTerms(interactType);
                }
            }
            return matchingTerms;
        };
    }

    public static List<Term> getTerms(InteractType interactType) {
        List<Term> matchingTerms;
        matchingTerms = new ArrayList<Term>() {{
            add(new TermImpl(interactType.getIRI(), interactType.getLabel()));
        }};
        return matchingTerms;
    }

    public static boolean isNullOrEmpty(List<Term> ignoredTerms) {
        return ignoredTerms == null || ignoredTerms.size() == 0;
    }

}
