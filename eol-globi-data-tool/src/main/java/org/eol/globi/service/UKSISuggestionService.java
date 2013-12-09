package org.eol.globi.service;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.taxon.TaxonLookupServiceImpl;
import org.eol.globi.data.taxon.TaxonTerm;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class UKSISuggestionService implements TaxonPropertyLookupService, NameSuggestor {
    private static final Log LOG = LogFactory.getLog(UKSISuggestionService.class);

    private TaxonLookupServiceImpl service;

    @Override
    public String suggest(String name) {
        String suggestion = null;
        try {
            TaxonTerm match = findMatch(name);
            suggestion = match == null ? name : match.getName();
        } catch (TaxonPropertyLookupServiceException e) {
            LOG.warn("failed to find suggestion for name [" + name + "]", e);
        }
        return suggestion;
    }

    @Override
    public void lookupPropertiesByName(String taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        TaxonTerm match = findMatch(taxonName);

        if (match != null) {
            properties.put(Taxon.NAME, match.getName());
            properties.put(Taxon.EXTERNAL_ID, match.getId());
        }

    }

    private TaxonTerm findMatch(String taxonName) throws TaxonPropertyLookupServiceException {
        TaxonTerm match = null;
        if (service == null) {
            doInit();
        }
        try {
            TaxonTerm[] taxonTerms = service.lookupTermsByName(taxonName);
            if (taxonTerms.length > 0) {
                match = taxonTerms[0];
            }
            if (taxonTerms.length > 1) {
                LOG.info("found more than 1 term for [" + taxonName + "], picking first [" + match.getName() + "]");
            }

        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("failed to lookup [" + taxonName + "]", e);
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

    private void doInit() throws TaxonPropertyLookupServiceException {
        LOG.info("[" + UKSISuggestionService.class.getSimpleName() + "] instantiating...");
        service = new TaxonLookupServiceImpl();
        service.start();
        try {
            InputStream is = new GZIPInputStream(getClass().getResourceAsStream("/org/eol/globi/data/uksi/NfWD.mdb.gz"));
            File tempFile = File.createTempFile("NfWD", "mdb");
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            IOUtils.copy(is, fileOutputStream);

            Database db = Database.open(tempFile, true);

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
            throw new TaxonPropertyLookupServiceException("failed to created index", e);
        }
        service.finish();
        LOG.info("[" + UKSISuggestionService.class.getSimpleName() + "] instantiated.");
    }

}
