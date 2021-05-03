package com.github.gidabotapp.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.github.gidabotapp.viewmodel.MapViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
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
import static com.github.gidabotapp.domain.Floor.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RouteSelectActivity extends AppCompatActivity implements OnMapReadyCallback {

    private HashMap<Floor, Marker> robotMarkers;

    private MapViewModel viewModel;

    private GoogleMap map;

    private Button publishBtn, cancelBtn;
    private AutoCompleteTextView act_origin, act_destination, act_floor;

    private final int MAX_MAP_ZOOM = 3;
    private HashMap<Floor, TileProvider> tileProviders;

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
                View bottomButtons = findViewById(R.id.cancelBtn);
                Snackbar.make(bottomButtons,message,Snackbar.LENGTH_SHORT).show();
            }
        });
        viewModel.getAlertObserver().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer stringResId) {
                if(stringResId == R.string.origin_reached_msg){
                    showNextGoalAlert();
                }
                else if(stringResId == R.string.destination_reached_msg){
                    showRouteEndAlert();
                }
                else {
                    String message = getString(stringResId);
                    showAlert(message);
                }
            }
        });
//        viewModel.getPositionObserver(ZEROTH_FLOOR).observe(this, new Observer<MapPosition>() {
//            @Override
//            public void onChanged(MapPosition position) {
//                updateMarker(position);
//            }
//        });
        for(final Floor floor: Floor.values()){
            viewModel.getPositionObserver(floor).observe(this, new Observer<MapPosition>() {
                @Override
                public void onChanged(MapPosition position) {
                    updateMarker(floor, position);
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
                viewModel.publishOrigin();
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
            }
        });

        act_floor = findViewById(R.id.act_floor);
        final ArrayAdapter<String> floorAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, Floor.getFloorList());
        act_floor.setAdapter(floorAdapter);
        act_floor.setAdapter(floorAdapter);
        act_floor.setText(floorAdapter.getItem(0),false);
        act_floor.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Floor currentFloor = Floor.values()[position];
                viewModel.selectFloor(currentFloor);
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
                showMarker(floor);
            }
        });

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
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

        showTiles(Floor.getStartingFloor());
        showMarker(Floor.getStartingFloor());
    }

    private void updateButtonsLock(AppNavPhase phase) {
         if (phase == AppNavPhase.WAITING_USER_INPUT){
            act_origin.setEnabled(true);
            act_destination.setEnabled(true);
            act_floor.setEnabled(true);
            publishBtn.setEnabled(true);
            cancelBtn.setEnabled(false);
        }
        else {
            act_origin.setEnabled(false);
            act_destination.setEnabled(false);
            act_floor.setEnabled(false);
            publishBtn.setEnabled(false);
            cancelBtn.setEnabled(true);
        }
    }

    private void showAlert(String msg) {
         MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.alert_title))
                .setMessage(msg)
                .setPositiveButton(R.string.accept_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // publish destination
                    }
                });
        dialog.show();
    }

    private void showNextGoalAlert(){
        String message = String.format(getString(R.string.origin_reached_msg),viewModel.getDestination());
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.alert_title)
                .setMessage(message)
                .setPositiveButton(R.string.accept_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        viewModel.publishDestination(); // publish destination
                    }
                })
                .setNegativeButton(R.string.cancel_btn, new DialogInterface.OnClickListener() {
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
        String message = getString(R.string.destination_reached_msg);
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.alert_title)
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

    private void setCameraOnRobot() {
        Floor currentFloor = viewModel.getCurrentFloor().getValue();
        map.animateCamera(CameraUpdateFactory.newLatLng(robotMarkers.get(currentFloor).getPosition()));
    }

    private void resetCamera(Floor currentFloor) {
        map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(robotMarkers.get(currentFloor).getPosition(),0,0,0)));
    }

    private void showTiles(Floor currentFloor) {
        if (map == null){
            return;
        }
        if(tileProviders == null){
            initializeTileProviders(); // loading problems, probably other thread needed --> on end event add overlay
        }
        map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProviders.get(currentFloor)));
    }


    public void initializeTileProviders(){
        tileProviders = new HashMap<>();
        for (Floor f: Floor.values()){
            TileProvider provider = getFloorTileProvider(f);
            tileProviders.put(f, provider);
        }
    }

    private void showMarker(Floor currentFloor) {
        if(map == null){
            return;
        }
        if(robotMarkers == null){
            initializeRobotMarkers();
        }
        // Set current floor marker visible
        robotMarkers.get(currentFloor).setVisible(true);
        resetCamera(currentFloor);

        // Set other markers not visible
        for(Floor floor : robotMarkers.keySet()){
            if(floor == currentFloor){
                continue;
            }
            robotMarkers.get(floor).setVisible(false);
        }
    }

    private void updateMarker(Floor currentFloor, MapPosition position) {
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
        final Marker tartalo = map.addMarker(new MarkerOptions()
            .title("Tartalo")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.tartalo_small))
            .position(ZEROTH_FLOOR.getStartLatLng())
            .zIndex(1.0f)
        );
        final Marker kbot = map.addMarker(new MarkerOptions()
                .title("Kbot")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.kbot_small))
                .position(FIRST_FLOOR.getStartLatLng())
                .zIndex(1.0f)
        );
        final Marker galtxa = map.addMarker(new MarkerOptions()
                .title("Galtxagorri")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.galtxa_small))
                .position(SECOND_FLOOR.getStartLatLng())
                .zIndex(1.0f)
        );
        final Marker mari = map.addMarker(new MarkerOptions()
                .title("Marisorgin")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.mari_small))
                .position(THIRD_FLOOR.getStartLatLng())
                .zIndex(1.0f)
        );
        robotMarkers = new HashMap<Floor, Marker>(){{
           put(ZEROTH_FLOOR,tartalo);
           put(FIRST_FLOOR,kbot);
           put(SECOND_FLOOR,galtxa);
           put(THIRD_FLOOR,mari);
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
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                    return new Tile(TILE_SIZE_DP, TILE_SIZE_DP, stream.toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        return floorTileProvider;
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
        viewModel.getNavPhaseObserver().removeObservers(this);
        viewModel.shutdownNode();
    }

}