package org.eol.globi.domain;

public class LocationDto {

	private String id;
    private String name;
	private Double latitude;
	private Double longitude;
	private Double altitude;

	public LocationDto() { }
	
	public LocationDto(String id, Double longitude, Double latitude, Double altitude) {
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

	public String getName() {
		if (name == null)
			name = "name";
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}