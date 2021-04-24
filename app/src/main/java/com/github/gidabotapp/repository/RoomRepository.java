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
    private static List<Room> roomList;
    private InputStream stream;
    private Context appContext;
    private List<Room> allRooms;
    private LiveData<List<Room>> currentFloorRooms;
    private final RoomRepositoryDAO roomDao;


    public RoomRepository(Context appContext){
        this.appContext = appContext;
        RoomDatabase db = RoomDatabase.getInstance(appContext);
        roomDao = db.roomRepositoryDAO();
    }

    public static List<Room> getRooms (){
        return roomList;
    }

    // TODO: get rooms from room_names.xml
    private void loadRooms() throws IOException, XmlPullParserException {
//        Room sarrera = new Room(0, 0, "Fakultateko sarrera nagusia", new MapPosition(3.5503,-18.4937,1.5708));
//        Room atezaintza = new Room(0, 8, "Atezaintza", new MapPosition(0.0357,-11.6329,1.5708));
//        Room kopistegia = new Room(0, 6, "Kopistegia", new MapPosition(-11.7704,-10.5290,3.1416));
//        Room labo_01 = new Room(0, 9, "0.1 laborategia", new MapPosition(-4.5373,-0.4442,3.1416));
//
//        this.roomList = Arrays.asList(sarrera,atezaintza,kopistegia,labo_01);
        List<Room> readRooms;
        final String fName = "room_names.xml";
        try {
            InputStream stream = appContext.getAssets().open(fName);
            RoomXmlParser roomXmlParser = new RoomXmlParser();
            readRooms = roomXmlParser.parse(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        this.roomList = readRooms;

    }

//    public List<Room> getRoomsByFloor(double floor){
//        List<Room> list = new ArrayList<>();
//        for (Room r : this.roomList) {
//            if (r.getFloor() == floor) {
//                list.add(r);
//            }
//        }
//        return list;
//    }

    public LiveData<List<Room>> getAllRooms(){
        return roomDao.getAllRooms();
    }

    public LiveData<List<Room>> getRoomsByFloor(Floor floor){
        return roomDao.getRoomsByFloor(floor.getFloorCode());
    }
}
