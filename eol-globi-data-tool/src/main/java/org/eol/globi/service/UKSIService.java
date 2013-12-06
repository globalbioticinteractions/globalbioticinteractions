package org.eol.globi.service;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;
import org.apache.commons.io.IOUtils;
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

public class UKSIService implements TaxonPropertyLookupService {
    private static final Log LOG = LogFactory.getLog(UKSIService.class);

    private TaxonLookupServiceImpl service;

    @Override
    public void lookupPropertiesByName(String taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        if (service == null) {
            doInit();
        }
        try {
            TaxonTerm[] taxonTerms = service.lookupTermsByName(taxonName);
            if (taxonTerms.length > 1) {
                LOG.info("found more than 1 term for [" + taxonName + "], picking first");
            }
            if (taxonTerms.length > 0) {
                TaxonTerm match = taxonTerms[0];
                properties.put(Taxon.NAME, match.getName());
                properties.put(Taxon.EXTERNAL_ID, match.getId());
            }
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("failed to lookup [" + taxonName + "]", e);
        }
    }

    @Override
    public void shutdown() {
        if (null != service) {
            service.destroy();
            service = null;
        }
    }

    private void doInit() throws TaxonPropertyLookupServiceException {
        LOG.info("[" + UKSIService.class.getSimpleName() + "] instantiating...");
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
            LOG.warn("[" + UKSIService.class.getSimpleName() + "] instantiation failed.");
            throw new TaxonPropertyLookupServiceException("failed to created index", e);
        }
        service.finish();
        LOG.info("[" + UKSIService.class.getSimpleName() + "] instantiated.");
    }
}
