package com.github.gidabotapp;

import android.content.Context;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RoomRepository {
    private List<Room> roomList;
    private InputStream stream;
    private Context context;


    public RoomRepository(Context context) throws IOException, XmlPullParserException {
//        this.stream = stream;
        this.context = context;
        this.loadRooms();
    }

    public List<Room> getRooms (){
        return this.roomList;
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
            InputStream stream = context.getAssets().open(fName);
            RoomXmlParser roomXmlParser = new RoomXmlParser();
            readRooms = roomXmlParser.parse(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        this.roomList = readRooms;
    }

    public List<Room> getFirstFloorRooms(){
        List<Room> list = new ArrayList<>();
        for(Room r: this.roomList){
            if(r.getFloor() == 0){
                list.add(r);
            }
        }
        return list;
    }

    public Room getNearestRoom(MapPosition current){
        List<Room> rooms = getFirstFloorRooms();

        // get first element of the roomlist
        Room nearestRoom = rooms.get(0);
        double nearestDistance = current.dSquare(nearestRoom.getPosition());

        // iterate through other elements
        for(Room r: rooms.subList(1,rooms.size())){
            MapPosition pos = r.getPosition();
            double distance = pos.dSquare(current);
            if(distance < nearestDistance){
                nearestDistance = distance;
                nearestRoom = r;
            }
        }
        return nearestRoom;
    }

    public Room getRoomByIndex(int i){
        return this.roomList.get(i);
    }
}
