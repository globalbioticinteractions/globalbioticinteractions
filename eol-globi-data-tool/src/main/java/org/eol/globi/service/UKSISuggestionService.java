package org.eol.globi.service;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.taxon.TaxonLookupServiceImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class UKSISuggestionService implements PropertyEnricher, NameSuggester {
    private static final Log LOG = LogFactory.getLog(UKSISuggestionService.class);

    private TaxonLookupServiceImpl service;

    @Override
    public String suggest(String name) {
        String suggestion = null;
        try {
            Taxon match = findMatch(name);
            suggestion = match == null ? name : match.getName();
        } catch (PropertyEnricherException e) {
            LOG.warn("failed to find suggestion for name [" + name + "]", e);
        }
        return suggestion;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);
        Taxon match = findMatch(enrichedProperties.get(PropertyAndValueDictionary.NAME));
        if (match != null) {
            enrichedProperties.put(PropertyAndValueDictionary.NAME, match.getName());
            enrichedProperties.put(PropertyAndValueDictionary.EXTERNAL_ID, match.getExternalId());
        }
        return Collections.unmodifiableMap(enrichedProperties);
    }

    private Taxon findMatch(String taxonName) throws PropertyEnricherException {
        Taxon match = null;
        if (service == null) {
            doInit();
        }
        try {
            Taxon[] taxonTerms = service.lookupTermsByName(taxonName);
            if (taxonTerms.length > 0) {
                // pick the first one
                match = taxonTerms[0];
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup [" + taxonName + "]", e);
        }
        return match;
    }

    @Override
    public void shutdown() {
        if (null != service) {
            service.destroy();
            service = null;
        }
    }

    private void doInit() throws PropertyEnricherException {
        LOG.info("[" + UKSISuggestionService.class.getSimpleName() + "] instantiating...");
        service = new TaxonLookupServiceImpl();
        service.start();
        File tmpFile = null;
        try {
            InputStream is = new GZIPInputStream(getClass().getResourceAsStream("/org/eol/globi/data/uksi/NfWD.mdb.gz"));
            tmpFile = File.createTempFile("NfWD", "mdb");
            FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
            IOUtils.copy(is, fileOutputStream);

            Database db = DatabaseBuilder.open(tmpFile);

            Table names = db.getTable("NAMESERVER_FOR_WIDER_DELIVERY");
            for (Map<String, Object> study : names) {
                Object taxonName = study.get("TAXON_NAME");
                Object externalId = study.get("NBN_TAXON_VERSION_KEY");
                Object recommendedScientificName = study.get("RECOMMENDED_SCIENTIFIC_NAME");
                Taxon taxonTerm = new TaxonImpl();
                taxonTerm.setExternalId(TaxonomyProvider.ID_PREFIX_USKI + externalId);
                taxonTerm.setName(recommendedScientificName.toString());
                service.addTerm(taxonName.toString(), taxonTerm);
            }
        } catch (IOException e) {
            LOG.warn("[" + UKSISuggestionService.class.getSimpleName() + "] instantiation failed.");
            throw new PropertyEnricherException("failed to created link", e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
        service.finish();
        LOG.info("[" + UKSISuggestionService.class.getSimpleName() + "] instantiated.");
    }

}
