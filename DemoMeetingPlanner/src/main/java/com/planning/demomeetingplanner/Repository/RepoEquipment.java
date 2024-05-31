package com.planning.demomeetingplanner.Repository;

import com.planning.demomeetingplanner.Model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepoEquipment extends JpaRepository<Equipment,Integer> {

}
