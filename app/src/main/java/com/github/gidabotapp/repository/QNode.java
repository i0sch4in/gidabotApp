package com.github.gidabotapp.repository;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.github.gidabotapp.data.MultiNavPhase;
import com.github.gidabotapp.data.PhaseMessage;
import com.github.gidabotapp.domain.Floor;
import com.github.gidabotapp.domain.MapPosition;
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
    // Singleton pattern instance
    private static QNode INSTANCE = null;

    // Node publishers
    private Publisher<Goal> pubGoal;
    private Publisher<CancelRequest> pubCancel;

    // Service to clear Global Costmap
    private ServiceClient<EmptyRequest, EmptyResponse> clearCostmapClient;

    // Node subscribers
    private HashMap<Floor, Subscriber<PoseWithCovarianceStamped>> positionSubHM;
    private HashMap<Floor, Subscriber<PendingGoals>> pendingReqSubHM;
    private Subscriber<std_msgs.Int8> subNavPhase;
    private Subscriber<std_msgs.Int8> subDialogMessage;

    // All MutableLiveData to expose ROS system's current data
    private final HashMap<Floor, MutableLiveData<MapPosition>> currentPositionsHM;
    private final HashMap<Floor, MutableLiveData<List<Goal>>> pendingRequestsHM;
    private final MutableLiveData<PhaseMessage> phaseMessageLD;
    private final MutableLiveData<MultiNavPhase> multiNavPhaseLD;

    // ConnectedNode will be injected through MainActivity (RosActivity)
    private ConnectedNode connectedNode;

    // User Id will allow to differentiate possible different users in the system
    private final String userId;

    // Will increment with each goal sent
    private int goalSequenceNumber;

    // Singleton pattern, private constructor
    private QNode() {
        // Get random UUID and set its first four characters as current instance's userID
        this.userId = UUID.randomUUID().toString().substring(0,4);
        this.goalSequenceNumber = 0;
        // Initialize current position's LiveData HashMap
        this.currentPositionsHM = new HashMap<Floor, MutableLiveData<MapPosition>>(){{
            for(Floor floor: Floor.values()){
                put(floor, new MutableLiveData<MapPosition>());
            }
        }};
        // Initialize pending requests' LiveData HashMap
        this.pendingRequestsHM = new HashMap<Floor, MutableLiveData<List<Goal>>>(){{
            for(Floor floor: Floor.values()){
                put(floor, new MutableLiveData<List<Goal>>());
            }
        }};
        this.phaseMessageLD = new MutableLiveData<>();
        this.multiNavPhaseLD = new MutableLiveData<>();
    }

    // Singleton lazy initialization
    public static synchronized QNode getInstance(){
        if(INSTANCE == null){
            INSTANCE = new QNode();
        }
        return INSTANCE;
    }

    // Node's name will be base name + current instance's userId
    // This will allow to differentiate different users' nodes
    public GraphName getDefaultNodeName() {
        String GRAPH_NAME_BASE = "GidabotApp/QNode_";
        return GraphName.of(GRAPH_NAME_BASE + userId);
    }

    // onStart event will initialize all publishers and subscribers needed
    // ConnectedNode will be the the node received from nodeMainExecutor
    public void onStart(final ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;

        pubGoal = connectedNode.newPublisher("/multilevel_goal", Goal._TYPE);
        pubGoal.setLatchMode(true);

        pubCancel = connectedNode.newPublisher("/cancel_request", CancelRequest._TYPE);
        pubCancel.setLatchMode(true);

        // Initialize position subscribers' HashMap with a subscriber for each Floor (and hence, each Robot)
        positionSubHM = new HashMap<Floor, Subscriber<PoseWithCovarianceStamped>>(){{
            final String amcl_topic_template = "/%s/amcl_pose";
            for(final Floor floor: Floor.values()) {
                String topic = String.format(amcl_topic_template, floor.getRobotNameShort());
                Subscriber<PoseWithCovarianceStamped> subscriber = connectedNode.newSubscriber(topic, PoseWithCovarianceStamped._TYPE);
                subscriber.addMessageListener(new MessageListener<PoseWithCovarianceStamped>() {
                    @Override
                    public void onNewMessage(PoseWithCovarianceStamped message) {
                        MapPosition position = new MapPosition(message);
                        currentPositionsHM.get(floor).postValue(position);
                    }
                });
                put(floor, subscriber);
            }
        }};

        // Initialize pending requests' subscribers' HashMap with a subscriber for each Floor (and hence, each Robot)
        pendingReqSubHM = new HashMap<Floor, Subscriber<PendingGoals>>(){{
            final String pReq_topic_template = "/%s/pending_requests";
            for(final Floor floor: Floor.values()){
                String topic = String.format(pReq_topic_template, floor.getRobotNameShort());
                Subscriber<PendingGoals> subscriber = connectedNode.newSubscriber(topic, PendingGoals._TYPE);
                subscriber.addMessageListener(new MessageListener<PendingGoals>() {
                    @Override
                    public void onNewMessage(PendingGoals message) {
                        List<Goal> pendingGoals = message.getGoals();
                        pendingRequestsHM.get(floor).postValue(pendingGoals);
                    }
                });
                put(floor,subscriber);
            }
        }};


        // Nav Phase subscriber connection
        subNavPhase = connectedNode.newSubscriber("/nav_phase", Int8._TYPE);
        subNavPhase.addMessageListener(new MessageListener<Int8>() {
            @Override
            public void onNewMessage(Int8 message) {
                int i = message.getData();
                MultiNavPhase currentNavPhase = MultiNavPhase.values()[i];
                multiNavPhaseLD.postValue(currentNavPhase);
            }
        });

        // Dialog QT message subscriber connection
        subDialogMessage = connectedNode.newSubscriber("/dialog_qt_message", Int8._TYPE);
        subDialogMessage.addMessageListener(new MessageListener<Int8>() {
            @Override
            public void onNewMessage(Int8 message) {
                int i = message.getData();
                PhaseMessage currentPhaseMessage = PhaseMessage.values()[i];
                phaseMessageLD.postValue(currentPhaseMessage);
            }
        });


        try {
            clearCostmapClient = connectedNode.newServiceClient("/move_base/clear_costmaps", Empty._TYPE);
        } catch (ServiceNotFoundException e) {
            e.printStackTrace();
        }

    }

    // Publishes a goal, posting currentRoom, goal and chosen way as information
    // for the robot to make the correct path
    public void publishGoal(Room current, Room goal, Way chosenWay){
        MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

        // Clear Global costmap. This clears current stored obstacles, to prevent errors, such as the robot getting stuck
        clearGlobalCostmap();

        Goal message = topicMessageFactory.newFromType(Goal._TYPE);
        message.setGoalSeq(goalSequenceNumber);
        message.setInitialFloor((float) current.getFloor());
        message.setGoalFloor((float) goal.getFloor());

        // Initial pose will be current Room's mapPosition coordinates
        Point initial_pose = topicMessageFactory.newFromType(Point._TYPE);
        initial_pose.setX(current.getX());
        initial_pose.setY(current.getY());
        initial_pose.setZ(current.getZ());
        message.setInitialPose(initial_pose);

        // Goal pose will be current Room's mapPosition coordinates
        Point goal_pose = topicMessageFactory.newFromType(Point._TYPE);
        goal_pose.setX(goal.getX());
        goal_pose.setY(goal.getY());
        goal_pose.setZ(goal.getZ());
        message.setGoalPose(goal_pose);

        message.setIntermediateRobot(false); //TODO
        message.setIntermediateFloor((float)0.0); //TODO

        // If not null, way will be chosenWay argument
        String way = "";
        if (chosenWay != null){
           way = chosenWay.toString();
        }
        message.setWay(way);
        message.setStartId(current.getNum());
        message.setGoalId(goal.getNum());
        message.setLanguage("EU");
        message.setUserName(this.userId);

        // Publish built message and increment sequenceNumber
        pubGoal.publish(message);
        this.goalSequenceNumber++;
    }

    // Publishes a cancel request message
    public void publishCancel(int goal_seq, boolean intermediateRobot, Floor initialFloor, Floor goalFloor, Floor intermediateFloor){
        MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

        CancelRequest message = topicMessageFactory.newFromType(CancelRequest._TYPE);
        message.setGoalSeq(goal_seq);
        message.setIntermediateRobot(intermediateRobot);
        message.setInitialFloor((float) initialFloor.getFloorCode());
        message.setGoalFloor((float) goalFloor.getFloorCode());
        message.setRequestFloor((float) initialFloor.getFloorCode()); // !! Important, if not set correctly, it won't work
        if (intermediateRobot){
            message.setIntermediateFloor((float) intermediateFloor.getFloorCode());
        }

        pubCancel.publish(message);
    }


    // Clears global costmap, sending an empty request to clear costmap topic
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

    // Shuts down every powered subscriber and listener
    // Finally sets instance to null
    public void shutdown() {
        try {
            for(Subscriber<PoseWithCovarianceStamped> sub: positionSubHM.values()){
                sub.shutdown();
            }
            for(Subscriber<PendingGoals> sub: pendingReqSubHM.values()){
                sub.shutdown();
            }

            subDialogMessage.shutdown();
            subNavPhase.shutdown();

            pubCancel.shutdown();
            pubGoal.shutdown();

            clearCostmapClient.shutdown();

            connectedNode.shutdown();
        }
        catch (Exception ignored){}
        finally {
            INSTANCE = null;
        }

    }

    // Getters
    public MutableLiveData<PhaseMessage> getPhaseMessageLD(){
        return this.phaseMessageLD;
    }
    public MutableLiveData<MultiNavPhase> getMultiNavPhaseLD(){
        return this.multiNavPhaseLD;
    }
    public HashMap<Floor, MutableLiveData<MapPosition>> getCurrentPositionsHM(){
        return this.currentPositionsHM;
    }
    public HashMap<Floor, MutableLiveData<List<Goal>>> getPendingRequestsHM(){
        return this.pendingRequestsHM;
    }
    public String getUserId(){
        return this.userId;
    }

}