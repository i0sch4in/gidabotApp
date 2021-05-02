package com.github.gidabotapp.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.github.gidabotapp.domain.AppNavPhase;
import com.github.gidabotapp.domain.Floor;
import com.github.gidabotapp.domain.MapPosition;
import com.github.gidabotapp.domain.MultiNavPhase;
import com.github.gidabotapp.domain.PhaseMessage;
import com.github.gidabotapp.domain.Robot;
import com.github.gidabotapp.repository.QNode;
import com.github.gidabotapp.R;
import com.github.gidabotapp.domain.Room;
import com.github.gidabotapp.repository.RoomRepository;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import org.ros.message.MessageListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import geometry_msgs.PoseWithCovarianceStamped;
import multilevel_navigation_msgs.Goal;
import multilevel_navigation_msgs.PendingGoals;
import std_msgs.Int8;
import static com.github.gidabotapp.domain.AppNavPhase.*;
import static com.github.gidabotapp.domain.Floor.*;

public class MapViewModel extends AndroidViewModel {
    private static QNode qNode;
    private RoomRepository roomRepository;

    private final MutableLiveData<String> toastObserver;
    private final MutableLiveData<Floor> currentFloor;
    private final LiveData<List<Room>> currentFloorRooms;
    private final MutableLiveData<Integer> alertObserver;
    private final MutableLiveData<MultiNavPhase> navPhaseObserver;
    private final MutableLiveData<MapPosition> positionObserver;
    private final LiveData<List<Room>> allRoomsLD;

    private MutableLiveData<AppNavPhase> appNavPhase;

    private Room origin;
    private Room destination;

    private static PhaseMessage currentPhaseMessage;
    final Floor STARTING_FLOOR = ZEROTH;
    private final int MAX_MAP_ZOOM = 3;

    private List<Goal> pendingGoals;

    private HashMap<Floor, Robot> robots;
    private HashMap<Floor, TileProvider> tileProviders;

    private GoogleMap map;

    public MapViewModel(@NonNull Application application) {
        super(application);
        qNode = QNode.getInstance();
        roomRepository = new RoomRepository(application.getApplicationContext());
        this.appNavPhase = new MutableLiveData<>(WAITING_USER_INPUT);

        this.currentFloor = new MutableLiveData<>(STARTING_FLOOR);
        this.toastObserver = new MutableLiveData<>();
        this.allRoomsLD = roomRepository.getAllRooms();
        this.currentFloorRooms = Transformations.switchMap(currentFloor, new Function<Floor, LiveData<List<Room>>>() {
            @Override
            public LiveData<List<Room>> apply(Floor floor) {
                return roomRepository.getRoomsByFloor(floor);
            }
        });
        this.alertObserver = new MutableLiveData<>();
        this.navPhaseObserver = new MutableLiveData<>();
        this.positionObserver = new MutableLiveData<>();

        qNode.setPhaseMsgListener(new MessageListener<Int8>() {
            @Override
            public void onNewMessage(Int8 message) {
                int i = message.getData();
                currentPhaseMessage = new PhaseMessage(i);
                if(currentPhaseMessage.getPhase() == PhaseMessage.message_enum.GOAL_REACHED){
                    if(appNavPhase.getValue() == REACHING_ORIGIN){
                        alertObserver.postValue(R.string.origin_reached_msg);
                    }
                    else if(appNavPhase.getValue() == REACHING_DESTINATION){
                        alertObserver.postValue(R.string.destination_reached_msg);
                    }
                }
                else {
                    alertObserver.postValue(currentPhaseMessage.getMessageResId());
                }
            }
        });

        qNode.setNavPhaseListener(new MessageListener<Int8>() {
            @Override
            public void onNewMessage(Int8 message) {
                int i = message.getData();
                navPhaseObserver.postValue(MultiNavPhase.values()[i]);
            }
        });

        qNode.setTartaloPosListener(new MessageListener<PoseWithCovarianceStamped>() {
            @Override
            public void onNewMessage(PoseWithCovarianceStamped message) {
                if(robots == null){
                    return;
                }
                MapPosition position = new MapPosition(message);
                if(currentFloor.getValue() == ZEROTH) {
                    Log.i("zeroth", "zeroth");
                    robots.get(ZEROTH).updateMarker(position);
                    drawRobot();
                }
            }
        });
        qNode.setKbotPosListener(new MessageListener<PoseWithCovarianceStamped>() {
            @Override
            public void onNewMessage(PoseWithCovarianceStamped message) {
                if(robots == null){
                    return;
                }
                MapPosition position = new MapPosition(message);
                if(currentFloor.getValue() == FIRST) {
                    robots.get(FIRST).updateMarker(position);
                    drawRobot();
                }
            }
        });
        qNode.setGaltxaPosListener(new MessageListener<PoseWithCovarianceStamped>() {
            @Override
            public void onNewMessage(PoseWithCovarianceStamped message) {
                if(robots == null){
                    return;
                }
                MapPosition position = new MapPosition(message);
                if(currentFloor.getValue() == SECOND) {
                    robots.get(SECOND).updateMarker(position);
                    drawRobot();
                }
            }
        });
        qNode.setMariPosListener(new MessageListener<PoseWithCovarianceStamped>() {
            @Override
            public void onNewMessage(PoseWithCovarianceStamped message) {
                if(robots == null){
                    return;
                }
                MapPosition position = new MapPosition(message);
                if(currentFloor.getValue() == THIRD) {
                    robots.get(THIRD).updateMarker(position);
                    drawRobot();
                }
            }
        });

        qNode.setPendingGoalsListener(new MessageListener<PendingGoals>() {
            @Override
            public void onNewMessage(PendingGoals message) {
                pendingGoals = message.getGoals();
            }
        });

    }

