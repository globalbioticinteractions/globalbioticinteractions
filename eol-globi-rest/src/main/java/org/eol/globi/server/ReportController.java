package org.eol.globi.server;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
public class ReportController {

    @RequestMapping(value = "/contributors", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Cacheable(value = "contributorCache")
    public String contributors(@RequestParam(required = false) final String source) throws IOException {
        return new CypherQueryExecutor(CypherQueryBuilder.references(source)).execute(null);
    }

    @RequestMapping(value = "/info", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Cacheable(value = "infoCache")
    public String info(@RequestParam(required = false) final String source) throws IOException {
        return new CypherQueryExecutor(CypherQueryBuilder.stats(source)).execute(null);
    }

    @RequestMapping(value = "/sources", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Cacheable(value = "sourcesCache")
    public String sources() throws IOException {
        return new CypherQueryExecutor(CypherQueryBuilder.sourcesQuery()).execute(null);
    }


}
