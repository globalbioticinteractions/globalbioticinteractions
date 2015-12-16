package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.ResourceUtil;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TaxonCacheService implements PropertyEnricher {
    private static final Log LOG = LogFactory.getLog(TaxonCacheService.class);

    private HTreeMap<String, Map<String, String>> taxaById = null;
    private HTreeMap<String, List<Map<String, String>>> taxaMapById = null;
    private HTreeMap<String, List<Map<String, String>>> taxaMapByName = null;
    private String taxonCacheResource;
    private final String taxonMapResource;

    public TaxonCacheService(String taxonCacheResource, String taxonMapResource) {
        this.taxonCacheResource = taxonCacheResource;
        this.taxonMapResource = taxonMapResource;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        lazyInit();
        Taxon taxon = TaxonUtil.mapToTaxon(properties);
        Map<String, String> enriched = null;
        if (StringUtils.isNotBlank(taxon.getExternalId())) {
            enriched = getFirst(taxaMapById.get(taxon.getExternalId()));
        }
        if (enriched == null && StringUtils.isNotBlank(taxon.getName())) {
            enriched = getFirst(taxaMapByName.get(taxon.getName()));
        }
        return enriched == null ? Collections.unmodifiableMap(properties) : enriched;
    }

    public Map<String, String> getFirst(List<Map<String, String>> targets) {
        Map<String, String> enriched = null;
        if (targets != null) {
            for (Map<String, String> target : targets) {
                if (target != null) {
                    enriched = taxaById.get(TaxonUtil.mapToTaxon(target).getExternalId());
                    if (enriched != null) {
                        break;
                    }
                }
            }
        }
        return enriched;
    }

    public void lazyInit() throws PropertyEnricherException {
        if (taxaById == null || taxaMapById == null || taxaMapByName == null) {
            init();
        }
    }

    public void init() throws PropertyEnricherException {
        LOG.info("taxon cache initializing...");
        DB db = DBMaker
                .newMemoryDirectDB()
                .compressionEnable()
                .transactionDisable()
                .make();
        final HTreeMap<String, Map<String, String>> lookupById = db
                .createHashMap("taxonCacheById")
                .make();
        final HTreeMap<String, List<Map<String, String>>> mappingById = db
                .createHashMap("taxonMappingById")
                .make();
        final HTreeMap<String, List<Map<String, String>>> mappingByName = db
                .createHashMap("taxonMappingByName")
                .make();

        TaxonCacheListener taxonListener = new TaxonCacheListener() {
            @Override
            public void start() {

            }

            @Override
            public void addTaxon(Taxon taxon) {
                final String externalId = taxon.getExternalId();
                if (StringUtils.isNotBlank(externalId)) {
                    lookupById.put(externalId, TaxonUtil.taxonToMap(taxon));
                }
            }

            @Override
            public void finish() {
                taxaById = lookupById;
            }
        };
        try {
            BufferedReader reader = createBufferedReader(taxonCacheResource);
            new TaxonCacheParser().parse(reader, taxonListener);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse [" + taxonCacheResource + "]", e);
        }

        try {
            BufferedReader reader = createBufferedReader(taxonMapResource);
            new TaxonMapParser().parse(reader, new TaxonMapListener() {

                @Override
                public void start() {

                }

                @Override
                public void addMapping(final Taxon srcTaxon, Taxon targetTaxon) {
                    populate(srcTaxon.getExternalId(), targetTaxon.getExternalId(), taxaById, mappingById);
                    populate(srcTaxon.getName(), targetTaxon.getExternalId(), taxaById, mappingByName);
                }

                @Override
                public void finish() {
                    taxaMapById = mappingById;
                    taxaMapByName = mappingByName;
                }
            });
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse [" + taxonCacheResource + "]", e);
        }
        LOG.info("taxon cache initialized.");
    }

    public void populate(String sourceValue, String targetValue, Map<String, Map<String, String>> taxaBy, Map<String, List<Map<String, String>>> mappingBy) {
        if (StringUtils.isNotBlank(sourceValue)
                && !StringUtils.equals(sourceValue, PropertyAndValueDictionary.NO_MATCH)
                && !StringUtils.equals(sourceValue, PropertyAndValueDictionary.NO_NAME)
                && StringUtils.isNotBlank(targetValue)) {

            final Map<String, String> target = taxaBy.get(targetValue);
            if (target != null) {
                final List<Map<String, String>> currentTargets = mappingBy.get(sourceValue);
                final List<Map<String, String>> newTargets = currentTargets == null
                        ? new ArrayList<Map<String, String>>()
                        : new ArrayList<Map<String, String>>(currentTargets);
                newTargets.add(target);
                mappingBy.put(sourceValue, newTargets);
            }
        }
    }

    public BufferedReader createBufferedReader(String taxonResourceUrl) throws IOException {
        return new BufferedReader(new InputStreamReader(ResourceUtil.asInputStream(taxonResourceUrl, TaxonCacheService.class)));
    }

    @Override
    public void shutdown() {

    }
}
