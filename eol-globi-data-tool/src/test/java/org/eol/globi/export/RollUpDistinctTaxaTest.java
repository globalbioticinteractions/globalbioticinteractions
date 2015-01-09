package org.eol.globi.export;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class RollUpDistinctTaxaTest extends RollUpTest {

    @Test
    public void export() throws NodeFactoryException, ParseException, IOException {
        Study myStudy = createStudy();
        StringWriter writer = new StringWriter();
        new RollUpDistinctTaxa().doExportStudy(myStudy, writer, true);
        String exported = writer.toString();
        assertThat(exported, containsString("genus id 2,genus 2,,,phylum 2,,,family 2,genus 2,genus,,,,,"));
        assertThat(exported, containsString("genus id 1,genus 1,,,phylum 1,,,,genus 1,genus,,,,,"));
        assertThat(exported, containsString("species id 2,species 2,,,phylum 2,,,family 2,genus 2,species,,,,,"));
        assertThat(exported, containsString("species id 1,species 1,,,phylum 1,,,,genus 1,species,,,,,"));
        assertThat(exported, containsString("family id 2,family 2,,,phylum 2,,,family 2,,family,,,,,"));
        assertThat(exported, not(containsString("phylum id 1,phylum 1,")));
    }

    @Test
    public void collectUniqueExpandTaxa() {
        Map<String, Object> result = new HashMap<String, Object>() {
            {
                put("rank", "the taxon rank");
                put("pathNames", "kingdom | phylum | class | order | family | genus");
                put("path", "the kingdom | the phylum | the class | the order | the family | the genus");
                put("pathIds", "EOL:1 | EOL:2 | EOL:3 | EOL:4 | EOL:5 | EOL:1234");
                put("scientificName", "Some namus");
                put("taxonId", "EOL:1234");
            }
        };

        List<Taxon> expandedResults = RollUpDistinctTaxa.expandTaxonResult(result);

        assertThat(expandedResults.size(), is(3));
        Taxon genus = expandedResults.get(2);
        assertThat(genus.getRank(), is("genus"));
        assertThat(genus.getName(), is("the genus"));
        assertThat(genus.getPath(), is("the kingdom | the phylum | the class | the order | the family | the genus"));
        assertThat(genus.getPathNames(), is("kingdom | phylum | class | order | family | genus"));
        assertThat(genus.getExternalId(), is("EOL:1234"));

        Taxon family = expandedResults.get(1);
        assertThat(family.getRank(), is("family"));
        assertThat(family.getName(), is("the family"));
        assertThat(family.getPath(),  is("the kingdom | the phylum | the class | the order | the family"));
        assertThat(family.getExternalId(),  is("EOL:5"));

        Taxon order = expandedResults.get(0);
        assertThat(order.getRank(), is("order"));
        assertThat(order.getName(), is("the order"));
        assertThat(order.getPath(), is("the kingdom | the phylum | the class | the order"));
        assertThat(order.getPathNames(), is("kingdom | phylum | class | order"));
        assertThat(order.getExternalId(), is("EOL:4"));

    }


}