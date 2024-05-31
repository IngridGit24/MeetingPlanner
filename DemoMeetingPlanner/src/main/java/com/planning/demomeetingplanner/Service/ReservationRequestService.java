package com.planning.demomeetingplanner.Service;

import com.planning.demomeetingplanner.Model.Meeting;
import com.planning.demomeetingplanner.Model.Room;

public class ReservationRequestService {

    private Room room;
    private Meeting meeting;

    //Getter and Setter
    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }
}
