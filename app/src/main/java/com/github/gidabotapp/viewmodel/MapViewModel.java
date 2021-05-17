package com.github.gidabotapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.github.gidabotapp.R;
import com.github.gidabotapp.data.AppNavPhase;
import com.github.gidabotapp.domain.Floor;
import com.github.gidabotapp.domain.MapPosition;
import com.github.gidabotapp.data.MultiNavPhase;
import com.github.gidabotapp.domain.Room;
import com.github.gidabotapp.domain.Way;
import com.github.gidabotapp.repository.Repository;

import java.util.List;

import multilevel_navigation_msgs.Goal;

import static com.github.gidabotapp.data.AppNavPhase.*;

public class MapViewModel extends AndroidViewModel {
    private final Repository repository;

    private final LiveData<List<Room>> currentFloorRoomsLD;
    private final MutableLiveData<Floor> currentFloorLD;
    private final LiveData<String> toastLD;

    private Room origin;
    private Room destination;

    public MapViewModel(@NonNull Application application) {
        super(application);

        // Instantiate Room repository with application's context,
        // in order to be able to query data from database
        repository = new Repository(application.getApplicationContext());

        this.currentFloorLD = new MutableLiveData<>(Floor.getStartingFloor());

        // Get repository and qNode's liveData, which will be exposed in viewModel
        // for the view to observe

        // Whenever currentFloor changes, returns list of Rooms for selected Floor
        this.currentFloorRoomsLD = Transformations.switchMap(currentFloorLD, new Function<Floor, LiveData<List<Room>>>() {
            @Override
            public LiveData<List<Room>> apply(Floor floor) {
                return repository.getRoomsByFloor(floor);
            }
        });

        this.toastLD = Transformations.map(repository.getToastLD(), new Function<Integer, String>() {
            @Override
            public String apply(Integer input) {
                String message = getApplication().getString(input);
                if(input == R.string.publish_success_msg) {
                    Room goal = origin;
                    if (repository.getAppNavPhaseLD().getValue() == REACHING_DESTINATION) {
                        goal = destination;
                    }
                    return String.format(message, goal);
                }
                return message;
            }
        });

    }

    // shut qNode down
    public void shutdownNode() {
        repository.qNodeShutdown();
    }

    // Publishes selected origin to qNode
    public void publishOrigin(Way chosenWay){
        // Get current floor (won't be null)
        Floor currentFloor = this.currentFloorLD.getValue();
        // Get current floor's robot's position on map
        MapPosition currentPosition = repository.getCurrentPositionsHM().get(currentFloor).getValue();
        // Get nearest known room to that position, by euclidean distance
        assert currentPosition != null;
        Room nearest = getNearestRoom(currentPosition);
        // Publish origin to ros Node
        repository.publishOrigin(nearest, origin, chosenWay);
    }

    // Publishes selected destination to qNode
    public void publishDestination(Way chosenWay){
        // Get current floor (won't be null)
        Floor currentFloor = this.currentFloorLD.getValue();
        // Get current floor's robot's position on map
        MapPosition currentPosition = repository.getCurrentPositionsHM().get(currentFloor).getValue();
        // Get nearest known room to that position, by euclidean distance
        assert currentPosition != null;
        Room nearest = getNearestRoom(currentPosition);
        // Publish destination to ros Node
        repository.publishDestination(nearest, destination, chosenWay);
    }

    // Publishes a cancel message to qNode
    public void publishCancel() {
        Floor currentFloor = this.currentFloorLD.getValue();
        repository.publishCancel(currentFloor);
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
        List<Goal> pending = repository.getPendingRequestsHM().get(goalFloor).getValue();
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
        if(repository.getAppNavPhaseLD().getValue() == WAITING_USER_INPUT){
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
        return repository.getAlertLD();
    }
    public LiveData<AppNavPhase> getAppNavPhaseLD(){
        return repository.getAppNavPhaseLD();
    }
    public LiveData<List<Room>> getAllRoomsLD(){
        return repository.getAllRooms();
    }
    public void resetAppNavPhase(){
        repository.getAppNavPhaseLD().setValue(WAITING_USER_INPUT);
    }
    public MutableLiveData<Floor> getCurrentFloorLD() {
        return this.currentFloorLD;
    }
    public void selectFloor(Floor floor) {
        this.currentFloorLD.setValue(floor);
    }
    public LiveData<String> getToastLD() {
        return this.toastLD;
    }
    public MutableLiveData<MapPosition> getPositionLD(Floor f){
        return repository.getCurrentPositionsHM().get(f);
    }

    // Returns nearest known Room to current MapPosition
    private Room getNearestRoom(MapPosition current) {
        // Nearest room will be in current floor's Room list
        List<Room> rooms = currentFloorRoomsLD.getValue();
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
