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
//    private static final int NUMBER_OF_THREADS = 4;
//    static final ExecutorService databaseWriteExecutor =
//            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
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

//    private static final androidx.room.RoomDatabase.Callback populateDB = new androidx.room.RoomDatabase.Callback(){
//        @Override
//        public void onCreate(@NonNull SupportSQLiteDatabase db) {
//            super.onCreate(db);
////        public void onOpen(@NonNull SupportSQLiteDatabase db) {
////            super.onOpen(db);
//
//            databaseWriteExecutor.execute(new Runnable() {
//                @Override
//                public void run() {
//                    Log.i("DAO", "callback called");
//                    RoomRepositoryDAO dao = INSTANCE.roomRepositoryDAO();
//                    Room sarrera = new Room(0, "000", "Fakultateko sarrera nagusia", new MapPosition(3.5503,-18.4937,1.5708));
//                    Room atezaintza = new Room(0, "008", "Atezaintza", new MapPosition(0.0357,-11.6329,1.5708));
//                    Room kopistegia = new Room(0, "006", "Kopistegia", new MapPosition(-11.7704,-10.5290,3.1416));
//                    Room labo_01 = new Room(0, "009", "0.1 laborategia", new MapPosition(-4.5373,-0.4442,3.1416));
//                    Room proba = new Room(0, "100", "proba", new MapPosition(-4.5373,-0.4442,3.1416));
//
////                    dao.insertAll(Arrays.asList(sarrera,atezaintza,kopistegia,labo_01));
//                    dao.insertAll(RoomRepository.getRooms());
//                }
//            });
//        }
//    };
}
