package com.github.gidabotapp.domain;

import com.github.gidabotapp.R;

public enum Way {
    LIFT(R.string.lift),
    STAIRS(R.string.stairs)
    ;

    private final int resourceId;

    Way(int resId){
        this.resourceId = resId;
    }

    public int getResourceId() {
        return resourceId;
    }


    public String toString() {
        String name = this.name();
        return name.substring(0,1) + name.substring(1).toLowerCase();
    }
}
