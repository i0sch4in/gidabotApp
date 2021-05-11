package com.github.gidabotapp.viewmodel;

import android.app.Application;

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
import com.github.gidabotapp.domain.Way;
import com.github.gidabotapp.repository.QNode;
import com.github.gidabotapp.R;
import com.github.gidabotapp.domain.Room;
import com.github.gidabotapp.repository.RoomRepository;

import java.util.HashMap;
import java.util.List;

import multilevel_navigation_msgs.Goal;

import static com.github.gidabotapp.domain.AppNavPhase.*;
import static com.github.gidabotapp.domain.Floor.*;
import static com.github.gidabotapp.domain.PhaseMessage.GOAL_REACHED;

public class MapViewModel extends AndroidViewModel {
    private static QNode qNode;
    private final RoomRepository roomRepository;

    private final MutableLiveData<String> toastObserver;
    private final MutableLiveData<Floor> currentFloor;
    private final LiveData<List<Room>> currentFloorRooms;
    private final LiveData<Integer> alertObserver;
    private final MutableLiveData<MultiNavPhase> multiNavPhaseLD;
    private final HashMap<Floor, MutableLiveData<MapPosition>> positionObserver;
    private final LiveData<List<Room>> allRoomsLD;
    private final HashMap<Floor, MutableLiveData<List<Goal>>> pendingRequests;
    private final MutableLiveData<AppNavPhase> appNavPhase;

    private Room origin;
    private Room destination;

    public MapViewModel(@NonNull Application application) {
        super(application);
        qNode = QNode.getInstance();
        roomRepository = new RoomRepository(application.getApplicationContext());
        this.appNavPhase = new MutableLiveData<>(WAITING_USER_INPUT);

        this.currentFloor = new MutableLiveData<>(Floor.getStartingFloor());
        this.toastObserver = new MutableLiveData<>();
        this.allRoomsLD = roomRepository.getAllRooms();
        this.currentFloorRooms = Transformations.switchMap(currentFloor, new Function<Floor, LiveData<List<Room>>>() {
            @Override
            public LiveData<List<Room>> apply(Floor floor) {
                return roomRepository.getRoomsByFloor(floor);
            }
        });
        this.positionObserver = qNode.getCurrentPositions();
        this.alertObserver = Transformations.map(qNode.getPhaseMessageLD(), new Function<PhaseMessage, Integer>() {
            @Override
            public Integer apply(PhaseMessage currentPhaseMessage) {
                if(currentPhaseMessage == GOAL_REACHED){
                    if(appNavPhase.getValue() == REACHING_ORIGIN){
                        return R.string.origin_reached_msg;
                    }
                    else if(appNavPhase.getValue() == REACHING_DESTINATION){
                        return R.string.destination_reached_msg;
                    }
                }
                else {
                    return currentPhaseMessage.getMessageResId();
                }
                return R.string.empty;
            }
        });
        this.multiNavPhaseLD = qNode.getMultiNavPhaseLD();
        this.pendingRequests = qNode.getPendingRequests();
    }

    public void publishOrigin(Way chosenWay) {
        String message;
        Floor currentFloor = getCurrentFloor().getValue();
        MapPosition currentPosition = positionObserver.get(currentFloor).getValue();
        Room nearest = getNearestRoom(currentPosition);
        if (origin == null) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_origin_empty);
        } else {
           qNode.publishGoal(nearest, origin, chosenWay);
           message = String.format(getApplication().getApplicationContext().getString(R.string.publish_success_msg), origin);
        }
        toastObserver.postValue(message);
        this.appNavPhase.setValue(REACHING_ORIGIN);
    }

    public void publishDestination(Way chosenWay){
        String message;
        Floor currentFloor = this.currentFloor.getValue();
        MapPosition currentPosition = positionObserver.get(currentFloor).getValue();
        Room nearest = getNearestRoom(currentPosition);
        if (destination == null) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_destination_empty);
        } else {
            qNode.publishGoal(nearest, destination, chosenWay);
            message = String.format(getApplication().getApplicationContext().getString(R.string.publish_success_msg), destination);
        }
        toastObserver.postValue(message);
        this.appNavPhase.setValue(REACHING_DESTINATION);
    }

    public void publishCancel() {
        Floor currentFloor = this.currentFloor.getValue();
        List<Goal> pendingGoals = pendingRequests.get(currentFloor).getValue();
        if(pendingGoals == null){
            toastObserver.postValue(getApplication().getApplicationContext().getString(R.string.error_empty_cancel));
            return;
        }
        if(pendingGoals.size() == 0){
            toastObserver.postValue(getApplication().getApplicationContext().getString(R.string.error_empty_cancel));
            return;
        }
        String userId = qNode.getUserId();
        Goal firstGoal = pendingGoals.get(0);
        if (userId.compareTo(firstGoal.getUserName()) == 0) {
            qNode.publishCancel(firstGoal.getGoalSeq(), false, currentFloor, currentFloor, null); // TODO
            appNavPhase.setValue(WAITING_USER_INPUT);
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
    public LiveData<Integer> getAlertObserver() {
        return this.alertObserver;
    }
    public MutableLiveData<MultiNavPhase> getMultiNavPhaseLD(){return this.multiNavPhaseLD;}
    public MutableLiveData<MapPosition> getPositionObserver(Floor f){return this.positionObserver.get(f);}


    public void selectFloor(Floor floor) {
        this.currentFloor.setValue(floor);
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

    private Room getNearestRoom(MapPosition current) {
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

    public void shutdownNode() {
        qNode.shutdown();
    }

    public boolean isLiftNeeded(){
        if(emptyRoute()){
            return false;
        }
        Floor goalFloor = getGoalFloor();
        return goalFloor != currentFloor.getValue();
    }

    public int getGoalPendingReq(){
        Floor goalFloor = getGoalFloor();
        List<Goal> pending = pendingRequests.get(goalFloor).getValue();
        if(pending == null){
            return 0;
        }
        return pending.size();
    }

    public Floor getGoalFloor(){
        Floor goalFloor;
        if(appNavPhase.getValue() == WAITING_USER_INPUT){
            goalFloor = Floor.values()[(int) origin.getFloor()];
        }
        else{ // appNavPhase != WAITING_USER_INPUT
            goalFloor = Floor.values()[(int) destination.getFloor()];
        }
        return goalFloor;
    }

    public boolean emptyRoute(){
        return origin == null || destination == null;
    }

    public boolean destOnCurrentFloor() {
        if(destination == null){
            return false;
        }
        Floor currentFloor = this.currentFloor.getValue();
        Floor destFloor = Floor.getFromDouble(destination.getFloor());
        return currentFloor == destFloor;
    }
}
