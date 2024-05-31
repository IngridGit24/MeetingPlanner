package com.planning.demomeetingplanner.Repository;

import com.planning.demomeetingplanner.Model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepoRoom extends JpaRepository<Room,Integer> {

}
