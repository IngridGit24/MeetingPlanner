package com.planning.demomeetingplanner.Model;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data @NoArgsConstructor @AllArgsConstructor @Getter @Setter
public class Room {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private int capacity;
    private Time closeTime;
    private Time openTime;
    private boolean availability;

    @OneToMany(mappedBy = "room")
    private List<Equipment> equipment=new ArrayList<>();
}