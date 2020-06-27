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

    public InteractTypeMapperFactoryImpl() {
        this(getResourceServiceForDefaultInteractionTypeMapping());
    }

    public InteractTypeMapperFactoryImpl(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    private static ResourceService getResourceServiceForDefaultInteractionTypeMapping() {
        return new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                URI supportedURI = getSupportedURI(resourceName);
                return supportedURI == null
                        ? null
                        : ResourceUtil.asInputStream(supportedURI.toString());
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

        return new TermLookupService() {
            @Override
            public List<Term> lookupTermByName(String name) throws TermLookupServiceException {
                return typesIgnored.contains(StringUtils.lowerCase(StringUtils.lowerCase(name)))
                        ? Collections.singletonList(new TermImpl(name, name))
                        : Collections.emptyList();
            }
        };
    }

    @Override
    public InteractTypeMapper create() throws TermLookupServiceException {

        final TermLookupService termMappingService
                = getTermLookupService(resourceService,
                "interaction_type_ignored",
                "provided_interaction_type_id",
                "provided_interaction_type_label",
                "mapped_to_interaction_type_id",
                TYPE_IGNORED_URI_DEFAULT,
                TYPE_MAP_URI_DEFAULT);

        final TermLookupService ignoredTermLookupService
                = getIgnoredTermService(resourceService,
                "interaction_type_ignored",
                TYPE_IGNORED_URI_DEFAULT);

        return new InteractTypeMapperWithFallbackImpl(
                new InteractTypeMapperImpl(ignoredTermLookupService, termMappingService),
                new InteractTypeMapperFactoryForRO().create());
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
            String providedInteractionId = StringUtils.trim(StringUtils.lowerCase(provideInteractionIdString));

            String provideInteractionNameString = labeledCSVParser.getValueByLabel(providedInteractionTypeNameColumnName);
            String providedInteractionName = StringUtils.trim(StringUtils.lowerCase(provideInteractionNameString));

            String interactionTypeId = StringUtils.lowerCase(StringUtils.trim(labeledCSVParser.getValueByLabel(mappedInteractionTypeIdColumnName)));
            InteractType interactType = InteractType.typeOf(interactionTypeId);
            if (interactType == null) {
                throw new TermLookupServiceException("failed to map interaction type to [" + interactionTypeId + "] on line [" + labeledCSVParser.lastLineNumber() + "]: interaction type unknown to GloBI");
            } else {
                if (StringUtils.isNotBlank(providedInteractionId)) {
                    if (typeMap.containsKey(providedInteractionId)) {
                        throw new TermLookupServiceException("provided id [" + providedInteractionId + "] already mapped");
                    }
                    typeMap.put(providedInteractionId, interactType);
                }

                if (StringUtils.isNotBlank(providedInteractionName)) {
                    InteractType interactType1 = typeMap.get(providedInteractionName);
                    if (interactType1 == null) {
                        typeMap.put(providedInteractionName, interactType);
                    } else {
                        if (StringUtils.isBlank(providedInteractionId)) {
                            throw new TermLookupServiceException("provided name [" + providedInteractionName + "] already mapped: please provide unique interaction type name/id");
                        }
                    }

                }
            }
        }
        return typeMap;
    }

    public static void assertColumnNamePresent(List<String> columnNames, String columnNameToCheck) throws TermLookupServiceException {
        if (!columnNames.contains(columnNameToCheck)) {
            throw new TermLookupServiceException("missing column [" + columnNameToCheck + "] in mapping file");
        }
    }

    public static List<String> buildTypesIgnored(LabeledCSVParser labeledCSVParser, String ignoredInteractionTypeColumnName) throws IOException {
        List<String> typeMap1 = new ArrayList<>();
        while (labeledCSVParser.getLine() != null) {
            String inatIdString = StringUtils.trim(StringUtils.lowerCase(labeledCSVParser.getValueByLabel(ignoredInteractionTypeColumnName)));
            if (StringUtils.isNotBlank(inatIdString)) {
                typeMap1.add(inatIdString);
            }
        }
        return typeMap1;
    }

    public static TermLookupService getTermLookupService(ResourceService resourceService,
                                                         String ignoredInteractionTypeColumnName,
                                                         String providedInteractionTypeIdColumnName,
                                                         String providedInteractionTypeNameColumnName,
                                                         String resolvedInteractionTypeIdColumnName,
                                                         URI ignoredTypeListURI,
                                                         URI interactionTypeMapURI) throws TermLookupServiceException {
        List<String> typesIgnored = getAndParseIgnoredTypeList(
                resourceService,
                ignoredInteractionTypeColumnName,
                ignoredTypeListURI);

        Map<String, InteractType> typeMap = getAndParseTypeMap(resourceService,
                providedInteractionTypeIdColumnName,
                providedInteractionTypeNameColumnName,
                resolvedInteractionTypeIdColumnName,
                interactionTypeMapURI);

        return getTermLookupService(typesIgnored, typeMap);
    }

    public static Map<String, InteractType> getAndParseTypeMap(ResourceService resourceService,
                                                               String providedInteractionTypeIdColumnName,
                                                               String providedInteractionTypeNameColumnName,
                                                               String resolvedInteractionTypeIdColumnName, URI interactionTypeMapURI) throws TermLookupServiceException {
        Map<String, InteractType> typeMap;
        try {
            LabeledCSVParser labeledCSVParser = parserFor(interactionTypeMapURI, resourceService);
            typeMap = buildTypeMap(labeledCSVParser, providedInteractionTypeIdColumnName, providedInteractionTypeNameColumnName, resolvedInteractionTypeIdColumnName);
            labeledCSVParser.close();
        } catch (IOException e) {
            throw new TermLookupServiceException("failed to load interaction mapping from [" + TYPE_MAP_URI_DEFAULT + "]", e);
        }
        return typeMap;
    }

    public static List<String> getAndParseIgnoredTypeList(ResourceService resourceService,
                                                          String ignoredInteractionTypeColumnName, URI ignoredTypeListURI) throws TermLookupServiceException {
        List<String> typesIgnored;
        try {
            LabeledCSVParser labeledCSVParser = parserFor(ignoredTypeListURI, resourceService);
            typesIgnored = buildTypesIgnored(labeledCSVParser, ignoredInteractionTypeColumnName);
            labeledCSVParser.close();
        } catch (IOException e) {
            throw new TermLookupServiceException("failed to load ignored interaction types from [" + TYPE_IGNORED_URI_DEFAULT + "]", e);
        }
        return typesIgnored;
    }

    public static LabeledCSVParser parserFor(URI resourceURI, ResourceService resourceService) throws IOException {
        InputStream is = resourceService.retrieve(resourceURI);
        return CSVTSVUtil.createLabeledCSVParser(FileUtils.getUncompressedBufferedReader(is, CharsetConstant.UTF8));
    }

    public static TermLookupService getTermLookupService(List<String> typesIgnored, Map<String, InteractType> typeMap) {
        return new TermLookupService() {


            @Override
            public List<Term> lookupTermByName(String name) throws TermLookupServiceException {
                List<Term> matchingTerms = Collections.emptyList();
                String lowercaseName = StringUtils.lowerCase(StringUtils.trim(name));
                if (!typesIgnored.contains(lowercaseName)) {
                    InteractType interactType = typeMap.get(lowercaseName);
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
