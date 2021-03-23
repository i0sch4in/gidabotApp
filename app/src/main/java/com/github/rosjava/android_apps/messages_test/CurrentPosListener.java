package com.github.rosjava.android_apps.messages_test;

//import org.apache.commons.logging.Log;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

import geometry_msgs.Point;
import geometry_msgs.PoseWithCovarianceStamped;

public class CurrentPosListener extends AbstractNodeMain {
    final String TOPIC_NAME = "/tartalo/amcl_pose";
    final String TOPIC_TYPE = "geometry_msgs/PoseWithCovarianceStamped";
    private geometry_msgs.PoseWithCovarianceStamped position;
    private MainActivity mainActivity;


    public CurrentPosListener(MainActivity activity) {
        this.mainActivity = activity;
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava_tutorial_pubsub/listener");
    }

    public void onStart(ConnectedNode connectedNode) {
//        final Log log = connectedNode.getLog();
        Subscriber<geometry_msgs.PoseWithCovarianceStamped> subscriber = connectedNode.newSubscriber(TOPIC_NAME, TOPIC_TYPE);
        subscriber.addMessageListener(new MessageListener<geometry_msgs.PoseWithCovarianceStamped>() {
            public void onNewMessage(geometry_msgs.PoseWithCovarianceStamped message) {
//                log.info("I heard: \"" + message.getData() + "\"");
                Point position = message.getPose().getPose().getPosition();
                String text = "X:" + position.getX() + ", Y:" + position.getY() + ", Z:" + position.getZ();
//                mainActivity.showToast(text);
            }
        });
    }

    public PoseWithCovarianceStamped getPosition(){
        return this.position;
    }
}
