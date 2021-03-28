package com.github.gidabotapp;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RoomXmlParser {
    private static final String ns = null;

    public List<Room> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readRooms(parser);
        } finally {
            in.close();
        }
    }

    private List<Room> readRooms(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Room> rooms = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, "rooms");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("room")) {
                rooms.add(readRoom(parser));
            } else {
                skip(parser);
            }
        }
        return rooms;
    }

    private Room readRoom(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "room");
        double floor = 0;
        String num = null;
        String name = null;
        double x = 0.0, y = 0.0, yaw = 0.0;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String elName = parser.getName();
            switch (elName) {
                case "floor":
                    floor = readFloor(parser);
                    break;
                case "num":
                    num = readText(parser);
                    break;
                case "name":
                    name = readName(parser);
                    break;
                case "x":
                    x = readX(parser);
                    break;
                case "y":
                    y = readY(parser);
                    break;
                case "yaw":
                    yaw = readYaw(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return new Room(floor, num, name, new MapPosition(x, y, yaw));
    }

    private Double readFloor(XmlPullParser parser) throws IOException, XmlPullParserException {
        double floor;
        parser.require(XmlPullParser.START_TAG, ns, "floor");
        floor = readDouble(parser);
        parser.require(XmlPullParser.END_TAG, ns, "floor");
        return floor;
    }

    private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
        String name;
        parser.require(XmlPullParser.START_TAG, ns, "name");
        name = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "name");
        return name;
    }

    private Double readX(XmlPullParser parser) throws IOException, XmlPullParserException {
        double x;
        parser.require(XmlPullParser.START_TAG, ns, "x");
        x = readDouble(parser);
        parser.require(XmlPullParser.END_TAG, ns, "x");
        return x;
    }

    private Double readY(XmlPullParser parser) throws IOException, XmlPullParserException {
        double y;
        parser.require(XmlPullParser.START_TAG, ns, "y");
        y = readDouble(parser);
        parser.require(XmlPullParser.END_TAG, ns, "y");
        return y;
    }

    private Double readYaw(XmlPullParser parser) throws IOException, XmlPullParserException {
        double yaw;
        parser.require(XmlPullParser.START_TAG, ns, "yaw");
        yaw = readDouble(parser);
        parser.require(XmlPullParser.END_TAG, ns, "yaw");
        return yaw;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private int readInt(XmlPullParser parser) throws IOException, XmlPullParserException {
        int result = 0;
        if (parser.next() == XmlPullParser.TEXT) {
            result = Integer.parseInt(parser.getText());
            parser.nextTag();
        }
        return result;
    }

    private double readDouble(XmlPullParser parser) throws IOException, XmlPullParserException {
        double result = 0.0;
        if (parser.next() == XmlPullParser.TEXT) {
            result = Double.parseDouble(parser.getText());
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
