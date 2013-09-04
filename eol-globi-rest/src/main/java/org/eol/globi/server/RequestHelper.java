package org.eol.globi.server;

import org.apache.commons.collections.CollectionUtils;
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

    public static List<LatLng> parseSpatialSearchParams(Map<String, String[]> parameterMap) {
        ArrayList<LatLng> latLngs = new ArrayList<LatLng>();
        addPoints(parameterMap, latLngs, POINT_PARAM);
        if (latLngs.size() == 0) {
            addPoints(parameterMap, latLngs, SQUARE_PARAMS);
        }
        return latLngs;
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
