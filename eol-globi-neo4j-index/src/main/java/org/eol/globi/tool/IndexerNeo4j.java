package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;

public interface IndexerNeo4j {

    void index(GraphServiceFactory graphServiceFactory) throws StudyImporterException;

}
