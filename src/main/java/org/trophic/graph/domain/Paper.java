package org.trophic.graph.domain;

import static org.springframework.data.neo4j.core.Direction.INCOMING;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

@NodeEntity
public class Paper {

    @Indexed
    String id;

    @Indexed(fulltext = true, indexName = "search")
    String title;

    @Indexed(fulltext = true, indexName = "search")
    String description;

    @RelatedTo(elementClass = Researcher.class, type = "CONTRIBUTED_TO", direction = INCOMING)
    Set<Researcher> researchers;

    private String language;
    private Date publicationDate;
    private String journal;
    private Date lastModified;

    public Paper() {
    }

    public Paper(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public Set<Researcher> getResearchers() {
        return researchers;
    }

    public int getYear() {
        if (publicationDate==null) return 0;
        Calendar cal = Calendar.getInstance();
        cal.setTime(publicationDate);
        return cal.get(Calendar.YEAR);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) [%s]", title, publicationDate, id);
    }

    public String getDescription() {
        return description;
    }

    public void setTitle(String title) {
        this.title=title;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getLanguage() {
        return language;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public Date getLastModified() {
        return lastModified;
    }

	public void setJournal(String journal) {
		this.journal = journal;
	}

	public String getJournal() {
		return journal;
	}
}

