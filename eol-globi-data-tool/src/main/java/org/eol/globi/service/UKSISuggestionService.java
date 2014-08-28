package org.eol.globi.service;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.taxon.TaxonLookupServiceImpl;
import org.eol.globi.data.taxon.TaxonTerm;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class UKSISuggestionService implements PropertyEnricher, NameSuggestor {
    private static final Log LOG = LogFactory.getLog(UKSISuggestionService.class);

    private TaxonLookupServiceImpl service;

    @Override
    public String suggest(String name) {
        String suggestion = null;
        try {
            TaxonTerm match = findMatch(name);
            suggestion = match == null ? name : match.getName();
        } catch (PropertyEnricherException e) {
            LOG.warn("failed to find suggestion for name [" + name + "]", e);
        }
        return suggestion;
    }

    @Override
    public void enrich(Map<String, String> properties) throws PropertyEnricherException {
        TaxonTerm match = findMatch(properties.get(PropertyAndValueDictionary.NAME));
        if (match != null) {
            properties.put(PropertyAndValueDictionary.NAME, match.getName());
            properties.put(PropertyAndValueDictionary.EXTERNAL_ID, match.getId());
        }
    }

    private TaxonTerm findMatch(String taxonName) throws PropertyEnricherException {
        TaxonTerm match = null;
        if (service == null) {
            doInit();
        }
        try {
            TaxonTerm[] taxonTerms = service.lookupTermsByName(taxonName);
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

            Database db = Database.open(tmpFile, true);

            Table names = db.getTable("NAMESERVER_FOR_WIDER_DELIVERY");
            for (Map<String, Object> study : names) {
                Object taxonName = study.get("TAXON_NAME");
                Object externalId = study.get("NBN_TAXON_VERSION_KEY");
                Object recommendedScientificName = study.get("RECOMMENDED_SCIENTIFIC_NAME");
                TaxonTerm taxonTerm = new TaxonTerm();
                taxonTerm.setId(TaxonomyProvider.ID_PREFIX_USKI + externalId);
                taxonTerm.setName(recommendedScientificName.toString());
                service.addTerm(taxonName.toString(), taxonTerm);
            }
        } catch (IOException e) {
            LOG.warn("[" + UKSISuggestionService.class.getSimpleName() + "] instantiation failed.");
            throw new PropertyEnricherException("failed to created index", e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
        service.finish();
        LOG.info("[" + UKSISuggestionService.class.getSimpleName() + "] instantiated.");
    }

}