    public void publishOrigin() {
        String message;
//        Log.i("robot", robots.get(FIRST).getPosition().toString());
//        Room nearest = getNearestRoom(robots.get(currentFloor.getValue()).getPosition());
        Room nearest = new Room(0,"placehlder","000",new MapPosition(0,0,0));
        if (origin == null) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_origin_empty);
        } else if (nearest.equals(origin)) { // Robot Position == origin
            publishDestination();
            return;
        } else {
           qNode.publishGoal(nearest, origin);
           message = String.format(getApplication().getApplicationContext().getString(R.string.publish_success_msg), origin);
        }
        toastObserver.postValue(message);
        this.appNavPhase.setValue(REACHING_ORIGIN);
    }

    public void publishDestination(){
        String message;
        Room nearest = getNearestRoom(positionObserver.getValue());
        if (destination == null) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_destination_empty);
        } else if (nearest.equals(destination)) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_same);
        } else {
            qNode.publishGoal(nearest, destination);
            message = String.format(getApplication().getApplicationContext().getString(R.string.publish_success_msg), destination);
        }
        toastObserver.postValue(message);
        this.appNavPhase.setValue(REACHING_DESTINATION);
    }

    public void publishCancel() {
        if(!pendingGoals.isEmpty()){
            String userId = qNode.getUserId();
            Goal first = pendingGoals.get(0);
            if (userId.compareTo(first.getUserName()) == 0) {
                qNode.publishCancel(first.getGoalSeq(), false, 0, 0);
                appNavPhase.setValue(WAITING_USER_INPUT);
            }
        }
        else {
            toastObserver.postValue(getApplication().getApplicationContext().getString(R.string.error_empty_cancel));
        }
    }

    public MutableLiveData<String> getToastObserver() {
        return toastObserver;
    }

    public LiveData<List<Room>> getCurrentFloorRooms() {
        return this.currentFloorRooms;
    }

    public MutableLiveData<Floor> getCurrentFloor() {
        return this.currentFloor;
    }
    public MutableLiveData<Integer> getAlertObserver() {
        return this.alertObserver;
    }
    public MutableLiveData<MultiNavPhase> getNavPhaseObserver(){return this.navPhaseObserver;}
    public MutableLiveData<MapPosition> getPositionObserver(){return this.positionObserver;}


    public void selectFloor(int index) {
        this.currentFloor.setValue(Floor.values()[index]);
        showTiles(Floor.values()[index]);

    }

    private void showTiles(Floor f) {
        map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProviders.get(f)));
    }


    public void selectOrigin(Room origin) {
        this.origin = origin;
    }

    public void selectDestination(Room dest) {
        this.destination = dest;
    }

    public LiveData<List<Room>> getAllRoomsLD(){
        return this.allRoomsLD;
    }

    public Room getNearestRoom(MapPosition current) {
        List<Room> rooms = getCurrentFloorRooms().getValue();

        // current Floor always has a value
        assert rooms != null;
        Room nearestRoom = rooms.get(0);
        double nearestDistance = current.dSquare(nearestRoom.getPosition());

        // iterate through other elements
        for (Room r : rooms.subList(1, rooms.size())) {
            MapPosition pos = r.getPosition();
            double distance = pos.dSquare(current);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestRoom = r;
            }
        }

        return nearestRoom;
    }

    public Room getDestination() {
        return this.destination;
    }

    public LiveData<AppNavPhase> getAppNavPhase(){
        return this.appNavPhase;
    }

    public void resetAppNavPhase(){
        this.appNavPhase.setValue(WAITING_USER_INPUT);
    }

    public void initializeRobots(){
        final Robot tartalo = new Robot(ZEROTH,"tartalo", R.drawable.tartalo_small);
        tartalo.setMarker(map.addMarker(new MarkerOptions()
            .title("Tartalo")
            .position(ZEROTH.getStartLatLng())
            .icon(BitmapDescriptorFactory.fromResource(tartalo.iconResId))
            .visible(false)
        ));
        final Robot kbot = new Robot(FIRST,"kbot", R.drawable.kbot_small);
        kbot.setMarker(map.addMarker(new MarkerOptions()
                .title("Kbot")
                .position(FIRST.getStartLatLng())
                .icon(BitmapDescriptorFactory.fromResource(kbot.iconResId))
                .visible(false)
        ));
        final Robot galtxa = new Robot(SECOND,"galtxa", R.drawable.galtxa_small);
        galtxa.setMarker(map.addMarker(new MarkerOptions()
                .title("Galtxagorri")
                .position(SECOND.getStartLatLng())
                .icon(BitmapDescriptorFactory.fromResource(galtxa.iconResId))
                .visible(false)
        ));
        final Robot mari = new Robot(THIRD,"mari", R.drawable.galtxa_small);
        mari.setMarker(map.addMarker(new MarkerOptions()
                .title("Marisorgin")
                .position(THIRD.getStartLatLng())
                .icon(BitmapDescriptorFactory.fromResource(mari.iconResId))
                .visible(false)
        ));
        robots = new HashMap<Floor, Robot>(){{
            put(ZEROTH, tartalo);
            put(FIRST, kbot);
            put(SECOND, galtxa);
            put(THIRD, mari);
        }};
    }

    public void shutdownNode() {
        qNode.shutdown();
    }
    
    public void setMap(GoogleMap map){
        this.map = map;
        mapSetup();
        map.addMarker(new MarkerOptions()
        .title("")
        .position(new LatLng(0,0)));
        initializeRobots();
        robots.get(ZEROTH).show();
    }

    private void mapSetup() {
        map.setMapType(GoogleMap.MAP_TYPE_NONE);
        map.setMaxZoomPreference(MAX_MAP_ZOOM);

        // Set camera bounds: Horizontal scroll ends with map
        LatLng SOUTHWEST_BOUND = new LatLng(-65,-110);
        LatLng NORTHEAST_BOUND = new LatLng(+65,+110);
        LatLngBounds bounds = new LatLngBounds(SOUTHWEST_BOUND,NORTHEAST_BOUND);
        map.setLatLngBoundsForCameraTarget(bounds);

        /*
         * Set custom onClickListener for all markers,
         * so that it only shows Marker's information (if it has any),
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

        tileProviders = new HashMap<>();
        for (Floor f: Floor.values()){
            TileProvider provider = getFloorTileProvider(f);
            tileProviders.put(f, provider);
        }
        showTiles(STARTING_FLOOR);

    }

    private TileProvider getFloorTileProvider(final Floor currentFloor){
        TileProvider floorTileProvider;
        floorTileProvider = new TileProvider() {
            final String FLOOR_MAP_URL_FORMAT =
                    "map_tiles/floor_%d/%d/tile_%d_%d.png";
            final int TILE_SIZE_DP = 256;

            @Override
            public Tile getTile(int x, int y, int zoom) {
                if (!checkTileExists(x, y, zoom)) {
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


    private boolean checkTileExists(int x, int y, int zoom) {
        int minZoom = 0;

        return (zoom >= minZoom && zoom <= MAX_MAP_ZOOM);
    }

    public void resetCamera() {
        map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(robots.get(currentFloor.getValue()).getMarker().getPosition(),0,0,0)));
    }

    public void setCameraOnRobot() {
        map.animateCamera(CameraUpdateFactory.newLatLng(robots.get(currentFloor.getValue()).getMarker().getPosition()));
    }

    private void drawRobot(){
        if(map != null) {
            MapPosition position = positionObserver.getValue();
            Floor current = currentFloor.getValue();
            robots.get(current).updateMarker(position);
            robots.get(current).show();
            for (Floor f : robots.keySet()) {
                if (f == current) {
                    continue;
                }
                robots.get(f).hide();
            }
        }
    }

}
