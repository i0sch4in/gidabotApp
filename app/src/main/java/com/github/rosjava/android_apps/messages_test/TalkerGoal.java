package com.github.rosjava.android_apps.messages_test;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageFactory;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import geometry_msgs.Point;
import multilevel_navigation_msgs.Goal;
import org.apache.commons.logging.Log;

public class TalkerGoal extends AbstractNodeMain {
    final String TOPIC_NAME = "multilevel_goal";
    final String TOPIC_TYPE = "multilevel_navigation_msgs/Goal";
    final String GRAPH_NAME = "GidabotApp/talkerGoal";

    public TalkerGoal() {
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of(GRAPH_NAME);
    }

    public void onStart(final ConnectedNode connectedNode) {
        final Publisher<multilevel_navigation_msgs.Goal> publisher = connectedNode.newPublisher(this.TOPIC_NAME, TOPIC_TYPE);
        publisher.setLatchMode(true);
        connectedNode.executeCancellableLoop(new CancellableLoop() {
            private int sequenceNumber;
            private long timeStart;
            private Log log;

            protected void setup() {
                final Log log = connectedNode.getLog();
                this.sequenceNumber = 0;
                this.timeStart = System.nanoTime();
                this.log = connectedNode.getLog();
            }

            protected void loop() throws InterruptedException {
                MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

                // FROM: 000 -> Hasiera
                // TO: 006 -> Kopistegia

                Goal message = topicMessageFactory.newFromType(Goal._TYPE);
                long now = System.nanoTime() - timeStart;

                message.setGoalSeq(sequenceNumber);
                message.setInitialFloor((float) 0.0);

                Point initial_pose = topicMessageFactory.newFromType(Point._TYPE);
                initial_pose.setX(3.72289156914);
                initial_pose.setY(-18.5215454102);
                initial_pose.setZ(0.0);
                message.setInitialPose(initial_pose);

                Point goal_pose = topicMessageFactory.newFromType(Point._TYPE);
                goal_pose.setX(-11.7704000473);
                goal_pose.setY(-10.5290002823);
                goal_pose.setZ(3.14159989357);
                message.setGoalPose(goal_pose);

                message.setIntermediateRobot(false);
                message.setIntermediateFloor((float)0.0);
                message.setWay("None");
                message.setStartId("000");
                message.setGoalId("006");
                message.setLanguage("EU");
                message.setUserName("");

                publisher.publish(message);
                ++this.sequenceNumber;
                if(sequenceNumber==1){
                    Thread.currentThread().join();
                    log.info("Thread stopped. sequenceNumber:" + sequenceNumber);
                }
                Thread.sleep(1000L);

            }
        });
    }
}
