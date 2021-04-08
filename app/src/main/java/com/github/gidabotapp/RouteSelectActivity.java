package com.github.gidabotapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

public class RouteSelectActivity extends AppCompatActivity implements OnMapReadyCallback {

    // TODO: use local resource
    private static final String FLOOR_MAP_URL_FORMAT=
            "https://raw.githubusercontent.com/i0sch4in/floor_tiles/master/%s/%d/tile_%d_%d.png";

    private TileOverlay floorTiles;
    private Marker robotMarker;
    private static QNode qNode;

    private final int mInterval = 1000;
    private Handler mHandler;

    private ArrayList<Marker> roomMarkers;
    private RoomRepository modelRooms;

    public RouteSelectActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_select);

        qNode = QNode.getInstance();
        try {
            modelRooms = new RoomRepository(this);
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.i("Zoom", "Min: "+ map.getMinZoomLevel());
        Log.i("Zoom", "Max: "+ map.getMaxZoomLevel());
        Log.i("Zoom", "Position: "+ map.getCameraPosition());
//        map.moveCamera(CameraUpdateFactory.zoomTo(0.0f));
        map.setMaxZoomPreference(4.0f);

        map.setMapType(GoogleMap.MAP_TYPE_NONE);

        // TODO: set camera bounds
//        LatLng NORTHEAST_BOUND = new LatLng(-90,-180);
//        LatLng SOUTHWEST_BOUND = new LatLng(90,180);
//        LatLngBounds bounds = new LatLngBounds(NORTHEAST_BOUND,SOUTHWEST_BOUND);
//        map.setLatLngBoundsForCameraTarget(bounds);

//        TileProvider tileProvider = new UrlTileProvider(256, 256) {
//            //        TileProvider tileProvider = new UrlTileProvider(64, 64) {
//            @Override
//            public synchronized URL getTileUrl(int x, int y, int zoom) {
////                String CURRENT_FLOOR = "floor0"; // TODO
//                String CURRENT_FLOOR = "floor0";
//                String s = String.format(Locale.US, FLOOR_MAP_URL_FORMAT, CURRENT_FLOOR, zoom, x, y);
////                String s = String.format(Locale.US, FLOOR_MAP_URL_FORMAT, zoom, x, y);
////                int reversedY = (1 << zoom) - y - 1;
////                String s = String.format(Locale.US, MOON_MAP_URL_FORMAT, zoom, x, reversedY);
//                URL url;
//                if (!checkTileExists(x,y,zoom)){
//                    return null;
//                }
//                try {
//                    url = new URL(s);
//                } catch (MalformedURLException e) {
//                    throw new AssertionError(e);
//                }
//                return url;
//            }
//        };
        TileProvider tileProvider = new TileProvider() {
            final String FLOOR_MAP_URL_FORMAT =
                    "map_tiles/floor%d/%d/tile_%d_%d.png";
            final int CURRENT_FLOOR = 0;
            final int TILE_SIZE_DP = 256;

            @Override
            public Tile getTile(int x, int y, int zoom)
            {
                if(!checkTileExists(x,y,zoom)){
                    return null;
                }
                String s = String.format(Locale.US, FLOOR_MAP_URL_FORMAT,CURRENT_FLOOR,zoom,x,y);
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

        floorTiles = map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));

        TileProvider coordTileProvider = new CoordTileProvider(this.getApplicationContext());
//        map.addTileOverlay(new TileOverlayOptions().tileProvider(coordTileProvider));
        LatLng robotLatLng = toLatLng(qNode.currentPos);
        robotMarker = map.addMarker(new MarkerOptions()
                .position(robotLatLng)
                .title("Tartalo") // TODO
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.tartalo)) // TODO
                .zIndex(0.9f)
        );
        // Move camera to robot's current location
        map.moveCamera(CameraUpdateFactory.newLatLng(robotLatLng));

        final Marker[] lastOpened = {null};
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
//                // Check if there is an open info window
//                if (lastOpened[0] != null) {
//                    // Close the info window
//                    lastOpened[0].hideInfoWindow();
//
//                    // Is the marker the same marker that was already open
//                    if (lastOpened[0].equals(marker)) {
//                        // Nullify the lastOpened object
//                        lastOpened[0] = null;
//                        // Return so that the info window isn't opened again
//                        return true;
//                    }
//                }
//
//                // Open the info window for the marker
//                marker.showInfoWindow();
//                // Re-assign the last opened such that we can close it later
//                lastOpened[0] = marker;
//
//                // Event was handled by our code do not launch default behaviour.
                return true;
            }
        });

//        map.setInfoWindowAdapter(new InfoAdapter(getLayoutInflater()));

        mHandler = new Handler();
        startRepeatingTask();

        generateRoomMarkers(map);

    }

    private void generateRoomMarkers(GoogleMap map) {
        roomMarkers = new ArrayList<>();

        for(Room room: modelRooms.getFirstFloorRooms()){
            LatLng latLng = this.toLatLng(room.getPosition());
            Bitmap icon = this.textAsBitmap(room.getName());
            Marker marker = map.addMarker(new MarkerOptions()
                .position(latLng)
                .title(room.getName())
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
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

    private LatLng toLatLng(MapPosition position){
        double FACTOR_X = 5.333;
        double FACTOR_Y = 3.3;

        double x = position.getX();
        double y = position.getY();

        double lng = FACTOR_X * (x+30) - 180;
        double lat = FACTOR_Y * (y+22.6) - 65;

        return new LatLng(lat,lng);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRepeatingTask();
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                updateStatus();
            }
            finally {
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    private void updateStatus() {
        LatLng goal = toLatLng(qNode.currentPos);
//        LatLng mid = midPoint(robotMarker.getPosition(),goal);
//        this.robotMarker.setPosition(mid);
        this.robotMarker.setPosition(goal);
    }

    LatLng midPoint(LatLng current, LatLng goal){
        double midLat = (current.latitude + goal.latitude)/2;
        double midLng = (current.longitude + goal.longitude)/2;
        return new LatLng(midLat,midLng);
    }

    void startRepeatingTask(){
        mStatusChecker.run();
    }

    void stopRepeatingTask(){
        mHandler.removeCallbacks(mStatusChecker);
    }

    private Bitmap textAsBitmap(String text){
        float scaleFactor = getApplicationContext().getResources().getDisplayMetrics().density;
        float textSize = 17f * scaleFactor;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
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