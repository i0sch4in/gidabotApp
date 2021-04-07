package com.github.gidabotapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

//import org.ros.android.RosActivity;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.PoseStamped;
import geometry_msgs.Quaternion;
import std_msgs.Header;

public class MainActivity extends RosActivity {
    private QNode qNode;
    NodeConfiguration nodeConfiguration;
    private RoomRepository modelRooms;
    private Room non;
    private Room nora;

    // TODO: strings.xml fitxategia erabili string-entzat
    // TODO: Intent = another activity -> MVVM pattern
    // TODO: beste solairuetan funtzionatzeko -> beste robotekn?
    public MainActivity() {
        // The RosActivity constructor configures the notification title and ticker
        // messages.
        super("GidabotApp", "GidabotApp");
    }


    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.qNode = QNode.getInstance();

        final Button publishBtn = findViewById(R.id.publishBtn);
        publishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (non != null && nora != null) {
                    qNode.publishGoal(modelRooms.getNearestRoom(qNode.currentPos),non);
                    qNode.publishGoal(modelRooms.getNearestRoom(qNode.currentPos),nora);
                    showToast("Ibilbidea zehaztuta:" + non.getName() + "-tik " + nora.getName() + "-ra.");
                }
                else{
                    showToast("Errorea: ez duzu zehaztu non zauden edo nora joan nahi duzun");
                }
            }
        });

        final Button cancelBtn = findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qNode.publishCancel();
                showToast("Goal cancelled");
            }
        });

        final Button CurrentPosBtn = findViewById(R.id.CurrentPosBtn);
        CurrentPosBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = modelRooms.getNearestRoom(qNode.currentPos).getName();
                showToast("Robota " + name + "-n dago");
            }
        });

        final Button navPhaseBtn = findViewById(R.id.navPhaseBtn);
        navPhaseBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showToast("Current nav phase: " + qNode.getNavPhase());
            }
        });

        final Button mapBtn = findViewById(R.id.mapBtn);
        mapBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RouteSelectActivity.class);
                startActivity(intent);
            }
        });

//        final String[] locationList = new String[]{"Sarrera", "Atezaintza", "Kopistegia", "0.1 laborategia"};
        try {
            modelRooms = new RoomRepository(this);
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
            Log.i("XMLParser", "error creating model");
        }
        final List<Room> roomList = modelRooms.getRooms();
        final ArrayAdapter<Room> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, roomList);

        final Spinner spinnerNon = findViewById(R.id.spinnerNon);
        spinnerNon.setAdapter(adapter);
        spinnerNon.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                non = roomList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        final Spinner spinnerNora = findViewById(R.id.spinnerNora);
        spinnerNora.setAdapter(adapter);
        spinnerNora.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                nora = roomList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {

        nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        Log.i("HostName", getRosHostname());

        nodeConfiguration.setMasterUri(getMasterUri());
        Log.i("MasterUri", getMasterUri().toString());

        nodeMainExecutor.execute(qNode, nodeConfiguration);

//        Intent intent = new Intent(this, RouteSelectActivity.class);
//        intent.putExtra("message", "mezua jaso dut");
//        startActivity(intent);
    }

    public void showToast(String text){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}