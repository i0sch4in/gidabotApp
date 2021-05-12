package com.github.gidabotapp.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.github.gidabotapp.domain.MapPosition;
import com.github.gidabotapp.domain.Room;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Database(entities = {Room.class}, version = 2)
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
                            .addMigrations(MIGRATION_1_2)
                            .build();
                    Log.i("DAO", "Database instantiated");
                }
            }
        }
        return INSTANCE;
    }
    static final Migration MIGRATION_1_2 = new Migration(1,2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE new_Rooms(" +
                             "num TEXT NOT NULL, floor INTEGER NOT NULL," +
                             "name TEXT," +
                             "x REAL, y REAL, z REAL, PRIMARY KEY(num)) ");

            database.execSQL("INSERT INTO new_Rooms(num, floor, name, x, y, z) " +
                             "SELECT num, cast(floor as int), name, x, y, z FROM Rooms");

            database.execSQL("DROP TABLE Rooms");

            database.execSQL("ALTER TABLE new_Rooms RENAME TO Rooms");
        }
    };
}
