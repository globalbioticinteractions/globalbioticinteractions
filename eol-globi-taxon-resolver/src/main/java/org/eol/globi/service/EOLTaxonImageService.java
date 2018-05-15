package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.taxon.EOLService;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.util.Map;

public class EOLTaxonImageService implements ImageSearch {

    public TaxonImage lookupImageForExternalId(String externalId) throws IOException {
        try {
            return ExternalIdUtil.isSupported(externalId) ? lookupImage(externalId) : null;
        } catch (PropertyEnricherException e) {
            throw new IOException(e);
        }
    }

    private TaxonImage lookupImage(String externalId) throws PropertyEnricherException {
        TaxonImage image;
        Map<String, String> enrich = new EOLService().enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));
        Taxon taxon = TaxonUtil.mapToTaxon(enrich);
        image = taxonToTaxonImage(taxon);
        return image;
    }

    private static TaxonImage taxonToTaxonImage(Taxon taxon) {
        TaxonImage image = new TaxonImage();
        if (StringUtils.isNotBlank(taxon.getCommonNames())) {
            String[] names = StringUtils.split(taxon.getCommonNames(), CharsetConstant.SEPARATOR_CHAR);
            for (String name : names) {
                String[] nameParts = StringUtils.split(name, CharsetConstant.LANG_SEPARATOR_CHAR);
                if (nameParts.length > 1) {
                    String vernacularName = StringUtils.trim(nameParts[0]);
                    String language = StringUtils.trim(nameParts[1]);
                    if ("en".equals(language)) {
                        String commonName = vernacularName.replaceAll("\\(.*\\)", "");
                        String capitalize = WordUtils.capitalize(commonName);
                        image.setCommonName(capitalize.replaceAll("\\sAnd\\s", " and "));
                        break;
                    }

                }
            }
        }

        image.setThumbnailURL(taxon.getThumbnailUrl());
        image.setImageURL(taxon.getThumbnailUrl());
        image.setInfoURL(taxon.getExternalUrl());
        image.setScientificName(taxon.getName());
        image.setTaxonPath(taxon.getPath());
        if (StringUtils.startsWith(taxon.getExternalId(), TaxonomyProvider.ID_PREFIX_EOL)) {
            image.setPageId(StringUtils.replace(taxon.getExternalId(), TaxonomyProvider.ID_PREFIX_EOL, ""));
        }
        return image;
    }


    public void shutdown() {

    }

}
