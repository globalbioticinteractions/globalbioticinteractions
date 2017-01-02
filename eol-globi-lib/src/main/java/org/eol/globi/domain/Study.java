package org.eol.globi.domain;

import java.util.List;
import java.util.logging.Level;

public interface Study extends Named {
    String getTitle();

    @Deprecated
    // citation / doi's are used to convey the source
    void setContributor(String contributor);

    @Deprecated
    // use citation instead
    String getDescription();

    String getSource();

    void setSource(String source);

    void setDOI(String doi);

    void setDOIWithTx(String doi);

    String getDOI();

    void setCitation(String citation);

    void setCitationWithTx(String citation);

    String getCitation();

    void appendLogMessage(String message, Level warning);

    List<LogMessage> getLogMessages();
}
