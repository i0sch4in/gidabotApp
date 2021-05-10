package com.github.gidabotapp.repository;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.github.gidabotapp.domain.Room;

import java.util.List;

@Dao
public interface RoomRepositoryDAO {
    @Insert
    void insertAll(List<Room> rooms);

    @Insert
    void insert(Room room);

    @Query("SELECT * FROM rooms WHERE num = :num")
    LiveData<Room> getRoom(double num);

    @Query("SELECT * FROM rooms WHERE floor = :floor")
    LiveData<List<Room>> getRoomsByFloor(double floor);

    @Query("SELECT * FROM rooms")
    LiveData<List<Room>> getAllRooms();
}
