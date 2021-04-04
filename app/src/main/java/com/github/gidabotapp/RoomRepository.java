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

    public List<String> getRoomNames(){
        List<String> names = new ArrayList<>();
        for (Room r: roomList){
            names.add(r.getNum() + " - " + r.getName());
        }
        return names;
    }

    public List<String> getFirstFloorRoomNames(){
        List<String> names = new ArrayList<>();
        for (Room r: roomList){
            if(r.getFloor() == 0) {
                names.add(r.getNum() + " - " + r.getName());
            }
        }
        return names;
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
}
