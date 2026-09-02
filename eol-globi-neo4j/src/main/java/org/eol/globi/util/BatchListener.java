package org.eol.globi.util;

import org.neo4j.graphdb.Transaction;

public interface BatchListener {
    void onStart();
    void onFinish();
    Transaction getTx();

}
