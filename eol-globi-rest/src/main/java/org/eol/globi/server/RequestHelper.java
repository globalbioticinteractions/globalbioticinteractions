package org.eol.globi.server;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import uk.me.jstott.jcoord.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RequestHelper {

    private static final List<String> SQUARE_PARAMS = new ArrayList<String>() {
        {
            add("nw_lat");
            add("nw_lng");
            add("se_lat");
            add("se_lng");
        }
    };

    private static final List<String> POINT_PARAM = new ArrayList<String>() {
        {
            add("lat");
            add("lng");
        }
    };
    public static final String BOUNDING_BOX_PARAMETER_NAME = "bbox";
    public static final String GEOMETRY_PARAMETER = "g";

    public static List<LatLng> parseSpatialSearchParams(Map<String, String[]> parameterMap) {
        ArrayList<LatLng> latLngs = new ArrayList<LatLng>();

        addBoundingBox(parameterMap, latLngs);
        if (hasNoSpatialParams(latLngs)) {
            addPointGeometry(parameterMap, latLngs);
        }

        if (hasNoSpatialParams(latLngs)) {
            addPoints(parameterMap, latLngs, POINT_PARAM);
        }

        if (hasNoSpatialParams(latLngs)) {
            addPoints(parameterMap, latLngs, SQUARE_PARAMS);
        }
        return latLngs;
    }

    private static void addPointGeometry(Map<String, String[]> parameterMap, ArrayList<LatLng> latLngs) {
        if (parameterMap.containsKey(GEOMETRY_PARAMETER)) {
            String[] geometries = parameterMap.get(GEOMETRY_PARAMETER);
            if (geometries.length > 0) {
                String geometry = geometries[0];
                if (geometry.startsWith("POINT")) {
                    geometry = geometry.replace("POINT", "");
                    geometry = geometry.replace("(", "");
                    geometry = geometry.replace(")", "");
                    String[] split = StringUtils.split(geometry);
                    if (split.length == 2) {
                        try {
                            latLngs.add(new LatLng(Double.parseDouble(split[0]), Double.parseDouble(split[1])));
                        } catch (NumberFormatException ex) {
                            throw new NumberFormatException("malformed geometry parameter found, expected something like [...&g=POINT(12.2 23.2)...], but got [...&g=" + geometry + "...]");
                        }
                    }

                }
            }
        }
    }

    private static boolean hasNoSpatialParams(ArrayList<LatLng> latLngs) {
        return latLngs.size() == 0;
    }

    private static void addBoundingBox(Map<String, String[]> parameterMap, ArrayList<LatLng> latLngs) {
        if (parameterMap.containsKey(BOUNDING_BOX_PARAMETER_NAME)) {
            String[] bboxes = parameterMap.get(BOUNDING_BOX_PARAMETER_NAME);
            if (bboxes.length > 0) {
                String points = bboxes[0];
                String[] split = StringUtils.split(points, ",");
                if (split.length == 4) {
                    try {
                        latLngs.add(new LatLng(Double.parseDouble(split[3]), Double.parseDouble(split[0])));
                        latLngs.add(new LatLng(Double.parseDouble(split[1]), Double.parseDouble(split[2])));
                    } catch (NumberFormatException ex) {
                        throw new NumberFormatException("failed to parse bbox query: [" + points + "]");
                    }
                }
            }
        }
    }

    private static void addPoints(Map<String, String[]> parameterMap, ArrayList<LatLng> latLngs, List<String> pointParam) {
        if (CollectionUtils.subtract(pointParam, parameterMap.keySet()).isEmpty()) {
            for (int i = 0; i < pointParam.size(); i += 2) {
                // account for http://stackoverflow.com/questions/1928675/servletrequest-getparametermap-returns-mapstring-string-and-servletreques
                String[] lats = parameterMap.get(pointParam.get(i));
                String[] lngs = parameterMap.get(pointParam.get(i + 1));
                if (lats.length > 0 && lngs.length > 0) {
                    Double lat = Double.parseDouble(lats[0]);
                    Double lng = Double.parseDouble(lngs[0]);
                    latLngs.add(new LatLng(lat, lng));
                }
            }
        }
    }

    public static String buildCypherSpatialWhereClause(Map<String, String[]> paramMap) {
        return buildCypherSpatialWhereClause(RequestHelper.parseSpatialSearchParams(paramMap));
    }

    public static String buildCypherSpatialWhereClause(List<LatLng> points) {
        StringBuilder builder = new StringBuilder();
        builder.append("WHERE loc is not null ");
        if (points.size() == 1) {
            builder.append("AND loc.latitude = ");
            builder.append(points.get(0).getLat());
            builder.append(" AND loc.longitude = ");
            builder.append(points.get(0).getLng());
        } else if (points.size() == 2) {
            builder.append("AND loc.latitude < ");
            builder.append(points.get(0).getLat());
            builder.append(" AND loc.longitude > ");
            builder.append(points.get(0).getLng());
            builder.append(" AND loc.latitude > ");
            builder.append(points.get(1).getLat());
            builder.append(" AND loc.longitude < ");
            builder.append(points.get(1).getLng());
        }
        return builder.append(" ").toString();
    }
}
