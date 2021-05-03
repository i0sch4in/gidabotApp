package com.github.gidabotapp.repository;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.room.Database;

import com.github.gidabotapp.domain.Floor;
import com.github.gidabotapp.domain.MapPosition;
import com.github.gidabotapp.domain.Room;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoomRepository {
    private final RoomRepositoryDAO roomDao;

    public RoomRepository(Context appContext){
        RoomDatabase db = RoomDatabase.getInstance(appContext);
        roomDao = db.roomRepositoryDAO();
    }

    public LiveData<List<Room>> getAllRooms(){
        return roomDao.getAllRooms();
    }

    public LiveData<List<Room>> getRoomsByFloor(Floor floor){
        return roomDao.getRoomsByFloor(floor.getFloorCode());
    }
}
