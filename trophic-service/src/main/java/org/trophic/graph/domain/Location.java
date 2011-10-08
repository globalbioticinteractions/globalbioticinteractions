package org.trophic.graph.domain;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class Location {

	@Indexed
	private String id;

    @Indexed
    private String name;

    @Indexed
	private Double latitude;

    @Indexed
	private Double longitude;

    @Indexed
	private Double altitude;

	public Location() {
	}

	public Location(String id, Double longitude, Double latitude, Double altitude) {
		this.id = id;
		this.setLongitude(longitude);
		this.setLatitude(latitude);
		this.setAltitude(altitude);
	}

	public void setAltitude(Double altitude) {
		this.altitude = altitude;
	}

	public Double getAltitude() {
		return altitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
