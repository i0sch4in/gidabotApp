package com.github.gidabotapp;

public class Topic {
    final String name;
    final String type;
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
}
