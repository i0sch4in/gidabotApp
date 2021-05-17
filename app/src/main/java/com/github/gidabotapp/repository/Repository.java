package com.github.gidabotapp.repository;

import android.content.Context;
import android.util.Log;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.github.gidabotapp.R;
import com.github.gidabotapp.data.AppNavPhase;
import com.github.gidabotapp.data.MultiNavPhase;
import com.github.gidabotapp.data.PhaseMessage;
import com.github.gidabotapp.domain.Floor;
import com.github.gidabotapp.domain.MapPosition;
import com.github.gidabotapp.domain.Room;
import com.github.gidabotapp.domain.Way;

import java.util.HashMap;
import java.util.List;

import multilevel_navigation_msgs.Goal;

import static com.github.gidabotapp.data.PhaseMessage.GOAL_REACHED;
import static com.github.gidabotapp.data.AppNavPhase.REACHING_DESTINATION;
import static com.github.gidabotapp.data.AppNavPhase.REACHING_ORIGIN;
import static com.github.gidabotapp.data.AppNavPhase.WAITING_USER_INPUT;

public class Repository {
    private final RoomRepositoryDAO roomDao;
    private final QNode qNode;

    private final LiveData<Integer> alertLD;
    private final MutableLiveData<Integer> toastLD;
    private final MutableLiveData<AppNavPhase> appNavPhaseLD;

    public Repository(Context appContext){
        RoomDatabase db = RoomDatabase.getInstance(appContext);
        roomDao = db.roomRepositoryDAO();
        qNode = QNode.getInstance();

        this.toastLD = new MutableLiveData<>();
        this.appNavPhaseLD = new MutableLiveData<>(WAITING_USER_INPUT);

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

    public LiveData<List<Room>> getAllRooms(){
        return roomDao.getAllRooms();
    }

    public LiveData<List<Room>> getRoomsByFloor(Floor floor){
        return roomDao.getRoomsByFloor(floor.getFloorCode());
    }


    // Publishes selected origin to qNode
    // Precondition: nearest and origin != null
    public void publishOrigin(Room nearest, Room origin, Way chosenWay) {
        if (origin == null) {
            toastLD.postValue(R.string.publish_error_msg_origin_empty);
            return;
        }
        // publish origin route to Ros Node
        qNode.publishGoal(nearest, origin, chosenWay);
        // Post feedback message to toast
        toastLD.postValue(R.string.publish_success_msg);
        // Change navPhase to next
        this.appNavPhaseLD.setValue(REACHING_ORIGIN);
    }

    // Publishes selected destination to qNode
    // Precondition: nearest and destination != null
    public void publishDestination(Room nearest, Room destination, Way chosenWay){
        if (destination == null) {
            toastLD.postValue(R.string.publish_error_msg_destination_empty);
            return;
        }
        // publish destination route to Ros Node
        qNode.publishGoal(nearest, destination, chosenWay);
        // Post feedback message to toast
        toastLD.postValue(R.string.publish_success_msg);
        // Change navPhase to next
        this.appNavPhaseLD.setValue(REACHING_DESTINATION);
    }

    // Publishes a cancel message to qNode
    public void publishCancel(Floor currentFloor) {
        // Get current floor's robot's pending goals
        List<Goal> pendingGoals = qNode.getPendingRequestsHM().get(currentFloor).getValue();
        // if pending goals are null, show feedback
        if(pendingGoals == null){
            toastLD.postValue(R.string.error_empty_cancel);
            return;
        }
        // if there are not pending goal, show feedback
        if(pendingGoals.size() == 0){
            toastLD.postValue(R.string.error_empty_cancel);
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
    public void qNodeShutdown() {
        this.qNode.shutdown();
    }

    public LiveData<Integer> getAlertLD(){
        return this.alertLD;
    }
    public MutableLiveData<Integer> getToastLD(){
        return this.toastLD;
    }
    public HashMap<Floor, MutableLiveData<List<Goal>>> getPendingRequestsHM(){
        return qNode.getPendingRequestsHM();
    }
    public HashMap<Floor, MutableLiveData<MapPosition>> getCurrentPositionsHM(){
        return qNode.getCurrentPositionsHM();
    }
    public MutableLiveData<AppNavPhase> getAppNavPhaseLD(){
        return this.appNavPhaseLD;
    }


}
