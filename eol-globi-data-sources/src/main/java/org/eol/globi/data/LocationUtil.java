package org.eol.globi.data;

import org.eol.globi.geo.LatLng;
import org.eol.globi.util.InvalidLocationException;

public class LocationUtil {
    public static Double parseDegrees(String latString) {
        String[] split = latString.split("º");
        if (split.length == 1) {
            split = latString.split("�");
        }
        if (split.length == 1) {
            split = latString.split("°");
        }
        Double degrees = 0.0;
        Double minutes = 0.0;
        Double seconds = 0.0;
        if (split.length > 1) {
            degrees = Double.parseDouble(split[0]);
            split = split[1].split("'");
            if (split.length > 1) {
                minutes = Double.parseDouble(split[0]);
                split = split[1].split("''");
                if (split.length > 1) {
                    seconds = Double.parseDouble(split[0]);
                }
            }


        }
        Double lat = degrees + minutes / 60.0 + seconds / 3600.0;
        lat = lat * (latString.endsWith("N") || latString.endsWith("E") ? 1.0 : -1.0);
        return lat;
    }

    public static boolean isValidLongitude(Double longitude) {
        return longitude <= 180.0 && longitude >= -180.0;
    }

    public static boolean isValidLatitude(Double latitude) {
        return latitude <= 90.0 && latitude >= -90.0;
    }

    public static LatLng parseLatLng(String latitude, String longitude) throws InvalidLocationException {
        LatLng point = null;
        try {
            double lat = Double.parseDouble(latitude);
            double lng = Double.parseDouble(longitude);
            if (!isValidLatitude(lat)) {
                throw new InvalidLocationException("range of latitude [" + latitude + "] not valid");
            } else if (!isValidLongitude(lng)) {
                throw new InvalidLocationException("range of longitude [" + longitude + "] not valid");
            }
            if (isValidLatitude(lat) && isValidLongitude(lng)) {
                point = new LatLng(lat, lng);
            }
        } catch (NumberFormatException ex) {
            throw new InvalidLocationException("invalid (latitude, longitude) = (" + latitude + "," + longitude + ")", ex);
        }
        return point;
    }
}
