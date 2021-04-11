package com.github.gidabotapp;

public class PhaseMessageSingleton {
    private static PhaseMessageSingleton INSTANCE;
    private message_enum current_msg;
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

    private PhaseMessageSingleton(){}

    public static PhaseMessageSingleton getInstance(){
        if(INSTANCE == null){
            INSTANCE = new PhaseMessageSingleton();
        }
        return INSTANCE;
    }

    public void setMessage(int i){
        if(i<message_enum.WAIT_NAVIGATION_END.ordinal() || i>message_enum.NAV_CANCELLED_BY_USER.ordinal())
        throw new IllegalStateException("Unexpected value: " + current_msg);
        current_msg = message_enum.values()[i];
    }

    public int getMessageId(){
        int messageId = -1;
        switch (current_msg){
            case WAIT_NAVIGATION_END:
                messageId = R.string.WAIT_NAVIGATION_END;
                break;
            case CONTINUE_GOAL:
                messageId = R.string.CONTINUE_GOAL;
                break;
            case GOAL_REACHED:
                messageId = R.string.GOAL_REACHED;
                break;
            case WAIT_ROBOT:
                messageId = R.string.WAIT_ROBOT;
                break;
            case GOAL_ERROR_TRY_AGAIN:
                messageId = R.string.GOAL_ERROR_TRY_AGAIN;
                break;
            case GOAL_ERROR_GO_TO_GOAL_FLOOR:
                messageId = R.string.GOAL_ERROR_GO_TO_GOAL_FLOOR;
                break;
            case GOAL_ERROR_CANNOT_MEET_USER:
                messageId = R.string.GOAL_ERROR_CANNOT_MEET_USER;
                break;
            case NAV_CANCELLED_OTHER_FLOOR:
                messageId = R.string.NAV_CANCELLED_OTHER_FLOOR;
                break;
            case NAV_REMOTELY_CANCELLED:
                messageId = R.string.NAV_REMOTELY_CANCELLED;
                break;
            case NAV_CANCELLED_BY_USER:
                messageId = R.string.NAV_CANCELLED_BY_USER;
                break;
        }
        return messageId;
    }
}
