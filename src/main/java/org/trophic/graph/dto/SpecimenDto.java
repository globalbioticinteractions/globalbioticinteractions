package org.trophic.graph.dto;

import java.util.ArrayList;
import java.util.List;

public class SpecimenDto {

    private long id;
    private Double lengthInMm;
    private String species;
    private Double longitude;
    private Double latitude;
    private Double altitude;
    private String season;
    private String thumbnail;
    private int count = 0;

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(" species: ");
        sb.append(species);
        sb.append(" lng: ");
        sb.append(longitude);
        sb.append(" lat: ");
        sb.append(latitude);
        return sb.toString();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void increaseCount(){
        this.count++;
    }

    public Double getLengthInMm() {
        return lengthInMm;
    }

    public void setLengthInMm(Double lengthInMm) {
        this.lengthInMm = lengthInMm;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public Double getLongLat(){
        Double longLat = longitude + latitude;
        return longLat;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

}