package com.github.gidabotapp;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.view.RosTextView;
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
    private Room selectedGoal;

    // TODO: aplikaziotik ateratzen bada, erroreak ematen ditu eta aplikazioa "hilda" geratzen da --> viewModel horretarako
    // TODO: strings.xml fitxategia erabili string-entzat
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

//        MainViewModel model = new ViewModelProvider(this).get(MainViewModel.class);
//        ViewModelProvider.Factory factory = new ViewModelProvider.NewInstanceFactory();
//        MainViewModel viewmodel = new ViewModelProvider(this, factory).get(MainViewModel.class);

        final Button publishBtn = findViewById(R.id.publishBtn);
        publishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qNode.publishGoal(selectedGoal);
                showToast("Goal published");
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
                String name = qNode.getCurrentPos();
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

//        final String[] locationList = new String[]{"Sarrera", "Atezaintza", "Kopistegia", "0.1 laborategia"};
        try {
            modelRooms = new RoomRepository(this);
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
            Log.i("XMLParser", "error creating model");
        }
        final List<Room> locationList = modelRooms.getRooms();
        List<String> locationNames = modelRooms.getRoomNames();
        final ListView listview = findViewById(R.id.listView);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.list_layout, locationNames);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.setSelected(true);
                selectedGoal = locationList.get(position);
//                showToast("Selected goal: " + selectedGoal.getName());
            }
        });

    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
//        talker = new TalkerPoseStamped();
        qNode = new QNode(modelRooms);

        // At this point, the user has already been prompted to either enter the URI
        // of a master to use or to start a master locally.

        // The user can easily use the selected ROS Hostname in the master chooser
        // activity.

        nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        Log.i("HostName", getRosHostname());
        //NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress().toString(), getMasterUri());
        nodeConfiguration.setMasterUri(getMasterUri());
        Log.i("MasterUri", getMasterUri().toString());

        // The RosTextView is also a NodeMain that must be executed in order to
        // start displaying incoming messages.

        // TALKER:
        nodeMainExecutor.execute(qNode, nodeConfiguration);
//        nodeMainExecutor.execute(rosTextViewTalker, nodeConfiguration);


    }

    public String poseStampedToString(PoseStamped poseStamped) {
        Header header = poseStamped.getHeader();
        Pose pose = poseStamped.getPose();
        Point position = pose.getPosition();
        Quaternion orientation = pose.getOrientation();

        String str =
                "header: " + header.getSeq() + ", " + header.getStamp() + ", " + header.getFrameId() + System.lineSeparator();
        str += "pose: " + System.lineSeparator();
        str += "position: " + position.getX() + ", " + position.getY() + ", " + position.getZ() + System.lineSeparator();
        str += "orientation: " + orientation.getX() + ", " + orientation.getY() + ", " + orientation.getZ() + ", " + orientation.getW() + ", " + System.lineSeparator();

        return str;
    }

    public void showToast(String text){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}