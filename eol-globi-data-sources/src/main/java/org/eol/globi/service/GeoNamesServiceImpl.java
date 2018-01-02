package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.util.HttpUtil;
import org.eol.globi.geo.LatLng;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.eol.globi.domain.TaxonomyProvider.GEONAMES;

public class GeoNamesServiceImpl implements GeoNamesService {
    private static final Log LOG = LogFactory.getLog(GeoNamesServiceImpl.class);

    public static final TermImpl GEO_TERM_EARTH = new TermImpl(GEONAMES.getIdPrefix() + "6295630", "Earth");

    private static Map<String, TermImpl> LOCALE_TO_GEONAMES = new HashMap<String, TermImpl>() {{
        put("Country: New Zealand;   State: Otago;   Locality: Catlins, Craggy Tor catchment", new TermImpl(GEONAMES.getIdPrefix() + "6612109", "Otago"));
        put("Country: Scotland", new TermImpl(GEONAMES.getIdPrefix() + "2638360", "Scotland"));
        put("Country: USA;   State: Georgia", new TermImpl(GEONAMES.getIdPrefix() + "4197000", "State of Georgia"));
        put("Country: USA;   State: Iowa", new TermImpl(GEONAMES.getIdPrefix() + "4862182", "State of Iowa"));
        put("Country: Southern Ocean", new TermImpl(GEONAMES.getIdPrefix() + "4036776", "Southern Ocean"));
        put("Country: USA", new TermImpl(GEONAMES.getIdPrefix() + "6252001", "United States of America"));
        put("Country: USA;   State: Iowa;   Locality: Mississippi River", new TermImpl(GEONAMES.getIdPrefix() + "4862182", "State of Iowa"));
        put("Country: Japan", new TermImpl(GEONAMES.getIdPrefix() + "1861060", "Japan"));
        put("Japan", new TermImpl(GEONAMES.getIdPrefix() + "1861060", "Japan"));
        put("Country: Malaysia;   Locality: W. Malaysia", new TermImpl(GEONAMES.getIdPrefix() + "1745185", "Peninsular Malaysia"));
        put("Country: Chile;   Locality: central Chile", new TermImpl(GEONAMES.getIdPrefix() + "3882554", "Central Valley"));
        put("Country: USA;   State: New Mexico;   Locality: Aden Crater", new TermImpl(GEONAMES.getIdPrefix() + "5454273", "Aden Crater"));
        put("Country: USA;   State: Alaska;   Locality: Torch Bay", new TermImpl(GEONAMES.getIdPrefix() + "5558058", "Torch Bay"));
        put("Country: USA;   State: Pennsylvania", new TermImpl(GEONAMES.getIdPrefix() + "6254927", "Pennsylvania"));
        put("Country: Costa Rica", new TermImpl(GEONAMES.getIdPrefix() + "3624060", "Costa Rica"));
        put("Costa Rica", new TermImpl(GEONAMES.getIdPrefix() + "3624060", "Costa Rica"));
        put("Country: Pacific", new TermImpl(GEONAMES.getIdPrefix() + "2363254", "Pacific Ocean"));
        put("Country: USA;   State: California;   Locality: Cabrillo Point", new TermImpl(GEONAMES.getIdPrefix() + "5332459", "Cabrillo Point"));
        put("Country: USA;   State: Texas", new TermImpl(GEONAMES.getIdPrefix() + "4736286", "Texas"));
        put("Country: Portugal", new TermImpl(GEONAMES.getIdPrefix() + "2264397", "Portugal"));
        put("Country: USA;   Locality: Northeastern US contintental shelf", new TermImpl(GEONAMES.getIdPrefix() + "6252001", "United States of America"));
        put("Country: Sri Lanka", new TermImpl(GEONAMES.getIdPrefix() + "1227603", "Sri Lanka"));
        put("Country: USA;   State: Maine;   Locality: Troy", new TermImpl(GEONAMES.getIdPrefix() + "4981180", "Troy"));
        put("Country: New Zealand", new TermImpl(GEONAMES.getIdPrefix() + "2186224", "New Zealand"));
        put("Country: USA;   State: Maine;   Locality: Gulf of Maine", new TermImpl(GEONAMES.getIdPrefix() + "4971067", "Gulf of Maine"));
        put("Country: New Zealand;   State: Otago;   Locality: Dempster's Stream, Taieri River, 3 O'Clock catchment", new TermImpl(GEONAMES.getIdPrefix() + "6612109", "Otago"));
        put("Country: Panama;   Locality: Gatun Lake", new TermImpl(GEONAMES.getIdPrefix() + "3709289", "Lake Gatun"));
        put("Country: USA;   State: Maryland;   Locality: Chesapeake Bay", new TermImpl(GEONAMES.getIdPrefix() + "4351177", "Chesapeake Bay"));
        put("Country: India;   Locality: Cochin", new TermImpl(GEONAMES.getIdPrefix() + "1273874", "Cochin"));
        put("Country: Ethiopia;   Locality: Lake Abaya", new TermImpl(GEONAMES.getIdPrefix() + "345844", "Lake Abaya"));
        put("Country: unknown;   State: Black Sea", new TermImpl(GEONAMES.getIdPrefix() + "630673", "Black Sea"));
        put("Country: St. Martin;   Locality: Caribbean", new TermImpl(GEONAMES.getIdPrefix() + "3578421", "Saint Martin"));
        put("Country: USA;   State: Yellowstone", new TermImpl(GEONAMES.getIdPrefix() + "5843642", "Yellowstone National Park"));
        put("Country: Scotland;   Locality: Loch Leven", new TermImpl(GEONAMES.getIdPrefix() + "2644575", "Loch Leven"));
        put("Country: New Zealand;   State: Otago;   Locality: Sutton Stream, Taieri River, Sutton catchment", new TermImpl(GEONAMES.getIdPrefix() + "6612109", "Otago"));
        put("Country: USA;   State: Alaska;   Locality: Barrow", new TermImpl(GEONAMES.getIdPrefix() + "5880054", "Barrow"));
        put("Country: Malawi;   Locality: Lake Nyasa", new TermImpl(GEONAMES.getIdPrefix() + "924329", "Lake Malawi"));
        put("Country: USA;   State: Alaska;   Locality: Aleutian Islands", new TermImpl(GEONAMES.getIdPrefix() + "5879148", "Aleutian Islands"));
        put("Country: USA;   State: California;   Locality: Southern California", new TermImpl(GEONAMES.getIdPrefix() + "5332921", "California"));
        put("Country: Canada;   State: Manitoba", new TermImpl(GEONAMES.getIdPrefix() + "6065171", "Province of Manitoba"));
        put("Country: USA;   State: Maine", new TermImpl(GEONAMES.getIdPrefix() + "4971068", "State Of Maine"));
        put("Country: Polynesia", new TermImpl(GEONAMES.getIdPrefix() + "7729901", "Polynesia"));
        put("Country: South Africa", new TermImpl(GEONAMES.getIdPrefix() + "953987", "South Africa"));
        put("South Africa", new TermImpl(GEONAMES.getIdPrefix() + "953987", "South Africa"));
        put("Country: New Zealand;   State: Otago;   Locality: Berwick, Meggatburn", new TermImpl(GEONAMES.getIdPrefix() + "2193374", "Berwick"));
        put("Country: New Zealand;   State: Otago;   Locality: Venlaw, Mimihau catchment", new TermImpl(GEONAMES.getIdPrefix() + "2180525", "Venlaw"));
        put("Country: USA;   State: Montana", new TermImpl(GEONAMES.getIdPrefix() + "5667009", "Montana"));
        // TODO introduce one to many mapping
        put("Country: UK;   State: Yorkshire;   Locality: Aire,  Nidd & Wharfe Rivers", new TermImpl(GEONAMES.getIdPrefix() + "2657602", "River Aire"));
        put("Country: UK;   State: Yorkshire;   Locality: Aire,  Nidd & Wharfe Rivers", new TermImpl(GEONAMES.getIdPrefix() + "2641500", "River Nidd"));
        put("Country: UK;   State: Yorkshire;   Locality: Aire,  Nidd & Wharfe Rivers", new TermImpl(GEONAMES.getIdPrefix() + "2634186", "River Wharfe"));
        put("Country: Hong Kong", new TermImpl(GEONAMES.getIdPrefix() + "1819730", "Hong Kong"));
        put("Country: Pacific;   State: Bay of Panama", new TermImpl(GEONAMES.getIdPrefix() + "3703442", "Panama Bay"));
        put("Country: Netherlands;   State: Wadden Sea;   Locality: Ems estuary", new TermImpl(GEONAMES.getIdPrefix() + "3230285", "Niedersaechsisches Wattenmeer"));
        put("Country: New Zealand;   State: Otago;   Locality: North Col, Silver catchment", new TermImpl(GEONAMES.getIdPrefix() + "6612109", "Otago"));
        put("Country: USA;   State: North Carolina", new TermImpl(GEONAMES.getIdPrefix() + "4482348", "State of North Carolina"));
        put("Country: USA;   State: Washington", new TermImpl(GEONAMES.getIdPrefix() + "5815135", "State of Washington"));
        put("Country: USA;   State: Alaska", new TermImpl(GEONAMES.getIdPrefix() + "5879092", "State of Alaska"));
        put("Country: USA;   State: Hawaii", new TermImpl(GEONAMES.getIdPrefix() + "5855797", "State of Hawaii"));
        put("Country: Uganda;   Locality: Lake George", new TermImpl(GEONAMES.getIdPrefix() + "233416", "Lake George"));
        put("Country: Costa Rica;   State: Guanacaste", new TermImpl(GEONAMES.getIdPrefix() + "3623582", "Guanacaste Province"));
        put("Country: USA;   State: Massachusetts;   Locality: Cape Ann", new TermImpl(GEONAMES.getIdPrefix() + "4929094", "Cape Ann"));
        put("Country: USA;   State: Maine;   Locality: Martins", new TermImpl(GEONAMES.getIdPrefix() + "4971068", "State of Maine"));
        put("Country: USA;   State: New York", new TermImpl(GEONAMES.getIdPrefix() + "5128638", "State of New York"));
        put("Country: General;   Locality: General", GEO_TERM_EARTH);
        put("Country: New Zealand;   State: Otago;   Locality: Stony, Sutton catchment", new TermImpl(GEONAMES.getIdPrefix() + "6612109", "Otago"));
        put("Country: Tibet", new TermImpl(GEONAMES.getIdPrefix() + "1279685", "Tibet Autonomous Region"));
        put("Country: USA;   State: Texas;   Locality: Franklin Mtns", new TermImpl(GEONAMES.getIdPrefix() + "5521842", "Franklin Mountains"));
        put("Country: Russia", new TermImpl(GEONAMES.getIdPrefix() + "2017370", "Russia"));
        put("Country: New Zealand;   State: Otago;   Locality: Broad, Lee catchment", new TermImpl(GEONAMES.getIdPrefix() + "6612109", "Otago"));
        put("Country: Africa;   Locality: Lake McIlwaine", new TermImpl(GEONAMES.getIdPrefix() + "886254", "Lake Chivero"));
        put("Country: England;   State: River Medway", new TermImpl(GEONAMES.getIdPrefix() + "2642831", "River Medway"));
        put("Country: South Africa;   Locality: Southwest coast", new TermImpl(GEONAMES.getIdPrefix() + "953987", "Republic of South Africa"));
        put("Country: USA;   State: Kentucky", new TermImpl(GEONAMES.getIdPrefix() + "4296940", "Commonwealth of Kentucky"));
        put("Country: USA;   State: Washington;   Locality: Cape Flattery", new TermImpl(GEONAMES.getIdPrefix() + "5794609", "Cape Flattery"));
        put("Country: USA;   State: New Jersey", new TermImpl(GEONAMES.getIdPrefix() + "5101760", "State of New Jersey"));
        put("Country: India;   Locality: Rajasthan Desert", new TermImpl(GEONAMES.getIdPrefix() + "1270835", "Thar Desert"));
        put("Country: England", new TermImpl(GEONAMES.getIdPrefix() + "6269131", "England"));
        put("Country: Austria;   Locality: Hafner Lake", new TermImpl(GEONAMES.getIdPrefix() + "2782113", "Republic of Austria"));
        put("Country: USA;   State:  NE USA", new TermImpl(GEONAMES.getIdPrefix() + "6252001", "United States of America"));
        put("Country: England;   Locality: Sheffield", new TermImpl(GEONAMES.getIdPrefix() + "2638077", "Sheffield"));
        put("Country: Uganda", new TermImpl(GEONAMES.getIdPrefix() + "226074", "Uganda"));
        put("Country: USA;   State:  California;   Locality: Monterey Bay", new TermImpl(GEONAMES.getIdPrefix() + "5374363", "Monterey Bay"));
        put("Country: Germany", new TermImpl(GEONAMES.getIdPrefix() + "2921044", "Federal Republic of Germany"));
        put("Country: England;   Locality: Skipwith Pond", new TermImpl(GEONAMES.getIdPrefix() + "7296912", "Skipwidth"));
        put("Country: USA;   State: Wisconsin;   Locality: Little Rock Lake", new TermImpl(GEONAMES.getIdPrefix() + "5260513", "Little Rock Lake"));
        put("Country: USA;   State: California;   Locality: Coachella Valley", new TermImpl(GEONAMES.getIdPrefix() + "5338176", "Coachella Valley"));
        put("Country: Arctic", new TermImpl(GEONAMES.getIdPrefix() + "2960860", "Arctic Ocean"));
        put("Country: USA;   State: Michigan", new TermImpl(GEONAMES.getIdPrefix() + "5001836", "State of Michigan"));
        put("Country: Mexico;   State: Guerrero", new TermImpl(GEONAMES.getIdPrefix() + "3527213", "Guerrero"));
        put("Country: Norway;   State: Spitsbergen", new TermImpl(GEONAMES.getIdPrefix() + "2728573", "Spitzbergen"));
        put("Country: USA;   State: Kentucky;   Locality: Station 1", new TermImpl(GEONAMES.getIdPrefix() + "6254925", "Commonwealth of Kentucky"));
        put("Country: New Zealand;   State: Otago;   Locality: Kye Burn", new TermImpl(GEONAMES.getIdPrefix() + "6211668", "Kye Burn"));
        put("Country: New Zealand;   State: Otago;   Locality: Little Kye, Kye Burn catchment", new TermImpl(GEONAMES.getIdPrefix() + "6612109", "Otago"));
        put("Country: USA;   State: North Carolina;   Locality: Pamlico", new TermImpl(GEONAMES.getIdPrefix() + "4482348", "North Carolina"));
        put("Country: Antarctic", new TermImpl(GEONAMES.getIdPrefix() + "6255152", "Antarctica"));
        put("Country: USA;   State: Arizona", new TermImpl(GEONAMES.getIdPrefix() + "5551752", "State of Arizona"));
        put("Country: England;   Locality: Lancaster", new TermImpl(GEONAMES.getIdPrefix() + "2644972", "Lancaster"));
        put("Country: USA;   State: Florida;   Locality: Everglades", new TermImpl(GEONAMES.getIdPrefix() + "4154663", "Everglades National Park"));
        put("Country: Barbados", new TermImpl(GEONAMES.getIdPrefix() + "3374084", "Barbados"));
        put("Country: USA;   State: New York;   Locality: Bridge Brook", new TermImpl(GEONAMES.getIdPrefix() + "5110126", "Bridge Brooke Pond"));
        put("Country: England;   Locality: Oxshott Heath", new TermImpl(GEONAMES.getIdPrefix() + "2640718", "Oxshott"));
        put("Country: New Zealand;   State: Otago;   Locality: Blackrock, Lee catchment", new TermImpl(GEONAMES.getIdPrefix() + "6612109", "Otago"));
        put("Country: Canada;   State: Ontario", new TermImpl(GEONAMES.getIdPrefix() + "6093943", "Ontario"));
        put("Country: Puerto Rico;   Locality: El Verde", new TermImpl(GEONAMES.getIdPrefix() + "4564245", "El Verde"));
        put("Country: Quebec", new TermImpl(GEONAMES.getIdPrefix() + "6115047", "Quebec"));
        put("Country: Ireland", new TermImpl(GEONAMES.getIdPrefix() + "2963597", "Ireland"));
        put("Country: Wales;   Locality: Dee River", new TermImpl(GEONAMES.getIdPrefix() + "2651430", "River Dee [Wales]"));
        put("Country: Marshall Islands", new TermImpl(GEONAMES.getIdPrefix() + "2080185", "Marshall Islands"));
        put("Country: New Zealand;   State: South Island;   Locality: Canton Creek, Taieri River, Lee catchment", new TermImpl(GEONAMES.getIdPrefix() + "2182504", "South Island"));
        put("Country: Seychelles", new TermImpl(GEONAMES.getIdPrefix() + "241170", "Republic of Seychelles"));
        put("Country: Namibia;   Locality: Namib Desert", new TermImpl(GEONAMES.getIdPrefix() + "3347020", "Namib Desert"));
        put("Country: USA;   State: Rhode Island", new TermImpl(GEONAMES.getIdPrefix() + "5224323", "State of Rhode Island"));
        put("Country: USA;   State: Idaho-Utah;   Locality: Deep Creek", new TermImpl(GEONAMES.getIdPrefix() + "5596512", "Idaho"));
        put("Country: Malawi", new TermImpl(GEONAMES.getIdPrefix() + "927384", "Republic of Malawi"));
        put("Country: Malaysia", new TermImpl(GEONAMES.getIdPrefix() + "1733045", "Malaysia"));
        put("Malaysia", new TermImpl(GEONAMES.getIdPrefix() + "1733045", "Malaysia"));
        put("Country: Europe;   State: Central Europe", new TermImpl(GEONAMES.getIdPrefix() + "6255148", "Europe"));
        put("Country: USA;   State: Florida", new TermImpl(GEONAMES.getIdPrefix() + "4155751", "Florida"));
        put("Country: Norway;   State: Oppland;   Locality: Ovre Heimdalsvatn Lake", new TermImpl(GEONAMES.getIdPrefix() + "3144873", "Heimdalsvatnet Nedre"));
        put("Country: Austria;   Locality: Vorderer Finstertaler Lake", new TermImpl(GEONAMES.getIdPrefix() + "2779556", "Finstertaler Seen"));
        put("Country: Canada;   Locality: high Arctic", new TermImpl(GEONAMES.getIdPrefix() + "6251999", "Canada"));
        put("Country: unknown", new TermImpl(GEONAMES.getIdPrefix() + "6295630", "Earth"));
        put("Country: Peru", new TermImpl(GEONAMES.getIdPrefix() + "3932488", "Republic of Peru"));
        put("Country: USA;   State: New England", new TermImpl(GEONAMES.getIdPrefix() + "6252001", "United States of America"));
        put("Country: Great Britain", new TermImpl(GEONAMES.getIdPrefix() + "2635167", "United Kingdom"));
        put("Country: New Zealand;   State: Otago;   Locality: German, Kye Burn catchment", new TermImpl(GEONAMES.getIdPrefix() + "6612109", "Otago"));
        put("Country: USA;   State: Colorado", new TermImpl(GEONAMES.getIdPrefix() + "5417618", "State of Colorado"));
        put("Country: USA;   State: Texas;   Locality: Hueco Tanks", new TermImpl(GEONAMES.getIdPrefix() + "5523568", "State of Texas"));
        put("Country: Canada;   State: Ontario;   Locality: Mad River", new TermImpl(GEONAMES.getIdPrefix() + "6064001", "Mad River"));
        put("Country: Wales;   Locality: River Rheidol", new TermImpl(GEONAMES.getIdPrefix() + "2639467", "Afon Rheidol"));
        put("Country: Costa Rica;   State: de Osa", new TermImpl(GEONAMES.getIdPrefix() + "3622657", "Cantón de Osa"));
        put("Country: Finland", new TermImpl(GEONAMES.getIdPrefix() + "660013", "Republic of Finland"));
        put("Country: Africa;   Locality: Crocodile Creek,  Lake Nyasa", new TermImpl(GEONAMES.getIdPrefix() + "924329", "Lake Malawi"));
        put("Country: USA;   State: Florida;   Locality: South Florida", new TermImpl(GEONAMES.getIdPrefix() + "4155751", "Florida"));
        put("Country: USA;   State: Illinois", new TermImpl(GEONAMES.getIdPrefix() + "4896861", "State of Illinois"));
        put("Country: Puerto Rico;   Locality: Puerto Rico-Virgin Islands shelf", new TermImpl(GEONAMES.getIdPrefix() + "4566966", "Puerto Rico"));
        put("Country: England;   Locality: River Thames", new TermImpl(GEONAMES.getIdPrefix() + "2636063", "Thames"));
        put("Country: Madagascar", new TermImpl(GEONAMES.getIdPrefix() + "1062947", "Madagascar"));
        put("Madagascar", new TermImpl(GEONAMES.getIdPrefix() + "1062947", "Madagascar"));
        put("Country: USA;   State: New Mexico;   Locality: White Sands", new TermImpl(GEONAMES.getIdPrefix() + "5497915", "White Sands"));
        put("Country: England;   Locality: River Cam", new TermImpl(GEONAMES.getIdPrefix() + "2653956", "River Cam"));
        put("Country: Australia", new TermImpl(GEONAMES.getIdPrefix() + "2077456", "Australia"));
        put("Australia", new TermImpl(GEONAMES.getIdPrefix() + "2077456", "Australia"));
        put("Country: USA;   State: North Carolina;   Locality: Coweeta", new TermImpl(GEONAMES.getIdPrefix() + "4462207", "Coweeta Gap"));
        put("Country: Scotland;   Locality: Ythan estuary", new TermImpl(GEONAMES.getIdPrefix() + "2633329", "River Ythan"));
        put("Country: Wales;   Locality: River Clydach", new TermImpl(GEONAMES.getIdPrefix() + "3333248", "Swansea"));
        put("Country: USA;   State: Texas;   Locality: Hueco Mountains", new TermImpl(GEONAMES.getIdPrefix() + "5523564", "Hueco Mountains"));
        put("Country: Wales", new TermImpl(GEONAMES.getIdPrefix() + "2634895", "Wales"));
        put("Country: USA;   State: Arizona;   Locality: Sonora Desert", new TermImpl(GEONAMES.getIdPrefix() + "4018698", "Sonoran Desert"));
        put("Country: England;   Locality: Silwood Park", new TermImpl(GEONAMES.getIdPrefix() + "2656992", "Ascot"));
        put("Country: Austria;   Locality: Neusiedler Lake", new TermImpl(GEONAMES.getIdPrefix() + "3052649", "Lake Neusiedl"));
        put("Country: New Zealand;   State: Otago;   Locality: Narrowdale catchment", new TermImpl(GEONAMES.getIdPrefix() + "6210297", "Lower Narrowdale Stream"));
        put("Country: USA;   State: California", new TermImpl(GEONAMES.getIdPrefix() + "5332921", "State of California"));
        put("Country: England;   State: Oxfordshire;   Locality: Wytham Wood", new TermImpl(GEONAMES.getIdPrefix() + "7300975", "Wytham"));
        put("Country: USA;   State: Michigan;   Locality: Tuesday Lake", new TermImpl(GEONAMES.getIdPrefix() + "5012680", "Tuesday Lake"));
        put("Country: USA;   State: Alabama", new TermImpl(GEONAMES.getIdPrefix() + "4829764", "State of Alabama"));
        put("Country: New Zealand;   State: Otago;   Locality: Healy Stream, Taieri River, Kye Burn catchment", new TermImpl(GEONAMES.getIdPrefix() + "6211668", "Kye Burn"));
        put("Country: USA;   State: New York;   Locality: Long Island", new TermImpl(GEONAMES.getIdPrefix() + "5125123", "Long Island"));
        put("Country: Venezuela", new TermImpl(GEONAMES.getIdPrefix() + "3625428", "Venezuela"));
        put("Country: New Zealand;   State: Otago;   Locality: Akatore, Akatore catchment", new TermImpl(GEONAMES.getIdPrefix() + "2194057", "Akatore"));
        put("Kerguelen Island", new TermImpl(GEONAMES.getIdPrefix() + "1546557", "Île Kerguelen"));
        put("Antarctic continental slope & oceanic areas", new TermImpl(GEONAMES.getIdPrefix() + "2208337", "Campbell Escarpment"));
        put("South Georgia", new TermImpl(GEONAMES.getIdPrefix() + "3426222", "South Sandwich Islands"));
        put("Iles Kerguelen", new TermImpl(GEONAMES.getIdPrefix() + "1546556", "Îles Kerguelen"));
        put("Scotia Sea", new TermImpl(GEONAMES.getIdPrefix() + "3426293", "Scotia Sea"));
        put("Adjacent to Vestfold Hills", new TermImpl(GEONAMES.getIdPrefix() + "6627488", "Vestfold Hills"));
        put("Antarctic Peninsula", new TermImpl(GEONAMES.getIdPrefix() + "6632680", "Antarctic Peninsula"));
        put("Prydz Bay", new TermImpl(GEONAMES.getIdPrefix() + "6623681", "Prydz Bay"));
        put("Ross Sea", new TermImpl(GEONAMES.getIdPrefix() + "4036625", "Ross Sea"));
        put("Not described.", GEO_TERM_EARTH);
        put("New Zealand", new TermImpl(GEONAMES.getIdPrefix() + "2186224", "New Zealand"));
        put("Crozet Island", new TermImpl(GEONAMES.getIdPrefix() + "936338", "Îles Crozet"));
        put("Weddell Sea", new TermImpl(GEONAMES.getIdPrefix() + "4036624", "Weddell Sea"));
        put("South Orkney Islands", new TermImpl(GEONAMES.getIdPrefix() + "6625763", "South Orkney Islands"));
        put("Iles Crozets", new TermImpl(GEONAMES.getIdPrefix() + "936338", "Îles Crozet"));
        put("Southern ocean", new TermImpl(GEONAMES.getIdPrefix() + "4036776", "Southern Ocean"));
        put("Kerguelen Islands", new TermImpl(GEONAMES.getIdPrefix() + "1546556", "Îles Kerguelen"));
        put("Prince Edward Islands", new TermImpl(GEONAMES.getIdPrefix() + "7778803", "Prince Edward Island"));
        put("Marion Island", new TermImpl(GEONAMES.getIdPrefix() + "7778802", "Marion Island"));
        put("South Indian Ocean", new TermImpl(GEONAMES.getIdPrefix() + "4036667", "South Indian Basin"));
        put("Crozet Island waters", new TermImpl(GEONAMES.getIdPrefix() + "936338", "Îles Crozet"));
        put("southern Weddell Sea", new TermImpl(GEONAMES.getIdPrefix() + "4036624", "Weddell Sea"));
        put("Heard Island", new TermImpl(GEONAMES.getIdPrefix() + "1547315", "Heard Island"));
        put("Terra Nova Bay", new TermImpl(GEONAMES.getIdPrefix() + "6626583", "Terra Nova Bay"));
        put("Straits of Magellan", new TermImpl(GEONAMES.getIdPrefix() + "3845265", "Strait of Magellan"));
        put("Antarctic and subantarctic waters", new TermImpl(GEONAMES.getIdPrefix() + "6632710", "Antarctic"));
        put("Antarctic waters", new TermImpl(GEONAMES.getIdPrefix() + "6632710", "Antarctic"));
        put("McMurdo Sound", new TermImpl(GEONAMES.getIdPrefix() + "6637890", "McMurdo Sound"));
        put("Zambia", new TermImpl(GEONAMES.getIdPrefix() + "895949", "Republic of Zambia"));
        put("Ivory Coast", new TermImpl(GEONAMES.getIdPrefix() + "2287781", "Ivory Coast"));
        put("Cameroun", new TermImpl(GEONAMES.getIdPrefix() + "2233387", "Cameroon"));
        put("Gabon", new TermImpl(GEONAMES.getIdPrefix() + "2400553", "Gabon"));
        put("Mozambique", new TermImpl(GEONAMES.getIdPrefix() + "1036973", "Mozambique"));
        put("Sao Tome", new TermImpl(GEONAMES.getIdPrefix() + "2410763", "São Tomé"));
        put("France", new TermImpl(GEONAMES.getIdPrefix() + "3017382", "France"));
        put("China", new TermImpl(GEONAMES.getIdPrefix() + "1814991", "China"));
        put("Papua New Guinea", new TermImpl(GEONAMES.getIdPrefix() + "2088628", "Papua New Guinea"));
        put("Vanuatu Islands", new TermImpl(GEONAMES.getIdPrefix() + "2134431", "Vanuatu"));
        put("La Réunion", new TermImpl(GEONAMES.getIdPrefix() + "935317", "Réunion"));
        put("Burkina Faso", new TermImpl(GEONAMES.getIdPrefix() + "2361809", "Burkina Faso"));
        put("Philippines", new TermImpl(GEONAMES.getIdPrefix() + "1694008", "Philippines"));
        put("Taiwan", new TermImpl(GEONAMES.getIdPrefix() + "1668284", "Taiwan"));
        put("Solomon Islands", new TermImpl(GEONAMES.getIdPrefix() + "2103350", "Solomon Islands"));
        put("Indonesia", new TermImpl(GEONAMES.getIdPrefix() + "1643084", "Indonesia"));
        put("Uganda", new TermImpl(GEONAMES.getIdPrefix() + "226074", "Uganda"));
        put("Mexico", new TermImpl(GEONAMES.getIdPrefix() + "3996063", "Mexico"));
        put("French Guiana", new TermImpl(GEONAMES.getIdPrefix() + "3381670", "French Guiana"));
        put("Brunei", new TermImpl(GEONAMES.getIdPrefix() + "1820814", "Brunei"));
        put("Singapore", new TermImpl(GEONAMES.getIdPrefix() + "1880251", "Singapore"));
        put("New Caledonia", new TermImpl(GEONAMES.getIdPrefix() + "2139685", "New Caledonia"));
        put("Brazil", new TermImpl(GEONAMES.getIdPrefix() + "3469034", "Brazil"));
        put("Panama", new TermImpl(GEONAMES.getIdPrefix() + "3703430", "Panama"));
        put("Zimbabwe", new TermImpl(GEONAMES.getIdPrefix() + "878675", "Zimbabwe"));
        put("Colombia", new TermImpl(GEONAMES.getIdPrefix() + "3686110", "Colombia"));
    }};
    private Map<String, LatLng> pointCache = new ConcurrentHashMap<String, LatLng>();

