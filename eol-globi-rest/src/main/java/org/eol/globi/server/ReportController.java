package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
public class ReportController {

    @RequestMapping(value = "/contributors", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery contributorsNew(@RequestParam(required = false) final String source) throws IOException {
        return CypherQueryBuilder.references(source);
    }

    @RequestMapping(value = "/info", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery infoNew(@RequestParam(required = false) final String source) throws IOException {
        return CypherQueryBuilder.stats(source);
    }

    @RequestMapping(value =
            "/spatialInfo", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery spatialInfoNew(HttpServletRequest req) throws IOException {
        CypherQuery cypherQuery = CypherQueryBuilder.spatialInfo(req.getParameterMap());
        return CypherQueryBuilder.createPagedQuery(req, cypherQuery);
    }

    @RequestMapping(value = "/sources", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery sourcesNew() throws IOException {
        return CypherQueryBuilder.sourcesQuery();
    }

}
