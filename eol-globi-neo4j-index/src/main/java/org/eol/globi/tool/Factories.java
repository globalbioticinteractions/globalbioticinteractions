package org.eol.globi.tool;

import org.eol.globi.db.GraphServiceFactory;

public interface Factories {
    GraphServiceFactory getGraphServiceFactory();

    NodeFactoryFactory getNodeFactoryFactory();
}
