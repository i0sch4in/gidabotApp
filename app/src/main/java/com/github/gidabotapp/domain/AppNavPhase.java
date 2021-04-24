package com.github.gidabotapp.domain;

public enum AppNavPhase {
    WAIT_USER_INPUT,
    REACHING_ORIGIN,
    REACHING_DESTINATION
    ;

    private static AppNavPhase[] vals = values();
    public AppNavPhase next()
    {
        return vals[(this.ordinal()+1) % vals.length];
    }
}
