package com.github.gidabotapp;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

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
    private final MutableLiveData<Integer> currentFloorObserver;
    private final MutableLiveData<List<Room>> currentFloorRoomsObserver;
    private final MutableLiveData<Integer> alertObserver;
    private final MutableLiveData<NavPhase> navPhaseObserver;
    private final MutableLiveData<MapPosition> positionObserver;

    private Room origin;
    private Room destination;
    private static PhaseMessage currentPhaseMessage;
    final int STARTING_FLOOR = 0;
    private List<Goal> pendingGoals;

    public MapViewModel(@NonNull Application application) {
        super(application);
        qNode = QNode.getInstance();
        try {
            roomRepository = new RoomRepository(application.getApplicationContext());
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

        this.currentFloorObserver = new MutableLiveData<>(STARTING_FLOOR);
        this.toastObserver = new MutableLiveData<>();
        this.currentFloorRoomsObserver = new MutableLiveData<>(roomRepository.getRoomsByFloor(STARTING_FLOOR));
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


    // TODO: Should be without try/catch
    public void publishCancel() {
        String userId = qNode.getUserId();

            try {
                Goal first = pendingGoals.get(0);
                if(userId.compareTo(first.getUserName()) == 0){
                    qNode.publishCancel(first.getGoalSeq(), false, 0, 0);
                }

                final Goal second = pendingGoals.get(1);
                if(userId.compareTo(second.getUserName()) == 0) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            qNode.publishCancel(second.getGoalSeq(), false, 0, 0);
                        }
                    },5000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

//        toastObserver.postValue(getApplication().getApplicationContext().getString(R.string.cancel_msg_error));
    }

    public MutableLiveData<String> getToastObserver() {
        return toastObserver;
    }

    public MutableLiveData<List<Room>> getCurrentFloorRoomsObserver() {
        return this.currentFloorRoomsObserver;
    }

    public MutableLiveData<Integer> getCurrentFloorObserver() {
        return this.currentFloorObserver;
    }
    public MutableLiveData<Integer> getAlertObserver() {
        return this.alertObserver;
    }
    public MutableLiveData<NavPhase> getNavPhaseObserver(){return this.navPhaseObserver;}
    public MutableLiveData<MapPosition> getPositionObserver(){return this.positionObserver;}


    public List<Room> getCurrentFloorRooms() {
        List<Room> currentFloorRooms = null;

        // Observer can be null
        if(this.currentFloorObserver.getValue() != null){
            currentFloorRooms = roomRepository.getRoomsByFloor(this.currentFloorObserver.getValue());
        }
        return currentFloorRooms;
    }

    public void selectFloor(int floor) {
        this.currentFloorObserver.postValue(floor);
        this.currentFloorRoomsObserver.postValue(roomRepository.getRoomsByFloor(floor));
    }


    public int getRobotIconId() {
        int iconId = R.drawable.tartalo_small;
        Integer currentFloor = currentFloorObserver.getValue();
        assert currentFloor != null;
            switch (currentFloor) {
                // case 0 = ic_tartalo (default)
                case 1:
                    iconId = R.drawable.kbot_small;
                    break;
                case 2:
                    iconId = R.drawable.galtxa_small;
                    break;
                case 3:
                    iconId = R.drawable.mari_small;
                    break;
        }
        return iconId;
    }

    public void selectOrigin(int spinnerIndex) {
        assert this.currentFloorRoomsObserver.getValue() != null;
        this.origin = this.currentFloorRoomsObserver.getValue().get(spinnerIndex);
    }

    public void selectDestination(int spinnerIndex) {
        assert this.currentFloorRoomsObserver.getValue() != null;
        this.destination = this.currentFloorRoomsObserver.getValue().get(spinnerIndex);
    }

}
