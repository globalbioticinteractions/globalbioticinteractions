package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.service.EOLTaxonImageService;
import org.eol.globi.service.ImageSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class ImageService {

    private ImageSearch imageSearch = new EOLTaxonImageService();

    @Autowired
    private TaxonSearch taxonSearch;

    @RequestMapping(value = "/imagesForName/{scientificName}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public TaxonImage findTaxonImagesForTaxonWithName(@PathVariable("scientificName") String scientificName) throws IOException {
        Map<String, String> taxon = taxonSearch.findTaxon(scientificName, null);
        TaxonImage taxonImage = null;
        if (taxon != null && taxon.containsKey(PropertyAndValueDictionary.EXTERNAL_ID)) {
            taxonImage = imageSearch.lookupImageForExternalId(taxon.get(PropertyAndValueDictionary.EXTERNAL_ID));
            if (taxonImage == null) {
                taxonImage = new TaxonImage();
            }
            populateFromTaxonIfNeeded(taxon, taxonImage);
        }
        if (taxonImage == null) {
            throw new ResourceNotFoundException("no image for [" + scientificName + "]");
        }
        return taxonImage;
    }

    protected void populateFromTaxonIfNeeded(Map<String, String> taxon, TaxonImage taxonImage) {
        if (StringUtils.isBlank(taxonImage.getCommonName())) {
            String commonName = taxon.get(PropertyAndValueDictionary.COMMON_NAMES);
            if (StringUtils.isNotBlank(commonName)) {
                String[] splits = StringUtils.split(commonName, CharsetConstant.SEPARATOR_CHAR);
                for (String split : splits) {
                    if (StringUtils.contains(split, "@en")) {
                        taxonImage.setCommonName(StringUtils.trim(StringUtils.replace(split, "@en", "")));
                    }
                }
            }
        }

        if (StringUtils.isBlank(taxonImage.getScientificName())) {
            taxonImage.setScientificName(taxon.get(PropertyAndValueDictionary.NAME));
        }
    }

    @RequestMapping(value = "/imagesForName", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public TaxonImage findTaxonImagesForTaxonWithName2(@RequestParam(value = "name", required = false) String[] names, @RequestParam(value = "externalId", required = false) String[] externalIds) throws IOException {
        TaxonImage image = null;
        if (externalIds != null && externalIds.length > 0) {
            image = findTaxonImagesForExternalId(externalIds[0]);
        } else if (names != null && names.length > 0) {
            image = findTaxonImagesForTaxonWithName(names[0]);
        } else {
            throw new BadRequestException("no names nor externalIds provided");
        }

        return image;
    }

    @RequestMapping(value = "/imagesForNames", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<TaxonImage> findImagesForNames(@RequestParam(value = "name") String[] names) throws IOException {
        List<TaxonImage> images = new ArrayList<TaxonImage>();
        for (String name : names) {
            TaxonImage image = findTaxonImagesForTaxonWithName(name);
            if (image != null) {
                images.add(image);
            }
        }
        return images;
    }

    @RequestMapping(value = "/images/{externalId}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public TaxonImage findTaxonImagesForExternalId(@PathVariable("externalId") String externalId) throws IOException {
        TaxonImage taxonImage = imageSearch.lookupImageForExternalId(externalId);
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
