package com.planning.demomeetingplanner.Service;

import com.planning.demomeetingplanner.Model.*;
import com.planning.demomeetingplanner.Repository.RepoMeeting;
import com.planning.demomeetingplanner.Repository.RepoRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.util.*;

@Service
public class ReservationService {

    private final RepoRoom repoRoom;
    private final RepoMeeting repoMeeting;

    @Autowired
    public ReservationService(RepoRoom repoRoom, RepoMeeting repoMeeting) {
        this.repoRoom = repoRoom;
        this.repoMeeting = repoMeeting;
    }

    //effectuer une reservation en fonction de la salle et de la réunion
    public boolean reserveRoom(Room room, Meeting meeting) {
        if (isRoomAvailable(room, meeting)) {
            room.setAvailability(false);
            room.setOpenTime(meeting.getEndTime());
            repoRoom.save(room);
            //supprime la réunion de la liste des demandes
            repoMeeting.delete(meeting);
            return true;
        } else {
            return false;
        }
    }

    //applique les différents critères pour voir si une salle est réservable
    private boolean isRoomAvailable(Room room, Meeting meeting) {
        // Vérifier si la salle est disponible pour le créneau horaire spécifié
        if (!isWeekend(meeting.getMeetingDate()) && isWithinWorkingHours(meeting.getStartTime(), meeting.getEndTime())) {
            // Vérifier si la salle est disponible pour ce créneau horaire en tenant compte de COVID
            if (isRoomAvailableForTimeSlot(room, meeting.getStartTime(), meeting.getEndTime())) {
                // Vérifier si la salle a les équipements nécessaires pour la réunion
                return hasRequiredEquipment(room, meeting.getMeetingType());
            }
        }
        return false;
    }

    //Vérifie si c'est le weekend
    private boolean isWeekend(Date date) {
        // Vérifier si la date est un week-end (samedi ou dimanche)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);
    }

    //Vérifie si l'heure de réservation est entre 8H et 20H
    private boolean isWithinWorkingHours(Time startTime, Time endTime) {
        // Vérifier si le créneau horaire est dans les heures de travail (8h-20h)
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startTime);
        int startHour = startCalendar.get(Calendar.HOUR_OF_DAY);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endTime);
        int endHour = endCalendar.get(Calendar.HOUR_OF_DAY);

        return (startHour >= 8 && endHour < 20);
    }

    //Vérifie si le créneau demandé et libre en tenant compte de Covid donc nettoyage.
    private boolean isRoomAvailableForTimeSlot(Room room, Time meetingStartTime, Time meetingEndTime) {
        //récupérer l'heure à laquelle la reunion commence
        // ce qui représente notre heure de réservation
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(meetingStartTime);
        int startHour = startCalendar.get(Calendar.HOUR_OF_DAY);

        //récupérer l'heure à laquelle la reunion termine
        // ce qui représente notre heure de réservation
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(meetingEndTime);
        int endHour = startCalendar.get(Calendar.HOUR_OF_DAY);

        Calendar availableCalendar = Calendar.getInstance();
        availableCalendar.setTime(room.getOpenTime());
        int availableHour = availableCalendar.get(Calendar.HOUR_OF_DAY);

        //comparer si l'heure demandé est supérieur à l'heure
        // disponible pour la salle + 1H à cause de COVID
        // et aussi si l'heure de fin de la réunion sera avant la fermeture
        return (startHour >= availableHour+1 && endHour < 20);
    }

    //Vérifie si la salle contient les équipements suivant le type de réunion
    private boolean hasRequiredEquipment(Room room, MeetingType type) {
        // Vérifier si la salle a les équipements nécessaires pour la réunion
        List<EquipmentType> requiredEquipments = getRequiredEquipments(type);
        List<Equipment> roomEquipments = room.getEquipment();

        // Convertir les équipements de la salle en types d'équipements pour la comparaison
        List<EquipmentType> roomEquipmentTypes = new ArrayList<>();
        for (Equipment roomEquipment : roomEquipments) {
            roomEquipmentTypes.add(roomEquipment.getEquipmentType());
        }

        // Vérifier si la salle contient tous les équipements requis
        return new HashSet<>(roomEquipmentTypes).containsAll(requiredEquipments);
    }

    //Renvoi la liste des équipements suivant le type de réunion
    private List<EquipmentType> getRequiredEquipments(MeetingType type) {
        // Récupérer la liste des équipements nécessaires pour chaque type de réunion
        List<EquipmentType> requiredEquipments = new ArrayList<>();

        // Ajouter les équipements requis en fonction du type de réunion
        switch (type) {
            case VC:
                requiredEquipments.add(EquipmentType.ECRAN);
                requiredEquipments.add(EquipmentType.PIEUVRE);
                requiredEquipments.add(EquipmentType.WEBCAM);
                break;
            case RC:
                requiredEquipments.add(EquipmentType.TABLEAU);
                requiredEquipments.add(EquipmentType.ECRAN);
                requiredEquipments.add(EquipmentType.PIEUVRE);
                break;
            case SPEC:
                requiredEquipments.add(EquipmentType.TABLEAU);
                break;
            case RS:
                // Pour les réunions simples, aucun équipement spécifique requis donc Néant
                requiredEquipments.add(EquipmentType.NEANT);
                break;
        }

        return requiredEquipments;
    }


}
