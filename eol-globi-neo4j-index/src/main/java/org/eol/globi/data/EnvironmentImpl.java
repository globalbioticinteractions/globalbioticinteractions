package org.eol.globi.data;

import org.eol.globi.domain.Environment;

public class EnvironmentImpl implements Environment {
    private String externalId;
    private String name;

    public EnvironmentImpl(String externalId) {
        this.externalId = externalId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }
}
