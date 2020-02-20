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
    public static final URI TYPE_MAP_URI_DEFAULT = URI.create("interaction_types.csv");
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

            @Override
            public URI getLocalURI(URI resourceName) throws IOException {
                return getSupportedURI(resourceName);
            }

            public URI getSupportedURI(URI resourceName) {
                List<URI> supportedResources = Arrays.asList(TYPE_IGNORED_URI_DEFAULT, TYPE_MAP_URI_DEFAULT);
                URI resource = URI.create("classpath:/org/globalbioticinteractions/" + resourceName);
                return supportedResources.contains(resourceName) ? resource : null;

            }
        };
    }

    public static TermLookupService getIgnoredTermService(ResourceService resourceService) throws TermLookupServiceException {
        final List<String> typesIgnored = getAndParseIgnoredTypeList(resourceService);

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
                = getTermLookupService(resourceService);

        final TermLookupService ignoredTermLookupService
                = getIgnoredTermService(resourceService);

        return new InteractTypeMapper() {

            @Override
            public boolean shouldIgnoreInteractionType(String interactionTypeNameOrId) {
                boolean shouldIgnore = false;
                try {
                    if (StringUtils.isBlank(interactionTypeNameOrId)) {
                        shouldIgnore = true;
                    } else {
                        List<Term> terms = ignoredTermLookupService.lookupTermByName(interactionTypeNameOrId);
                        shouldIgnore = terms != null && !terms.isEmpty();
                    }
                } catch (TermLookupServiceException e) {

                }
                return shouldIgnore;
            }

            @Override
            public InteractType getInteractType(String interactionTypeNameOrId) {
                InteractType firstMatchingType = InteractType.typeOf(interactionTypeNameOrId);
                if (firstMatchingType == null) {
                    try {
                        List<Term> terms = termMappingService.lookupTermByName(interactionTypeNameOrId);
                        if (terms != null && terms.size() > 0) {
                            firstMatchingType = InteractType.typeOf(terms.get(0).getId());
                        }
                    } catch (TermLookupServiceException e) {
                        //
                    }
                }
                return firstMatchingType;

            }
        };
    }

    public static Map<String, InteractType> buildTypeMap(LabeledCSVParser labeledCSVParser) throws TermLookupServiceException, IOException {
        Map<String, InteractType> typeMap = new TreeMap<>();
        while (labeledCSVParser.getLine() != null) {
            String provideInteractionIdString = labeledCSVParser.getValueByLabel("observation_field_id");
            String providedInteractionId = StringUtils.trim(StringUtils.lowerCase(provideInteractionIdString));

            String provideInteractionNameString = labeledCSVParser.getValueByLabel("observation_field_name");
            String providedInteractionName = StringUtils.trim(StringUtils.lowerCase(provideInteractionNameString));

            String interactionTypeId = StringUtils.lowerCase(StringUtils.trim(labeledCSVParser.getValueByLabel("interaction_type_id")));
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

    public static List<String> buildTypesIgnored(LabeledCSVParser labeledCSVParser) throws IOException {
        List<String> typeMap1 = new ArrayList<>();
        while (labeledCSVParser.getLine() != null) {
            String inatIdString = StringUtils.trim(StringUtils.lowerCase(labeledCSVParser.getValueByLabel("observation_field_id")));
            if (StringUtils.isNotBlank(inatIdString)) {
                typeMap1.add(inatIdString);
            }
        }
        return typeMap1;
    }

    public static TermLookupService getTermLookupService(ResourceService resourceService) throws TermLookupServiceException {
        List<String> typesIgnored = getAndParseIgnoredTypeList(resourceService);
        Map<String, InteractType> typeMap = getAndParseTypeMap(resourceService);
        return getTermLookupService(typesIgnored, typeMap);
    }

    public static Map<String, InteractType> getAndParseTypeMap(ResourceService resourceService) throws TermLookupServiceException {
        Map<String, InteractType> typeMap;
        try {
            LabeledCSVParser labeledCSVParser = parserFor(TYPE_MAP_URI_DEFAULT, resourceService);
            typeMap = buildTypeMap(labeledCSVParser);
            labeledCSVParser.close();
        } catch (IOException e) {
            throw new TermLookupServiceException("failed to load interaction mapping from [" + TYPE_MAP_URI_DEFAULT + "]", e);
        }
        return typeMap;
    }

    public static List<String> getAndParseIgnoredTypeList(ResourceService resourceService) throws TermLookupServiceException {
        List<String> typesIgnored;
        try {
            LabeledCSVParser labeledCSVParser = parserFor(TYPE_IGNORED_URI_DEFAULT, resourceService);
            typesIgnored = buildTypesIgnored(labeledCSVParser);
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
