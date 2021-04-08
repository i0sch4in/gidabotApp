package com.github.gidabotapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public class MainActivity extends RosActivity {
    private QNode qNode;
    NodeConfiguration nodeConfiguration;

    // TODO: strings.xml fitxategia erabili string-entzat
    // TODO: Intent = another activity -> MVVM pattern
    // TODO: beste solairuetan funtzionatzeko -> beste robotekn?
    public MainActivity() {
        super("GidabotApp", "GidabotApp");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.qNode = QNode.getInstance();

        final Button mapBtn = findViewById(R.id.mapBtn);
        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RouteSelectActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {

        nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        Log.i("HostName", getRosHostname());

        nodeConfiguration.setMasterUri(getMasterUri());
        Log.i("MasterUri", getMasterUri().toString());

        nodeMainExecutor.execute(qNode, nodeConfiguration);

    }

}