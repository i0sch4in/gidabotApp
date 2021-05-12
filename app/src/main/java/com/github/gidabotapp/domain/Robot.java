package com.github.gidabotapp.domain;


import com.github.gidabotapp.R;

public enum Robot{
    // StartPoint => 000 - Fakultateko Sarrera Nagusia
    TARTALO ("Tartalo",     R.drawable.tartalo_small,3.5503, -18.4937),

    // StartPoint => 122 - Dekanotza
    KBOT    ("Kbot",        R.drawable.kbot_small,  -16.2700, 4.42812),

    // StartPoint => Ezkerreko eskailerak (2nd floor)
    GALTXA  ("Galtxagorri", R.drawable.galtxa_small, 5.2744, -35.9580),

    // StartPoint => 302 - DCI, Egokituz
    MARI    ("Marisorgin",  R.drawable.mari_small,  -0.3069, -8.7494)
    ;

    protected String longName;
    protected int iconResId;
    protected double start_x, start_y;

    Robot(String longName, int iconResId, double start_x, double start_y){
        this.longName = longName;
        this.iconResId = iconResId;
        this.start_x = start_x;
        this.start_y = start_y;
    }

    protected String getShortName(){
        return this.name().toLowerCase();
    }
}