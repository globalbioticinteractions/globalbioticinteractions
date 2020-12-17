package org.eol.globi.process;

import org.eol.globi.data.StudyImporterException;

import java.util.Map;

public interface InteractionEmitter {
    void emit(Map<String, String> interaction) throws StudyImporterException;
}
