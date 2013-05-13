package org.eol.globi.server;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eol.globi.domain.TaxonomyProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class CypherProxyController {
    public static final String OBSERVATION_MATCH =
            "MATCH (predatorTaxon)<-[:CLASSIFIED_AS]-(predator)-[:ATE]->(prey)-[:CLASSIFIED_AS]->(preyTaxon)," +
                    "(predator)-[:COLLECTED_AT]->(loc)," +
                    "(predator)<-[collected_rel:COLLECTED]-(study) ";

    public static final String INCLUDE_OBSERVATIONS_TRUE = "includeObservations=true";

    public static final String INTERACTION_PREYS_ON = "preysOn";
    public static final String INTERACTION_PREYED_UPON_BY = "preyedUponBy";
    public static final String DEFAULT_RETURN_LIST = "loc.latitude as latitude," +
            "loc.longitude as longitude," +
            "loc.altitude? as altitude," +
            "study.title," +
            "collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch," +
            "ID(predator) as tmp_and_unique_specimen_id," +
            "predator.lifeStage? as predator_life_stage," +
            "prey.lifeStage? as prey_life_stage\"";

    @RequestMapping(value = "/taxon/{scientificName}/" + INTERACTION_PREYS_ON, method = RequestMethod.GET)
    @ResponseBody
    public String findPreyOf(HttpServletRequest request, @PathVariable("scientificName") String scientificName) throws IOException {
        String query1 = "{\"query\":\"START predatorTaxon = node:taxons(name={scientificName}) " +
                "MATCH predatorTaxon<-[:CLASSIFIED_AS]-predator-[:ATE]->prey-[:CLASSIFIED_AS]->preyTaxon ";
        query1 = addLocationClausesIfNecessary(request, query1);
        query1 += "RETURN distinct(preyTaxon.name) as preyName\", " +
                "\"params\":" + buildParams(scientificName) + "}";
        String query = query1;
        return execute(query);
    }

    private String addLocationClausesIfNecessary(HttpServletRequest request, String query) {
        query += " , predator-[:COLLECTED_AT]->loc ";
        query += request == null ? "" : RequestHelper.buildCypherSpatialWhereClause(request.getParameterMap());
        return query;
    }

    private String buildParams(String scientificName) {
        String params = "{";

        if (scientificName != null) {
            params += "\"scientificName\":\"" + scientificName + "\"";
        }

        params += "}";
        return params;
    }

    @RequestMapping(value = "/interaction", method = RequestMethod.GET)
    @ResponseBody
    public String findInteractions(HttpServletRequest request) throws IOException {
        String query = "{\"query\":\"START loc" + " = node:locations('*:*') " +
                "MATCH predatorTaxon<-[:CLASSIFIED_AS]-predator-[interactionType:" + allInteractionTypes() + "]->prey-[:CLASSIFIED_AS]->preyTaxon ";
        query = addLocationClausesIfNecessary(request, query);
        query += "RETURN predatorTaxon.externalId, predatorTaxon.name as predatorName, type(interactionType), preyTaxon.externalId, preyTaxon.name as preyTaxon\", " +
                "\"params\":" + buildParams(null) + "}";
        return execute(query);
    }

    private String allInteractionTypes() {
        return "PREYS_UPON|PARASITE_OF|HAS_HOST|INTERACTS_WITH|ATE";
    }


    @RequestMapping(value = "/taxon/{scientificName}/" + INTERACTION_PREYED_UPON_BY, method = RequestMethod.GET)
    @ResponseBody
    public String findPredatorsOf(HttpServletRequest request, @PathVariable("scientificName") String scientificName) throws IOException {
        String query = "{\"query\":\"START preyTaxon" + " = node:taxons(name={scientificName}) " +
                "MATCH predatorTaxon<-[:CLASSIFIED_AS]-predator-[:ATE]->prey-[:CLASSIFIED_AS]->preyTaxon ";
        query = addLocationClausesIfNecessary(request, query);
        query += "RETURN distinct(predatorTaxon.name) as predatorName\", " +
                "\"params\":" + buildParams(scientificName) + "}";
        return execute(query);
    }

    @RequestMapping(value = "/findTaxon/{taxonName}", method = RequestMethod.GET)
    @ResponseBody
    public String findTaxon(@PathVariable("taxonName") String taxonName) throws IOException {
        String query = "{\"query\":\"START taxon = node:taxons('*:*') " +
                "WHERE taxon.name =~ '" + taxonName + ".*'" +
                "RETURN distinct(taxon.name) " +
                "LIMIT 15\"}";
        return execute(query);
    }

    @RequestMapping(value = "/taxon/{predatorName}/" + INTERACTION_PREYS_ON, params = {INCLUDE_OBSERVATIONS_TRUE}, method = RequestMethod.GET)
    @ResponseBody
    public String findPreyObservationsOf(HttpServletRequest request, @PathVariable("predatorName") String predatorName) throws IOException {
        String query = "{\"query\":\"START predatorTaxon = node:taxons(name={predatorName}) " +
                OBSERVATION_MATCH +
                getSpatialWhereClause(request) +
                " RETURN preyTaxon.name as preyName, " +
                DEFAULT_RETURN_LIST +
                ", \"params\": { \"predatorName\" : \"" + predatorName + "\" } }";
        return execute(query);
    }

    @RequestMapping(value = "/taxon/{preyName}/" + INTERACTION_PREYED_UPON_BY, params = {INCLUDE_OBSERVATIONS_TRUE}, method = RequestMethod.GET)
    @ResponseBody
    public String findPredatorObservationsOf(HttpServletRequest request, @PathVariable("preyName") String preyName) throws IOException {
        String query = "{\"query\":\"START preyTaxon = node:taxons(name={preyName}) " +
                OBSERVATION_MATCH +
                getSpatialWhereClause(request) +
                " RETURN predatorTaxon.name as predatorName, " +
                DEFAULT_RETURN_LIST +
                ", \"params\": { \"preyName\" : \"" + preyName + "\" } }";
        return execute(query);
    }

    private String getSpatialWhereClause(HttpServletRequest request) {
        return request == null ? "" : RequestHelper.buildCypherSpatialWhereClause(request.getParameterMap());
    }

    @RequestMapping(value = "/locations", method = RequestMethod.GET)
    @ResponseBody
    @Cacheable(value = "locationCache")
    public String locations() throws IOException {
        return execute("{\"query\":\"START loc = node:locations('*:*') RETURN loc.latitude, loc.longitude\"}");
    }


    @RequestMapping(value = "/contributors", method = RequestMethod.GET)
    @ResponseBody
    @Cacheable(value = "contributorCache")
    public String contributors() throws IOException {
        String query = "{\"query\":\"START study=node:studies('*:*')" +
                " MATCH study-[:COLLECTED]->sourceSpecimen-[interact:ATE|PREYS_UPON|PARASITE_OF|HAS_HOST|INTERACTS_WITH]->prey-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon " +
                " RETURN study.institution, study.period, study.description, study.contributor, count(interact), count(distinct(sourceTaxon)), count(distinct(targetTaxon))\", \"params\": { } }";
        return execute(query);
    }


    private String execute(String query) throws IOException {
        org.apache.http.client.HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("http://46.4.36.142:7474/db/data/cypher");
        HttpClient.addJsonHeaders(httpPost);
        httpPost.setEntity(new StringEntity(query));
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        return httpclient.execute(httpPost, responseHandler);
    }


    @RequestMapping(value = "/findExternalUrlForTaxon/{taxonName}", method = RequestMethod.GET)
    @ResponseBody
    public String findExternalLinkForTaxonWithName(@PathVariable("taxonName") String taxonName) throws IOException {
        String query = "{\"query\":\"START taxon = node:taxons(name={taxonName}) " +
                " RETURN taxon.externalId\"" +
                ", \"params\": { \"taxonName\" : \"" + taxonName + "\" } }";

        String result = execute(query);

        String url = null;
        for (Map.Entry<String, String> stringStringEntry : getURLPrefixMap().entrySet()) {
            url = getUrl(result, stringStringEntry.getKey(), stringStringEntry.getValue());
            if (url != null) {
                break;
            }

        }
        return buildJsonUrl(url);
    }

    private String buildJsonUrl(String url) {
        return url == null ? "{}" : "{\"url\":\"" + url + "\"}";
    }

    @RequestMapping(value = "/findExternalUrlForExternalId/{externalId}", method = RequestMethod.GET)
    @ResponseBody
    public String findExternalLinkForExternalId(@PathVariable("externalId") String externalId) {
        String url = null;
        for (Map.Entry<String, String> idPrefixToUrlPrefix : getURLPrefixMap().entrySet()) {
            if (externalId.startsWith(idPrefixToUrlPrefix.getKey())) {
                url = idPrefixToUrlPrefix.getValue() + externalId.replaceAll(idPrefixToUrlPrefix.getKey(), "");
            }
            if (url != null) {
                break;
            }

        }
        return buildJsonUrl(url);
    }

    @RequestMapping(value = "/shortestPathsBetweenTaxon/{startTaxon}/andTaxon/{endTaxon}", method = RequestMethod.GET)
    @ResponseBody
    public String findShortestPaths(@PathVariable("startTaxon") String startTaxon, @PathVariable("endTaxon") String endTaxon) throws IOException {
        String query = "{\"query\":\"START startNode = node:taxons(name={startTaxon}),endNode = node:taxons(name={endTaxon}) " +
                "MATCH p = allShortestPaths(startNode-[:" + allInteractionTypes() + "|CLASSIFIED_AS*..100]-endNode) " +
                "RETURN extract(n in (filter(x in nodes(p) : has(x.name))) : " +
                "coalesce(n.name?)) as shortestPaths " +
                "LIMIT 10\",\"params\":{\"startTaxon\": \"" + startTaxon + "\", \"endTaxon\":\"" + endTaxon + "\"}}";
        return execute(query);
    }

    private Map<String, String> getURLPrefixMap() {
        return new HashMap<String, String>() {{
            put(TaxonomyProvider.ID_PREFIX_EOL, "http://eol.org/pages/");
            put(TaxonomyProvider.ID_PREFIX_GULFBASE, "http://gulfbase.org/biogomx/biospecies.php?species=");
        }};
    }

    private String getUrl(String result, String externalIdPrefix, String urlPrefix) {
        String url = "";
        if (result.contains(externalIdPrefix)) {
            String[] split = result.split(externalIdPrefix);
            if (split.length > 1) {
                String[] eolId = split[1].split("\"");
                if (eolId.length > 1) {
                    url = urlPrefix + eolId[0];
                }
            }
        }
        return url;
    }
}