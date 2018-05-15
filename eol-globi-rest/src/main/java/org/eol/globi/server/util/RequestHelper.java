package org.eol.globi.server.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.geo.LatLng;
import org.eol.globi.server.QueryType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RequestHelper {

    private static final List<String> POINT_PARAM = new ArrayList<String>() {
        {
            add("lat");
            add("lng");
        }
    };
    public static final String BOUNDING_BOX_PARAMETER_NAME = "bbox";
    public static final String GEOMETRY_PARAMETER = "g";

    public static boolean isSpatialSearch(Map<String, String[]> parameterMap) {
        List<LatLng> latLngs = parseSpatialSearchParams(parameterMap);
        return isPointOrBox(latLngs);
    }

    public static List<LatLng> parseSpatialSearchParams(Map<String, String[]> parameterMap) {
        ArrayList<LatLng> latLngs = new ArrayList<LatLng>();

        addBoundingBox(parameterMap, latLngs);
        if (hasNoSpatialParams(latLngs)) {
            addPointGeometry(parameterMap, latLngs);
        }

        if (hasNoSpatialParams(latLngs)) {
            addPoints(parameterMap, latLngs, POINT_PARAM);
        }

        return latLngs;
    }

    private static void addPointGeometry(Map<String, String[]> parameterMap, ArrayList<LatLng> latLngs) {
        if (parameterMap != null && parameterMap.containsKey(GEOMETRY_PARAMETER)) {
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
        if (parameterMap != null && parameterMap.containsKey(BOUNDING_BOX_PARAMETER_NAME)) {
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
        if (parameterMap != null && CollectionUtils.subtract(pointParam, parameterMap.keySet()).isEmpty()) {
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

    public static void appendSpatialClauses(Map<String, String[]> paramMap, StringBuilder query, QueryType queryType) {
        addSpatialClause(RequestHelper.parseSpatialSearchParams(paramMap), query, queryType);
    }

    public static void addSpatialClause(List<LatLng> points, StringBuilder query, QueryType queryType) {
        if (isPointOrBox(points)) {
            query.append(", sourceSpecimen-[:COLLECTED_AT]->loc ");
        } else if (QueryType.MULTI_TAXON_ALL.equals(queryType)
                || QueryType.SINGLE_TAXON_ALL.equals(queryType)){
            query.append(", sourceSpecimen-[?:COLLECTED_AT]->loc ");
        } else {
            query.append(" ");
        }

        if (isPointOrBox(points)) {
            query.append(" WHERE ");
        }
        addSpatialWhereClause(points, query);
    }

    public static void addSpatialWhereClause(List<LatLng> points, StringBuilder query) {
        if (points.size() == 1 || points.size() == 2) {
            query.append("has(loc.latitude) AND has(loc.longitude) AND ");
        }
        if (points.size() == 1) {
            query.append("loc.latitude = ");
            query.append(points.get(0).getLat());
            query.append(" AND loc.longitude = ");
            query.append(points.get(0).getLng());
            query.append(" ");
        } else if (points.size() == 2) {
            query.append("loc.latitude < ");
            query.append(points.get(0).getLat());
            query.append(" AND loc.longitude > ");
            query.append(points.get(0).getLng());
            query.append(" AND loc.latitude > ");
            query.append(points.get(1).getLat());
            query.append(" AND loc.longitude < ");
            query.append(points.get(1).getLng());
            query.append(" ");
        }
    }

    private static boolean isPointOrBox(List<LatLng> points) {
        return points.size() == 1 || points.size() == 2;
    }

    public static JsonNode parse(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(content);
    }
}
