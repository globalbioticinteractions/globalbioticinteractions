package org.eol.globi.data;

public class StudyImporterException extends Exception {
    public StudyImporterException(Throwable e) {
        super(e);
    }

    public StudyImporterException(String msg) {
        super(msg);
    }

    public StudyImporterException(String msg, Throwable e) {
       super(msg, e);

    }
}
