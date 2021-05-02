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
import android.graphics.Paint;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RouteSelectActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Marker robotMarker;

    private MapViewModel viewModel;

    private TileOverlay tileOverlay;

    private Button publishBtn, cancelBtn;
    private AutoCompleteTextView act_origin, act_destination, act_floor;

    private HashMap<Floor, Marker> robotMarkers;

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
//                else if (viewModel.getAppNavPhase().getValue() != AppNavPhase.WAITING_USER_INPUT){
                else{
                    String message = getString(stringResId);
                    showAlert(message);
                }
            }
        });
//        viewModel.getPositionObserver().observe(this, new Observer<MapPosition>() {
//            @Override
//            public void onChanged(MapPosition position) {
//                viewModel.drawRobot(position);
//            }
//        });
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
                viewModel.setCameraOnRobot();
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
                viewModel.selectFloor(position);
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

//        viewModel.getNavPhaseObserver().observe(this,new Observer<MultiNavPhase>(){
//            @Override
//            public void onChanged(MultiNavPhase multiNavPhase) {
//                enableButtons(multiNavPhase);
//            }
//
//        });

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
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

    @Override
    public void onMapReady(GoogleMap map) {
        viewModel.setMap(map);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.getCurrentFloor().removeObservers(this);
        viewModel.getToastObserver().removeObservers(this);
        viewModel.getAlertObserver().removeObservers(this);
        viewModel.getPositionObserver().removeObservers(this);
        viewModel.getCurrentFloorRooms().removeObservers(this);
        viewModel.getNavPhaseObserver().removeObservers(this);
        viewModel.shutdownNode();
    }

}