    @Override
    public boolean hasTermForLocale(String locality) {
        return locality != null && LOCALE_TO_GEONAMES.containsKey(locality);
    }

    @Override
    public LatLng findLatLng(String geoNameTermOrLocale) throws IOException {
        LatLng point = pointCache.get(geoNameTermOrLocale);
        return point == null ? getCentroid(geoNameTermOrLocale) : point;
    }

    private LatLng getCentroid(String geoNameTermOrLocale) throws IOException {
        LatLng point;

        if (StringUtils.startsWith(geoNameTermOrLocale, GEONAMES.getIdPrefix())) {
            point = getCentroidForGeoNameTerm(geoNameTermOrLocale);
        } else {
            point = getCentroidForLocale(geoNameTermOrLocale);
        }
        if (point == null) {
            LOG.warn("failed to locate (lat,lng) for term [" + geoNameTermOrLocale + "]");
        } else {
            pointCache.put(geoNameTermOrLocale, point);
        }

        return point;
    }

    private LatLng getCentroidForLocale(String locale) throws IOException {
        LatLng point = null;
        TermImpl term = LOCALE_TO_GEONAMES.get(locale);
        // see https://github.com/jhpoelen/eol-globi-data/issues/39
        if (term != null && !term.equals(GEO_TERM_EARTH)) {
            String geoNamesTerm = term.getId();
            if (LOCALE_TO_GEONAMES.containsKey(locale)) {
                point = getCentroidForGeoNameTerm(geoNamesTerm);
            }

            if (point != null) {
                pointCache.put(geoNamesTerm, point);
            }
        }
        return point;
    }

    private LatLng getCentroidForGeoNameTerm(String geoNamesTerm) throws IOException {
        LatLng resolvedPoint = null;
        if (geoNamesTerm.startsWith(GEONAMES.getIdPrefix())) {
            Long id = parseGeoId(geoNamesTerm);
            if (id != null) {
                resolvedPoint = getCentroid(id);
            }
        }
        return resolvedPoint;
    }

    public static Long parseGeoId(String idString) {
        String s = idString.replaceFirst(GEONAMES.getIdPrefix(), "");
        Long id = null;
        if (StringUtils.isNotBlank(s)) {
            try {
                id = Long.parseLong(s);
            } catch (NumberFormatException ex) {
                //
            }
        }
        return id;
    }

    public LatLng getCentroid(Long id) throws IOException {
        LatLng point = null;
        String jsonString = HttpUtil.getRemoteJson("http://api.geonames.org/getJSON?formatted=true&geonameId=" + id + "&username=globi&style=full");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(jsonString);
        if (node.has("lat") && node.has("lng")) {
            double lat = Double.parseDouble(node.get("lat").asText());
            double lng = Double.parseDouble(node.get("lng").asText());
            point = new LatLng(lat, lng);

        }
        return point;
    }
}
