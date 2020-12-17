package org.eol.globi.data;

import java.util.Map;

public interface InteractionListener {
    void on(Map<String, String> interaction) throws StudyImporterException;
}
