package org.eol.globi.server;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class CypherProxyController {

    public static final String OBSERVATION_MATCH =
            "MATCH (predatorTaxon)<-[:CLASSIFIED_AS]-(predator)-[:ATE]->(prey)-[:CLASSIFIED_AS]->(preyTaxon)," +
                    "(predator)-[:COLLECTED_AT]->(location)," +
                    "(predator)<-[collected_rel:COLLECTED]-(study) " +
                    "WHERE location is not null ";

    public static final String INCLUDE_OBSERVATIONS_TRUE = "includeObservations=true";

    public static final String INTERACTION_PREYS_ON = "preysOn";
    public static final String INTERACTION_PREYED_UPON_BY = "preyedUponBy";


    @RequestMapping(value = "/predator/{scientificName}/listPrey", method = RequestMethod.GET)
    @ResponseBody
    @Deprecated
    public String oldFindPreyOf(@PathVariable("scientificName") String scientificName) throws IOException {
        return findPreyOf(scientificName);
    }

    @RequestMapping(value = "/{scientificName}/" + INTERACTION_PREYS_ON, method = RequestMethod.GET)
    @ResponseBody
    public String findPreyOf(@PathVariable("scientificName") String scientificName) throws IOException {
        String query = "{\"query\":\"START predatorTaxon = node:taxons(name={predatorName}) " +
                "MATCH predatorTaxon<-[:CLASSIFIED_AS]-predator-[:ATE]->prey-[:CLASSIFIED_AS]->preyTaxon " +
                "RETURN distinct(preyTaxon.name) as preyName\", \"params\": { \"predatorName\" : \"" + scientificName + "\" } }";
        return execute(query);
    }

    @RequestMapping(value = "/prey/{scientificName}/listPredators", method = RequestMethod.GET)
    @ResponseBody
    @Deprecated
    public String oldFindPredatorsOf(@PathVariable("scientificName") String scientificName) throws IOException {
        return findPredatorsOf(scientificName);
    }

    @RequestMapping(value = "/{scientificName}/" + INTERACTION_PREYED_UPON_BY, method = RequestMethod.GET)
    @ResponseBody
    public String findPredatorsOf(@PathVariable("scientificName") String scientificName) throws IOException {
        String query = "{\"query\":\"START preyTaxon = node:taxons(name={preyName}) " +
                "MATCH predatorTaxon<-[:CLASSIFIED_AS]-predator-[:ATE]->prey-[:CLASSIFIED_AS]->preyTaxon " +
                "RETURN distinct(predatorTaxon.name) as predatorName\", \"params\": { \"preyName\" : \"" + scientificName + "\" } }";
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

    @RequestMapping(value = "/predator/{predatorName}/listPreyObservations", method = RequestMethod.GET)
    @ResponseBody
    @Deprecated
    public String oldFindPreyObservationsOf(@PathVariable("predatorName") String predatorName) throws IOException {
        return findPreyObservationsOf(predatorName);
    }

    @RequestMapping(value = "/{predatorName}/" + INTERACTION_PREYS_ON, params = {INCLUDE_OBSERVATIONS_TRUE}, method = RequestMethod.GET)
    @ResponseBody
    public String findPreyObservationsOf(@PathVariable("predatorName") String predatorName) throws IOException {
        String query = "{\"query\":\"START predatorTaxon = node:taxons(name={predatorName}) " +
                OBSERVATION_MATCH +
                " RETURN preyTaxon.name as preyName, location.latitude as latitude, location.longitude as longitude, location.altitude? as altitude, study.contributor as contributor, collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch\"" +
                ", \"params\": { \"predatorName\" : \"" + predatorName + "\" } }";
        return execute(query);
    }

    @RequestMapping(value = "/prey/{preyName}/listPredatorObservations", method = RequestMethod.GET)
    @ResponseBody
    @Deprecated
    public String oldFindPredatorObservationsOf(@PathVariable("preyName") String preyName) throws IOException {
        return findPredatorObservationsOf(preyName);
    }

    @RequestMapping(value = "/{preyName}/" + INTERACTION_PREYED_UPON_BY, params = {INCLUDE_OBSERVATIONS_TRUE}, method = RequestMethod.GET)
    @ResponseBody
    public String findPredatorObservationsOf(@PathVariable("preyName") String preyName) throws IOException {
        String query = "{\"query\":\"START preyTaxon = node:taxons(name={preyName}) " +
                OBSERVATION_MATCH +
                " RETURN predatorTaxon.name as predatorName, location.latitude as latitude, location.longitude as longitude, location.altitude? as altitude, study.contributor as contributor, collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch\"" +
                ", \"params\": { \"preyName\" : \"" + preyName + "\" } }";
        return execute(query);
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

        Map<String, String> idURLPrefixMap = new HashMap<String, String>() {{
            put("EOL:", "http://eol.org/pages/");
            put("BioGoMx:", "http://gulfbase.org/biogomx/biospecies.php?species=");
        }};

        String url = null;

        for (Map.Entry<String, String> stringStringEntry : idURLPrefixMap.entrySet()) {
            url = getUrl(result, stringStringEntry.getKey(), stringStringEntry.getValue());
            if (url != null) {
                break;
            }

        }
        return url == null ? "{}" : "{\"url\":\"" + url + "\"}";
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