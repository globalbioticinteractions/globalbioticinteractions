package org.eol.globi.server;

import org.eol.globi.domain.TaxonImage;
import org.eol.globi.service.EOLTaxonImageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ImageService {

    private EOLTaxonImageService service = new EOLTaxonImageService();

    @RequestMapping(value = "/images/{externalId}", method = RequestMethod.GET)
    @ResponseBody
    public TaxonImage findTaxonImagesForExternalId(@PathVariable("externalId") String externalId) throws IOException {
        Map<String,String> urls = new HashMap<String, String>();
        TaxonImage taxonImage = service.lookupImageForExternalId(externalId);
        return taxonImage;
    }

}
