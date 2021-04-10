package com.github.gidabotapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import org.apache.commons.lang.mutable.Mutable;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

public class MapViewModel extends AndroidViewModel {
    private static QNode qNode;
    private RoomRepository roomRepository;
    private MutableLiveData<String> toastMessageObserver;
    private MutableLiveData<Integer> currentFloorObserver;
    private MutableLiveData<List<Room>> currentFloorRoomsObserver;
    final int DEFAULT_FLOOR = 1;


    public MapViewModel(@NonNull Application application) {
        super(application);
        qNode = QNode.getInstance();
        try {
            roomRepository = new RoomRepository(application.getApplicationContext());
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
        this.currentFloorObserver = new MutableLiveData<>(DEFAULT_FLOOR);
        this.toastMessageObserver = new MutableLiveData<>();
        this.currentFloorRoomsObserver = new MutableLiveData<>();
    }

    // TODO: Should not use context (leaks) --> locale change
    public void publishGoals(Room non, Room nora) {
        String message;
        if (non == null || nora == null) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_empty);
        } else if (non.equals(nora)) {
            message = getApplication().getApplicationContext().getString(R.string.publish_error_msg_same);
        } else {
            Room nearest = roomRepository.getNearestRoom(qNode.getCurrentNav().getCurrent());
            if (!nearest.equals(non)) {
                qNode.publishGoal(nearest, non);
            }
            qNode.publishGoal(non, nora);
            message = String.format(getApplication().getApplicationContext().getString(R.string.publish_success_msg), non, nora);
        }
        toastMessageObserver.postValue(message);
    }


    public void publishCancel() {
        qNode.publishCancel();
        toastMessageObserver.postValue(getApplication().getApplicationContext().getString(R.string.cancel_msg));
    }

    public MutableLiveData<String> getToastMessageObserver() {
        return toastMessageObserver;
    }

    public MutableLiveData<List<Room>> getCurrentFloorRoomsObserver() {
        return this.currentFloorRoomsObserver;
    }

    public MutableLiveData<Integer> getCurrentFloorObserver() {
        return this.currentFloorObserver;
    }

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
}
