package org.eol.globi.data;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CMECSService implements TermLookupService {

    private static Logger LOG = LoggerFactory.getLogger(CMECSService.class);

    private Map<String, Term> termMap = null;

    private final ResourceService service;

    public CMECSService(ResourceService resourceService) {
        this.service = resourceService;
    }

    @Override
    public List<Term> lookupTermByName(String name) throws TermLookupServiceException {
        if (termMap == null) {
            try {
                termMap = buildTermMap(getService());
            } catch (IOException e) {
                throw new TermLookupServiceException("failed to instantiate terms", e);
            }
        }
        Term term = termMap.get(StringUtils.lowerCase(StringUtils.trim(name)));
        return term == null ? Collections.emptyList() : Collections.singletonList(term);
    }

    private static Map<String, Term> buildTermMap(ResourceService service) throws IOException {
        LOG.info(CMECSService.class.getSimpleName() + " instantiating...");
        URI uri = URI.create("https://cmecscatalog.org/cmecs/documents/cmecs4.accdb");
        LOG.info("CMECS data [" + uri + "] downloading ...");

        File mdbFile = null;
        try {
            mdbFile = File.createTempFile("cmecs", "tmp.mdb");
            mdbFile.deleteOnExit();
            FileUtils.copyToFile(service.retrieve(uri), mdbFile);

            Database db = new DatabaseBuilder()
                    .setFile(mdbFile)
                    .setReadOnly(true)
                    .open();

            Map<String, Term> aquaticSettingsTerms = new HashMap<>();

            Table table = db.getTable("Aquatic Setting");
            Map<String, Object> row;
            while ((row = table.getNextRow()) != null) {
                Integer id = (Integer) row.get("AquaticSetting_Id");
                String name = (String) row.get("AquaticSettingName");
                String termId = TaxonomyProvider.ID_CMECS + id;
                aquaticSettingsTerms.put(StringUtils.lowerCase(StringUtils.strip(name)), new TermImpl(termId, name));
            }
            LOG.info(CMECSService.class.getSimpleName() + " instantiated.");
            return aquaticSettingsTerms;
        } finally {
            FileUtils.deleteQuietly(mdbFile);
        }
    }

    public ResourceService getService() {
        return service;
    }

}
