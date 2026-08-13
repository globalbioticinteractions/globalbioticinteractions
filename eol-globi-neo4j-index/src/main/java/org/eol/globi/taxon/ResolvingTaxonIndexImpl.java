package org.eol.globi.taxon;

import org.eol.globi.service.PropertyEnricher;
import org.neo4j.graphdb.Transaction;

public class ResolvingTaxonIndexImpl extends ResolvingTaxonIndexNoTx {


    public ResolvingTaxonIndexImpl(PropertyEnricher enricher, Transaction tx) {
        super(enricher, tx);
    }

}
