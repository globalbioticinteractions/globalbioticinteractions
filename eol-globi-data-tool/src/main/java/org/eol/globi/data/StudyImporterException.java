package org.eol.globi.data;

public class StudyImporterException extends Exception {
    public StudyImporterException(String msg) {
        super(msg);
    }

    public StudyImporterException(String msg, Throwable e) {
       super(msg, e);

    }
}
