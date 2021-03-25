package com.github.gidabotapp;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageFactory;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.PoseStamped;
import geometry_msgs.Quaternion;
import std_msgs.Header;

public class TalkerPoseStamped extends AbstractNodeMain {
    final String topic_name = "move_base_simple/goal";

    public TalkerPoseStamped() {
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava_tutorial_pubsub/talker");
    }

    public void onStart(final ConnectedNode connectedNode) {
        final Publisher<geometry_msgs.PoseStamped> publisher = connectedNode.newPublisher(this.topic_name, "geometry_msgs/PoseStamped");
        connectedNode.executeCancellableLoop(new CancellableLoop() {
            private int sequenceNumber;
            private long timeStart;

            protected void setup() {
                this.sequenceNumber = 0;
                this.timeStart = System.nanoTime();
            }

            protected void loop() throws InterruptedException {
                MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

                PoseStamped message = topicMessageFactory.newFromType(PoseStamped._TYPE);
                long now = System.nanoTime() - timeStart;

                Header header = message.getHeader();
                header.setStamp(Time.fromNano(now));
                header.setFrameId("map");
                header.setSeq(sequenceNumber);

                Point position = topicMessageFactory.newFromType(Point._TYPE);
                position.setX(7.9);
                position.setY(-18.5);
                position.setZ(0.0);

                Quaternion orientation = topicMessageFactory.newFromType(Quaternion._TYPE);
                orientation.setX(0.0);
                orientation.setY(0.0);
                orientation.setZ(0.0);
                orientation.setW(1.0);

                Pose pose = topicMessageFactory.newFromType(Pose._TYPE);
                pose.setPosition(position);
                pose.setOrientation(orientation);

                message.setPose(pose);

                publisher.publish(message);
                ++this.sequenceNumber;
                Thread.sleep(1000L);
            }
        });
    }
}
