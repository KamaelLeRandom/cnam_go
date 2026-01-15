package com.example.cnam_go;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.cnam_go.models.AuditeurMap;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private ArrayList<AuditeurMap> auditeursActifs = new ArrayList<AuditeurMap>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int MAX_AUDITEURS = 5;
    private static final long DUREE_VIE_MS = 60_000;
    private static final double DISTANCE_MAX_METRES = 50.0;
    private static final long INTERVALLE_GENERATION_MS = 15_000;
    private GeoPoint currentPlayerPosition = null;




    private MapView map;

    private FusedLocationProviderClient locationClient;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK); // Pour le render.

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        startLocationUpdates();
        startAuditeurScheduler();
    }

    private void startAuditeurScheduler() {
        Runnable generateTask = new Runnable() {
            @Override
            public void run() {
                if (auditeursActifs.size() < MAX_AUDITEURS) {
                    GeoPoint playerPos = getCurrentPlayerPosition();
                    if (playerPos != null) {
                        spawnAuditeur(playerPos);
                    }
                }
                // Relancer la tâche
                mainHandler.postDelayed(this, INTERVALLE_GENERATION_MS);
            }
        };
        mainHandler.post(generateTask);
    }

    private GeoPoint getCurrentPlayerPosition() {
        return currentPlayerPosition;
    }

    private void spawnAuditeur(GeoPoint center) {
        Random random = new Random();
        double radiusMeters = 50.0;
        double radiusInDegrees = radiusMeters / 111320.0;

        double u = random.nextDouble();
        double v = random.nextDouble();
        double w = radiusInDegrees * Math.sqrt(u);
        double t = 2 * Math.PI * v;

        double xOffset = w * Math.cos(t);
        double yOffset = w * Math.sin(t);

        double lat = center.getLatitude() + yOffset;
        double lon = center.getLongitude() + xOffset;

        GeoPoint pos = new GeoPoint(lat, lon);

        Marker marker = new Marker(map);
        marker.setPosition(pos);
        marker.setTitle("Auditeur");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        ShapeDrawable circle = new ShapeDrawable(new OvalShape());
        circle.setIntrinsicHeight(40);
        circle.setIntrinsicWidth(40);
        circle.getPaint().setColor(Color.RED);
        marker.setIcon(circle);

        map.getOverlays().add(marker);
        map.invalidate();

        AuditeurMap auditeur = new AuditeurMap(pos, marker);
        auditeursActifs.add(auditeur);

        Log.d("AUDITEUR", "Nouvel auditeur à " + pos.getLatitude() + ", " + pos.getLongitude());
    }

    private void cleanupAuditeurs() {
        if (currentPlayerPosition == null) return;

        Iterator<AuditeurMap> it = auditeursActifs.iterator();
        while (it.hasNext()) {
            AuditeurMap a = it.next();

            // Vérifier durée de vie
            if (System.currentTimeMillis() - a.creationTime > DUREE_VIE_MS) {
                removeAuditeurFromMap(a);
                it.remove();
                continue;
            }

            // Vérifier distance
            double distance = distanceInMeters(currentPlayerPosition, a.position);
            if (distance > DISTANCE_MAX_METRES) {
                removeAuditeurFromMap(a);
                it.remove();
            }
        }
    }

    private void removeAuditeur(AuditeurMap a) {
        removeAuditeurFromMap(a);
        auditeursActifs.remove(a);
    }

    private void removeAuditeurFromMap(AuditeurMap a) {
        map.getOverlays().remove(a.marker);
        map.invalidate();

        Log.d("AUDITEUR", "Auditeur supprimé");
    }

    private double distanceInMeters(GeoPoint p1, GeoPoint p2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(
                p1.getLatitude(), p1.getLongitude(),
                p2.getLatitude(), p2.getLongitude(),
                results
        );
        return results[0];
    }











    private void startLocationUpdates() {
        // Vérifier les permissions
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Créer la requête de localisation
        LocationRequest request = LocationRequest.create();
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        request.setInterval(3000); // mise à jour toutes les 5 secondes

        // Demander les mises à jour GPS
        locationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {

                    GeoPoint userPos = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                    currentPlayerPosition = userPos;

                    // Caméra suit joueur
                    IMapController mapController = map.getController();
                    mapController.setZoom(20.0);
                    mapController.setCenter(userPos);

                    // ===== Nettoyage : garder uniquement le marqueur "Moi" =====
                    Marker oldPlayerMarker = null;
                    for (Overlay o : map.getOverlays()) {
                        if (o instanceof Marker) {
                            Marker m = (Marker) o;
                            if ("Moi".equals(m.getTitle())) {
                                oldPlayerMarker = m;
                                break;
                            }
                        }
                    }
                    if (oldPlayerMarker != null) {
                        map.getOverlays().remove(oldPlayerMarker);
                    }


                    // ===== Ajouter/rafraîchir le marqueur du joueur =====
                    Marker playerMarker = new Marker(map);
                    playerMarker.setPosition(userPos);
                    playerMarker.setTitle("Moi");
                    map.getOverlays().add(playerMarker);

                    // ===== Nettoyer les auditeurs obsolètes (trop vieux ou trop loin) =====
                    cleanupAuditeurs();

                    // ❌ Supprime ces lignes si tu n'utilises plus les OverlayItem :
                    // map.getOverlays().remove(characterOverlay);
                    // map.getOverlays().add(characterOverlay);

                    map.invalidate();
                }
            }
        }, Looper.getMainLooper());
    }




    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
    }
}