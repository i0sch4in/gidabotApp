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
    @Query("SELECT * FROM rooms WHERE floor = :floor")
    LiveData<List<Room>> getRoomsByFloor(double floor);

    @Query("SELECT * FROM rooms")
    LiveData<List<Room>> getAllRooms();
}
