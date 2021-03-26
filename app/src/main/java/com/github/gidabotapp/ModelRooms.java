package com.github.gidabotapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModelRooms {
    private List<Room> roomList;


    public ModelRooms(){
        this.loadRooms();
    }

    public List<Room> getRooms (){
        return this.roomList;
    }

    public List<String> getRoomNames(){
        List<String> names = new ArrayList<>();
        for (Room r: roomList){
            names.add(r.getName());
        }
        return names;
    }

    // TODO: get rooms from room_names.xml
    private void loadRooms(){
        Room sarrera = new Room(0, 0, "Fakultateko sarrera nagusia", new MapPosition(3.5503,-18.4937,1.5708));
        Room atezaintza = new Room(0, 8, "Atezaintza", new MapPosition(0.0357,-11.6329,1.5708));
        Room kopistegia = new Room(0, 6, "Kopistegia", new MapPosition(-11.7704,-10.5290,3.1416));
        Room labo_01 = new Room(0, 9, "0.1 laborategia", new MapPosition(-4.5373,-0.4442,3.1416));

        this.roomList = Arrays.asList(sarrera,atezaintza,kopistegia,labo_01);
    }


}
