package com.github.gidabotapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class RouteSelectActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Marker robotMarker;
    private ArrayList<Marker> roomMarkers;

    private Room non;
    private Room nora;

    private MapViewModel viewModel;

    private GoogleMap map;
    private TileOverlay tileOverlay;

    private Handler mHandler;
    private Runnable mStatusChecker;
    private final int MAP_UPDATE_INTERVAL = 1000; // milliseconds

    public RouteSelectActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_select);
        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())).get(MapViewModel.class);
        viewModel.getCurrentFloorObserver().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer floor) {
                drawNewTiles(floor);
            }
        });
        viewModel.getToastMessageObserver().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });


        final Button publishBtn = findViewById(R.id.publishBtn);
        publishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.publishGoals(non,nora);
            }
        });

        final Button cancelBtn = findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.publishCancel();
            }
        });

        final Spinner spinnerNon = findViewById(R.id.spinnerNon);
        final ArrayAdapter<Room> adapterRooms = new ArrayAdapter<>(this, R.layout.spinner_item, viewModel.getCurrentFloorRooms());
        spinnerNon.setAdapter(adapterRooms);
        spinnerNon.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                non = viewModel.getRoom(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        final Spinner spinnerNora = findViewById(R.id.spinnerNora);
        spinnerNora.setAdapter(adapterRooms);
        spinnerNora.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                nora = viewModel.getRoom(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        final Spinner spinnerFloor = findViewById(R.id.spinnerFloor);
        Log.i("Null", Arrays.toString(getResources().getStringArray((R.array.floorArray))));
        final ArrayAdapter<String> floorAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, getResources().getStringArray(R.array.floorArray));
        spinnerFloor.setAdapter(floorAdapter);
        spinnerFloor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.selectFloor(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        viewModel.getCurrentFloorRoomsObserver().observe(this, new Observer<List<Room>>() {
            @Override
            public void onChanged(List<Room> rooms) {
                adapterRooms.clear();
                adapterRooms.addAll(rooms);
            }
        });

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.setMapType(GoogleMap.MAP_TYPE_NONE);
        map.setMaxZoomPreference(4.0f);

        // TODO: set camera bounds
//        LatLng NORTHEAST_BOUND = new LatLng(-90,-180);
//        LatLng SOUTHWEST_BOUND = new LatLng(90,180);
//        LatLngBounds bounds = new LatLngBounds(NORTHEAST_BOUND,SOUTHWEST_BOUND);
//        map.setLatLngBoundsForCameraTarget(bounds);

//        LatLng robotLatLng = toLatLng(qNode.currentPos);
        // Move camera to robot's current location
        //TODO
//        map.moveCamera(CameraUpdateFactory.newLatLng(robotLatLng));

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });
        this.map = map;

        mHandler = new Handler();
        mStatusChecker = new Runnable() {
            @Override
            public void run() {
                try {
                    updateRobotPos();
                }
                finally {
                    mHandler.postDelayed(mStatusChecker, MAP_UPDATE_INTERVAL);
                }
            }
        };

        // Draw map on floor 0
        drawNewTiles(viewModel.DEFAULT_FLOOR);

        // Start redrawing robot every second
        mStatusChecker.run();
    }

    private void updateRobotPos() {
        MapPosition pos = viewModel.getCurrentPos();
        drawRobot(pos);
    }

    // TODO: map can be null
    private void drawNewTiles(final int floor){
        if(map != null) {
            generateRoomMarkers(map);

            TileProvider tileProvider = new TileProvider() {
                final String FLOOR_MAP_URL_FORMAT =
                        "map_tiles/floor%d/%d/tile_%d_%d.png";
                final int TILE_SIZE_DP = 256;

                @Override
                public Tile getTile(int x, int y, int zoom) {
                    if (!checkTileExists(x, y, zoom)) {
                        return null;
                    }
                    String s = String.format(Locale.US, FLOOR_MAP_URL_FORMAT, floor, zoom, x, y);
                    try {
                        InputStream is = getAssets().open(s);
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
            // Remove current overlay and robot
            if (tileOverlay != null) {
                tileOverlay.remove();
                robotMarker.remove();
                robotMarker = null;
            }
            tileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
        }
    }

    private void drawRobot(MapPosition position){
        LatLng latLng = toLatLng(position);
        int iconId = viewModel.getRobotIconId();
        BitmapDescriptor current_icon = BitmapDescriptorFactory.fromResource(iconId);
//        String current_name = getResources().getResourceEntryName(iconId).split("_")[1];
//        current_name = current_name.substring(0,1).toUpperCase() + current_name.substring(1);
        if(robotMarker == null){
            robotMarker = map.addMarker(new MarkerOptions()
//                    .title(current_name)
                    .title("Tartalo")
                    .icon(current_icon)
                    .position(latLng)
                    .zIndex(0.9f)
            );
            map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
        robotMarker.setPosition(latLng);
    }

    private void generateRoomMarkers(GoogleMap map) {
        // Remove map's current markers markers
        if(roomMarkers != null) {
            for(Marker marker:roomMarkers){
                marker.remove();
            }
        }

        // if instantiated, remove current old markers overwriting with new array
        // else, instantiate marker array
        roomMarkers = new ArrayList<>();

        for(Room room: viewModel.getCurrentFloorRooms()){
            LatLng latLng = this.toLatLng(room.getPosition());
            Bitmap textIcon = this.textAsBitmap(room.getName());
            Marker marker = map.addMarker(new MarkerOptions()
                .position(latLng)
                .title(room.getName())
                .icon(BitmapDescriptorFactory.fromBitmap(textIcon))
                .zIndex(1.0f)
            );
            roomMarkers.add(marker);
        }

    }

    /*
     * Check that the tile server supports the requested x, y and zoom.
     * Complete this stub according to the tile range you support.
     * If you support a limited range of tiles at different zoom levels, then you
     * need to define the supported x, y range at each zoom level.
     */
    private boolean checkTileExists(int x, int y, int zoom) {
        int minZoom = 0;
        int maxZoom = 4;

        return (zoom >= minZoom && zoom <= maxZoom);
    }

    // TODO: refine precision
    private LatLng toLatLng(MapPosition position){
        double FACTOR_X = 5.333;
        double FACTOR_Y = 3.3;

        double x = position.getX();
        double y = position.getY();

        double lng = FACTOR_X * (x+30) - 180;
        double lat = FACTOR_Y * (y+22.6) - 65;

        return new LatLng(lat,lng);
    }


    private Bitmap textAsBitmap(String text){
        final float scaleFactor = getApplicationContext().getResources().getDisplayMetrics().density;
        final float TEXT_SIZE = 17f;
        final float render_size = TEXT_SIZE * scaleFactor;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(render_size);
        paint.setColor(getColor(R.color.material_black));
        paint.setTextAlign(Paint.Align.LEFT);

        float baseLine = -paint.ascent();
        int width = (int) (paint.measureText(text) + 1f); // round
        int height = (int) (baseLine + paint.descent() + 0.5f);

        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(Color.WHITE);
        canvas.drawText(text, 0, baseLine, paint);
        return image;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop updating robot
        mHandler.removeCallbacks(mStatusChecker);
    }

    private static class CoordTileProvider implements TileProvider {

        private static final int TILE_SIZE_DP = 256;

        private final float scaleFactor;

        private final Bitmap borderTile;

        public CoordTileProvider(Context context) {
            /* Scale factor based on density, with a 0.6 multiplier to increase tile generation
             * speed */
            scaleFactor = context.getResources().getDisplayMetrics().density * 0.6f;
            Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderTile = Bitmap.createBitmap((int) (TILE_SIZE_DP * scaleFactor),
                    (int) (TILE_SIZE_DP * scaleFactor), android.graphics.Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(borderTile);
            canvas.drawRect(0, 0, TILE_SIZE_DP * scaleFactor, TILE_SIZE_DP * scaleFactor,
                    borderPaint);
        }

        @Override
        public Tile getTile(int x, int y, int zoom) {
            Bitmap coordTile = drawTileCoords(x, y, zoom);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            coordTile.compress(Bitmap.CompressFormat.PNG, 0, stream);
            byte[] bitmapData = stream.toByteArray();
            return new Tile((int) (TILE_SIZE_DP * scaleFactor),
                    (int) (TILE_SIZE_DP * scaleFactor), bitmapData);
        }

        private Bitmap drawTileCoords(int x, int y, int zoom) {
            // Synchronize copying the bitmap to avoid a race condition in some devices.
            Bitmap copy = null;
            synchronized (borderTile) {
                copy = borderTile.copy(android.graphics.Bitmap.Config.ARGB_8888, true);
            }
            Canvas canvas = new Canvas(copy);
            String tileCoords = "(" + x + ", " + y + ")";
            String zoomLevel = "zoom = " + zoom;
            /* Paint is not thread safe. */
            Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setTextSize(18 * scaleFactor);
            canvas.drawText(tileCoords, TILE_SIZE_DP * scaleFactor / 2,
                    TILE_SIZE_DP * scaleFactor / 2, mTextPaint);
            canvas.drawText(zoomLevel, TILE_SIZE_DP * scaleFactor / 2,
                    TILE_SIZE_DP * scaleFactor * 2 / 3, mTextPaint);
            return copy;
        }
    }

}