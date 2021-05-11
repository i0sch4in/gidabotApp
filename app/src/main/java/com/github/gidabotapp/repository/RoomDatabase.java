package com.github.gidabotapp.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.github.gidabotapp.domain.MapPosition;
import com.github.gidabotapp.domain.Room;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Database(entities = {Room.class}, version = 1)
public abstract class RoomDatabase extends androidx.room.RoomDatabase {
    private static final String db_name = "Rooms.db";
    private static RoomDatabase INSTANCE;
    public abstract RoomRepositoryDAO roomRepositoryDAO();

    public static RoomDatabase getInstance(final Context context){
        if(INSTANCE == null){
            synchronized (Room.class) {
                if(INSTANCE == null){
                    INSTANCE = androidx.room.Room.databaseBuilder(context,
                            RoomDatabase.class, db_name)
//                            .addCallback(populateDB)
                            .createFromAsset("database/"+db_name)
                            .build();
                    Log.i("DAO", "Database instantiated");
                }
            }
        }
        return INSTANCE;
    }

}
