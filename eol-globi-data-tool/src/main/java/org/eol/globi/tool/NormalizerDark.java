package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterForFWDP;
import org.eol.globi.data.StudyImporterForFishbase;

import java.util.ArrayList;
import java.util.Collection;

public class NormalizerDark extends Normalizer {

    @Override
    protected Collection<Class> getImporters() {
        return new ArrayList<Class>() {{
            add(StudyImporterForFWDP.class);
            add(StudyImporterForFishbase.class);
        }};
    }
}