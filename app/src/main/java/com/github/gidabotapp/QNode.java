package com.github.gidabotapp;

import android.util.Log;

import org.ros.exception.RemoteException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.message.MessageFactory;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.Locale;

import geometry_msgs.Point;
import geometry_msgs.PoseWithCovarianceStamped;
import multilevel_navigation_msgs.CancelRequest;
import multilevel_navigation_msgs.Goal;
import multilevel_navigation_msgs.PendingGoals;
import std_srvs.Empty;
import std_srvs.EmptyRequest;
import std_srvs.EmptyResponse;

public class QNode extends AbstractNodeMain {
    final String GRAPH_NAME = "GidabotApp/QNode";
    private final long timeStart;

    Publisher<Goal> pubGoal;
    // TODO: ez dakit beharrezkoak diren Topic-ak
    Topic goal = new Topic("/multilevel_goal", Goal._TYPE);
    Publisher<CancelRequest> pubCancel;
    Topic cancel = new Topic("/cancel_request", CancelRequest._TYPE);

    ServiceClient<EmptyRequest, EmptyResponse> clientClearCostmap;

    Subscriber<PoseWithCovarianceStamped> subPosition;
    Subscriber<PendingGoals> subPendingGoals;
    Subscriber<std_msgs.Int8> subNavPhase;

    ConnectedNode connectedNode;
    MapPosition currentPos;

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

        subPosition = connectedNode.newSubscriber("/tartalo/amcl_pose", PoseWithCovarianceStamped._TYPE);
        subPosition.addMessageListener(new MessageListener<PoseWithCovarianceStamped>() {
            @Override
            public void onNewMessage(PoseWithCovarianceStamped message) {
                currentPos = new MapPosition(message);
            }
        });

        try {
            clientClearCostmap = connectedNode.newServiceClient("/move_base/clear_costmaps", Empty._TYPE);
            Log.i("globalCostmap", "/move_base/clear_costmaps service client successfully created");
        } catch (ServiceNotFoundException e) {
            Log.e("globalCostmap", "Error creating /move_base/clear_costmaps service client");
            e.printStackTrace();
        }

    }

    // TODO: get current position and add it to the message
    // TODO: get position as parameter
    // TODO: kontrolatu helburua solairu berdinean dagoen (long-term)
    public void publishGoal(Room room){
        MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

        // Clear Global costmap
        this.clearGlobalCostmap();

        Goal message = topicMessageFactory.newFromType(Goal._TYPE);
        long now = System.nanoTime() - timeStart;

        message.setGoalSeq(goal.getSequenceNumber());
        message.setInitialFloor((float) 0.0); //osatzeko

        Point initial_pose = topicMessageFactory.newFromType(Point._TYPE);

        // etengabe eguneratzen dagoenez, uneko posizioaren "kopia" tenporala
        // QT interfazean --> azken initial_pose (ez oraingoa)
        //TODO: uneko posizioa --> hurbilen dagoen kokalekua
        MapPosition current = this.currentPos;
        initial_pose.setX(current.getX());
        initial_pose.setY(current.getY());
        initial_pose.setZ(current.getZ());
        message.setInitialPose(initial_pose);

        Point goal_pose = topicMessageFactory.newFromType(Point._TYPE);
        goal_pose.setX(room.getX());
        goal_pose.setY(room.getY());
        goal_pose.setZ(room.getZ());
        message.setGoalPose(goal_pose);

        message.setIntermediateRobot(false); //TODO
        message.setIntermediateFloor((float)0.0); //TODO
        message.setWay("None");
        message.setStartId("000"); //TODO
//        message.setGoalId(String.format(Locale.getDefault(),"%03d", room.getNum()));
        message.setGoalId(room.getNum());
        message.setLanguage("EU");
        message.setUserName("");

        pubGoal.publish(message);
        goal.add();
    }

    // TODO: get cancel request info from current position and request
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

    public MapPosition getCurrentPos(){
        return this.currentPos;
    }
}
