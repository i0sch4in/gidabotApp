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

    private GoogleMap map;
    private HashMap<Floor, TileOverlay> tileOverlays;
    private HashMap<Floor, Marker> robotMarkers;
    private Marker markerOrigin, markerDest;

    private MapViewModel viewModel;

    private Button publishBtn, cancelBtn;
    private AutoCompleteTextView act_origin, act_destination, act_floor;

    public RouteSelectActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_select);

        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())).get(MapViewModel.class);
        viewModel.getToastLD().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                View bottom = findViewById(R.id.mapFragment);
                final Snackbar snackbar = Snackbar.make(bottom,message,Snackbar.LENGTH_INDEFINITE);
                snackbar.setActionTextColor(ContextCompat.getColor(getApplicationContext(), R.color.primaryTextColor));
                snackbar.setAction(R.string.accept_btn, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        snackbar.dismiss();
                    }
                });
                snackbar.show();
            }
        });
        viewModel.getAlertLD().observe(this, new Observer<Integer>() {
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
                    int floorCode = viewModel.getCurrentGoalFloor().getFloorCode();
                    int pendingRequests = viewModel.getGoalFloorPending();
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
        viewModel.getAppNavPhaseLD().observe(this, new Observer<AppNavPhase>() {
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

        viewModel.getCurrentFloorRoomsLD().observe(this, new Observer<List<Room>>() {
            @Override
            public void onChanged(List<Room> rooms) {
                ArrayAdapter<Room> adapterFloorRooms = new ArrayAdapter<>(getApplicationContext(), R.layout.support_simple_spinner_dropdown_item, rooms);
                act_origin.setAdapter(adapterFloorRooms);
                act_origin.setText("");
                viewModel.selectOrigin(null);
            }
        });

        viewModel.getCurrentFloorLD().observe(this, new Observer<Floor>() {
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

    @Override
    public void onMapReady(GoogleMap map) {
        final int MAX_MAP_ZOOM = 3;

        this.map = map;

        map.setMapType(GoogleMap.MAP_TYPE_NONE);
        map.setMaxZoomPreference(MAX_MAP_ZOOM);

        // Set camera bounds: Horizontal scroll ends with map
        LatLng SOUTHWEST_BOUND = new LatLng(-65,-110);
        LatLng NORTHEAST_BOUND = new LatLng(+65,+110);
        LatLngBounds bounds = new LatLngBounds(SOUTHWEST_BOUND,NORTHEAST_BOUND);
        map.setLatLngBoundsForCameraTarget(bounds);

        // Set custom onClickListener for all markers, so that it only shows Marker's information (if it has any),
        // and hides Google's default buttons, which we don't want.
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

        // Initialize robot marker on starting floor
        showRobotMarker(Floor.getStartingFloor());
        // Initialize tiles on starting floor
        showTiles(Floor.getStartingFloor());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Remove all (Mutable)LiveData observers
        viewModel.getCurrentFloorLD().removeObservers(this);
        viewModel.getToastLD().removeObservers(this);
        viewModel.getAlertLD().removeObservers(this);
        for(Floor floor: Floor.values()){
            viewModel.getPositionObserver(floor).removeObservers(this);
        }
        viewModel.getCurrentFloorRoomsLD().removeObservers(this);
        viewModel.getMultiNavPhaseLD().removeObservers(this);

        // Shut down robot query Node (qNode)
        viewModel.shutdownNode();
    }

    // Update origin's marker position.
    // Origin is always on current floor, so always sets it visible.
    private void updateOriginMarker(Room origin) {
        Floor currentFloor = viewModel.getCurrentFloorLD().getValue();
        assert currentFloor != null;
        LatLng position = origin.getPosition().toLatLng(currentFloor);

        // If null, instantiate it with proper icon
        if(markerOrigin == null){
            BitmapDescriptor icon = bitmapDescriptorFromVector(this, R.drawable.ic_origin);
            markerOrigin = map.addMarker(new MarkerOptions()
                .icon(icon)
                .position(position)
            );
        }
        else { // Already instantiated, so just update position and set it visible.
            markerOrigin.setPosition(position);
            markerOrigin.setVisible(true);
        }
    }

    // Updates destination's marker.
    // Destination is not always on current floor, so depending on that set it visible or not.
    private void updateDestMarker(Room destination) {
        Floor destFloor = Floor.getFromDouble(destination.getFloor());
        LatLng position = destination.getPosition().toLatLng(destFloor);

        // If null, instantiate it with proper icon
        if(markerDest == null){
            BitmapDescriptor icon = bitmapDescriptorFromVector(this, R.drawable.ic_destination);
            markerDest = map.addMarker(new MarkerOptions()
                    .icon(icon)
                    .position(position)
                    .visible(true)
            );
        }
        else { // Already instantiated, so update position
            markerDest.setPosition(position);
        }
        // if currentFloor = destinationFloor then set it visible
        boolean visible = viewModel.destOnCurrentFloor(); // true if dest.floor = currentFloor
        markerDest.setVisible(visible);
    }

    // Temporarily disabled, it generates quite a lot of bugs
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

    // Shows standard alert. One accept button that dismisses the alert
    private void showAlert(String msg) {
         MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.alert_title))
                .setMessage(msg)
                .setPositiveButton(R.string.accept_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        dialog.show();
    }

    // Shows next goal alert. Two buttons that decide whether the route continues or stops.
    private void showNextGoalAlert(){
        String message = String.format(getString(R.string.origin_reached_msg),viewModel.getDestination());
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.alert_title)
                .setMessage(message)
                // Accept button -->  publish destination
                .setPositiveButton(R.string.accept_btn, new DialogInterface.OnClickListener() { // Ados
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If it is a multilevel request, show lift alert, which will publish destination depending on the choice
                        if(viewModel.isLiftNeeded()){
                            showLiftAlert();
                            return;
                        }
                        // Otherwise it is a single floor request, so publish destination with Null Way
                        viewModel.publishDestination(null);
                    }
                })
                // Cancel button --> reset appNavPhase (phase == WAIT_USER_INPUT) and dismiss alert
                .setNegativeButton(R.string.cancel_btn, new DialogInterface.OnClickListener() { // Ezeztatu
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        viewModel.resetAppNavPhase();
                        dialog.dismiss();
                    }
                })
                // Cancel event (tap out) --> reset appNavPhase (phase == WAIT_USER_INPUT)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        viewModel.resetAppNavPhase();
                    }
                });
        dialog.show();
    }

    // Show end route alert and reset AppNavPhase
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

    // Shows a dialog to choose between Lift and Stairs
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
                        // if road has not started, current goal is origin
                        if(viewModel.getAppNavPhaseLD().getValue() == AppNavPhase.WAITING_USER_INPUT){
                            viewModel.publishOrigin(chosenWay);
                        }
                        // if road has already started, current goal is destination
                        if(viewModel.getAppNavPhaseLD().getValue() == AppNavPhase.REACHING_ORIGIN){
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

    // Centers camera on current floor's robot
    private void setCameraOnRobot() {
        Floor currentFloor = viewModel.getCurrentFloorLD().getValue();
        map.animateCamera(CameraUpdateFactory.newLatLng(robotMarkers.get(currentFloor).getPosition()));
    }

    // Shows tiles in current floor. If tiles hashmap is null, instantiate it with all floors' tiles.
    private void showTiles(Floor currentFloor) {
        if (map == null){
            return;
        }
        if(tileOverlays == null) {
            tileOverlays = initTileOverlays();
        }

        for(Floor floor: tileOverlays.keySet()){
            if(floor == currentFloor){
                tileOverlays.get(floor).setVisible(true);
                continue;
            }
            tileOverlays.get(floor).setVisible(false);
        }
    }


    // Shows marker of robot in given floor. If markers hashmap is null, instantiate it with all robot's markers.
    private void showRobotMarker(Floor currentFloor) {
        if(map == null){
            return;
        }
        if(robotMarkers == null){
            robotMarkers = initRobotMarkers();
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

    // Updates marker of robot in given floor. If markers hashmap is null, instantiate it with all robot's markers.
    private void updateRobotMarker(Floor currentFloor, MapPosition position) {
        if(map == null){
            return;
        }
        if(robotMarkers == null){
            robotMarkers = initRobotMarkers();
        }
        LatLng currentLatLng = position.toLatLng(currentFloor);
        robotMarkers.get(currentFloor).setPosition(currentLatLng);
    }

    // Instantiate Robot Markers' hashmap with all robot icons
    private HashMap<Floor,Marker> initRobotMarkers() {
        return new HashMap<Floor, Marker>(){{
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

    // Gets a TileProvider for given floor.
    // Tiles are loaded from assets/map_tiles
    private TileProvider getFloorTileProvider(final Floor currentFloor){
        TileProvider floorTileProvider;
        floorTileProvider = new TileProvider() {
            final String FLOOR_MAP_URL_FORMAT =
                    "map_tiles/floor_%d/%d/tile_%d_%d.png";
            final int TILE_SIZE_DP = 256;

            @Override
            public Tile getTile(int x, int y, int zoom) {
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

    // Initializes Tile Overlays hashmap.
    public HashMap<Floor,TileOverlay> initTileOverlays(){
        return new HashMap<Floor, TileOverlay>(){
            {
                for (Floor floor : Floor.values()) {
                    // Take a tileProvider for current floor
                    TileProvider provider = getFloorTileProvider(floor);
                    // add TileOverlay to map with generated TileProvider
                    TileOverlay overlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
                    // Put tileOverlay into hashmap
                    put(floor, overlay);
                }
            }};
    }

    // Provides a bitmapDescriptor for given drawable resource.
    // Needed to draw Origin and Destination markers from vector assets.
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

}