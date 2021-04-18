package com.github.gidabotapp.domain;

public class Topic {
    private final String name;
    private final String type;
    private int sequenceNumber;

    public Topic(String name, String type){
        this.name = name;
        this.type = type;
        this.sequenceNumber = 0;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }

    public void add(){
        this.sequenceNumber++;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
