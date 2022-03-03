package org.eol.globi.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResultFormatterJSONTestUtil {

    public static String oldTaxonQueryResult() throws JsonProcessingException {
        return new ObjectMapper().readTree("{\n" +
                "  \"columns\" : [ \"taxon_name\", \"taxon_common_names\", \"taxon_external_id\", \"taxon_path\", \"taxon_path_ids\", \"taxon_path_ranks\" ],\n" +
                "  \"data\" : [ [ \"Isopoda\", \"Asseln @de | isopods @en | Siirat @fi | Isopoda @fr | zeepissebedden @nl | gråsuggor och tånglöss @sv | \", \"EOL:7230\", \"Animalia | Arthropoda | Malacostraca | Isopoda\", \"EOL:1 | EOL:164 | EOL:1157 | EOL:7230\", \"kingdom | phylum | class | order\" ] ]" +
                "\n}").toString();
    }

    public static String newTaxonQueryResult() {
        return "{\n" +
                "  \"results\" : [ " +
                "{\n" +
                "  \"columns\" : [ \"taxon_name\", \"taxon_common_names\", \"taxon_external_id\", \"taxon_path\", \"taxon_path_ids\", \"taxon_path_ranks\" ],\n" +
                "  \"data\" : [ " +
                "    { " +
                "       \"row\":  " +
                "           [ \"Isopoda\", \"Asseln @de | isopods @en | Siirat @fi | Isopoda @fr | zeepissebedden @nl | gråsuggor och tånglöss @sv | \", \"EOL:7230\", \"Animalia | Arthropoda | Malacostraca | Isopoda\", \"EOL:1 | EOL:164 | EOL:1157 | EOL:7230\", \"kingdom | phylum | class | order\" ]" +
                "     } " +
                "   ]\n" +
                "}" +
                " ],\n" +
                "  \"errors\" : [ ]\n" +
                "}";
    }

    public static String getNewInteractionResults() {
        return "{\n" +
                    "  \"results\": [\n" +
                    "    {\n" +
                    "      \"columns\": [\n" +
                    "        \"source_taxon_name\",\n" +
                    "        \"source_taxon_external_id\",\n" +
                    "        \"target_taxon_name\",\n" +
                    "        \"target_taxon_external_id\",\n" +
                    "        \"interaction_type\"\n" +
                    "      ],\n" +
                    "      \"data\": [\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Pseudacris triseriata\",\n" +
                    "            \"EOL:1048370\",\n" +
                    "            \"parasiteOf\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Pseudacris crucifer\",\n" +
                    "            \"EOL:1048371\",\n" +
                    "            \"parasiteOf\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Pseudacris triseriata\",\n" +
                    "            \"EOL:1048370\",\n" +
                    "            \"parasiteOf\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Pseudacris crucifer\",\n" +
                    "            \"EOL:1048371\",\n" +
                    "            \"parasiteOf\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Pseudacsis triseriata\",\n" +
                    "            \"no:match\",\n" +
                    "            \"hasHost\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Pseudacris crucifer\",\n" +
                    "            \"ITIS:207303\",\n" +
                    "            \"hasHost\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Lithobates catesbeianus\",\n" +
                    "            \"EOL_V2:330963\",\n" +
                    "            \"hasHost\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Pseudacris crucifer\",\n" +
                    "            \"ITIS:207303\",\n" +
                    "            \"hasHost\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Pseudacris triseriata\",\n" +
                    "            \"EOL:1048370\",\n" +
                    "            \"hasHost\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Pseudacris crucifer\",\n" +
                    "            \"EOL:1048371\",\n" +
                    "            \"hasHost\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Pseudacris triseriata\",\n" +
                    "            \"EOL:1048370\",\n" +
                    "            \"hasHost\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Pseudacris crucifer\",\n" +
                    "            \"EOL:1048371\",\n" +
                    "            \"hasHost\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Lithobates catesbeianus\",\n" +
                    "            \"EOL_V2:330963\",\n" +
                    "            \"hasHost\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"row\": [\n" +
                    "            \"Glypthelmins pennsylvaniensis\",\n" +
                    "            \"GBIF:9203090\",\n" +
                    "            \"Pseudacsis triseriata\",\n" +
                    "            \"no:match\",\n" +
                    "            \"hasHost\"\n" +
                    "          ],\n" +
                    "          \"meta\": [\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null,\n" +
                    "            null\n" +
                    "          ]\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"errors\": []\n" +
                    "}\n";
    }

    public static String oldInteractionJsonResult() {
        return "{ \"columns\" : [ \"source_taxon_external_id\", \"source_taxon_name\", \"source_taxon_path\", \"interaction_type\", \"target_taxon_external_id\", \"target_taxon_name\", \"target_taxon_path\" ], " +
                    "\"data\" : " +
                    "[[ \"EOL:917146\", \"Todus mexicanus\", \"Animalia Chordata Aves Coraciiformes Todidae Todus\", \"ATE\", \"EOL:345\", \"Coleoptera\", \"Animalia Arthropoda Insecta\" ], " +
                    "[  \"EOL:1024936\", \"Otus nudipes\", \"Animalia Chordata Aves Strigiformes Strigidae Megascops\", \"ATE\", \"EOL:421\", \"Diptera\", \"Animalia Arthropoda Insecta\" ], " +
                    "[  \"EOL:13798\", \"Anolis gundlachi\", \"Animalia Chordata Reptilia Squamata Polychrotidae Anolis\", \"ATE\", \"EOL:4938647\", \"Stylomatophora\", \"Metazoa Eumetazoa Triploblastica Nephrozoa Protostomia Spiralia Trochozoa Eutrochozoa Mollusca Gastropoda Streptoneura\" ]]}";
    }
}
