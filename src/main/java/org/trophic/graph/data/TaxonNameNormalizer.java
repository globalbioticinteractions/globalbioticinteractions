package org.trophic.graph.data;

import java.util.HashMap;
import java.util.Map;

public class TaxonNameNormalizer {

    protected static final Map<String, String> SAME_AS_MAP = new HashMap<String, String>() {{
        put("Eggs", "Animalia");
        put("Egg capsules", "Animalia");
        put("Unidentified gastropod", "Gastropoda");
        put("Lag rhomboides", "Lagodon rhomboides");
        put("Cynoscion arenurius", "Cynoscion arenarius");
        put("Teleost scales", "Teleostei");
        put("Penaeid shrimp post larvae", "Penaeidae");
        put("Unid fish", "Actinopterygii");
        put("Copepoda nauplii", "Copepoda");
        put("Menidia berylina", "Menidia beryllina");
        put("Molluks", "Mollusca");
        put("Lei xanthurus", "Leiostomus xanthurus");
        put("Unidentified fish larvae", "Actinopterygii");
        put("Cycloid fish scale", "Actinopterygii");
        put("Fun grandis", "Fundulus grandis");
        put("Opi robinsi", "Opisthonema robinsi");
        put("Euc gula", "Eucinostomus gula");
        put("Lucania parva eggs", "Lucania parva");
        put("Sciaenops ocellatus*", "Sciaenops ocellatus");
        put("Bai chrysoura", "Bairdiella chrysoura");
        put("Zoea", "Decapoda");
        put("Other Ephemeroptera", "Ephemeroptera");
        put("Fun majalis", "Fundulus majalis");
        put("Unidentified fish", "Actinopterygii");
        put("Har jaguana", "Harengula jaguana");
        put("Elo saurus", "Elops saurus");
        put("Other annelid worms", "Annelida");
        put("Cyprinodon variagatus", "Cyprinodon variegatus");
        put("Other Mollusks", "Mollusca");
        put("Bairdiella chyrsoura", "Bairdiella chrysoura");
        put("Unidentified insect larvae", "Insecta");
        put("Mollusks", "Mollusca");
        put("Flo carpio", "Floridichthys carpio");
        put("Oligoliptes saurus", "Oligoplites saurus");
        put("Ctenoid fish scale", "Actinopterygii");
        put("Logodon rhomboides", "Lagodon rhomboides");
        put("Cyn nebulosus", "Cynoscion nebulosus");
        put("Opi oglinum", "Opisthonema oglinum");
        put("Luc parva", "Lucania parva");
        put("Syngathus louisianae", "Syngnathus louisanae");
        put("Lepiosteus oculatus", "Lepisosteus oculatus");
        put("Syn foetens", "Synodus foetens");
        put("Squ empusa", "Squilla empusa");
        put("Carrcharhinus limbatus", "Carcharhinus limbatus");
        put("Ari felis", "Arius felis");
        put("Peneaus setiferus", "Litopeneaus setiferus");
        put("Bivalave", "Bivalvia");
        put("Archirus lineatus", "Achirus lineatus");
        put("Ort chrysoptera", "Orthopristis chrysoptera");
        put("Lagadon rhomboides", "Lagodon rhomboides");
        put("Chironomidae larvae", "Chironomidae");
        put("Isopods", "Isopoda");
        put("Cynosciona arenurius", "Cynoscion arenarius");
        put("Airus felis", "Arius felis");
        put("Unidentified insects", "Insecta");
        put("Invertebrate eggs", "Invertebrata");
        put("Neopunope sayi", "Neopanope sayi");
        put("Gobiosoma robustom", "Gobiosoma robustum");
        put("Decopoda larvae", "Decopoda");
        put("Catfish egg", "Catfish");
        put("Seeds", "Plantae");
        put("Cyp. Variegatus", "Cyprinodon variegatus");
        put("Cyprinodon varieagatus", "Cyprinodon variegatus");
        put("Polycheate", "Polycheata");
        put("Citharichtys macrops", "Citharichthys macrops");
        put("Adina xenica", "Adinia xenica");
        put("Unidentified bivalve", "Bivalvia");
        put("Other fundulus species", "Fundulus spp");
        put("Gobiosex strumosus", "Gobiesox strumosus");
        put("Citharichtys spilopterus", "Citharichthys spilopterus");
        put("Nematode", "Nematoda");
        put("Unidentified bone", "Animalia");
        put("Gobioosma bosc", "Gobiosoma bosc");
        put("Anidia xenica", "Adinia xenica");
        put("Fish eyes", "Actinopterygii");
        put("Other Molluks", "Mollusca");
        put("Mug cephalus", "Mugil cephalus");
        put("Other Hymenoptera", "Hymenoptera");
        put("Menida peninsulae", "Menidia peninsulae");
        put("Poe latipinna", "Poecilia latipinna");
        put("Bivalves", "Bivalvia");
        put("Mic gulosus", "Microgobius gulosus");
        put("Mug gyrans", "Mugil gyrans");
        put("Eurypaopeus depressus", "Eurypanopeus depressus");
        put("Other Gobiidae", "Gobiidae");
        put("Peneaus aztecus", "Farfantepeneaus aztecus");
        put("Other fish eggs", "Actinopterygii");
        put("Far duoraum", "Farfantepenaeus duoraum");
        put("Fish otolith", "Actinopterygii");
        put("Synodus poey", "Synodus poeyi");
        put("Neopenope sayi", "Neopanope sayi");
        put("Other Sciaenidae species", "Sciaenidae");
        put("Lucaina parva", "Lucania parva");
        put("Sci ocellatus", "Sciaenops ocellatus");
        put("Micropoganias undulatus", "Micropogonias undulatus");
        put("Cynoiscon nebulosus", "Cynoscion nebulosus");
        put("Gambuisa affinis", "Gambusia affinis");
        put("Cyprid larvae", "Barnacle");
        put("Nemotoda", "Nematoda");
        put("Citharicthys spilopterus", "Citharichthys spilopterus");
        put("Synathus louisanae", "Syngnathus louisanae");
        put("Fish bones", "Actinopterygii");
        put("Unidentified invertebrate", "Invertebrata");
    }};

    private static String clean(String name) {
        name = name.replaceAll("\\(.*\\)", "");
        name = name.replaceAll("[¬†*]", "");
        String trim = name.trim();
        return trim.replaceAll("(\\s+)", " ");
    }

    public String normalize(String taxonName) {
        String cleanName = clean(taxonName);
        String suggestedReplacement = SAME_AS_MAP.get(cleanName);
        if (suggestedReplacement != null) {
            cleanName = suggestedReplacement;
        }
        return cleanName;
    }
}
