package org.eol.globi.domain;

import java.util.Arrays;
import java.util.List;

public enum TaxonomyProvider {
    ITIS("ITIS:", "https://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value="),
    NBN("NBN:"),
    WORMS("WORMS:"),
    NCBI("NCBI:"),
    NCBITaxon("NCBITaxon:"),
    EOL("EOL:"),
    EOL_V2("EOL_V2:"),
    GEONAMES("GEONAMES:"),
    WIKIPEDIA("W:"),
    ENVO("ENVO:"),
    GBIF("GBIF:", "https://www.gbif.org/species/", "http://www.gbif.org/species/"),
    ATLAS_OF_LIVING_AUSTRALIA("ALATaxon:"),
    AUSTRALIAN_FAUNAL_DIRECTORY("AFD:"),
    BIODIVERSITY_AUSTRALIA("urn:lsid:biodiversity.org.au:apni.taxon:"),
    INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA("IRMNG:"),
    INDEX_FUNGORUM("IF:"),
    OPEN_TREE_OF_LIFE("OTT:"),
    NATIONAL_OCEANOGRAPHIC_DATA_CENTER("NODC:"),
    INATURALIST_TAXON("INAT_TAXON:"),
    WIKIDATA("WD:"),
    FISHBASE_CACHE("FBC:"),
    FISHBASE_SPECCODE("FBC:FB:SpecCode:"),
    SEALIFEBASE_SPECCODE("FBC:SLB:SpecCode:"),
    BATPLANT("batbase:taxon:"),
    OPEN_BIODIV("http://openbiodiv.net/"),
    GULFBASE("BioGoMx:");

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
