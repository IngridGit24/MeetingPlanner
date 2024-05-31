package com.planning.demomeetingplanner.Repository;

import com.planning.demomeetingplanner.Model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepoMeeting extends JpaRepository<Meeting,Integer> {
}
