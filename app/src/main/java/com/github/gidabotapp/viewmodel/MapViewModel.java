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
import static com.github.gidabotapp.domain.PhaseMessage.GOAL_REACHED;

public class MapViewModel extends AndroidViewModel {
    private static QNode qNode;
    private final RoomRepository roomRepository;

    private final LiveData<List<Room>> currentFloorRoomsLD;
    private final LiveData<Integer> alertLD;
    private final LiveData<List<Room>> allRoomsLD;
    private final MutableLiveData<String> toastLD;
    private final MutableLiveData<Floor> currentFloorLD;
    private final MutableLiveData<MultiNavPhase> multiNavPhaseLD;
    private final MutableLiveData<AppNavPhase> appNavPhaseLD;

    private final HashMap<Floor, MutableLiveData<MapPosition>> positionsHM;
    private final HashMap<Floor, MutableLiveData<List<Goal>>> pendingRequestsHM;

    private Room origin;
    private Room destination;

    public MapViewModel(@NonNull Application application) {
        super(application);

        // Get qNode instance, which should have already been instantiated
        // Since MainActivity sets its connectedNode on execution start
        qNode = QNode.getInstance();

        // Instantiate Room repository with application's context,
        // in order to be able to query data from database
        roomRepository = new RoomRepository(application.getApplicationContext());

        this.appNavPhaseLD = new MutableLiveData<>(WAITING_USER_INPUT);
        this.currentFloorLD = new MutableLiveData<>(Floor.getStartingFloor());
        this.toastLD = new MutableLiveData<>();

        // Get repository and qNode's liveData, which will be exposed in viewModel
        // for the view to observe
        this.allRoomsLD = roomRepository.getAllRooms();
        this.positionsHM = qNode.getCurrentPositionsHM();
        this.multiNavPhaseLD = qNode.getMultiNavPhaseLD();
        this.pendingRequestsHM = qNode.getPendingRequestsHM();

        // Whenever currentFloor changes, returns list of Rooms for selected Floor
        this.currentFloorRoomsLD = Transformations.switchMap(currentFloorLD, new Function<Floor, LiveData<List<Room>>>() {
            @Override
            public LiveData<List<Room>> apply(Floor floor) {
                return roomRepository.getRoomsByFloor(floor);
            }
        });

        // Whenever PhaseMessage changes, transform it to invoke corresponding alert on View
        this.alertLD = Transformations.map(qNode.getPhaseMessageLD(), new Function<PhaseMessage, Integer>() {
            @Override
            public Integer apply(PhaseMessage currentPhaseMessage) {
                if(currentPhaseMessage == GOAL_REACHED){
                    if(appNavPhaseLD.getValue() == REACHING_ORIGIN){
                        return R.string.origin_reached_msg;
                    }
                    else if(appNavPhaseLD.getValue() == REACHING_DESTINATION){
                        return R.string.destination_reached_msg;
                    }
                }
                else {
                    return currentPhaseMessage.getMessageResId();
                }
                return R.string.empty;
            }
        });
    }

    // shut qNode down
    public void shutdownNode() {
        qNode.shutdown();
    }

