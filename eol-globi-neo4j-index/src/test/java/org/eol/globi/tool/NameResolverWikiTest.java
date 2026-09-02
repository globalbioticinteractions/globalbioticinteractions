package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.PropertyEnricherNoop;
import org.eol.globi.data.ResolvingTaxonIndex;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.ResolvingTaxonIndexImpl;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;

public class NameResolverWikiTest extends GraphDBTestCase {

    @Test
    public void iNaturalistTaxonWikidata() throws NodeFactoryException {
        TaxonImpl taxon = new TaxonImpl("Ficus", "INAT_TAXON:50999");
        taxon.setPath("Plantae | Tracheophyta | Magnoliopsida | Rosales | Moraceae | Ficus");
        taxon.setPathNames("kingdom | phylum | class | order | family | genus");
        TaxonImpl taxon1 = new TaxonImpl("Ficus", "INAT_TAXON:208863");
        taxon1.setPath("Animalia | Mollusca | Gastropoda | Littorinimorpha | Ficidae | Ficus");
        taxon1.setPathNames("kingdom | phylum | class | order | family | genus");
        TaxonImpl taxon2 = new TaxonImpl("Ficus", "12345");
        TaxonImpl taxon3 = new TaxonImpl("Ficus", "WD:Q1938846");
        Study study = new StudyImpl("bla", null, null);
        Specimen someOtherOrganism = nodeFactory.createSpecimen(study, taxon);
        Specimen someOtherOrganism2 = nodeFactory.createSpecimen(study, taxon1);
        Specimen someOtherOrganism3 = nodeFactory.createSpecimen(study, taxon2);
        Specimen someOtherOrganism4 = nodeFactory.createSpecimen(study, taxon3);
        someOtherOrganism.ate(someOtherOrganism2);
        someOtherOrganism.ate(someOtherOrganism3);
        someOtherOrganism.ate(someOtherOrganism4);

        GraphServiceFactory graphServiceFactory = new GraphServiceFactoryProxy(getGraphDb());

        final NameResolver nameResolver = new NameResolver(
                graphServiceFactory,
                new TaxonIndexFactory() {
                    @Override
                    public ResolvingTaxonIndex create(Transaction tx) {
                        ResolvingTaxonIndex taxonIndexNew = new ResolvingTaxonIndexImpl(new PropertyEnricherNoop(), tx);
                        taxonIndexNew.setIndexResolvedTaxaOnly(false);
                        return taxonIndexNew;
                    }
                }
        );
        nameResolver.setBatchSize(1L);
        nameResolver.index();

        try (Transaction tx = getGraphDb().beginTx()) {
            Taxon resolvedTaxon = getTaxonIndexFactory().create(tx).findTaxonById("INAT_TAXON:50999");
            assertThat(resolvedTaxon, is(notNullValue()));
            assertThat(resolvedTaxon.getExternalId(), is("INAT_TAXON:50999"));
            assertThat(resolvedTaxon.getName(), is("Ficus"));
            assertThat(resolvedTaxon.getPath(), is("Plantae | Tracheophyta | Magnoliopsida | Rosales | Moraceae | Ficus"));
            tx.commit();
        }
        try (Transaction tx = getGraphDb().beginTx()) {
            Taxon resolvedTaxon = getTaxonIndexFactory().create(tx).findTaxonById("INAT_TAXON:208863");
            assertThat(resolvedTaxon, is(notNullValue()));
            assertThat(resolvedTaxon.getExternalId(), is("INAT_TAXON:208863"));
            assertThat(resolvedTaxon.getName(), is("Ficus"));
            assertThat(resolvedTaxon.getPath(), is("Animalia | Mollusca | Gastropoda | Littorinimorpha | Ficidae | Ficus"));
            tx.commit();
        }

        try (Transaction tx = getGraphDb().beginTx()) {
            Taxon resolvedTaxon2 = getTaxonIndexFactory().create(tx).findTaxonByName("Ficus");
            assertThat(resolvedTaxon2, is(notNullValue()));
            assertThat(resolvedTaxon2.getExternalId(), is("INAT_TAXON:50999"));
            tx.commit();
        }

        try (Transaction tx = getGraphDb().beginTx()) {
            TaxonImpl taxon4 = new TaxonImpl("Ficus");
            taxon4.setPath("Animalia | Mollusca | Gastropoda | Littorinimorpha | Ficidae | Ficus");
            taxon4.setPathNames("kingdom | phylum | class | order | family | genus");
            Taxon taxonMollusk = getTaxonIndexFactory().create(tx).getOrCreateTaxon(taxon4);
            assertThat(taxonMollusk.getPath(), is("Animalia | Mollusca | Gastropoda | Littorinimorpha | Ficidae | Ficus"));
            tx.commit();
        }
    }


}
