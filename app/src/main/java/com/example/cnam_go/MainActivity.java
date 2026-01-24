package com.example.cnam_go;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private static final int MAX_AUDITEURS = 5;

    private static final long DUREE_VIE_MS = 60_000;

    private static final double DISTANCE_MAX_METRES = 50.0;

    private static final long INTERVALLE_GENERATION_MS = 15_000;

    private final ArrayList<AuditeurMap> auditeursActifs = new ArrayList<>();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private GeoPoint currentPlayerPosition = null;

    private MapView map;

    private FusedLocationProviderClient locationClient;

    private boolean firstLocationReceived = false;

    private Marker playerMarker = null;

    private View loadingContainer;

    private ImageButton fabCenter;

    private LinearLayout bottomBar;

    private Button btnProfil, btnCollection, btnShop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setVisibility(View.GONE);
        fabCenter = findViewById(R.id.fab_center);
        fabCenter.setVisibility(View.GONE);
        btnProfil = findViewById(R.id.btnProfil);
        btnCollection  = findViewById(R.id.btnCollection);
        btnShop = findViewById(R.id.btnShop);
        loadingContainer = findViewById(R.id.loadingContainer);
        map = findViewById(R.id.map);
        map.setVisibility(View.GONE);
        map.setTileSource(TileSourceFactory.MAPNIK);

        fabCenter.setOnClickListener(v -> {
            if (currentPlayerPosition != null && map.getVisibility() == View.VISIBLE) {
                IMapController controller = map.getController();
                controller.animateTo(currentPlayerPosition);
                controller.setZoom(20.0);
            } else {
                Toast.makeText(this, "Position en cours de chargement...", Toast.LENGTH_SHORT).show();
            }
        });

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        startLocationUpdates();
        startAuditeurScheduler();
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

        double maxDistance = 80;
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * maxDistance;

        double latOffset = (distance * Math.cos(angle)) / 111320.0;
        double lonOffset = (distance * Math.sin(angle)) /
                (111320.0 * Math.cos(Math.toRadians(center.getLatitude())));

        double lat = center.getLatitude() + latOffset;
        double lon = center.getLongitude() + lonOffset;

        GeoPoint pos = new GeoPoint(lat, lon);

        Marker marker = new Marker(map);
        marker.setPosition(pos);

        Drawable drawable = getResources().getDrawable(R.drawable.alexis_1_shadow, null);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 160, 160, true);
        Drawable auditeurIcon = new BitmapDrawable(getResources(), scaled);

        marker.setIcon(auditeurIcon);
        marker.setAnchor(0.5f, 0.5f);

        // 👇 AJOUT DU LISTENER ICI
        marker.setOnMarkerClickListener((m, mapView) -> {
            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            // Exemple si tu veux envoyer le type :
            // intent.putExtra("auditeur_type", "alexis_1");
            startActivity(intent);
            return true; // consomme le clic
        });

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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Créer la requête de localisation
        LocationRequest request = LocationRequest.create();
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        request.setInterval(3000);

        // Demander les mises à jour GPS
        locationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location loc = result.getLastLocation();

                if (loc == null || firstLocationReceived) return;

                // On traite uniquement la première localisation valide
                firstLocationReceived = true;

                GeoPoint userPos = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                currentPlayerPosition = userPos;

                // Configurer la carte maintenant qu’on a la position
                runOnUiThread(() -> {
                    // Afficher la carte
                    map.setVisibility(View.VISIBLE);
                    // Masquer le chargement
                    loadingContainer.setVisibility(View.GONE);

                    // Afficher la barre du bas et le bouton recentrer
                    bottomBar.setVisibility(View.VISIBLE);
                    fabCenter.setVisibility(View.VISIBLE);

                    // Configurer caméra et marker
                    IMapController mapController = map.getController();
                    mapController.setZoom(20.0);
                    mapController.setCenter(userPos);

                    // Ajouter le marker joueur
                    playerMarker = new Marker(map);
                    playerMarker.setPosition(userPos);
                    playerMarker.setTitle("Moi");
                    map.getOverlays().add(playerMarker);

                    map.invalidate();
                });

                cleanupAuditeurs();
            }
        }, Looper.getMainLooper());
    }
}