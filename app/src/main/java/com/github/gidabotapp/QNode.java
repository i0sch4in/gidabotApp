package com.github.gidabotapp;

import android.util.Log;

import org.ros.exception.RemoteException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.node.service.ServiceFactory;
import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import geometry_msgs.Point;
import geometry_msgs.PoseWithCovarianceStamped;
import multilevel_navigation_msgs.CancelRequest;
import multilevel_navigation_msgs.Goal;
import multilevel_navigation_msgs.PendingGoals;
import std_srvs.Empty;
import std_srvs.EmptyRequest;
import std_srvs.EmptyResponse;
import std_srvs.Trigger;

public class QNode extends AbstractNodeMain {
    final String GRAPH_NAME = "GidabotApp/QNode";
    private int sequenceNumber;
    private final long timeStart;

    Publisher<Goal> pubGoal;
    Topic goal = new Topic("/multilevel_goal", Goal._TYPE);
    Publisher<CancelRequest> pubCancel;
    Topic cancel = new Topic("/cancel_request", CancelRequest._TYPE);

    ServiceClient<EmptyRequest, EmptyResponse> clientClearCostmap;

    Subscriber<PoseWithCovarianceStamped> subPosition;
    Subscriber<PendingGoals> subPendingGoals;
    Subscriber<std_msgs.Int8> subNavPhase;

    ConnectedNode connectedNode;

    public QNode() {
        this.timeStart = System.nanoTime();
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of(GRAPH_NAME);
    }

    public void onStart(final ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;

        // Set Publishers
        pubGoal = connectedNode.newPublisher(goal.name, goal.type);
        pubGoal.setLatchMode(true);

        pubCancel = connectedNode.newPublisher(cancel.name, cancel.type);
        pubCancel.setLatchMode(true);

        try {
            clientClearCostmap = connectedNode.newServiceClient("/move_base/clear_costmaps", Empty._TYPE);
            Log.i("globalCostmap", "/move_base/clear_costmaps service client successfully created");
        } catch (ServiceNotFoundException e) {
            Log.e("globalCostmap", "Error creating /move_base/clear_costmaps service client");
            e.printStackTrace();
        }

    }

    // TODO: get current position and add it to the message
    public void publishGoal(){
        MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

        // Clear Global costmap
        this.clearGlobalCostmap();

        // FROM: 000 -> Hasiera
        // TO: 006 -> Kopistegia

        Goal message = topicMessageFactory.newFromType(Goal._TYPE);
        long now = System.nanoTime() - timeStart;

        message.setGoalSeq(goal.getSequenceNumber());
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

        pubGoal.publish(message);
        goal.add();
    }

    // TODO: get information from currentnav
    public void publishCancel(){
        MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

        CancelRequest message = topicMessageFactory.newFromType(cancel.type);

        pubCancel.publish(message);
    }

    public void clearGlobalCostmap(){
        MessageFactory requestMessageFactory = connectedNode.getServiceRequestMessageFactory();

        EmptyRequest empty_srv = requestMessageFactory.newFromType(Empty._TYPE);

        clientClearCostmap.call(empty_srv, new ServiceResponseListener<EmptyResponse>() {
            @Override
            public void onSuccess(EmptyResponse empty) {
                Log.i("globalCostmap", "globalCostmap successfully reset");
            }

            @Override
            public void onFailure(RemoteException e) {
                Log.e("globalCostmap", "globalCostmap reset failed");
            }
        });
    }
}
