package org.trophic.graph.domain;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class Family extends Taxon {
    public Family(String name) {
        this.name = name;
    }

    public Family() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("%s", name);
    }

}
