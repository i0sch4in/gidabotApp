package com.github.gidabotapp.viewmodel;

import android.app.Application;
import android.os.Handler;

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
import com.github.gidabotapp.repository.QNode;
import com.github.gidabotapp.R;
import com.github.gidabotapp.domain.Room;
import com.github.gidabotapp.repository.RoomRepository;

import org.ros.message.MessageListener;

import java.util.List;

import geometry_msgs.PoseWithCovarianceStamped;
import multilevel_navigation_msgs.Goal;
import multilevel_navigation_msgs.PendingGoals;
import std_msgs.Int8;

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

    private AppNavPhase appNavPhase;

    private Room origin;
    private Room destination;

    private static PhaseMessage currentPhaseMessage;
    final Floor STARTING_FLOOR = Floor.ZEROTH;
    private List<Goal> pendingGoals;

    public MapViewModel(@NonNull Application application) {
        super(application);
        qNode = QNode.getInstance();
        roomRepository = new RoomRepository(application.getApplicationContext());
        this.appNavPhase = AppNavPhase.WAIT_USER_INPUT;

        this.currentFloor = new MutableLiveData<>(STARTING_FLOOR);
        this.toastObserver = new MutableLiveData<>();

//        MutableLiveData<Boolean> forceLoad = new MutableLiveData<>(true);
//        this.allRoomsLD = Transformations.switchMap(forceLoad, new Function<Boolean, LiveData<List<Room>>>() {
//            @Override
//            public LiveData<List<Room>> apply(Boolean input) {
//                return roomRepository.getAllRooms();
//            }
//        });
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
                    if(appNavPhase == AppNavPhase.REACHING_ORIGIN){
                        alertObserver.postValue(R.string.origin_reached_msg);
                    }
                    else if(appNavPhase == AppNavPhase.REACHING_DESTINATION){
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

        qNode.setPositionListener(new MessageListener<PoseWithCovarianceStamped>() {
            @Override
            public void onNewMessage(PoseWithCovarianceStamped message) {
                MapPosition position = new MapPosition(message);
                positionObserver.postValue(position);
            }
        });

        qNode.setPendingGoalsListener(new MessageListener<PendingGoals>() {
            @Override
            public void onNewMessage(PendingGoals message) {
                pendingGoals = message.getGoals();
            }
        });

    }

    // TODO: Should not use context (leaks) --> locale change
    public void publishOrigin() {
        String message = null;
        Room nearest = getNearestRoom(positionObserver.getValue());
        if (origin == null) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_origin_empty);
        } else if (nearest.equals(origin)) {
            publishDestination();
        } else {
           qNode.publishGoal(nearest, origin);
           message = String.format(getApplication().getApplicationContext().getString(R.string.publish_success_msg), origin);
        }
        toastObserver.postValue(message);
        this.appNavPhase = appNavPhase.next();
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
        this.appNavPhase = appNavPhase.next();
    }

    public void publishCancel() {
        String userId = qNode.getUserId();
        try {
            Goal first = pendingGoals.get(0);
            if (userId.compareTo(first.getUserName()) == 0) {
                qNode.publishCancel(first.getGoalSeq(), false, 0, 0);
            }

            final Goal second = pendingGoals.get(1);
            if (userId.compareTo(second.getUserName()) == 0) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        qNode.publishCancel(second.getGoalSeq(), false, 0, 0);
                    }
                }, 5000);
            }
        }catch (Exception e){
            e.printStackTrace();
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


//    public List<Room> getCurrentFloorRooms() {
//        List<Room> currentFloorRooms = new ArrayList<>();
//        if (this.currentFloorObserver.getValue() != null) {
//            currentFloorRooms = roomRepository.getRoomsByFloor(this.currentFloorObserver.getValue());
//        }
//        return currentFloorRooms;
//    }

    public void selectFloor(int index) {
        this.currentFloor.setValue(Floor.values()[index]);
//        this.currentFloorRooms.postValue(roomRepository.getRoomsByFloor(floor));
    }


    public int getRobotIconId() {
        int iconId = R.drawable.tartalo_small;
        Floor currentFloor = this.currentFloor.getValue();
        assert currentFloor != null;
        switch (currentFloor) {
            // case 0 = ic_tartalo (default)
            case FIRST:
                iconId = R.drawable.kbot_small;
                break;
            case SECOND:
                iconId = R.drawable.galtxa_small;
                break;
            case THIRD:
                iconId = R.drawable.mari_small;
                break;
        }
        return iconId;
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

    public AppNavPhase getAppNavPhase(){
        return this.appNavPhase;
    }

//    public void setAllRooms(List<Room> rooms){
//        this.allRooms = rooms;
//    }
}
