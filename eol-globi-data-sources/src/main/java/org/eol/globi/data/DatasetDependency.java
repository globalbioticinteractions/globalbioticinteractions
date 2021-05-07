package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.globalbioticinteractions.dataset.DatasetProxy;

public class DatasetDependency extends DatasetProxy {

    public DatasetDependency(Dataset datasetProxied) {
        super(datasetProxied);
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return StringUtils.equals(key, DatasetConstant.IS_DEPENDENCY)
                ? "true"
                : super.getOrDefault(key, defaultValue);
    }
}
