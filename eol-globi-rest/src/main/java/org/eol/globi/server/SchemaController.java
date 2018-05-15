package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.server.util.InteractionTypeExternal;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.server.util.ResultFormatterCSV;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

import static org.eol.globi.util.ExternalIdUtil.getURLPrefixMap;
import static org.eol.globi.util.ExternalIdUtil.getURLSuffixMap;

@Controller
public class SchemaController {

    @RequestMapping(value = "/interactionTypes", method = RequestMethod.GET)
    @ResponseBody
    public String getInteractionTypes(HttpServletRequest request) throws IOException {
        Collection<InteractionTypeExternal> availableTypes = Arrays.asList(InteractionTypeExternal.values());
        if (request != null) {
            if (StringUtils.isNotBlank(request.getParameter(ParamName.TAXON.getName()))) {
                CypherQuery cypherQuery = CypherQueryBuilder.buildInteractionTypeQuery(request.getParameterMap());
                String interactionTypes = new ResultFormatterCSV().format(CypherUtil.executeRemote(cypherQuery));
                String[] interactionType = StringUtils.replace(interactionTypes, "\"", "").split("\n");
                availableTypes = new HashSet<InteractionTypeExternal>();
                for (String type : interactionType) {
                    InteractionTypeExternal value = CypherQueryBuilder.INTERACTION_TYPE_INTERNAL_EXTERNAL_MAP.get(type);
                    if (value != null) {
                        availableTypes.add(value);
                    }
                }
            }
        }
        return "csv".equals(getRequestType(request)) ? interactionMapCsv(availableTypes) : interactionMapJson(availableTypes);
    }

    protected String getRequestType(HttpServletRequest request) {
        return request == null ? "json" : request.getParameter("type");
    }

    @RequestMapping(value = "/interactionFields", method = RequestMethod.GET)
    @ResponseBody
    public String getInteractionFields(HttpServletRequest request) throws IOException {
        return "csv".equals(getRequestType(request)) ? csvFields() : jsonFields();
    }

    protected String jsonFields() {
        List<String> fields = new ArrayList<String>();
        for (ResultField resultField : ResultField.values()) {
            fields.add("\"" + resultField.getLabel() + "\":{\"description\":\"" + resultField.getDescription() + "\"}");
        }
        return "{" + StringUtils.join(fields, ",") + "}";
    }

    protected String csvFields() {
        List<String> fields = new ArrayList<String>();
        fields.add("name,description");
        for (ResultField resultField : ResultField.values()) {
            fields.add("" + resultField.getLabel() + "," + resultField.getDescription() + "");
        }
        return StringUtils.join(fields, "\n");
    }

    protected String interactionMapJson(Collection<InteractionTypeExternal> availableTypes) {
        List<String> interactions = new ArrayList<String>();
        for (InteractionTypeExternal value : availableTypes) {
            interactions.add("\"" + value.getLabel() + "\":"
                    + "{\"source\":\"" + value.getSource()
                    + "\",\"target\":\"" + value.getTarget()
                    + "\"," + "\"termIRI\":\"" + value.getTermIRI() + "\"}");
        }
        return "{" + StringUtils.join(interactions, ",") + "}";
    }

    protected String interactionMapCsv(Collection<InteractionTypeExternal> availableTypes) {
        StringBuilder builder = new StringBuilder();
        builder.append("interaction,source,target,termIRI\n");
        for (InteractionTypeExternal value : availableTypes) {
            builder.append(value.getLabel());
            builder.append(",").append(value.getSource());
            builder.append(",").append(value.getTarget());
            builder.append(",").append(value.getTermIRI()).append("\n");
        }
        return builder.toString();
    }

    @RequestMapping(value = "/prefixes", method = RequestMethod.GET)
    @ResponseBody
    public String getPrefixes(HttpServletRequest request) {
        String prefixes;
        if ("csv".equalsIgnoreCase(getRequestType(request))) {
            prefixes = tabularPrefixes(",");
        } else if ("tsv".equalsIgnoreCase(getRequestType(request))) {
            prefixes = tabularPrefixes("\t");
        } else {
            prefixes = jsonPrefixes();
        }
        return prefixes;
    }

    private String tabularPrefixes(String delimiter) {
        StringBuilder builder = new StringBuilder();
        builder.append("id_prefix").append(delimiter).append("url_prefix").append(delimiter).append("url_suffix\n");
        Map<String, String> urlPrefixMap = getURLPrefixMap();
        Map<String, String> urlSuffixMap = getURLSuffixMap();
        for (String idPrefix : urlPrefixMap.keySet()) {
            builder.append(idPrefix);
            builder.append(delimiter).append(urlPrefixMap.getOrDefault(idPrefix, ""));
            builder.append(delimiter).append(urlSuffixMap.getOrDefault(idPrefix, ""));
            builder.append("\n");
        }
        return builder.toString();
    }

    private String jsonPrefixes() {
        List<String> prefixes = new ArrayList<String>();
        Map<String, String> urlPrefixMap = getURLPrefixMap();
        Map<String, String> urlSuffixMap = getURLSuffixMap();
        for (String idPrefix : urlPrefixMap.keySet()) {
            String builder = "\"" +
                    idPrefix +
                    "\":{\"url_prefix\":\"" +
                    urlPrefixMap.getOrDefault(idPrefix, "") +
                    "\",\"url_suffix\":\"" +
                    urlSuffixMap.getOrDefault(idPrefix, "") +
                    "\"}";
            prefixes.add(builder);
        }
        return "{" + StringUtils.join(prefixes, ",") + "}";
    }
}
