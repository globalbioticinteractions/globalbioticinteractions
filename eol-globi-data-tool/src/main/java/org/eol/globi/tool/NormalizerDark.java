package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterFactory;
import org.eol.globi.data.StudyImporterForFWDP;
import org.eol.globi.data.StudyImporterForSAID;

import java.util.ArrayList;
import java.util.Collection;

public class NormalizerDark {

    public static void main(final String[] commandLineArguments) throws StudyImporterException {
        new Normalizer().normalize(new ArrayList<Class>() {{
            add(StudyImporterForFWDP.class);
        }});
    }

}