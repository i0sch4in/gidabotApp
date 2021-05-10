package com.github.gidabotapp.repository;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.github.gidabotapp.domain.Floor;
import com.github.gidabotapp.domain.MapPosition;
import com.github.gidabotapp.domain.MultiNavPhase;
import com.github.gidabotapp.domain.NavInfo;
import com.github.gidabotapp.domain.PhaseMessage;
import com.github.gidabotapp.domain.Room;
import com.github.gidabotapp.domain.Way;

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

import java.util.HashMap;
import java.util.List;
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

    private Publisher<Goal> pubGoal;
    private Publisher<CancelRequest> pubCancel;

    private ServiceClient<EmptyRequest, EmptyResponse> clearCostmapClient;

    private HashMap<Floor, Subscriber<PoseWithCovarianceStamped>> positionSubs;
    private HashMap<Floor, Subscriber<PendingGoals>> pendingReqSubs;
    private Subscriber<std_msgs.Int8> subNavPhase;
    private Subscriber<std_msgs.Int8> subDialogMessage;

    private final HashMap<Floor, MutableLiveData<MapPosition>> currentPositions;
    private final HashMap<Floor, MutableLiveData<List<Goal>>> pendingRequests;
    private final MutableLiveData<PhaseMessage> phaseMessageLD;
    private final MutableLiveData<MultiNavPhase> multiNavPhaseLD;

    private ConnectedNode connectedNode;

    private final String userId;

    private final NavInfo currentNav;

    private int sequenceNumber;

    private QNode() {
        this.currentNav = new NavInfo();
        this.userId = UUID.randomUUID().toString().substring(0,4);
        this.sequenceNumber = 0;
        this.currentPositions = new HashMap<Floor, MutableLiveData<MapPosition>>(){{
            for(Floor floor: Floor.values()){
                put(floor, new MutableLiveData<MapPosition>());
            }
        }};
        this.pendingRequests = new HashMap<Floor, MutableLiveData<List<Goal>>>(){{
            for(Floor floor: Floor.values()){
                put(floor, new MutableLiveData<List<Goal>>());
            }
        }};
        this.phaseMessageLD = new MutableLiveData<>();
        this.multiNavPhaseLD = new MutableLiveData<>();
    }

    public static synchronized QNode getInstance(){
        if(INSTANCE == null){
            INSTANCE = new QNode();
        }
        return INSTANCE;
    }

    public GraphName getDefaultNodeName() {
        String GRAPH_NAME_BASE = "GidabotApp/QNode_";
        return GraphName.of(GRAPH_NAME_BASE + userId);
    }

    public void onStart(final ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;

        pubGoal = connectedNode.newPublisher("/multilevel_goal", Goal._TYPE);
        pubGoal.setLatchMode(true);

        pubCancel = connectedNode.newPublisher("/cancel_request", CancelRequest._TYPE);
        pubCancel.setLatchMode(true);

        positionSubs = new HashMap<>();
        final String amcl_topic_template = "/%s/amcl_pose";
        for(final Floor floor: Floor.values()){
            String topic = String.format(amcl_topic_template, floor.getRobotName());
            Subscriber<PoseWithCovarianceStamped> subscriber = connectedNode.newSubscriber(topic, PoseWithCovarianceStamped._TYPE);
            subscriber.addMessageListener(new MessageListener<PoseWithCovarianceStamped>() {
                @Override
                public void onNewMessage(PoseWithCovarianceStamped message) {
                    MapPosition position = new MapPosition(message);
                    currentPositions.get(floor).postValue(position);
                }
            });
            positionSubs.put(floor,subscriber);
        }

        pendingReqSubs = new HashMap<>();
        final String pReq_topic_template = "/%s/pending_requests";
        for(final Floor floor: Floor.values()){
            String topic = String.format(pReq_topic_template, floor.getRobotName());
            Subscriber<PendingGoals> subscriber = connectedNode.newSubscriber(topic, PendingGoals._TYPE);
            subscriber.addMessageListener(new MessageListener<PendingGoals>() {
                @Override
                public void onNewMessage(PendingGoals message) {
                    List<Goal> pendingGoals = message.getGoals();
                    pendingRequests.get(floor).postValue(pendingGoals);
                }
            });
            pendingReqSubs.put(floor,subscriber);
        }


        subNavPhase = connectedNode.newSubscriber("/nav_phase", Int8._TYPE);
        subNavPhase.addMessageListener(new MessageListener<Int8>() {
            @Override
            public void onNewMessage(Int8 message) {
                int i = message.getData();
                MultiNavPhase currentNavPhase = MultiNavPhase.values()[i];
                multiNavPhaseLD.postValue(currentNavPhase);
            }
        });

        subDialogMessage = connectedNode.newSubscriber("/dialog_qt_message", Int8._TYPE);
        subDialogMessage.addMessageListener(new MessageListener<Int8>() {
            @Override
            public void onNewMessage(Int8 message) {
                int i = message.getData();
                PhaseMessage currentPhaseMessage = new PhaseMessage(i);
                phaseMessageLD.postValue(currentPhaseMessage);
            }
        });


        try {
            clearCostmapClient = connectedNode.newServiceClient("/move_base/clear_costmaps", Empty._TYPE);
            Log.i("globalCostmap", "/move_base/clear_costmaps service client successfully created");
        } catch (ServiceNotFoundException e) {
            Log.e("globalCostmap", "Error creating /move_base/clear_costmaps service client");
            e.printStackTrace();
        }

    }


    public void publishGoal(Room current, Room goal, Way chosenWay){
        MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

        // Clear Global costmap
        clearGlobalCostmap();

        Goal message = topicMessageFactory.newFromType(Goal._TYPE);

        message.setGoalSeq(sequenceNumber);
        message.setInitialFloor((float) current.getFloor());
        message.setGoalFloor((float) goal.getFloor());

        Point initial_pose = topicMessageFactory.newFromType(Point._TYPE);

        initial_pose.setX(current.getX());
        initial_pose.setY(current.getY());
        initial_pose.setZ(current.getZ());
        message.setInitialPose(initial_pose);


        Point goal_pose = topicMessageFactory.newFromType(Point._TYPE);
        goal_pose.setX(goal.getX());
        goal_pose.setY(goal.getY());
        goal_pose.setZ(goal.getZ());
        message.setGoalPose(goal_pose);

        message.setIntermediateRobot(false); //TODO
        message.setIntermediateFloor((float)0.0); //TODO
        String way = "";
        if (chosenWay != null){
           way = chosenWay.toString();
        }
        message.setWay(way);
        message.setStartId(current.getNum());
        message.setGoalId(goal.getNum());
        message.setLanguage("EU");
        message.setUserName(this.userId);

        pubGoal.publish(message);
        this.sequenceNumber++;
    }


    public void publishCancel(int goal_seq, boolean intermediateRobot, Floor initialFloor, Floor goalFloor, Floor intermediateFloor){
        MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

        CancelRequest message = topicMessageFactory.newFromType(CancelRequest._TYPE);
        message.setGoalSeq(goal_seq);
        message.setIntermediateRobot(intermediateRobot);
        message.setInitialFloor((float) initialFloor.getFloorCode());
        message.setGoalFloor((float) goalFloor.getFloorCode());
        message.setRequestFloor((float) initialFloor.getFloorCode()); // IMPORTANTE: hau gabe ez doa
        if (intermediateRobot){
            message.setIntermediateFloor((float) intermediateFloor.getFloorCode());
        }

        pubCancel.publish(message);
    }



    public void clearGlobalCostmap(){
        MessageFactory requestMessageFactory = connectedNode.getServiceRequestMessageFactory();

        EmptyRequest empty_srv = requestMessageFactory.newFromType(Empty._TYPE);

        try {
            clearCostmapClient.call(empty_srv, new ServiceResponseListener<EmptyResponse>() {
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
        catch(NullPointerException e){
            e.printStackTrace();
        }
    }


    public MutableLiveData<PhaseMessage> getPhaseMessageLD(){
        return this.phaseMessageLD;
    }


    public MutableLiveData<MultiNavPhase> getMultiNavPhaseLD(){
        return this.multiNavPhaseLD;
    }


    public HashMap<Floor, MutableLiveData<MapPosition>> getCurrentPositions(){
        return this.currentPositions;
    }

    public HashMap<Floor, MutableLiveData<List<Goal>>> getPendingRequests(){
        return this.pendingRequests;
    }

    public NavInfo getCurrentNav(){
        return this.currentNav;
    }

    public String getUserId(){
        return this.userId;
    }

    public void shutdown() {
        try {
            for(Subscriber<PoseWithCovarianceStamped> sub: positionSubs.values()){
                sub.shutdown();
            }
            for(Subscriber<PendingGoals> sub: pendingReqSubs.values()){
                sub.shutdown();
            }

            subDialogMessage.shutdown();
            subNavPhase.shutdown();

            pubCancel.shutdown();
            pubGoal.shutdown();

            clearCostmapClient.shutdown();

            connectedNode.shutdown();
        }
        finally {
            INSTANCE = null;
        }

    }

}