package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.ImageSearch;
import org.eol.globi.service.SearchContext;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class ImageService {

    ImageSearch imageSearch = new WikiDataImageSearch();

    @Autowired
    private TaxonSearch taxonSearch;

    @RequestMapping(value = "/imagesForName/{scientificName}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public TaxonImage findTaxonImagesForTaxonWithName(
            @PathVariable("scientificName") String scientificName,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String preferredLanguage)
            throws IOException {
        TaxonImage taxonImage = null;
        if (TaxonUtil.isEmptyValue(scientificName)) {
            taxonImage = new TaxonImage();
            taxonImage.setScientificName(scientificName);
        } else {
            Map<String, String> taxon = taxonSearch.findTaxon(scientificName);
            if (taxon != null) {
                Collection<String> links = taxonSearch.findTaxonIds(scientificName);
                if (links != null) {
                    Stream<String> ids = replaceWithPrefix(links);
                    for (String id : ids.collect(Collectors.toList())) {
                        taxonImage = imageSearch.lookupImageForExternalId(id, new SearchContext() {

                            @Override
                            public String getPreferredLanguage() {
                                return preferredLanguage;
                            }
                        });
                        if (taxonImage != null) {
                            break;
                        }
                    }

                    if (taxonImage == null && !links.isEmpty()) {
                        taxonImage = new TaxonImage();
                        taxonImage.setInfoURL(ExternalIdUtil.urlForExternalId(links.iterator().next()));
                    }

                    TaxonUtil.enrichTaxonImageWithTaxon(taxon, taxonImage, preferredLanguage);
                }
            }
        }
        if (taxonImage == null) {
            throw new ResourceNotFoundException("no image for [" + scientificName + "]");
        }
        return taxonImage;
    }

    static Optional<String> replaceFullWithPrefix(Collection<String> links) {
        return replaceWithPrefix(links)
                .findFirst();
    }

    static Stream<String> replaceWithPrefix(Collection<String> links) {
        return links.stream()
                .map(x -> StringUtils.replace(x
                        , "https://www.wikidata.org/wiki/"
                        , TaxonomyProvider.WIKIDATA.getIdPrefix()))
                .filter(x -> x.startsWith(TaxonomyProvider.WIKIDATA.getIdPrefix()));
    }

    @RequestMapping(value = "/imagesForName", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public TaxonImage findTaxonImagesForTaxonWithName2(
            @RequestParam(value = "name", required = false) String[] names,
            @RequestParam(value = "externalId", required = false) String[] externalIds,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String preferredLanguage)
            throws IOException {
        TaxonImage image = null;
        if (externalIds != null && externalIds.length > 0) {
            image = findTaxonImagesForExternalId(externalIds[0], preferredLanguage);
        } else if (names != null && names.length > 0) {
            image = findTaxonImagesForTaxonWithName(names[0], preferredLanguage);
        } else {
            throw new BadRequestException("no names nor externalIds provided");
        }

        return image;
    }

    @RequestMapping(value = "/imagesForNames", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<TaxonImage> findImagesForNames(
            @RequestParam(value = "name") String[] names,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String preferredLanguage)
            throws IOException {
        List<TaxonImage> images = new ArrayList<TaxonImage>();
        for (String name : names) {
            TaxonImage image = findTaxonImagesForTaxonWithName(name, preferredLanguage);
            if (image != null) {
                images.add(image);
            }
        }
        return images;
    }

    @RequestMapping(value = "/images/{externalId}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public TaxonImage findTaxonImagesForExternalId(
            @PathVariable("externalId") String externalId,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String preferredLanguage)
            throws IOException {

        TaxonImage taxonImage = imageSearch.lookupImageForExternalId(externalId, () -> preferredLanguage);
        if (taxonImage == null) {
            throw new ResourceNotFoundException("no image for [" + externalId + "]");
        }
        return taxonImage;
    }

    protected void setTaxonSearch(TaxonSearch taxonSearch) {
        this.taxonSearch = taxonSearch;
    }

    protected void setImageSearch(ImageSearch imageSearch) {
        this.imageSearch = imageSearch;
    }

}
