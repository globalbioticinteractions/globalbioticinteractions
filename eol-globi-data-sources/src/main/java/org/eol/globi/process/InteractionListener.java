package org.eol.globi.process;

import org.eol.globi.data.StudyImporterException;

import java.util.Map;

public interface InteractionListener {
    void on(Map<String, String> interaction) throws StudyImporterException;
}