    // Publishes selected origin to qNode
    public void publishOrigin(Way chosenWay) {
        String message;
        Floor currentFloor = getCurrentFloorLD().getValue();
        // Get current floor's robot's position on map
        MapPosition currentPosition = positionsHM.get(currentFloor).getValue();
        // Get nearest known room to that position, by euclidean distance
        Room nearest = getNearestRoom(currentPosition);
        if (origin == null) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_origin_empty);
        } else {
           qNode.publishGoal(nearest, origin, chosenWay);
           message = String.format(getApplication().getApplicationContext().getString(R.string.publish_success_msg), origin);
        }
        // Post feedback message to toast
        toastLD.postValue(message);
        // Change navPhase to next
        this.appNavPhaseLD.setValue(REACHING_ORIGIN);
    }

    // Publishes selected destination to qNode
    public void publishDestination(Way chosenWay){
        String message;
        Floor currentFloor = this.currentFloorLD.getValue();
        // Get current floor's robot's position on map
        MapPosition currentPosition = positionsHM.get(currentFloor).getValue();
        // Get nearest known room to that position, by euclidean distance
        Room nearest = getNearestRoom(currentPosition);
        if (destination == null) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_destination_empty);
        } else {
            qNode.publishGoal(nearest, destination, chosenWay);
            message = String.format(getApplication().getApplicationContext().getString(R.string.publish_success_msg), destination);
        }
        // Post feedback message to toast
        toastLD.postValue(message);
        // Change navPhase to next
        this.appNavPhaseLD.setValue(REACHING_DESTINATION);
    }

    // Publishes a cancel message to qNode
    public void publishCancel() {
        Floor currentFloor = this.currentFloorLD.getValue();
        // Get current floor's robot's pending goals
        List<Goal> pendingGoals = pendingRequestsHM.get(currentFloor).getValue();
        // if pending goals are null, show feedback
        if(pendingGoals == null){
            toastLD.postValue(getApplication().getApplicationContext().getString(R.string.error_empty_cancel));
            return;
        }
        // if there are not pending goal, show feedback
        if(pendingGoals.size() == 0){
            toastLD.postValue(getApplication().getApplicationContext().getString(R.string.error_empty_cancel));
            return;
        }
        String userId = qNode.getUserId();
        // Get latest goal, which is the one to be cancelled
        Goal firstGoal = pendingGoals.get(0);
        // If current user requested that goal, cancel it. Otherwise do nothing
        if (userId.compareTo(firstGoal.getUserName()) == 0) {
            qNode.publishCancel(firstGoal.getGoalSeq(), false, currentFloor, currentFloor, null);
            appNavPhaseLD.setValue(WAITING_USER_INPUT);
        }
    }

    // Lift is needed if current goal's floor is different to current floor
    public boolean isLiftNeeded(){
        if(emptyRoute()){
            return false;
        }
        Floor goalFloor = getCurrentGoalFloor();
        return goalFloor != currentFloorLD.getValue();
    }

    // Route is empty if origin or destination are null
    public boolean emptyRoute(){
        return origin == null || destination == null;
    }

    // Decides whether destination is on current floor or not
    // Useful when we want decide to invoke stairs/lift dialog
    public boolean destOnCurrentFloor() {
        if(destination == null){
            return false;
        }
        Floor currentFloor = this.currentFloorLD.getValue();
        Floor destFloor = Floor.getFromDouble(destination.getFloor());
        return currentFloor == destFloor;
    }

    // Returns current goal's floor's pending request amount
    public int getGoalFloorPending(){
        Floor goalFloor = getCurrentGoalFloor();
        List<Goal> pending = pendingRequestsHM.get(goalFloor).getValue();
        if(pending == null){
            return 0;
        }
        return pending.size();
    }

    // Returns current goal's Floor.
    // Current goal depends on app navPhase
    public Floor getCurrentGoalFloor(){
        Floor goalFloor;
        // goal floor is origin's floor
        if(appNavPhaseLD.getValue() == WAITING_USER_INPUT){
            goalFloor = Floor.values()[(int) origin.getFloor()];
        }
        // goal floor is destination's floor
        else{ // appNavPhase != WAITING_USER_INPUT
            goalFloor = Floor.values()[(int) destination.getFloor()];
        }
        return goalFloor;
    }

    // Origin and Destination getters and setters
    public void selectOrigin(Room origin) {
        this.origin = origin;
    }
    public void selectDestination(Room dest) {
        this.destination = dest;
    }
    public Room getDestination() {
        return this.destination;
    }

    // (Mutable)LiveData getters and setters
    public LiveData<List<Room>> getCurrentFloorRoomsLD() {
        return this.currentFloorRoomsLD;
    }
    public LiveData<Integer> getAlertLD() {
        return this.alertLD;
    }
    public LiveData<AppNavPhase> getAppNavPhaseLD(){
        return this.appNavPhaseLD;
    }
    public LiveData<List<Room>> getAllRoomsLD(){
        return this.allRoomsLD;
    }
    public void resetAppNavPhase(){
        this.appNavPhaseLD.setValue(WAITING_USER_INPUT);
    }
    public MutableLiveData<Floor> getCurrentFloorLD() {
        return this.currentFloorLD;
    }
    public void selectFloor(Floor floor) {
        this.currentFloorLD.setValue(floor);
    }
    public MutableLiveData<String> getToastLD() {
        return toastLD;
    }
    public MutableLiveData<MultiNavPhase> getMultiNavPhaseLD(){
        return this.multiNavPhaseLD;
    }
    public MutableLiveData<MapPosition> getPositionObserver(Floor f){
        return this.positionsHM.get(f);
    }

    // Returns nearest known Room to current MapPosition
    private Room getNearestRoom(MapPosition current) {
        // Nearest room will be in current floor's Room list
        List<Room> rooms = getCurrentFloorRoomsLD().getValue();

        // current Floor always has a value, so current floor room's list will also always have a value
        assert rooms != null;
        // Start with first room on the list
        Room nearestRoom = rooms.get(0);
        // Compute square distance from current MapPosition and Room
        double nearestDistance = current.dSquare(nearestRoom.getPosition());

        // iterate through all other elements on the list
        for (Room r : rooms.subList(1, rooms.size())) {
            MapPosition pos = r.getPosition();
            double distance = pos.dSquare(current);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestRoom = r;
            }
        }

        // Nearest room will be the Room who's square distance is lower than all other Rooms in current Floor
        return nearestRoom;
    }
}
