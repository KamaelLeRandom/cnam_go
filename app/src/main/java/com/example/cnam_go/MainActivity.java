package com.example.cnam_go;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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

public class MainActivity extends AppCompatActivity {
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

        ItemizedOverlayWithFocus<OverlayItem> mOverlay = getOverlayItemItemizedOverlayWithFocus();
        map.getOverlays().add(mOverlay);
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
                    IMapController mapController = map.getController();
                    mapController.setZoom(20.0);
                    mapController.setCenter(userPos);

                    // Marker sur la position
                    Marker marker = new Marker(map);
                    marker.setPosition(userPos);
                    marker.setTitle("Moi");
                    map.getOverlays().clear(); // enlever anciens markers
                    map.getOverlays().add(marker);
                    map.invalidate(); // rafraîchir la carte
                }
            }
        }, Looper.getMainLooper());
    }

    @NonNull
    private ItemizedOverlayWithFocus<OverlayItem> getOverlayItemItemizedOverlayWithFocus() {
        ArrayList<OverlayItem> items = new ArrayList<>();
        OverlayItem resto = new OverlayItem("Restaurant", "Bar", new GeoPoint(43.64950, 7.00517));
        OverlayItem home = new OverlayItem("Rallo's office", "My office", new GeoPoint(43.65020, 7.00517));
        Drawable m = home.getMarker(0);
        items.add(resto);
        items.add(home);

        ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(
                getApplicationContext(), items, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        return false;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                }
        );

        mOverlay.setFocusItemsOnTap(true);
        return mOverlay;
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
}