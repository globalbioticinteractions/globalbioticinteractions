package org.trophic.graph.domain;

import java.util.Set;

public class Study extends NodeBacked<Study>{

	private String id;

	private String title;
	
    private Set<Specimen> specimens;

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

}
