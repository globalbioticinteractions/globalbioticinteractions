package org.trophic.graph.domain;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.annotation.Indexed;

import java.util.Date;
import java.util.Set;

/**
 * @author mh
 * @since 12.03.11
 */
@NodeEntity
public class Researcher {
    @Indexed
    String id;
    
    @Indexed(fulltext = true, indexName = "search")
    String name;

    public Researcher(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Researcher() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", name, id);
    }

    @RelatedTo(elementClass = Paper.class, type = "CONTRIBUTED_TO")
    private Set<Paper> papers;

    public Set<Paper> getPapers() {
        return papers;
    }

    public void contributedTo(Paper paper) {
        this.papers.add(paper);
    }


}
