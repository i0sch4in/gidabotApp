package com.github.gidabotapp.data;

import com.github.gidabotapp.R;

public enum PhaseMessage {
        WAIT_NAVIGATION_END(R.string.WAIT_NAVIGATION_END),
        CONTINUE_GOAL(R.string.CONTINUE_GOAL),
        GOAL_REACHED(R.string.GOAL_REACHED),
        WAIT_ROBOT(R.string.WAIT_ROBOT),
        GOAL_ERROR_TRY_AGAIN(R.string.GOAL_ERROR_TRY_AGAIN),
        GOAL_ERROR_GO_TO_GOAL_FLOOR(R.string.GOAL_ERROR_GO_TO_GOAL_FLOOR),
        GOAL_ERROR_CANNOT_MEET_USER(R.string.GOAL_ERROR_CANNOT_MEET_USER),
        NAV_CANCELLED_OTHER_FLOOR(R.string.NAV_CANCELLED_OTHER_FLOOR),
        NAV_REMOTELY_CANCELLED(R.string.NAV_REMOTELY_CANCELLED),
        NAV_CANCELLED_BY_USER(R.string.NAV_CANCELLED_BY_USER),
        ;

    private final int msgResId;

    PhaseMessage(int i){
        this.msgResId = i;
    }

    public int getMessageResId(){
        return msgResId;
    }
}
