package com.example.cnam_go.models;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

public class AuditeurMap {
    public GeoPoint position;

    public Marker marker;

    public long creationTime;

    public AuditeurMap(GeoPoint pos, Marker mark) {
        this.position = pos;
        this.marker = mark;
        this.creationTime = System.currentTimeMillis();
    }
}
