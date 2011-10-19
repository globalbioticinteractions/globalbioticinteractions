package org.trophic.graph.domain;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public abstract class Taxon {
    @Indexed
    String id;

    @Indexed
    String name;


    public abstract String getName();

    public abstract void setName(String name);
}
