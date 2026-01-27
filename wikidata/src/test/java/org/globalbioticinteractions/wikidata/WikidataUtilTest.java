package org.globalbioticinteractions.wikidata;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.eol.globi.domain.TaxonomyProvider.ATLAS_OF_LIVING_AUSTRALIA;
import static org.eol.globi.domain.TaxonomyProvider.AUSTRALIAN_FAUNAL_DIRECTORY;
import static org.eol.globi.domain.TaxonomyProvider.BATBASE;
import static org.eol.globi.domain.TaxonomyProvider.BATBASE_INTERACTION;
import static org.eol.globi.domain.TaxonomyProvider.BATPLANT;
import static org.eol.globi.domain.TaxonomyProvider.BIODIVERSITY_AUSTRALIA;
import static org.eol.globi.domain.TaxonomyProvider.BOLD_BIN;
import static org.eol.globi.domain.TaxonomyProvider.CHECKLIST_BANK;
import static org.eol.globi.domain.TaxonomyProvider.DISCOVERLIFE;
import static org.eol.globi.domain.TaxonomyProvider.ENVO;
import static org.eol.globi.domain.TaxonomyProvider.FISHBASE_CACHE;
import static org.eol.globi.domain.TaxonomyProvider.GEONAMES;
import static org.eol.globi.domain.TaxonomyProvider.GULFBASE;
import static org.eol.globi.domain.TaxonomyProvider.HESPEROMYS;
import static org.eol.globi.domain.TaxonomyProvider.MAMMAL_DIVERSITY_DATABASE;
import static org.eol.globi.domain.TaxonomyProvider.NATIONAL_OCEANOGRAPHIC_DATA_CENTER;
import static org.eol.globi.domain.TaxonomyProvider.OPEN_BIODIV;
import static org.eol.globi.domain.TaxonomyProvider.PBDB;
import static org.eol.globi.domain.TaxonomyProvider.MOURE;
import static org.eol.globi.domain.TaxonomyProvider.PLAZI_TAXON_CONCEPT;
import static org.eol.globi.domain.TaxonomyProvider.TERRESTRIAL_PARASITE_TRACKER;
import static org.eol.globi.domain.TaxonomyProvider.WIKIDATA;
import static org.eol.globi.domain.TaxonomyProvider.WIKIPEDIA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.Is.is;

public class WikidataUtilTest {

    @Test
    public void checkSupportForSupportedTaxonProviders() throws IOException, URISyntaxException {
        Collection<TaxonomyProvider> ignored = Arrays.asList(
                GEONAMES,
                WIKIPEDIA,
                ENVO,
                ATLAS_OF_LIVING_AUSTRALIA,
                AUSTRALIAN_FAUNAL_DIRECTORY,
                BIODIVERSITY_AUSTRALIA,
                NATIONAL_OCEANOGRAPHIC_DATA_CENTER,
                WIKIDATA,
                FISHBASE_CACHE,
                BATPLANT,
                BATBASE_INTERACTION,
                BATBASE,
                OPEN_BIODIV,
                GULFBASE,
                BOLD_BIN,
                PLAZI_TAXON_CONCEPT,
                TERRESTRIAL_PARASITE_TRACKER,
                MAMMAL_DIVERSITY_DATABASE,
                HESPEROMYS,
                CHECKLIST_BANK,
                DISCOVERLIFE,
                PBDB,
                MOURE

        );
        List<TaxonomyProvider> unsupported = new ArrayList<>();
        for (TaxonomyProvider provider : TaxonomyProvider.values()) {
            String wdProvider = WikidataUtil.PROVIDER_TO_WIKIDATA.get(provider);
            if (wdProvider == null && !ignored.contains(provider)) {
                unsupported.add(provider);
            }

        }
        assertThat("no mapping for supported taxon providers [" + unsupported.stream().map(TaxonomyProvider::getIdPrefix).collect(Collectors.joining(" ")) + "]"
                , unsupported, is(empty()));
    }

