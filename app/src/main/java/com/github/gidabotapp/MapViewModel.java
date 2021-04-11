package com.github.gidabotapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import org.ros.message.MessageListener;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

import std_msgs.Int8;

public class MapViewModel extends AndroidViewModel {
    private static QNode qNode;
    private RoomRepository roomRepository;

    private final MutableLiveData<String> toastObserver;
    private final MutableLiveData<Integer> currentFloorObserver;
    private final MutableLiveData<List<Room>> currentFloorRoomsObserver;
    private final MutableLiveData<Integer> alertObserver;
    private final MutableLiveData<NavPhase> navPhaseObserver;

    private Room origin;
    private Room destination;
    private static PhaseMessageSingleton currentPhaseMessage;
    final int STARTING_FLOOR = 0;

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
        currentPhaseMessage = PhaseMessageSingleton.getInstance();

        qNode.setPhaseMsgListener(new MessageListener<Int8>() {
            @Override
            public void onNewMessage(Int8 message) {
                int i = message.getData();
                currentPhaseMessage.setMessage(i);
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

    }

    // TODO: Should not use context (leaks) --> locale change
    public void publishRoute() {
        String message;
        if (origin == null || destination == null) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_empty);
        } else if (origin.equals(destination)) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_same);
        } else {
            Room nearest = roomRepository.getNearestRoom(qNode.getCurrentNav().getCurrent());
            if (!nearest.equals(origin)) {
                qNode.publishGoal(nearest, origin);
            }
            qNode.publishGoal(origin, destination);
            message = String.format(getApplication().getApplicationContext().getString(R.string.publish_success_msg), origin, destination);
        }
        toastObserver.postValue(message);
    }


    public void publishCancel() {
        qNode.publishCancel();
        toastObserver.postValue(getApplication().getApplicationContext().getString(R.string.cancel_msg));
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

    public Room getRoom(int listIndex) {
        return roomRepository.getRoomByFloorIndex(this.currentFloorObserver.getValue(), listIndex);
    }

    public List<Room> getCurrentFloorRooms() {
        return roomRepository.getRoomsByFloor(this.currentFloorObserver.getValue());
    }

    // TODO: Floors 0.5, 2, 3
    public void selectFloor(int floor) {
        this.currentFloorObserver.postValue(floor);
        this.currentFloorRoomsObserver.postValue(roomRepository.getRoomsByFloor(floor));
    }


    public MapPosition getCurrentPos(){
        return qNode.getCurrentNav().getCurrent();
    }

    public int getRobotIconId(){
        int iconId;
        switch (currentFloorObserver.getValue()){
            case 1:
                iconId = R.drawable.icon_kbot;
                break;
            case 2:
                iconId = R.drawable.icon_galtxa;
                break;
            case 3:
                iconId = R.drawable.icon_mari;
                break;
            default:
                iconId = R.drawable.icon_tartalo;
        }
        return iconId;
    }

    public void selectOrigin(int spinnerIndex) {
        this.origin = this.currentFloorRoomsObserver.getValue().get(spinnerIndex);
    }

    public void selectDestination(int spinnerIndex) {
        this.destination = this.currentFloorRoomsObserver.getValue().get(spinnerIndex);
    }

}
