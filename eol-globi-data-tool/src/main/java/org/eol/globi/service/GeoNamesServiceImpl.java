package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.Term;
import org.eol.globi.util.HttpUtil;
import org.eol.globi.geo.LatLng;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.eol.globi.domain.TaxonomyProvider.GEONAMES;

public class GeoNamesServiceImpl implements GeoNamesService {
    private static final Log LOG = LogFactory.getLog(GeoNamesServiceImpl.class);

    public static final Term GEO_TERM_EARTH = new Term("GEO:6295630", "Earth");

    private static Map<String, Term> LOCALE_TO_GEONAMES = new HashMap<String, Term>() {{
        put("Country: New Zealand;   State: Otago;   Locality: Catlins, Craggy Tor catchment", new Term("GEO:6612109", "Otago"));
        put("Country: Scotland", new Term("GEO:2638360", "Scotland"));
        put("Country: USA;   State: Georgia", new Term("GEO:4197000", "State of Georgia"));
        put("Country: USA;   State: Iowa", new Term("GEO:4862182", "State of Iowa"));
        put("Country: Southern Ocean", new Term("GEO:4036776", "Southern Ocean"));
        put("Country: USA", new Term("GEO:6252001", "United States of America"));
        put("Country: USA;   State: Iowa;   Locality: Mississippi River", new Term("GEO:4862182", "State of Iowa"));
        put("Country: Japan", new Term("GEO:1861060", "Japan"));
        put("Japan", new Term("GEO:1861060", "Japan"));
        put("Country: Malaysia;   Locality: W. Malaysia", new Term("GEO:1745185", "Peninsular Malaysia"));
        put("Country: Chile;   Locality: central Chile", new Term("GEO:3882554", "Central Valley"));
        put("Country: USA;   State: New Mexico;   Locality: Aden Crater", new Term("GEO:5454273", "Aden Crater"));
        put("Country: USA;   State: Alaska;   Locality: Torch Bay", new Term("GEO:5558058", "Torch Bay"));
        put("Country: USA;   State: Pennsylvania", new Term("GEO:6254927", "Pennsylvania"));
        put("Country: Costa Rica", new Term("GEO:3624060", "Costa Rica"));
        put("Costa Rica", new Term("GEO:3624060", "Costa Rica"));
        put("Country: Pacific", new Term("GEO:8411083", "Pacific Ocean"));
        put("Country: USA;   State: California;   Locality: Cabrillo Point", new Term("GEO:5332459", "Cabrillo Point"));
        put("Country: USA;   State: Texas", new Term("GEO:4736286", "Texas"));
        put("Country: Portugal", new Term("GEO:2264397", "Portugal"));
        put("Country: USA;   Locality: Northeastern US contintental shelf", new Term("GEO:6252001", "United States of America"));
        put("Country: Sri Lanka", new Term("GEO:1227603", "Sri Lanka"));
        put("Country: USA;   State: Maine;   Locality: Troy", new Term("GEO:4981180", "Troy"));
        put("Country: New Zealand", new Term("GEO:2186224", "New Zealand"));
        put("Country: USA;   State: Maine;   Locality: Gulf of Maine", new Term("GEO:4971067", "Gulf of Maine"));
        put("Country: New Zealand;   State: Otago;   Locality: Dempster's Stream, Taieri River, 3 O'Clock catchment", new Term("GEO:6612109", "Otago"));
        put("Country: Panama;   Locality: Gatun Lake", new Term("GEO:3709289", "Lake Gatun"));
        put("Country: USA;   State: Maryland;   Locality: Chesapeake Bay", new Term("GEO:4351177", "Chesapeake Bay"));
        put("Country: India;   Locality: Cochin", new Term("GEO:1273874", "Cochin"));
        put("Country: Ethiopia;   Locality: Lake Abaya", new Term("GEO:345844", "Lake Abaya"));
        put("Country: unknown;   State: Black Sea", new Term("GEO:630673", "Black Sea"));
        put("Country: St. Martin;   Locality: Caribbean", new Term("GEO:3578421", "Saint Martin"));
        put("Country: USA;   State: Yellowstone", new Term("GEO:5843642", "Yellowstone National Park"));
        put("Country: Scotland;   Locality: Loch Leven", new Term("GEO:2644575", "Loch Leven"));
        put("Country: New Zealand;   State: Otago;   Locality: Sutton Stream, Taieri River, Sutton catchment", new Term("GEO:6612109", "Otago"));
        put("Country: USA;   State: Alaska;   Locality: Barrow", new Term("GEO:5880054", "Barrow"));
        put("Country: Malawi;   Locality: Lake Nyasa", new Term("GEO:924329", "Lake Malawi"));
        put("Country: USA;   State: Alaska;   Locality: Aleutian Islands", new Term("GEO:5879148", "Aleutian Islands"));
        put("Country: USA;   State: California;   Locality: Southern California", new Term("GEO:5332921", "California"));
        put("Country: Canada;   State: Manitoba", new Term("GEO:6065171", "Province of Manitoba"));
        put("Country: USA;   State: Maine", new Term("GEO:4971068", "State Of Maine"));
        put("Country: Polynesia", new Term("GEO:7729901", "Polynesia"));
        put("Country: South Africa", new Term("GEO:953987", "South Africa"));
        put("South Africa", new Term("GEO:953987", "South Africa"));
        put("Country: New Zealand;   State: Otago;   Locality: Berwick, Meggatburn", new Term("GEO:2193374", "Berwick"));
        put("Country: New Zealand;   State: Otago;   Locality: Venlaw, Mimihau catchment", new Term("GEO:2180525", "Venlaw"));
        put("Country: USA;   State: Montana", new Term("GEO:5667009", "Montana"));
        // TODO introduce one to many mapping
        put("Country: UK;   State: Yorkshire;   Locality: Aire,  Nidd & Wharfe Rivers", new Term("GEO:2657602", "River Aire"));
        put("Country: UK;   State: Yorkshire;   Locality: Aire,  Nidd & Wharfe Rivers", new Term("GEO:2641500", "River Nidd"));
        put("Country: UK;   State: Yorkshire;   Locality: Aire,  Nidd & Wharfe Rivers", new Term("GEO:2634186", "River Wharfe"));
        put("Country: Hong Kong", new Term("GEO:1819730", "Hong Kong"));
        put("Country: Pacific;   State: Bay of Panama", new Term("GEO:3703442", "Panama Bay"));
        put("Country: Netherlands;   State: Wadden Sea;   Locality: Ems estuary", new Term("GEO:3230285", "Niedersaechsisches Wattenmeer"));
        put("Country: New Zealand;   State: Otago;   Locality: North Col, Silver catchment", new Term("GEO:6612109", "Otago"));
        put("Country: USA;   State: North Carolina", new Term("GEO:4482348", "State of North Carolina"));
        put("Country: USA;   State: Washington", new Term("GEO:5815135", "State of Washington"));
        put("Country: USA;   State: Alaska", new Term("GEO:5879092", "State of Alaska"));
        put("Country: USA;   State: Hawaii", new Term("GEO:5855797", "State of Hawaii"));
        put("Country: Uganda;   Locality: Lake George", new Term("GEO:233416", "Lake George"));
        put("Country: Costa Rica;   State: Guanacaste", new Term("GEO:3623582", "Guanacaste Province"));
        put("Country: USA;   State: Massachusetts;   Locality: Cape Ann", new Term("GEO:4929094", "Cape Ann"));
        put("Country: USA;   State: Maine;   Locality: Martins", new Term("GEO:4971068", "State of Maine"));
        put("Country: USA;   State: New York", new Term("GEO:5128638", "State of New York"));
        put("Country: General;   Locality: General", GEO_TERM_EARTH);
        put("Country: New Zealand;   State: Otago;   Locality: Stony, Sutton catchment", new Term("GEO:6612109", "Otago"));
        put("Country: Tibet", new Term("GEO:1279685", "Tibet Autonomous Region"));
        put("Country: USA;   State: Texas;   Locality: Franklin Mtns", new Term("GEO:5521842", "Franklin Mountains"));
        put("Country: Russia", new Term("GEO:2017370", "Russia"));
        put("Country: New Zealand;   State: Otago;   Locality: Broad, Lee catchment", new Term("GEO:6612109", "Otago"));
        put("Country: Africa;   Locality: Lake McIlwaine", new Term("GEO:886254", "Lake Chivero"));
        put("Country: England;   State: River Medway", new Term("GEO:2642831", "River Medway"));
        put("Country: South Africa;   Locality: Southwest coast", new Term("GEO:953987", "Republic of South Africa"));
        put("Country: USA;   State: Kentucky", new Term("GEO:4296940", "Commonwealth of Kentucky"));
        put("Country: USA;   State: Washington;   Locality: Cape Flattery", new Term("GEO:5794609", "Cape Flattery"));
        put("Country: USA;   State: New Jersey", new Term("GEO:5101760", "State of New Jersey"));
        put("Country: India;   Locality: Rajasthan Desert", new Term("GEO:1270835", "Thar Desert"));
        put("Country: England", new Term("GEO:6269131", "England"));
        put("Country: Austria;   Locality: Hafner Lake", new Term("GEO:2782113", "Republic of Austria"));
        put("Country: USA;   State:  NE USA", new Term("GEO:6252001", "United States of America"));
        put("Country: England;   Locality: Sheffield", new Term("GEO:2638077", "Sheffield"));
        put("Country: Uganda", new Term("GEO:226074", "Uganda"));
        put("Country: USA;   State:  California;   Locality: Monterey Bay", new Term("GEO:5374363", "Monterey Bay"));
        put("Country: Germany", new Term("GEO:2921044", "Federal Republic of Germany"));
        put("Country: England;   Locality: Skipwith Pond", new Term("GEO:7296912", "Skipwidth"));
        put("Country: USA;   State: Wisconsin;   Locality: Little Rock Lake", new Term("GEO:5260513", "Little Rock Lake"));
        put("Country: USA;   State: California;   Locality: Coachella Valley", new Term("GEO:5338176", "Coachella Valley"));
        put("Country: Arctic", new Term("GEO:2960860", "Arctic Ocean"));
        put("Country: USA;   State: Michigan", new Term("GEO:5001836", "State of Michigan"));
        put("Country: Mexico;   State: Guerrero", new Term("GEO:3527213", "Guerrero"));
        put("Country: Norway;   State: Spitsbergen", new Term("GEO:2728573", "Spitzbergen"));
        put("Country: USA;   State: Kentucky;   Locality: Station 1", new Term("GEO:6254925", "Commonwealth of Kentucky"));
        put("Country: New Zealand;   State: Otago;   Locality: Kye Burn", new Term("GEO:6211668", "Kye Burn"));
        put("Country: New Zealand;   State: Otago;   Locality: Little Kye, Kye Burn catchment", new Term("GEO:6612109", "Otago"));
        put("Country: USA;   State: North Carolina;   Locality: Pamlico", new Term("GEO:4482348", "North Carolina"));
        put("Country: Antarctic", new Term("GEO:6255152", "Antarctica"));
        put("Country: USA;   State: Arizona", new Term("GEO:5551752", "State of Arizona"));
        put("Country: England;   Locality: Lancaster", new Term("GEO:2644972", "Lancaster"));
        put("Country: USA;   State: Florida;   Locality: Everglades", new Term("GEO:4154663", "Everglades National Park"));
        put("Country: Barbados", new Term("GEO:3374084", "Barbados"));
        put("Country: USA;   State: New York;   Locality: Bridge Brook", new Term("GEO:5110126", "Bridge Brooke Pond"));
        put("Country: England;   Locality: Oxshott Heath", new Term("GEO:2640718", "Oxshott"));
        put("Country: New Zealand;   State: Otago;   Locality: Blackrock, Lee catchment", new Term("GEO:6612109", "Otago"));
        put("Country: Canada;   State: Ontario", new Term("GEO:6093943", "Ontario"));
        put("Country: Puerto Rico;   Locality: El Verde", new Term("GEO:4564245", "El Verde"));
        put("Country: Quebec", new Term("GEO:6115047", "Quebec"));
        put("Country: Ireland", new Term("GEO:2963597", "Ireland"));
        put("Country: Wales;   Locality: Dee River", new Term("GEO:2651430", "River Dee [Wales]"));
        put("Country: Marshall Islands", new Term("GEO:2080185", "Marshall Islands"));
        put("Country: New Zealand;   State: South Island;   Locality: Canton Creek, Taieri River, Lee catchment", new Term("GEO:2182504", "South Island"));
        put("Country: Seychelles", new Term("GEO:241170", "Republic of Seychelles"));
        put("Country: Namibia;   Locality: Namib Desert", new Term("GEO:3347020", "Namib Desert"));
        put("Country: USA;   State: Rhode Island", new Term("GEO:5224323", "State of Rhode Island"));
        put("Country: USA;   State: Idaho-Utah;   Locality: Deep Creek", new Term("GEO:5596512", "Idaho"));
        put("Country: Malawi", new Term("GEO:927384", "Republic of Malawi"));
        put("Country: Malaysia", new Term("GEO:1733045", "Malaysia"));
        put("Malaysia", new Term("GEO:1733045", "Malaysia"));
        put("Country: Europe;   State: Central Europe", new Term("GEO:6255148", "Europe"));
        put("Country: USA;   State: Florida", new Term("GEO:4155751", "Florida"));
        put("Country: Norway;   State: Oppland;   Locality: Ovre Heimdalsvatn Lake", new Term("GEO:3144873", "Heimdalsvatnet Nedre"));
        put("Country: Austria;   Locality: Vorderer Finstertaler Lake", new Term("GEO:2779556", "Finstertaler Seen"));
        put("Country: Canada;   Locality: high Arctic", new Term("GEO:6251999", "Canada"));
        put("Country: unknown", new Term("GEO:6295630", "Earth"));
        put("Country: Peru", new Term("GEO:3932488", "Republic of Peru"));
        put("Country: USA;   State: New England", new Term("GEO:6252001", "United States of America"));
        put("Country: Great Britain", new Term("GEO:2635167", "United Kingdom"));
        put("Country: New Zealand;   State: Otago;   Locality: German, Kye Burn catchment", new Term("GEO:6612109", "Otago"));
        put("Country: USA;   State: Colorado", new Term("GEO:5417618", "State of Colorado"));
        put("Country: USA;   State: Texas;   Locality: Hueco Tanks", new Term("GEO:5523568", "State of Texas"));
        put("Country: Canada;   State: Ontario;   Locality: Mad River", new Term("GEO:6064001", "Mad River"));
        put("Country: Wales;   Locality: River Rheidol", new Term("GEO:2639467", "Afon Rheidol"));
        put("Country: Costa Rica;   State: de Osa", new Term("GEO:3622657", "Cantón de Osa"));
        put("Country: Finland", new Term("GEO:660013", "Republic of Finland"));
        put("Country: Africa;   Locality: Crocodile Creek,  Lake Nyasa", new Term("GEO:924329", "Lake Malawi"));
        put("Country: USA;   State: Florida;   Locality: South Florida", new Term("GEO:4155751", "Florida"));
        put("Country: USA;   State: Illinois", new Term("GEO:4896861", "State of Illinois"));
        put("Country: Puerto Rico;   Locality: Puerto Rico-Virgin Islands shelf", new Term("GEO:4566966", "Puerto Rico"));
        put("Country: England;   Locality: River Thames", new Term("GEO:2636063", "Thames"));
        put("Country: Madagascar", new Term("GEO:1062947", "Madagascar"));
        put("Madagascar", new Term("GEO:1062947", "Madagascar"));
        put("Country: USA;   State: New Mexico;   Locality: White Sands", new Term("GEO:5497915", "White Sands"));
        put("Country: England;   Locality: River Cam", new Term("GEO:2653956", "River Cam"));
        put("Country: Australia", new Term("GEO:2077456", "Australia"));
        put("Australia", new Term("GEO:2077456", "Australia"));
        put("Country: USA;   State: North Carolina;   Locality: Coweeta", new Term("GEO:4462207", "Coweeta Gap"));
        put("Country: Scotland;   Locality: Ythan estuary", new Term("GEO:2633329", "River Ythan"));
        put("Country: Wales;   Locality: River Clydach", new Term("GEO:3333248", "Swansea"));
        put("Country: USA;   State: Texas;   Locality: Hueco Mountains", new Term("GEO:5523564", "Hueco Mountains"));
        put("Country: Wales", new Term("GEO:2634895", "Wales"));
        put("Country: USA;   State: Arizona;   Locality: Sonora Desert", new Term("GEO:4018698", "Sonoran Desert"));
        put("Country: England;   Locality: Silwood Park", new Term("GEO:2656992", "Ascot"));
        put("Country: Austria;   Locality: Neusiedler Lake", new Term("GEO:3052649", "Lake Neusiedl"));
        put("Country: New Zealand;   State: Otago;   Locality: Narrowdale catchment", new Term("GEO:6210297", "Lower Narrowdale Stream"));
        put("Country: USA;   State: California", new Term("GEO:5332921", "State of California"));
        put("Country: England;   State: Oxfordshire;   Locality: Wytham Wood", new Term("GEO:7300975", "Wytham"));
        put("Country: USA;   State: Michigan;   Locality: Tuesday Lake", new Term("GEO:5012680", "Tuesday Lake"));
        put("Country: USA;   State: Alabama", new Term("GEO:4829764", "State of Alabama"));
        put("Country: New Zealand;   State: Otago;   Locality: Healy Stream, Taieri River, Kye Burn catchment", new Term("GEO:6211668", "Kye Burn"));
        put("Country: USA;   State: New York;   Locality: Long Island", new Term("GEO:5125123", "Long Island"));
        put("Country: Venezuela", new Term("GEO:3625428", "Venezuela"));
        put("Country: New Zealand;   State: Otago;   Locality: Akatore, Akatore catchment", new Term("GEO:2194057", "Akatore"));
        put("Kerguelen Island", new Term("GEO:1546557", "Île Kerguelen"));
        put("Antarctic continental slope & oceanic areas", new Term("GEO:2208337", "Campbell Escarpment"));
        put("South Georgia", new Term("GEO:3426222", "South Sandwich Islands"));
        put("Iles Kerguelen", new Term("GEO:1546556", "Îles Kerguelen"));
        put("Scotia Sea", new Term("GEO:3426293", "Scotia Sea"));
        put("Adjacent to Vestfold Hills", new Term("GEO:6627488", "Vestfold Hills"));
        put("Antarctic Peninsula", new Term("GEO:6632680", "Antarctic Peninsula"));
        put("Prydz Bay", new Term("GEO:6623681", "Prydz Bay"));
        put("Ross Sea", new Term("GEO:4036625", "Ross Sea"));
        put("Not described.", GEO_TERM_EARTH);
        put("New Zealand", new Term("GEO:2186224", "New Zealand"));
        put("Crozet Island", new Term("GEO:936338", "Îles Crozet"));
        put("Weddell Sea", new Term("GEO:4036624", "Weddell Sea"));
        put("South Orkney Islands", new Term("GEO:6625763", "South Orkney Islands"));
        put("Iles Crozets", new Term("GEO:936338", "Îles Crozet"));
        put("Southern ocean", new Term("GEO:4036776", "Southern Ocean"));
        put("Kerguelen Islands", new Term("GEO:1546556", "Îles Kerguelen"));
        put("Prince Edward Islands", new Term("GEO:7778803", "Prince Edward Island"));
        put("Marion Island", new Term("GEO:7778802", "Marion Island"));
        put("South Indian Ocean", new Term("GEO:4036667", "South Indian Basin"));
        put("Crozet Island waters", new Term("GEO:936338", "Îles Crozet"));
        put("southern Weddell Sea", new Term("GEO:4036624", "Weddell Sea"));
        put("Heard Island", new Term("GEO:1547315", "Heard Island"));
        put("Terra Nova Bay", new Term("GEO:6626583", "Terra Nova Bay"));
        put("Straits of Magellan", new Term("GEO:3845265", "Strait of Magellan"));
        put("Antarctic and subantarctic waters", new Term("GEO:6632710", "Antarctic"));
        put("Antarctic waters", new Term("GEO:6632710", "Antarctic"));
        put("McMurdo Sound", new Term("GEO:6637890", "McMurdo Sound"));
        put("Zambia", new Term("GEO:895949", "Republic of Zambia"));
        put("Ivory Coast", new Term("GEO:2287781", "Ivory Coast"));
        put("Cameroun", new Term("GEO:2233387", "Cameroon"));
        put("Gabon", new Term("GEO:2400553", "Gabon"));
        put("Mozambique", new Term("GEO:1036973", "Mozambique"));
        put("Sao Tome", new Term("GEO:2410763", "São Tomé"));
        put("France", new Term("GEO:3017382", "France"));
        put("China", new Term("GEO:1814991", "China"));
        put("Papua New Guinea", new Term("GEO:2088628", "Papua New Guinea"));
        put("Vanuatu Islands", new Term("GEO:2134431", "Vanuatu"));
        put("La Réunion", new Term("GEO:935317", "Réunion"));
        put("Burkina Faso", new Term("GEO:2361809", "Burkina Faso"));
        put("Philippines", new Term("GEO:1694008", "Philippines"));
        put("Taiwan", new Term("GEO:1668284", "Taiwan"));
        put("Solomon Islands", new Term("GEO:2103350", "Solomon Islands"));
        put("Indonesia", new Term("GEO:1643084", "Indonesia"));
        put("Uganda", new Term("GEO:226074", "Uganda"));
        put("Mexico", new Term("GEO:3996063", "Mexico"));
        put("French Guiana", new Term("GEO:3381670", "French Guiana"));
        put("Brunei", new Term("GEO:1820814", "Brunei"));
        put("Singapore", new Term("GEO:1880251", "Singapore"));
        put("New Caledonia", new Term("GEO:2139685", "New Caledonia"));
        put("Brazil", new Term("GEO:3469034", "Brazil"));
        put("Panama", new Term("GEO:3703430", "Panama"));
        put("Zimbabwe", new Term("GEO:878675", "Zimbabwe"));
        put("Colombia", new Term("GEO:3686110", "Colombia"));
    }};
    private Map<String, LatLng> pointCache = new ConcurrentHashMap<String, LatLng>();

