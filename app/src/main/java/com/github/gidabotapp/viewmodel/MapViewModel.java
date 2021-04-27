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
import static com.github.gidabotapp.domain.AppNavPhase.*;

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
    final Floor STARTING_FLOOR = Floor.ZEROTH;
    private List<Goal> pendingGoals;

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
        String message;
        Room nearest = getNearestRoom(positionObserver.getValue());
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

    public LiveData<AppNavPhase> getAppNavPhase(){
        return this.appNavPhase;
    }

    public void resetAppNavPhase(){
        this.appNavPhase.setValue(WAITING_USER_INPUT);
    }

    public void closeNode() {
        qNode.shutdown();
    }

}
