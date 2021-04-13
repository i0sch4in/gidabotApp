package com.github.gidabotapp;

import android.util.Log;

import org.ros.exception.RemoteException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.message.MessageFactory;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.UUID;

import geometry_msgs.Point;
import geometry_msgs.PoseWithCovarianceStamped;
import multilevel_navigation_msgs.CancelRequest;
import multilevel_navigation_msgs.Goal;
import multilevel_navigation_msgs.PendingGoals;
import std_msgs.Int8;
import std_srvs.Empty;
import std_srvs.EmptyRequest;
import std_srvs.EmptyResponse;

public class QNode extends AbstractNodeMain {
    private static QNode INSTANCE = null;

    final String GRAPH_NAME_BASE = "GidabotApp/QNode_";
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
    Subscriber<std_msgs.Int8> subDialogMessage;

    ConnectedNode connectedNode;

    private final String userId;

    private final NavInfo currentNav;

    private QNode() {
        this.timeStart = System.nanoTime();
        this.currentNav = new NavInfo();
        this.userId = UUID.randomUUID().toString().substring(0,4);
    }

    public static synchronized QNode getInstance(){
        if(INSTANCE == null){
            INSTANCE = new QNode();
        }
        return INSTANCE;
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of(GRAPH_NAME_BASE + userId);
    }

    public void onStart(final ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;

        pubGoal = connectedNode.newPublisher(goal.name, goal.type);
        pubGoal.setLatchMode(true);

        pubCancel = connectedNode.newPublisher(cancel.name, cancel.type);
        pubCancel.setLatchMode(true);

        subPosition = connectedNode.newSubscriber("/tartalo/amcl_pose", PoseWithCovarianceStamped._TYPE);
//        subPosition.addMessageListener(new MessageListener<PoseWithCovarianceStamped>() {
//            @Override
//            public void onNewMessage(PoseWithCovarianceStamped message) {
//                currentNav.setCurrent(new MapPosition(message));
//            }
//        });

        subNavPhase = connectedNode.newSubscriber("/nav_phase", Int8._TYPE);
//        subNavPhase.addMessageListener(new MessageListener<Int8>() {
//            @Override
//            public void onNewMessage(Int8 message) {
//                int i = message.getData();
//                currentNav.setPhase(Phase.values()[i]);
//            }
//        });

        subDialogMessage = connectedNode.newSubscriber("/dialog_qt_message", Int8._TYPE);

        subPendingGoals = connectedNode.newSubscriber("tartalo/pending_requests", PendingGoals._TYPE);

        try {
            clientClearCostmap = connectedNode.newServiceClient("/move_base/clear_costmaps", Empty._TYPE);
            Log.i("globalCostmap", "/move_base/clear_costmaps service client successfully created");
        } catch (ServiceNotFoundException e) {
            Log.e("globalCostmap", "Error creating /move_base/clear_costmaps service client");
            e.printStackTrace();
        }

    }
//
//    public void onShutdown(Node node){
//        super.onShutdown(node);
//        Log.e("QNode", "QNode shut down");
//    }
//    public void onShutdownComplete(Node node) {
//        super.onShutdownComplete(node);
//        Log.e("QNode", "QNode shut down complete");
//    }
//
//    public void onError(Node node, Throwable throwable) {
//        super.onError(node, throwable);
//        Log.e("QNode", "QNode error");
//    }


    public void publishGoal(Room current, Room room){
        MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

        // Clear Global costmap
        clearGlobalCostmap();

        Goal message = topicMessageFactory.newFromType(Goal._TYPE);
        long now = System.nanoTime() - timeStart;

        message.setGoalSeq(goal.getSequenceNumber());
        message.setInitialFloor((float) 0.0); //osatzeko

        Point initial_pose = topicMessageFactory.newFromType(Point._TYPE);

        // etengabe eguneratzen dagoenez, uneko posizioaren "kopia" tenporala
        // QT interfazean --> azken initial_pose (ez oraingoa)
        //TODO: uneko posizioa --> hurbilen dagoen kokalekua
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
        message.setStartId(current.getNum()); //TODO
//        message.setGoalId(String.format(Locale.getDefault(),"%03d", room.getNum()));
        message.setGoalId(room.getNum());
        message.setLanguage("EU");
        message.setUserName(this.userId);

        pubGoal.publish(message);
        goal.add();
    }


    public void publishCancel(int goal_seq, boolean intermediateRobot, double...floors){
        MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

        CancelRequest message = topicMessageFactory.newFromType(cancel.type);
        message.setGoalSeq(goal_seq);
        message.setInitialFloor((float) floors[0]);
        message.setGoalFloor((float) floors[1]);
        message.setIntermediateRobot(intermediateRobot);
        if (intermediateRobot){
            message.setIntermediateFloor((float) floors[2]);
            message.setRequestFloor((float) floors[3]);
        }


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

    public void setPhaseMsgListener(MessageListener<Int8> listener){
        subDialogMessage.addMessageListener(listener);
        Log.i("listener", "phase message listener set");
    }

    public void setNavPhaseListener(MessageListener<Int8> listener){
        subNavPhase.addMessageListener(listener);
        Log.i("listener", "nav phase listener set");
    }

    public void setPositionListener(MessageListener<PoseWithCovarianceStamped> listener){
        subPosition.addMessageListener(listener);
    }

    public void setPendingGoalsListener(MessageListener<PendingGoals> listener){
        subPendingGoals.addMessageListener(listener);
    }

    public NavInfo getCurrentNav(){
        return this.currentNav;
    }

    public String getUserId(){
        return this.userId;
    }

}