    @Override
    public boolean hasPositionForLocality(String locality) {
        return locality != null && LOCALE_TO_GEONAMES.containsKey(locality);
    }

    @Override
    public LatLng findPointForLocality(String locality) throws IOException {
        LatLng point = null;
        if (hasPositionForLocality(locality)) {
            point = pointCache.get(locality);
        }
        return point == null ? retrievePointForLocality(locality) : point;
    }

    private LatLng retrievePointForLocality(String locality) throws IOException {
        LatLng point = null;
        Term term = LOCALE_TO_GEONAMES.get(locality);
        // see https://github.com/jhpoelen/eol-globi-data/issues/39
        if (term != null && !term.equals(GEO_TERM_EARTH)) {
            String idString = term.getId();
            if (LOCALE_TO_GEONAMES.containsKey(locality) && idString.startsWith(GEONAMES.getIdPrefix())) {
                Long id = parseGeoId(locality, idString);
                point = findLatLng(id);
                if (point != null) {
                    pointCache.put(locality, point);
                }
            }
        }

        return point;
    }

    protected static Long parseGeoId(String locality, String idString) {
        String s = idString.replaceFirst(GEONAMES.getIdPrefix(), "");
        Long id = null;
        if (StringUtils.isNotBlank(s)) {
            try {
                id = Long.parseLong(s);
            } catch (NumberFormatException ex) {
                LOG.warn("failed to parse geo id[" + idString + "] for locality [" + locality + "]");
            }
        }
        return id;
    }

    public LatLng findLatLng(Long id) throws IOException {
        LatLng point = null;
        String jsonString = HttpUtil.createHttpClient().execute(new HttpGet("http://api.geonames.org/getJSON?formatted=true&geonameId=" + id + "&username=globi&style=full"), new BasicResponseHandler());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(jsonString);
        if (node.has("lat") && node.has("lng")) {
            double lat = Double.parseDouble(node.get("lat").getValueAsText());
            double lng = Double.parseDouble(node.get("lng").getValueAsText());
            point = new LatLng(lat, lng);

        }
        return point;
    }
}
