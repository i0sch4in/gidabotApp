package com.github.rosjava.android_apps.messages_test;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.view.RosTextView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.PoseStamped;
import geometry_msgs.Quaternion;
import std_msgs.Header;

public class MainActivity extends RosActivity {
    private RosTextView<geometry_msgs.PoseStamped> rosTextViewTalker;

//    private TalkerPoseStamped talker;
    private TalkerGoal talker;
    private CurrentPosListener listener;

    public MainActivity() {
        // The RosActivity constructor configures the notification title and ticker
        // messages.
        super("Messages test", "Messages test");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // TALKER
        rosTextViewTalker = (RosTextView<geometry_msgs.PoseStamped>) findViewById(R.id.textTalker);
        rosTextViewTalker.setTopicName("move_base_simple/goal");
        rosTextViewTalker.setMessageType(geometry_msgs.PoseStamped._TYPE);
        rosTextViewTalker.setMessageToStringCallable(new MessageCallable<String, geometry_msgs.PoseStamped>() {
            @Override
            public String call(geometry_msgs.PoseStamped message) {
                Log.i("mezua", poseStampedToString(message));
                return poseStampedToString(message);
            }
        });
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
//        talker = new TalkerPoseStamped();
        talker = new TalkerGoal();
        listener = new CurrentPosListener(this);


        // At this point, the user has already been prompted to either enter the URI
        // of a master to use or to start a master locally.

        // The user can easily use the selected ROS Hostname in the master chooser
        // activity.

        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        Log.i("HostName", getRosHostname());
        //NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress().toString(), getMasterUri());
        nodeConfiguration.setMasterUri(getMasterUri());
        Log.i("MasterUri", getMasterUri().toString());

        // The RosTextView is also a NodeMain that must be executed in order to
        // start displaying incoming messages.

        // TALKER:
        nodeMainExecutor.execute(talker, nodeConfiguration);
//        nodeMainExecutor.execute(rosTextViewTalker, nodeConfiguration);

        // LISTENER:
        nodeMainExecutor.execute(listener, nodeConfiguration);
//        nodeMainExecutor.execute(rosTextViewListener, nodeConfiguration);

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
        Log.i("Toast","showToast()");
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}