package com.github.gidabotapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
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
import com.google.android.gms.maps.model.UrlTileProvider;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
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

    public RouteSelectActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_select);

        qNode = QNode.getInstance();

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
//        map.setMaxZoomPreference(3.0f);

        map.setMapType(GoogleMap.MAP_TYPE_NONE);

        // TODO: set camera bounds
//        LatLng NORTHEAST_BOUND = new LatLng(-90,-180);
//        LatLng SOUTHWEST_BOUND = new LatLng(90,180);
//        LatLngBounds bounds = new LatLngBounds(NORTHEAST_BOUND,SOUTHWEST_BOUND);
//        map.setLatLngBoundsForCameraTarget(bounds);

        TileProvider tileProvider = new UrlTileProvider(256, 256) {
            //        TileProvider tileProvider = new UrlTileProvider(64, 64) {
            @Override
            public synchronized URL getTileUrl(int x, int y, int zoom) {
//                String CURRENT_FLOOR = "floor0"; // TODO
                String CURRENT_FLOOR = "floor0";
                String s = String.format(Locale.US, FLOOR_MAP_URL_FORMAT, CURRENT_FLOOR, zoom, x, y);
//                String s = String.format(Locale.US, FLOOR_MAP_URL_FORMAT, zoom, x, y);
//                int reversedY = (1 << zoom) - y - 1;
//                String s = String.format(Locale.US, MOON_MAP_URL_FORMAT, zoom, x, reversedY);
                URL url;
                if (!checkTileExists(x,y,zoom)){
                    return null;
                }
                try {
                    url = new URL(s);
                } catch (MalformedURLException e) {
                    throw new AssertionError(e);
                }
                return url;
            }
        };

        floorTiles = map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));

        TileProvider coordTileProvider = new CoordTileProvider(this.getApplicationContext());
        map.addTileOverlay(new TileOverlayOptions().tileProvider(coordTileProvider));
//        LatLng robotLatLng = new LatLng(-32.18,38.87);
        LatLng robotLatLng = toLatLng(qNode.currentPos);
        robotMarker = map.addMarker(new MarkerOptions()
                .position(robotLatLng)
                .title("Tartalo")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.tartalo))
        );
        // Move camera to robot's current location
        map.moveCamera(CameraUpdateFactory.newLatLng(robotLatLng));

        mHandler = new Handler();
        startRepeatingTask();
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
//        int minZoom = 12;
//        int maxZoom = 16;
//        int minX = 0;
//        int maxX = 3;
//        int minY = 0;
//        int maxY = 3;

        return (zoom >= minZoom && zoom <= maxZoom
//                &&
//                x >= minX && x<=maxX &&
//                y >= minY && y<=maxY
        );
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
        LatLng newPos = toLatLng(qNode.currentPos);
        this.robotMarker.setPosition(newPos);
    }

    void startRepeatingTask(){
        mStatusChecker.run();
    }

    void stopRepeatingTask(){
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