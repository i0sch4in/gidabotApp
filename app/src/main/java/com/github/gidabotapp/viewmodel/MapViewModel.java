package com.github.gidabotapp.viewmodel;

import android.app.Application;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.github.gidabotapp.domain.Floor;
import com.github.gidabotapp.domain.MapPosition;
import com.github.gidabotapp.domain.NavPhase;
import com.github.gidabotapp.domain.PhaseMessage;
import com.github.gidabotapp.repository.QNode;
import com.github.gidabotapp.R;
import com.github.gidabotapp.domain.Room;
import com.github.gidabotapp.repository.RoomRepository;

import org.ros.message.MessageListener;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
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
    private final MutableLiveData<NavPhase> navPhaseObserver;
    private final MutableLiveData<MapPosition> positionObserver;

    private final LiveData<List<Room>> allRooms;

    private Room origin;
    private Room destination;
    private static PhaseMessage currentPhaseMessage;
    final Floor STARTING_FLOOR = Floor.ZEROTH;
    private List<Goal> pendingGoals;

    public MapViewModel(@NonNull Application application) {
        super(application);
        qNode = QNode.getInstance();
        try {
            roomRepository = new RoomRepository(application.getApplicationContext());
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

        this.allRooms = roomRepository.getAllRooms();

        this.currentFloor = new MutableLiveData<>(STARTING_FLOOR);
        this.toastObserver = new MutableLiveData<>();

//        List<Room> starting_rooms = roomRepository.getRoomsByFloor(STARTING_FLOOR);
//        this.currentFloorRoomsObserver = new MutableLiveData<>(starting_rooms);
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
                alertObserver.postValue(currentPhaseMessage.getMessageId());
            }
        });

        qNode.setNavPhaseListener(new MessageListener<Int8>() {
            @Override
            public void onNewMessage(Int8 message) {
                int i = message.getData();
                navPhaseObserver.postValue(NavPhase.values()[i]);
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
    public void publishRoute() {
        String message;
        if (origin == null || destination == null) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_empty);
        } else if (origin.equals(destination)) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_same);
        } else {
            Room nearest = roomRepository.getNearestRoom(positionObserver.getValue());
            if (!nearest.equals(origin)) {
                qNode.publishGoal(nearest, origin);
            }
            qNode.publishGoal(origin, destination);
            message = String.format(getApplication().getApplicationContext().getString(R.string.publish_success_msg), origin, destination);
        }
        toastObserver.postValue(message);
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
        }catch (IndexOutOfBoundsException e){
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
    public MutableLiveData<NavPhase> getNavPhaseObserver(){return this.navPhaseObserver;}
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
            case MEZZANINE:
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

    public void selectOrigin(int spinnerIndex) {
        assert this.currentFloorRooms.getValue() != null;
        this.origin = this.currentFloorRooms.getValue().get(spinnerIndex);
    }

    public void selectDestination(int spinnerIndex) {
        assert this.currentFloorRooms.getValue() != null;
        this.destination = this.currentFloorRooms.getValue().get(spinnerIndex);
    }

    public LiveData<List<Room>> getAllRooms(){
        return this.allRooms;
    }
}
