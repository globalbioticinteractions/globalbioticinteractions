package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.service.TaxonUtil;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;

public class AttackRemarksParser implements RemarksParser {

    static final Pattern ATTACKS_PHRASE
            = Pattern.compile(
            ".*(attacking|attacks)[ ]+(.*)",
            Pattern.CASE_INSENSITIVE
    );

    static final Pattern ATTACKED_BY_PHRASE
            = Pattern.compile(
            ".*(ATTACKED BY)[ ]+(.*)",
            Pattern.CASE_INSENSITIVE
    );
    static final Pattern CAT_ATTACK
            = Pattern.compile(
            ".*(cat attack).*",
            Pattern.CASE_INSENSITIVE
    );

    static final Pattern DOG_ATTACK
            = Pattern.compile(
            ".*(dog attack).*",
            Pattern.CASE_INSENSITIVE
    );

    static final Pattern ANIMAL_ATTACK
            = Pattern.compile(
            ".*(animal attack).*",
            Pattern.CASE_INSENSITIVE
    );


    @Override
    public Map<String, String> parse(String remarks) {

        Map<String, String> properties = new TreeMap<>();

        Matcher matcher1 = ATTACKED_BY_PHRASE.matcher(remarks);
        if (matcher1.matches()) {
            properties.put(TaxonUtil.TARGET_TAXON_NAME, matcher1.group(2));
            properties.put(INTERACTION_TYPE_NAME, "attacked by");
        } else {
            Matcher matcher = ATTACKS_PHRASE.matcher(remarks);
            if (matcher.matches()) {
                properties.put(TaxonUtil.TARGET_TAXON_NAME, matcher.group(2));
                properties.put(INTERACTION_TYPE_NAME, "attacks");
            } else if (CAT_ATTACK.matcher(remarks).matches()) {
                properties.put(TaxonUtil.TARGET_TAXON_NAME, "cat");
                properties.put(INTERACTION_TYPE_NAME, "attacked by");
            } else if (DOG_ATTACK.matcher(remarks).matches()) {
                properties.put(TaxonUtil.TARGET_TAXON_NAME, "dog");
                properties.put(INTERACTION_TYPE_NAME, "attacked by");
            } else if (ANIMAL_ATTACK.matcher(remarks).matches()) {
                properties.put(TaxonUtil.TARGET_TAXON_NAME, "animal");
                properties.put(INTERACTION_TYPE_NAME, "attacked by");
            }
        }
        return properties;

    }
}
