package com.github.gidabotapp.domain;

import com.github.gidabotapp.R;

public enum Way {
    LIFT("Lift", R.string.lift),
    STAIRS("Stairs",R.string.stairs)
    ;

    private final String name;
    private final int resourceId;

    Way(String name, int resId){
        this.name = name;
        this.resourceId = resId;
    }

    public int getResourceId() {
        return resourceId;
    }


    public String toString() {
        return name;
    }
}
