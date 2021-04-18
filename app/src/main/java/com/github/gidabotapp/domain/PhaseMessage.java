package com.github.gidabotapp.domain;

import com.github.gidabotapp.R;

public class PhaseMessage {
    private message_enum msg;
    private int msgResId;
    private enum message_enum {
        WAIT_NAVIGATION_END,
        CONTINUE_GOAL,
        GOAL_REACHED,
        WAIT_ROBOT,
        GOAL_ERROR_TRY_AGAIN,
        GOAL_ERROR_GO_TO_GOAL_FLOOR,
        GOAL_ERROR_CANNOT_MEET_USER,
        NAV_CANCELLED_OTHER_FLOOR,
        NAV_REMOTELY_CANCELLED,
        NAV_CANCELLED_BY_USER,
    }

    // pre: 0 <= i <= 9
    public PhaseMessage(int i){
        if(i<message_enum.WAIT_NAVIGATION_END.ordinal() || i>message_enum.NAV_CANCELLED_BY_USER.ordinal())
            throw new IllegalStateException("Unexpected value: " + msg);
        msg = message_enum.values()[i];
        switch (msg){
            case WAIT_NAVIGATION_END:
                msgResId = R.string.WAIT_NAVIGATION_END;
                break;
            case CONTINUE_GOAL:
                msgResId = R.string.CONTINUE_GOAL;
                break;
            case GOAL_REACHED:
                msgResId = R.string.GOAL_REACHED;
                break;
            case WAIT_ROBOT:
                msgResId = R.string.WAIT_ROBOT;
                break;
            case GOAL_ERROR_TRY_AGAIN:
                msgResId = R.string.GOAL_ERROR_TRY_AGAIN;
                break;
            case GOAL_ERROR_GO_TO_GOAL_FLOOR:
                msgResId = R.string.GOAL_ERROR_GO_TO_GOAL_FLOOR;
                break;
            case GOAL_ERROR_CANNOT_MEET_USER:
                msgResId = R.string.GOAL_ERROR_CANNOT_MEET_USER;
                break;
            case NAV_CANCELLED_OTHER_FLOOR:
                msgResId = R.string.NAV_CANCELLED_OTHER_FLOOR;
                break;
            case NAV_REMOTELY_CANCELLED:
                msgResId = R.string.NAV_REMOTELY_CANCELLED;
                break;
            case NAV_CANCELLED_BY_USER:
                msgResId = R.string.NAV_CANCELLED_BY_USER;
                break;
        }
    }

    public int getMessageId(){
        return msgResId;
    }
}
