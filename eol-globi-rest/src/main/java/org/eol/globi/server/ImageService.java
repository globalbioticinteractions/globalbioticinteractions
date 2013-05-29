package org.eol.globi.server;

import org.eol.globi.domain.TaxonImage;
import org.eol.globi.service.EOLTaxonImageService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private EmbeddedGraphDatabase graphDb;

    @RequestMapping(value = "/imagesForName/{scientificName}", method = RequestMethod.GET)
    @ResponseBody
    public TaxonImage findTaxonImagesForTaxonWithName(@PathVariable("scientificName") String scientificName) throws IOException {
        TaxonImage taxonImage = null;
        IndexHits<Node> hits = graphDb.index().forNodes("taxons").get("name", scientificName);
        if (hits.hasNext()) {
            Node firstHit = hits.next();
            if (firstHit.hasProperty("externalId")) {
                taxonImage = service.lookupImageForExternalId((String) firstHit.getProperty("externalId"));
            }
        }
        return taxonImage;
    }

    @RequestMapping(value = "/images/{externalId}", method = RequestMethod.GET)
    @ResponseBody
    public TaxonImage findTaxonImagesForExternalId(@PathVariable("externalId") String externalId) throws IOException {
        return service.lookupImageForExternalId(externalId);
    }

}
