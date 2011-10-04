package org.trophic.graph.domain;

import static org.springframework.data.neo4j.core.Direction.INCOMING;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * @author mh
 * @since 04.03.11
 */
@NodeEntity
public class Study {

	@Indexed
	private String id;

	@Indexed
	private String title;
	
    @RelatedTo(elementClass = Specimen.class, type = "COLLECTED")
	private Set<Specimen> specimens;

    @RelatedTo(elementClass = Paper.class, type = "PUBLISHED_IN")
	private Set<Paper> papers;

    public Study() {
	}

	public Study(String id, String title) {
		this.setId(id);
		this.setTitle(title);
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setSpecimens(Set<Specimen> specimens) {
		this.specimens = specimens;
	}

	public Set<Specimen> getSpecimens() {
		return specimens;
	}

	public void setPapers(Set<Paper> papers) {
		this.papers = papers;
	}

	public Set<Paper> getPapers() {
		return papers;
	}

	public void publishedIn(Paper paper) {
		getPapers().add(paper);
	}

}
