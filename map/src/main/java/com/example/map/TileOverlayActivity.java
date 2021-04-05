package com.example.map;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;

import androidx.fragment.app.FragmentActivity;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

public class TileOverlayActivity extends FragmentActivity implements SeekBar.OnSeekBarChangeListener, OnMapReadyCallback {

    private static final int TRANSPARENCY_MAX = 100;

    /** This returns moon tiles. */
    private static final String MOON_MAP_URL_FORMAT =
            "https://mw1.google.com/mw-planetary/lunar/lunarmaps_v1/clem_bw/%d/%d/%d.jpg";
    // TODO: use local resource
//    private static final String FLOOR_MAP_URL_FORMAT=
//            "file:///android_asset/floor%d/%d/tile_%d_%d.png";
    private static final String FLOOR_MAP_URL_FORMAT=
        "https://raw.githubusercontent.com/i0sch4in/floor_tiles/master/%s/%d/tile_%d_%d.png";
//    private static final String FLOOR_MAP_URL_FORMAT=
//       "https://raw.githubusercontent.com/i0sch4in/square_tiles/master/%d/tile_%d_%d.png";

    private TileOverlay floorTiles;
    private SeekBar transparencyBar;
    private Marker robotMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        transparencyBar = (SeekBar) findViewById(R.id.transparencySeekBar);
        transparencyBar.setMax(TRANSPARENCY_MAX);
        transparencyBar.setProgress(0);

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
        transparencyBar.setOnSeekBarChangeListener(this);

        TileProvider coordTileProvider = new CoordTileProvider(this.getApplicationContext());
        map.addTileOverlay(new TileOverlayOptions().tileProvider(coordTileProvider));
        LatLng robotLatLng = new LatLng(66.5,180);
        robotMarker = map.addMarker(new MarkerOptions()
            .position(robotLatLng)
            .title("Tartalo")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.tartalo))
        );
        // Move camera to robot's current location
        map.moveCamera(CameraUpdateFactory.newLatLng(robotLatLng));
    }

    /*
     * Check that the tile server supports the requested x, y and zoom.
     * Complete this stub according to the tile range you support.
     * If you support a limited range of tiles at different zoom levels, then you
     * need to define the supported x, y range at each zoom level.
     */
    private boolean checkTileExists(int x, int y, int zoom) {
        int minZoom = 0;
        int maxZoom = 20;
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

    public void setFadeIn(View v) {
        if (floorTiles == null) {
            return;
        }
        floorTiles.setFadeIn(((CheckBox) v).isChecked());

        LatLng current = robotMarker.getPosition();
        LatLng newPos = new LatLng(current.latitude-2, current.longitude);
        robotMarker.setPosition(newPos);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (floorTiles != null) {
            floorTiles.setTransparency((float) progress / (float) TRANSPARENCY_MAX);
        }
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