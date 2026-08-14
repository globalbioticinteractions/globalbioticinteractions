package org.eol.globi.tool;

import org.eol.globi.data.ResolvingTaxonIndex;
import org.neo4j.graphdb.Transaction;

public interface TaxonIndexFactory {
    ResolvingTaxonIndex create(Transaction tx);
}
