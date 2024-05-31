package com.planning.demomeetingplanner.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.sql.Time;
import java.util.Date;

@Entity
@Data @NoArgsConstructor @AllArgsConstructor @Getter @Setter
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private Time startTime;
    private Time endTime;
    private Date meetingDate;
    private int numberOfpeople;
    private MeetingType meetingType;

}
