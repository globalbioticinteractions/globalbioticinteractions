package org.trophic.graph.data;

public class StudyImporterException extends Throwable {
    public StudyImporterException(String msg) {
        super(msg);
    }

    public StudyImporterException(String msg, Throwable e) {
       super(msg, e);

    }
}
