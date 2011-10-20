package org.trophic.graph.domain;

public abstract class Taxon<T> extends NodeBacked<T> {
    String id;

    String name;


    public abstract String getName();

    public abstract void setName(String name);
}
