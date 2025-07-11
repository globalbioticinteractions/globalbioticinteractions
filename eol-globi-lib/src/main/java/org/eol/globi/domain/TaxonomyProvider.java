package org.eol.globi.domain;

import java.util.Arrays;
import java.util.List;

public enum TaxonomyProvider {
    ITIS("ITIS:",
            TaxonomyProviderConstants.ITIS_URL_PREFIX,
            TaxonomyProviderConstants.ITIS_URL_PREFIX_HTTPS,
            TaxonomyProviderConstants.ID_PREFIX_TSN,
            TaxonomyProviderConstants.ID_PREFIX_TSN_LOWERCASE
    ),
    NBN("NBN:", "https://data.nbn.org.uk/Taxa/", "https://species.nbnatlas.org/species/"),
    WORMS("WORMS:", "https://www.marinespecies.org/aphia.php?p=taxdetails&id=", "urn:lsid:marinespecies.org:taxname:"),
    NCBI("NCBI:", "NCBITaxon:", "NCBI:txid", "http://purl.obolibrary.org/obo/NCBITaxon_"),
    EOL("EOL:", "http://eol.org/pages/", "https://eol.org/pages/"),
    EOL_V2("EOL_V2:"),
    GEONAMES("GEONAMES:", "http://www.geonames.org/"),
    WIKIPEDIA("W:", "http://wikipedia.org/wiki/", "https://wikipedia.org/wiki", "https://en.wikipedia.org/wiki"),
    ENVO("ENVO:", "http://purl.obolibrary.org/obo/ENVO_"),
    GBIF("GBIF:", "https://www.gbif.org/species/", "http://www.gbif.org/species/"),
    ATLAS_OF_LIVING_AUSTRALIA("ALATaxon:"),
    AUSTRALIAN_FAUNAL_DIRECTORY("AFD:"),
    BIODIVERSITY_AUSTRALIA("urn:lsid:biodiversity.org.au:apni.taxon:"),
    INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA("IRMNG:",
            "https://www.irmng.org/aphia.php?p=taxdetails&id=",
            "http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?fam_id=",
            "http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?gen_id=",
            "http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?sp_id=",
            "urn:lsid:irmng.org:taxname:"),
    INDEX_FUNGORUM("IF:"),
    OPEN_TREE_OF_LIFE("OTT:"),
    NATIONAL_OCEANOGRAPHIC_DATA_CENTER("NODC:"),
    INATURALIST_TAXON("INAT_TAXON:", "https://inaturalist.org/taxa/", "https://www.inaturalist.org/taxa/"),
    WIKIDATA("WD:", "http://www.wikidata.org/entity/", "Wikidata:"),
    FISHBASE_CACHE("FBC:"),
    FISHBASE_SPECCODE("FBC:FB:SpecCode:", "http://fishbase.org/summary/"),
    SEALIFEBASE_SPECCODE("FBC:SLB:SpecCode:", "http://sealifebase.org/Summary/SpeciesSummary.php?id="),
    BATPLANT("batbase:taxon:"),
    BATBASE_INTERACTION("batbase:interaction:", "https://batbase.org/interaction/"),
    BATBASE("batbase:taxon:", "https://batbase.org/taxon/"),
    OPEN_BIODIV("http://openbiodiv.net/"),
    MSW("MSW:", "http://www.departments.bucknell.edu/biology/resources/msw3/browse.asp?s=y&id="),
    GULFBASE("BioGoMx:"),
    BOLD_BIN("BOLD:", "http://bins.boldsystems.org/index.php/Public_BarcodeCluster?clusteruri=BOLD:"),
    BOLD_TAXON("BOLDTaxon:", "http://www.boldsystems.org/index.php/Taxbrowser_Taxonpage?taxid="),
    PLAZI_TAXON_CONCEPT("PLAZITaxon:", "http://taxon-concept.plazi.org/id/"),
    PLAZI("PLAZI:", "http://treatment.plazi.org/id/"),
    CATALOGUE_OF_LIFE("COL:", "https://www.catalogueoflife.org/data/taxon/"),
    CHECKLIST_BANK("CLB:", "https://www.checklistbank.org/dataset/"),
    WORLD_OF_FLORA_ONLINE("WFO:", "http://www.worldfloraonline.org/taxon/wfo-"),
    TERRESTRIAL_PARASITE_TRACKER("TPT:"),
    MAMMAL_DIVERSITY_DATABASE("MDD:", "https://www.mammaldiversity.org/taxon/" ),
    HESPEROMYS("HES:", "http://hesperomys.com/n/"),
    DISCOVERLIFE("DL:", "https://www.discoverlife.org/mp/20q?guide=Apoidea_species&search="),
    PBDB("PBDB:", "https://paleobiodb.org/classic/basicTaxonInfo?taxon_no=txn:", "https://paleobiodb.org/classic/checkTaxonInfo?taxon_no=");

    private final List<String> idPrefixes;

    TaxonomyProvider(String... idPrefixes) {
        if (idPrefixes.length == 0) {
            throw new IllegalArgumentException("term provider must have at least one prefix");
        }
        this.idPrefixes = Arrays.asList(idPrefixes);
    }

    public String getIdPrefix() {
        return idPrefixes.get(0);
    }

    public List<String> getIdPrefixes() {
        return idPrefixes;
    }

    public static final String ID_PREFIX_EOL = EOL.getIdPrefix();
    public static final String ID_PREFIX_INATURALIST = "INAT:";
    public static final String ID_PREFIX_WORMS = WORMS.getIdPrefix();
    public static final String ID_PREFIX_ITIS = ITIS.getIdPrefix();
    public static final String ID_PREFIX_ENVO = ENVO.getIdPrefix();
    public static final String ID_PREFIX_WIKIPEDIA = WIKIPEDIA.getIdPrefix();
    public static final String ID_PREFIX_GULFBASE = GULFBASE.getIdPrefix();
    public static final String ID_PREFIX_GAME = "GAME:";
    public static final String ID_PREFIX_HTTP = "http://";
    public static final String ID_PREFIX_HTTPS = "https://";
    public static final String ID_PREFIX_DOI = "doi:";
    public static final String ID_PREFIX_GBIF = GBIF.getIdPrefix();
    public static final String ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY = AUSTRALIAN_FAUNAL_DIRECTORY.getIdPrefix();
    public static final String ID_PREFIX_BIODIVERSITY_AUSTRALIA = BIODIVERSITY_AUSTRALIA.getIdPrefix();
    public static final String ID_PREFIX_INDEX_FUNGORUM = INDEX_FUNGORUM.getIdPrefix();
    public static final String ID_PREFIX_NCBI = NCBI.getIdPrefix();
    public static final String ID_PREFIX_NBN = NBN.getIdPrefix();

    public static final String ID_CMECS = "https://cmecscatalog.org/cmecs/classification/aquaticSetting/";
    public static final String ID_BIO_INFO_REFERENCE = "bioinfo:ref:";
    public static final String BIO_INFO = "bioinfo:";

}
