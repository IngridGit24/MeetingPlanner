package com.planning.demomeetingplanner.Exception;

public class RoomNotAvailable extends RuntimeException{
    public RoomNotAvailable(String message) {
        super(message);
    }
}
