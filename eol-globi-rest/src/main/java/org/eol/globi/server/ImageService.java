package org.eol.globi.server;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.ImageSearch;
import org.eol.globi.service.TaxonUtil;
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
    private ImageSearch imageSearch = new ImageSearch() {
        @Override
        public TaxonImage lookupImageForExternalId(String externalId) throws IOException {
            return null;
        }

    }; //new EOLTaxonImageService();

    @Autowired
    private TaxonSearch taxonSearch;

    @RequestMapping(value = "/imagesForName/{scientificName}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public TaxonImage findTaxonImagesForTaxonWithName(@PathVariable("scientificName") String scientificName) throws IOException {
        TaxonImage taxonImage = new TaxonImage();
        if (TaxonUtil.isEmptyValue(scientificName)) {
            taxonImage.setScientificName(scientificName);
        } else {
            Map<String, String> taxonWithImage = taxonSearch.findTaxonWithImage(scientificName);
            if (taxonWithImage == null || taxonWithImage.isEmpty()) {
                Map<String, String> taxon = taxonSearch.findTaxon(scientificName, null);
                if (taxon != null) {
                    if (taxon.containsKey(PropertyAndValueDictionary.EXTERNAL_ID)) {
                        taxonImage = imageSearch.lookupImageForExternalId(taxon.get(PropertyAndValueDictionary.EXTERNAL_ID));
                        if (taxonImage == null) {
                            taxonImage = new TaxonImage();
                        }
                        TaxonUtil.enrichTaxonImageWithTaxon(taxon, taxonImage);
                    }
                }
            } else {
                taxonImage = TaxonUtil.enrichTaxonImageWithTaxon(taxonWithImage, new TaxonImage());
            }
        }
        if (taxonImage == null) {
            throw new ResourceNotFoundException("no image for [" + scientificName + "]");
        }
        return taxonImage;
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
