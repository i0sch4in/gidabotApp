package com.example.map;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * This shows how to add a ground overlay to a map.
 */
public class GroundOverlayDemoActivity extends AppCompatActivity
        implements OnSeekBarChangeListener, OnMapReadyCallback,
        GoogleMap.OnGroundOverlayClickListener {

    private static final int TRANSPARENCY_MAX = 100;

    private static final LatLng IFFAKUL = new LatLng(43.30737473783579, -2.0108205439129803);

//    private static final LatLng NEAR_NEWARK =
//            new LatLng(IFFAKUL.latitude - 0.001, IFFAKUL.longitude - 0.025);

    private final List<BitmapDescriptor> images = new ArrayList<BitmapDescriptor>();

    private GroundOverlay groundOverlay;

    private GroundOverlay groundOverlayRotated;

    private SeekBar transparencyBar;

    private int currentEntry = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ground_overlay_demo);

        transparencyBar = findViewById(R.id.transparencySeekBar);
        transparencyBar.setMax(TRANSPARENCY_MAX);
        transparencyBar.setProgress(0);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        // Register a listener to respond to clicks on GroundOverlays.
        map.setOnGroundOverlayClickListener(this);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(IFFAKUL, 21));

        images.clear();
        images.add(BitmapDescriptorFactory.fromResource(R.drawable.floor0));
//        images.add(BitmapDescriptorFactory.fromResource(R.drawable.newark_nj_1922));
//        images.add(BitmapDescriptorFactory.fromResource(R.drawable.newark_prudential_sunny));

        // Add a small, rotated overlay that is clickable by default
        // (set by the initial state of the checkbox.)
//        groundOverlayRotated = map.addGroundOverlay(new GroundOverlayOptions()
//                .image(images.get(1)).anchor(0, 1)
//                .position(NEAR_NEWARK, 4300f, 3025f)
//                .bearing(30)
//                .clickable(((CheckBox) findViewById(R.id.toggleClickability)).isChecked()));

        // Add a large overlay at Newark on top of the smaller overlay.
        groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
                .image(images.get(currentEntry)).anchor(0, 1)
                .position(IFFAKUL, 8600f, 6500f));

        transparencyBar.setOnSeekBarChangeListener(this);

        // Override the default content description on the view, for accessibility mode.
        // Ideally this string would be localised.
        map.setContentDescription("Google Map with ground overlay.");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (groundOverlay != null) {
            groundOverlay.setTransparency((float) progress / (float) TRANSPARENCY_MAX);
        }
    }

    public void switchImage(View view) {
        currentEntry = (currentEntry + 1) % images.size();
        groundOverlay.setImage(images.get(currentEntry));
    }

    /**
     * Toggles the visibility between 100% and 50% when a {@link GroundOverlay} is clicked.
     */
    @Override
    public void onGroundOverlayClick(GroundOverlay groundOverlay) {
        // Toggle transparency value between 0.0f and 0.5f. Initial default value is 0.0f.
        groundOverlayRotated.setTransparency(0.5f - groundOverlayRotated.getTransparency());
    }

    /**
     * Toggles the clickability of the smaller, rotated overlay based on the state of the View that
     * triggered this call.
     * This callback is defined on the CheckBox in the layout for this Activity.
     */
    public void toggleClickability(View view) {
        if (groundOverlayRotated != null) {
            groundOverlayRotated.setClickable(((CheckBox) view).isChecked());
        }
    }
}