package org.eol.globi.server;

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
        if (taxon != null && taxon.containsKey("externalId")) {
            taxonImage = imageSearch.lookupImageForExternalId(taxon.get("externalId"));
        }
        return taxonImage;
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
        return imageSearch.lookupImageForExternalId(externalId);
    }

    protected void setTaxonSearch(TaxonSearch taxonSearch) {
        this.taxonSearch = taxonSearch;
    }

    protected void setImageSearch(ImageSearch imageSearch) {
        this.imageSearch = imageSearch;
    }

}