    @Test
    public void generateRelatedSparql() {
        String relatedTaxonIdSparql = WikidataUtil.createRelatedTaxonIdSparql("NCBI:9606", TaxonomyProvider.NCBI);

        assertThat(relatedTaxonIdSparql, is("SELECT ?wdTaxonId ?taxonScheme ?taxonId ?wdTaxonName WHERE {\n" +
                "?wdTaxonId wdt:P685 \"9606\" .\n" +
                "?wdTaxonId ?taxonScheme ?taxonIdStatement .\n" +
                "  ?taxonIdStatement ?relation ?taxonId .\n" +
                "  ?taxonSchemeEntity wikibase:claim ?taxonScheme .\n" +
                "  ?taxonSchemeEntity wikibase:statementProperty ?relation .\n" +
                "  ?taxonSchemeEntity wikibase:claim ?taxonScheme .\n" +
                "  ?taxonSchemeEntity wdt:P31 wd:Q42396390 .\n" +
                "  OPTIONAL { ?wdTaxonId wdt:P225 ?wdTaxonName . }\n" +
                "}"));
    }

    @Test
    public void parseRelatedTaxonIds() throws JsonProcessingException {
        List<Taxon> taxons = WikidataUtil.parseRelatedTaxonIds("{\n" +
                "  \"head\" : {\n" +
                "    \"vars\" : [ \"wdTaxonId\", \"taxonScheme\", \"taxonId\", \"wdTaxonName\" ]\n" +
                "  },\n" +
                "  \"results\" : {\n" +
                "    \"bindings\" : [ {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P685\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"9606\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P815\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"180092\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P830\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"327955\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P842\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"83088\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P846\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"2436436\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P850\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"1455977\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P959\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"12100795\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P1746\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"58D31D52-713D-44B4-9FE9-CB2D9249C422\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P1939\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"233629\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P2752\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"d83185ac-1aa6-4f59-8645-fe8c040857b3\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P3031\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"HOMXSA\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P3088\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"408248\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P3151\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"43584\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P3186\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"815096\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P3240\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"NHMSYS0000376773\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P3606\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"12439\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P4024\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo_sapiens\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P4728\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"109086\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P5055\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"10857762\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P5397\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"8319\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P6105\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"83981\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P6864\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"65350\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P7905\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"6621\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P9157\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"770315\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P9503\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"61567\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P10064\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"homo-sapiens\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P10585\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"f2c748f9-2a4e-40fb-bce0-b82b88165cef\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/direct/P11084\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"8835\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    } ]\n" +
                "  }\n" +
                "}");
        assertThat(taxons.size(), greaterThan(1));
    }

    @Test
    public void parseRelatedTaxonIdsPropertyNonDirect() throws JsonProcessingException {
        List<Taxon> taxons = WikidataUtil.parseRelatedTaxonIds("{\n" +
                "  \"head\" : {\n" +
                "    \"vars\" : [ \"wdTaxonId\", \"taxonScheme\", \"taxonId\", \"wdTaxonName\" ]\n" +
                "  },\n" +
                "  \"results\" : {\n" +
                "    \"bindings\" : [ {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P685\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"9606\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P815\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"180092\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P830\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"327955\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P842\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"83088\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P846\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"2436436\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P850\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"1455977\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P959\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"12100795\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P1746\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"58D31D52-713D-44B4-9FE9-CB2D9249C422\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P1939\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"233629\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P2752\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"d83185ac-1aa6-4f59-8645-fe8c040857b3\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P3031\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"HOMXSA\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P3088\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"408248\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P3151\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"43584\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P3186\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"815096\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P3240\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"NHMSYS0000376773\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P3606\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"12439\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P4024\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo_sapiens\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P4728\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"109086\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P5055\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"10857762\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P5397\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"8319\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P6105\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"83981\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P6864\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"65350\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P7905\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"6621\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P9157\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"770315\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P9503\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"61567\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P10064\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"homo-sapiens\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P10585\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"f2c748f9-2a4e-40fb-bce0-b82b88165cef\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"wdTaxonId\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/entity/Q15978631\"\n" +
                "      },\n" +
                "      \"taxonScheme\" : {\n" +
                "        \"type\" : \"uri\",\n" +
                "        \"value\" : \"http://www.wikidata.org/prop/P11084\"\n" +
                "      },\n" +
                "      \"taxonId\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"8835\"\n" +
                "      },\n" +
                "      \"wdTaxonName\" : {\n" +
                "        \"type\" : \"literal\",\n" +
                "        \"value\" : \"Homo sapiens\"\n" +
                "      }\n" +
                "    } ]\n" +
                "  }\n" +
                "}");
        assertThat(taxons.size(), greaterThan(1));
    }


}