package com.planning.demomeetingplanner.Service;

import com.planning.demomeetingplanner.Exception.EquipmentNotFound;
import com.planning.demomeetingplanner.Exception.RoomNotAvailable;
import com.planning.demomeetingplanner.Model.*;
import com.planning.demomeetingplanner.Repository.RepoMeeting;
import com.planning.demomeetingplanner.Repository.RepoRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.util.*;
import java.util.stream.Collectors;

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

    // Vérifie si c'est le weekend
    private boolean isWeekend(Date date) {
        // Récupérer le jour de la semaine
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        // Vérifier si la date est un week-end (samedi ou dimanche)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            throw new RoomNotAvailable("La date est un week-end. Nous ne reservons les weekends!");
        }
        return false; // La date n'est pas un week-end
    }

    // Vérifie si l'heure de réservation est entre 8H et 20H
    private boolean isWithinWorkingHours(Time startTime, Time endTime) {
        // Récupérer l'heure de début
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startTime);
        int startHour = startCalendar.get(Calendar.HOUR_OF_DAY);

        // Récupérer l'heure de fin
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endTime);
        int endHour = endCalendar.get(Calendar.HOUR_OF_DAY);

        // Vérifier si le créneau horaire est dans les heures de travail (8h-20h)
        if (!(startHour >= 8 && endHour < 20)) {
            throw new RoomNotAvailable("L'heure de réservation doit être entre 8h et 20h.");
        }
        return true; // L'heure de réservation est dans les heures de travail
    }

    // Vérifie si la salle est disponible pour le créneau horaire spécifié
    private boolean isRoomAvailableForTimeSlot(Room room, Time meetingStartTime, Time meetingEndTime) {
        // Récupérer l'heure à laquelle la réunion commence
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(meetingStartTime);
        int startHour = startCalendar.get(Calendar.HOUR_OF_DAY);

        // Récupérer l'heure à laquelle la réunion se termine
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(meetingEndTime);
        int endHour = endCalendar.get(Calendar.HOUR_OF_DAY);

        // Récupérer l'heure d'ouverture de la salle
        Calendar availableCalendar = Calendar.getInstance();
        availableCalendar.setTime(room.getOpenTime());
        int availableHour = availableCalendar.get(Calendar.HOUR_OF_DAY);

        // Comparer si l'heure demandée est supérieure à l'heure disponible pour la salle + 1 heure
        // à cause de COVID, et aussi si l'heure de fin de la réunion sera avant la fermeture
        if (!(startHour >= availableHour + 1 && endHour < 20)) {
            throw new RoomNotAvailable("La salle n'est pas disponible pour ce créneau horaire.");
        }
        return true; // La salle est disponible pour le créneau horaire spécifié
    }

    // Vérifie si la salle contient les équipements suivant le type de réunion
    private boolean hasRequiredEquipment(Room room, MeetingType type) {
        // Vérifier si la salle a les équipements nécessaires pour la réunion
        List<EquipmentType> requiredEquipments = getRequiredEquipments(type);
        List<Equipment> roomEquipments = room.getEquipment();

        // Convertir les équipements de la salle en types d'équipements pour la comparaison
        List<EquipmentType> roomEquipmentTypes = roomEquipments.stream()
                .map(Equipment::getEquipmentType)
                .toList();

        // Vérifier si la salle contient tous les équipements requis
        if (!new HashSet<>(roomEquipmentTypes).containsAll(requiredEquipments)) {
            throw new EquipmentNotFound("La salle ne possède pas tous les équipements requis pour une réunion de type " + type);
        }

        // Si tous les équipements requis sont présents, retourner true
        return true;
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
