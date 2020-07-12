package org.eol.globi.domain;

import java.util.Objects;

public class TermImpl implements Term {
    private String name;
    private String id;

    public TermImpl(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TermImpl term = (TermImpl) o;
        return Objects.equals(name, term.name) &&
                Objects.equals(id, term.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }
}
