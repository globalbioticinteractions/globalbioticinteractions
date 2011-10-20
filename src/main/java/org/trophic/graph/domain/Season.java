package org.trophic.graph.domain;

public class Season extends NodeBacked<Season>{

    private String id;

    private String title;

    public Season() {
    }

    public Season(String id, String title) {
        this.id = id;
        this.title = title;
    }

     @Override
    public String toString() {
        return String.format("%s [%s]", title, id);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
