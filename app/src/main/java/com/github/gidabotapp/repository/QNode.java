package com.github.gidabotapp.repository;

import android.os.Message;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.github.gidabotapp.domain.Floor;
import com.github.gidabotapp.domain.MapPosition;
import com.github.gidabotapp.domain.MultiNavPhase;
import com.github.gidabotapp.domain.NavInfo;
import com.github.gidabotapp.domain.PhaseMessage;
import com.github.gidabotapp.domain.Room;

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

import static com.github.gidabotapp.domain.Floor.FIRST_FLOOR;
import static com.github.gidabotapp.domain.Floor.SECOND_FLOOR;
import static com.github.gidabotapp.domain.Floor.THIRD_FLOOR;
import static com.github.gidabotapp.domain.Floor.ZEROTH_FLOOR;

public class QNode extends AbstractNodeMain {
    private static QNode INSTANCE = null;

    private Publisher<Goal> pubGoal;
    private Publisher<CancelRequest> pubCancel;

    private ServiceClient<EmptyRequest, EmptyResponse> clientClearCostmap;

    private Subscriber<PoseWithCovarianceStamped> subTartaloPos;
    private Subscriber<PoseWithCovarianceStamped> subKbotPos;
    private Subscriber<PoseWithCovarianceStamped> subGaltxaPos;
    private Subscriber<PoseWithCovarianceStamped> subMariPos;
    private Subscriber<PendingGoals> subPendingGoals;
    private Subscriber<std_msgs.Int8> subNavPhase;
    private Subscriber<std_msgs.Int8> subDialogMessage;

    private final HashMap<Floor, MutableLiveData<MapPosition>> currentPositions;
    private final MutableLiveData<PhaseMessage> phaseMessageLD;
    private final MutableLiveData<MultiNavPhase> multiNavPhaseLD;
    private final MutableLiveData<List<Goal>> pendingGoalsLD;


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
        this.phaseMessageLD = new MutableLiveData<>();
        this.multiNavPhaseLD = new MutableLiveData<>();
        this.pendingGoalsLD = new MutableLiveData<>();
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

        subTartaloPos = connectedNode.newSubscriber("/tartalo/amcl_pose", PoseWithCovarianceStamped._TYPE);
        subTartaloPos.addMessageListener(new MessageListener<PoseWithCovarianceStamped>() {
            @Override
            public void onNewMessage(PoseWithCovarianceStamped message) {
                MapPosition position = new MapPosition(message);
                currentPositions.get(ZEROTH_FLOOR).postValue(position);
            }
        });

        subKbotPos = connectedNode.newSubscriber("/kbot/amcl_pose", PoseWithCovarianceStamped._TYPE);
        subKbotPos.addMessageListener(new MessageListener<PoseWithCovarianceStamped>() {
            @Override
            public void onNewMessage(PoseWithCovarianceStamped message) {
                MapPosition position = new MapPosition(message);
                currentPositions.get(FIRST_FLOOR).postValue(position);
            }
        });

        subGaltxaPos = connectedNode.newSubscriber("/galtxa/amcl_pose", PoseWithCovarianceStamped._TYPE);
        subGaltxaPos.addMessageListener(new MessageListener<PoseWithCovarianceStamped>() {
            @Override
            public void onNewMessage(PoseWithCovarianceStamped message) {
                MapPosition position = new MapPosition(message);
                currentPositions.get(SECOND_FLOOR).postValue(position);
            }
        });

        subMariPos = connectedNode.newSubscriber("/mari/amcl_pose", PoseWithCovarianceStamped._TYPE);
        subMariPos.addMessageListener(new MessageListener<PoseWithCovarianceStamped>() {
            @Override
            public void onNewMessage(PoseWithCovarianceStamped message) {
                MapPosition position = new MapPosition(message);
                currentPositions.get(THIRD_FLOOR).postValue(position);
            }
        });

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
        subPendingGoals = connectedNode.newSubscriber("tartalo/pending_requests", PendingGoals._TYPE);
        subPendingGoals.addMessageListener(new MessageListener<PendingGoals>() {
            @Override
            public void onNewMessage(PendingGoals message) {
                List<Goal> pendingGoals = message.getGoals();
                pendingGoalsLD.postValue(pendingGoals);
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


    public void publishGoal(Room current, Room goal){
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
        message.setWay("None");
        message.setStartId(current.getNum());
        message.setGoalId(goal.getNum());
        message.setLanguage("EU");
        message.setUserName(this.userId);

        pubGoal.publish(message);
        this.sequenceNumber++;
    }


    public void publishCancel(int goal_seq, boolean intermediateRobot, double...floors){
        MessageFactory topicMessageFactory = connectedNode.getTopicMessageFactory();

        CancelRequest message = topicMessageFactory.newFromType(CancelRequest._TYPE);
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

        try {
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

    public MutableLiveData<List<Goal>> getPendingGoalsLD(){
        return this.pendingGoalsLD;
    }

    public NavInfo getCurrentNav(){
        return this.currentNav;
    }

    public String getUserId(){
        return this.userId;
    }

    public void shutdown() {
        try {
            subTartaloPos.shutdown();
            subKbotPos.shutdown();
            subGaltxaPos.shutdown();
            subMariPos.shutdown();

            subDialogMessage.shutdown();
            subNavPhase.shutdown();
            subPendingGoals.shutdown();

            pubCancel.shutdown();
            pubGoal.shutdown();

            clientClearCostmap.shutdown();

            connectedNode.shutdown();
        }
        finally {
            INSTANCE = null;
        }

    }

}