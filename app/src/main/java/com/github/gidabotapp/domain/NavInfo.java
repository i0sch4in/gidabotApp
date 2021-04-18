package com.github.gidabotapp.domain;

public class NavInfo {
    private int goalSeq;
    private MapPosition start;
    private MapPosition goal;
    private MapPosition current;
    private int role;
    private boolean intermediateRobot;
    private double intermediateFloor;
    private boolean goalRobotWorking;
    private boolean goalRobotError;
    private boolean isIntermediateRobotError;
    private NavPhase navPhase;

    public NavInfo(){
        this.goalSeq = 0;

    }

    public int getGoalSeq() {
        return goalSeq;
    }

    public void setGoalSeq(int goalSeq) {
        this.goalSeq = goalSeq;
    }

    public MapPosition getStart() {
        return start;
    }

    public void setStart(MapPosition start) {
        this.start = start;
    }

    public MapPosition getGoal() {
        return goal;
    }

    public void setGoal(MapPosition goal) {
        this.goal = goal;
    }

    public MapPosition getCurrent() {
        return current;
    }

    public void setCurrent(MapPosition current) {
        this.current = current;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public boolean isIntermediateRobot() {
        return intermediateRobot;
    }

    public void setIntermediateRobot(boolean intermediateRobot) {
        this.intermediateRobot = intermediateRobot;
    }

    public double getIntermediateFloor() {
        return intermediateFloor;
    }

    public void setIntermediateFloor(double intermediateFloor) {
        this.intermediateFloor = intermediateFloor;
    }

    public boolean isGoalRobotWorking() {
        return goalRobotWorking;
    }

    public void setGoalRobotWorking(boolean goalRobotWorking) {
        this.goalRobotWorking = goalRobotWorking;
    }

    public boolean isGoalRobotError() {
        return goalRobotError;
    }

    public void setGoalRobotError(boolean goalRobotError) {
        this.goalRobotError = goalRobotError;
    }

    public boolean isIntermediateRobotError() {
        return isIntermediateRobotError;
    }

    public void setIntermediateRobotError(boolean intermediateRobotError) {
        isIntermediateRobotError = intermediateRobotError;
    }

    public NavPhase getNavPhase() {
        return navPhase;
    }

    public void setNavPhase(NavPhase navPhase) {
        this.navPhase = navPhase;
    }
}
