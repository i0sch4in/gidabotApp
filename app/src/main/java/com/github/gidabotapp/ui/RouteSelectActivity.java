package com.github.gidabotapp.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import com.github.gidabotapp.R;
import com.github.gidabotapp.domain.AppNavPhase;
import com.github.gidabotapp.domain.Floor;
import com.github.gidabotapp.domain.MapPosition;
import com.github.gidabotapp.domain.Room;
import com.github.gidabotapp.domain.Way;
import com.github.gidabotapp.viewmodel.MapViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RouteSelectActivity extends AppCompatActivity implements OnMapReadyCallback {

    private HashMap<Floor, Marker> robotMarkers;
    private Marker markerOrigin, markerDest;

    private MapViewModel viewModel;

    private GoogleMap map;

    private Button publishBtn, cancelBtn;
    private AutoCompleteTextView act_origin, act_destination, act_floor;

    private final int MAX_MAP_ZOOM = 3;
    private HashMap<Floor, TileOverlay> tileOverlays;
    public RouteSelectActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_select);

        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())).get(MapViewModel.class);
        viewModel.getToastObserver().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                View bottom = findViewById(R.id.mapFragment);
                final Snackbar snackbar = Snackbar.make(bottom,message,Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction(R.string.accept_btn, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        snackbar.dismiss();
                    }
                });
                snackbar.show();
            }
        });
        viewModel.getAlertObserver().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer stringResId) {
                if(stringResId == R.string.empty){
                    return;
                }

                if(stringResId == R.string.origin_reached_msg){
                    showNextGoalAlert();
                }
                else if(stringResId == R.string.destination_reached_msg){
                    showRouteEndAlert();
                }
                else if(stringResId == R.string.WAIT_ROBOT){
                    if(viewModel.emptyRoute()){
                        return;
                    }
                    int floorCode = viewModel.getGoalFloor().getFloorCode();
                    int pendingRequests = viewModel.getGoalPendingReq();
                    String message = String.format(getString(stringResId), floorCode, pendingRequests);
                    showAlert(message);
                }
                else {
                    String message = getString(stringResId);
                    showAlert(message);
                }
            }
        });

        for(final Floor floor: Floor.values()){
            viewModel.getPositionObserver(floor).observe(this, new Observer<MapPosition>() {
                @Override
                public void onChanged(MapPosition position) {
                    updateRobotMarker(floor, position);
                }
            });
        }

        viewModel.getAllRoomsLD().observe(this, new Observer<List<Room>>() {
            @Override
            public void onChanged(List<Room> rooms) {
                ArrayAdapter<Room> adapterAllRooms = new ArrayAdapter<>(getApplicationContext(), R.layout.support_simple_spinner_dropdown_item, rooms);
                act_destination.setAdapter(adapterAllRooms);
            }
        });
        viewModel.getAppNavPhase().observe(this, new Observer<AppNavPhase>() {
            @Override
            public void onChanged(AppNavPhase phase) {
                updateButtonsLock(phase);
            }
        });

        publishBtn = findViewById(R.id.publishBtn);
        publishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewModel.isLiftNeeded()){
                    showLiftAlert();
                    return;
                }
                viewModel.publishOrigin(null);
            }
        });

        cancelBtn = findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.publishCancel();
            }
        });

        FloatingActionButton locateRobotBtn = findViewById(R.id.locateRobotBtn);
        locateRobotBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCameraOnRobot();
            }
        });

        act_origin = findViewById(R.id.act_non);
        act_origin.setThreshold(1);
        act_origin.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Hide Keyboard
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);

                Room origin = (Room) parent.getItemAtPosition(position);
                viewModel.selectOrigin(origin);
                updateOriginMarker(origin);
            }
        });

        act_destination = findViewById(R.id.act_nora);
        act_destination.setThreshold(1);
        act_destination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Hide Keyboard
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);

                Room destination = (Room) parent.getItemAtPosition(position);
                viewModel.selectDestination(destination);
                updateDestMarker(destination);
            }
        });

        act_floor = findViewById(R.id.act_floor);
        final ArrayAdapter<String> floorAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, Floor.getFloorCodeList());
        act_floor.setAdapter(floorAdapter);
        act_floor.setText(floorAdapter.getItem(0),false);
        act_floor.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Floor currentFloor = Floor.values()[position];
                viewModel.selectFloor(currentFloor);
                if(markerOrigin != null){
                    markerOrigin.setVisible(false);
                }
                if(markerDest != null){
                    boolean visible = viewModel.destOnCurrentFloor();
                    markerDest.setVisible(visible);
                }
            }
        });

        viewModel.getCurrentFloorRooms().observe(this, new Observer<List<Room>>() {
            @Override
            public void onChanged(List<Room> rooms) {
                ArrayAdapter<Room> adapterFloorRooms = new ArrayAdapter<>(getApplicationContext(), R.layout.support_simple_spinner_dropdown_item, rooms);
                act_origin.setAdapter(adapterFloorRooms);
                act_origin.setText("");
                viewModel.selectOrigin(null);
            }
        });

        viewModel.getCurrentFloor().observe(this, new Observer<Floor>() {
            @Override
            public void onChanged(Floor floor) {
                showTiles(floor);
                showRobotMarker(floor);
            }
        });

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    private void updateOriginMarker(Room origin) {
        Floor currentFloor = viewModel.getCurrentFloor().getValue();
        assert currentFloor != null;
        LatLng position = origin.getPosition().toLatLng(currentFloor);
        if(markerOrigin == null){
            BitmapDescriptor icon = bitmapDescriptorFromVector(this, R.drawable.ic_origin);
            markerOrigin = map.addMarker(new MarkerOptions()
                .icon(icon)
                .position(position)
            );
        }
        else{
            markerOrigin.setPosition(position);
            markerOrigin.setVisible(true);
        }
    }

    private void updateDestMarker(Room destination) {
        Floor destFloor = destination.getFloor();
        LatLng position = destination.getPosition().toLatLng(destFloor);
        if(markerDest == null){
            BitmapDescriptor icon = bitmapDescriptorFromVector(this, R.drawable.ic_destination);
            markerDest = map.addMarker(new MarkerOptions()
                    .icon(icon)
                    .position(position)
                    .visible(true)
            );
        }
        else {
            markerDest.setPosition(position);
        }
        boolean visible = viewModel.destOnCurrentFloor();
        markerDest.setVisible(visible);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;

        map.setMapType(GoogleMap.MAP_TYPE_NONE);
        map.setMaxZoomPreference(MAX_MAP_ZOOM);

        // Set camera bounds: Horizontal scroll ends with map
        LatLng SOUTHWEST_BOUND = new LatLng(-65,-110);
        LatLng NORTHEAST_BOUND = new LatLng(+65,+110);
        LatLngBounds bounds = new LatLngBounds(SOUTHWEST_BOUND,NORTHEAST_BOUND);
        map.setLatLngBoundsForCameraTarget(bounds);

        /*
         * Set custom onClickListener for all markers, so that it only shows Marker's information (if it has any),
         * and hides Google's default buttons, which we don't want.
         */
        final Marker[] lastOpened = {null};
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                // Check if there is an open info window
                if (lastOpened[0] != null) {
                    // Close the info window
                    lastOpened[0].hideInfoWindow();
                    // Is the marker the same marker that was already open
                    if (lastOpened[0].equals(marker)) {
                        // Nullify the lastOpened object
                        lastOpened[0] = null;
                        // Return so that the info window isn't opened again
                        return true;
                    }
                }
                // Open the info window for the marker
                marker.showInfoWindow();
                // Re-assign the last opened such that we can close it later
                lastOpened[0] = marker;
                // Event was handled by our code, so do not launch default behaviour.
                return true;
            }
        });

        showRobotMarker(Floor.getStartingFloor());
        showTiles(Floor.getStartingFloor());
    }

    private void updateButtonsLock(AppNavPhase phase) {
//         if (phase == AppNavPhase.WAITING_USER_INPUT){
//            act_origin.setEnabled(true);
//            act_destination.setEnabled(true);
//            act_floor.setEnabled(true);
//            publishBtn.setEnabled(true);
//            cancelBtn.setEnabled(false);
//        }
//        else {
//            act_origin.setEnabled(false);
//            act_destination.setEnabled(false);
//            act_floor.setEnabled(false);
//            publishBtn.setEnabled(false);
//            cancelBtn.setEnabled(true);
//        }
    }

    private void showAlert(String msg) {
         MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.alert_title)) // Informazioa
                .setMessage(msg)
                .setPositiveButton(R.string.accept_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { // Ados
                        dialog.dismiss();
                    }
                });
        dialog.show();
    }

    private void showNextGoalAlert(){
        String message = String.format(getString(R.string.origin_reached_msg),viewModel.getDestination());
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.alert_title) // Informazioa
                .setMessage(message)
                .setPositiveButton(R.string.accept_btn, new DialogInterface.OnClickListener() { // Ados
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(viewModel.isLiftNeeded()){
                            showLiftAlert();
                            return;
                        }
                        viewModel.publishDestination(null); // publish destination
                    }
                })
                .setNegativeButton(R.string.cancel_btn, new DialogInterface.OnClickListener() { // Ezeztatu
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        viewModel.resetAppNavPhase();
                        dialog.dismiss();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        viewModel.resetAppNavPhase();
                    }
                });
        dialog.show();
    }

    private void showRouteEndAlert() {
        String message = getString(R.string.destination_reached_msg); // Iritsi zara zure helburura
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.alert_title) // Informazioa
                .setMessage(message)
                .setPositiveButton(R.string.accept_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        viewModel.resetAppNavPhase();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        viewModel.resetAppNavPhase();
                    }
                });
        dialog.show();
    }

    private void showLiftAlert(){
        String lift = getString(Way.LIFT.getResourceId());
        String stairs = getString(Way.STAIRS.getResourceId());
        String[] values = new String[]{lift, stairs};
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lift_stairs_question)
                .setItems(values, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Way chosenWay = Way.values()[which];
                        // publish goal
                        if(viewModel.getAppNavPhase().getValue() == AppNavPhase.WAITING_USER_INPUT){
                            viewModel.publishOrigin(chosenWay);
                        }
                        // publish destination
                        if(viewModel.getAppNavPhase().getValue() == AppNavPhase.REACHING_ORIGIN){
                            viewModel.publishDestination(chosenWay);
                        }
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        viewModel.resetAppNavPhase();
                    }
                })
                ;
        dialog.show();
    }

    private void setCameraOnRobot() {
        Floor currentFloor = viewModel.getCurrentFloor().getValue();
        map.animateCamera(CameraUpdateFactory.newLatLng(robotMarkers.get(currentFloor).getPosition()));
    }

    private void showTiles(Floor currentFloor) {
        if (map == null){
            return;
        }
        if(tileOverlays == null) {
            initializeTileOverlays();
        }

        for(Floor floor: tileOverlays.keySet()){
            if(floor == currentFloor){
                tileOverlays.get(floor).setVisible(true);
                continue;
            }
            tileOverlays.get(floor).setVisible(false);
        }
    }


    public void initializeTileOverlays(){
        tileOverlays = new HashMap<>();
        for (Floor floor: Floor.values()){
            TileProvider provider = getFloorTileProvider(floor);
            TileOverlay overlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
            tileOverlays.put(floor,overlay);
        }
    }

    private void showRobotMarker(Floor currentFloor) {
        if(map == null){
            return;
        }
        if(robotMarkers == null){
            initializeRobotMarkers();
        }

        for(Floor floor : robotMarkers.keySet()){
            if(floor == currentFloor){
                robotMarkers.get(floor).setVisible(true);
                robotMarkers.get(floor).showInfoWindow();
                continue;
            }
            robotMarkers.get(floor).setVisible(false);
        }
    }

    private void updateRobotMarker(Floor currentFloor, MapPosition position) {
        if(map == null){
            return;
        }
        if(robotMarkers == null){
            initializeRobotMarkers();
        }
        LatLng currentLatLng = position.toLatLng(currentFloor);
        robotMarkers.get(currentFloor).setPosition(currentLatLng);
    }

    private void initializeRobotMarkers() {
        robotMarkers = new HashMap<Floor, Marker>(){{
            for(Floor floor: Floor.values()){
                Marker marker = map.addMarker(new MarkerOptions()
                    .title(floor.getRobotNameLong())
                    .icon(BitmapDescriptorFactory.fromResource(floor.getRobotIconRes()))
                    .position(floor.getStartLatLng())
                    .zIndex(1.0f)
                );
                put(floor,marker);
            }
        }};
    }

    private boolean tileNotAvailable(int zoom) {
        final int MIN_MAP_ZOOM = 0;

        return (zoom < MIN_MAP_ZOOM || zoom > MAX_MAP_ZOOM);
    }

    private TileProvider getFloorTileProvider(final Floor currentFloor){
        TileProvider floorTileProvider;
        floorTileProvider = new TileProvider() {
            final String FLOOR_MAP_URL_FORMAT =
                    "map_tiles/floor_%d/%d/tile_%d_%d.png";
            final int TILE_SIZE_DP = 256;

            @Override
            public Tile getTile(int x, int y, int zoom) {
                if (tileNotAvailable(zoom)) {
                    return null;
                }
                String s = String.format(Locale.US, FLOOR_MAP_URL_FORMAT, currentFloor.getFloorCode(), zoom, x, y);
                try {
                    InputStream is = getApplication().getAssets().open(s);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    return new Tile(TILE_SIZE_DP, TILE_SIZE_DP, stream.toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        return floorTileProvider;
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        assert vectorDrawable != null;
        final int WIDTH = vectorDrawable.getIntrinsicWidth() * 2;
        final int HEIGHT = vectorDrawable.getIntrinsicHeight() * 2;
        vectorDrawable.setBounds(0, 0, WIDTH, HEIGHT);
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.getCurrentFloor().removeObservers(this);
        viewModel.getToastObserver().removeObservers(this);
        viewModel.getAlertObserver().removeObservers(this);
        for(Floor floor: Floor.values()){
            viewModel.getPositionObserver(floor).removeObservers(this);
        }
        viewModel.getCurrentFloorRooms().removeObservers(this);
        viewModel.getMultiNavPhaseLD().removeObservers(this);
        viewModel.shutdownNode();
    }

}