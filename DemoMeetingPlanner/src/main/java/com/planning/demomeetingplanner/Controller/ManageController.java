package com.planning.demomeetingplanner.Controller;

import com.planning.demomeetingplanner.Exception.EmptyObjectList;
import com.planning.demomeetingplanner.Exception.MissingArgument;
import com.planning.demomeetingplanner.Exception.NotFoundObject;
import com.planning.demomeetingplanner.Model.Equipment;
import com.planning.demomeetingplanner.Model.Meeting;
import com.planning.demomeetingplanner.Model.Room;
import com.planning.demomeetingplanner.Repository.RepoEquipment;
import com.planning.demomeetingplanner.Repository.RepoMeeting;
import com.planning.demomeetingplanner.Repository.RepoRoom;
import com.planning.demomeetingplanner.Service.ReservationRequestService;
import com.planning.demomeetingplanner.Service.ReservationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Time;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ManageController {

    public static final Time defaultOpenTime = Time.valueOf("08:00:00");
    public static final Time defaultCloseTime = Time.valueOf("20:00:00");

    @Autowired
    public RepoRoom repoRoom;

    @Autowired
    public RepoMeeting repoMeeting;

    @Autowired
    public RepoEquipment repoEquipment;

    private final ReservationService reservationService;
    @Autowired
    public ManageController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }


    //let's start with Room operations CRUD
    @PostMapping("/addRoom") //methode pour ajouter une nouvelle salle de réunion
    public Boolean addRoom(@RequestBody Room room){
        try{
            if(room!=null){
                //si la salle récupérée n'est pas null, on s'assure de mettre les valeurs par default
                room.setAvailability(true);
                room.setCloseTime(defaultOpenTime);
                room.setCloseTime(defaultCloseTime);
                repoRoom.save(room);//saved Before to asign equipments
                //puis on sauvegarde les équipements
                List<Equipment> equipments = room.getEquipment();
                if (equipments != null) {
                    for(Equipment e:equipments){
                        e.setRoom(room);
                        repoEquipment.save(e);
                    }
                }
                //sauvegarde la salle définitivement
                repoRoom.saveAndFlush(room);
                return true;
            } else {
                throw new MissingArgument("La variable Room ne peut pas être null !");
            }
        }catch (MissingArgument e){
            System.out.println("Une erreur s'est produite : " + e.getMessage());
            throw e;
        }
    }

    @GetMapping("/allRoom")//cette methode retourne toutes les salles
    public ResponseEntity<?> displayAllRoom() {
        try {
            List<Room> rooms = repoRoom.findAll();
            if (rooms.isEmpty()) {
                // Si la liste est vide, lever exception et le code d'état approprié
                throw new EmptyObjectList("La liste des salles de réunion est vide.");
            } else {
                // Si la liste n'est pas vide, retournez la liste des salles
                return ResponseEntity.ok(rooms);
            }
        } catch (EmptyObjectList e) {
            // Si la liste est vide, retournez un ResponseEntity avec une exception et le code d'état approprié
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            // Si une autre erreur se produit, retournez le message d'erreur et le code d'état approprié
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur s'est produite : " + e.getMessage());
        }
    }

    @DeleteMapping("/deleteRoom/{id}")//méthode pour modifier les infos d'une salle
    public ResponseEntity<String> deleteMeetingRoom(@PathVariable Integer id) {
        try {
            Optional<Room> existingRoomOptional = repoRoom.findById(id);
            if (existingRoomOptional.isPresent()) {
                Room existingRoom = existingRoomOptional.get();

                // Supprimer tous les équipements associés à cette salle de réunion
                List<Equipment> equipments = existingRoom.getEquipment();
                for(Equipment e: equipments){
                    repoEquipment.deleteById(e.getId());
                }

                // Supprimer la salle de réunion elle-même
                repoRoom.deleteById(id);

                // Retourner un message de succès
                return ResponseEntity.ok("L'élément avec l'ID " + id + " a été supprimé avec succès");
            } else {
                // Si la salle de réunion n'existe pas, retournez un message d'erreur
                throw new NotFoundObject("La salle de réunion avec l'ID " + id + " n'existe pas");
            }
        } catch (Exception e) {
            String errorMessage = "Une erreur s'est produite : " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }

    @PutMapping("/updateRoom/{id}")
    public ResponseEntity<?> updateMeetingRoom(@RequestBody Room roomUpdated, @PathVariable Integer id) {
        try {
            Optional<Room> existingRoomOptional = repoRoom.findById(id);
            if (existingRoomOptional.isPresent()) {
                //récupérer la salle à modifier en s'assurant encore que réellement elle n'est pas null
                Room existingRoom = existingRoomOptional.get();

                // Supprimer les anciens équipements associés à la salle de réunion existante
                List<Equipment> existingEquipments = existingRoom.getEquipment();
                repoEquipment.deleteAll(existingEquipments);

                // Associer les nouveaux équipements à la salle de réunion mise à jour
                List<Equipment> updatedEquipments = roomUpdated.getEquipment();
                if (updatedEquipments != null) {
                    for (Equipment equipment : updatedEquipments) {
                        equipment.setRoom(existingRoom);
                    }
                    // Enregistrer les nouveaux équipements associés à la salle de réunion mise à jour
                    repoEquipment.saveAllAndFlush(updatedEquipments);
                }

                // Mettre à jour les autres champs de la salle de réunion sans toucher à l'ID
                BeanUtils.copyProperties(roomUpdated, existingRoom, "id");

                // Sauvegarder la salle de réunion mise à jour
                repoRoom.saveAndFlush(existingRoom);
                return ResponseEntity.ok("l'élément avec l'ID"+ id +" bien été modifié");
            } else {
                // Si la salle de réunion avec l'ID donné n'existe pas, retournez une exception et le statut HTTP approprié
                throw new NotFoundObject("La salle de réunion avec l'ID " + id + " n'existe pas");
            }
        } catch (NotFoundObject e) {
            // Si la salle de réunion avec l'ID donné n'existe pas, retournez exception et le statut HTTP approprié
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            // Si une autre erreur se produit, retournez le message d'erreur et le statut HTTP approprié
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur s'est produite : " + e.getMessage());
        }
    }


    //Now let's make some methods for Meeting CRUD
    @PostMapping("/addMeetingOrder")
    public Boolean meetingOrder(@RequestBody Meeting meeting){
        try{
            if(meeting!=null){
                repoMeeting.save(meeting);
                return true;
            }else {
                throw new MissingArgument("La variable meeting ne peut pas etre null !!");
            }
        }catch (MissingArgument e){
            System.out.println("Une erreur s'est produite : " + e.getMessage());
            throw e;
        }
    }

    @GetMapping("/allMeetingOrder")
    public ResponseEntity<?> displayAllMeetingOrder() {
        try {
            List<Meeting> meetings = repoMeeting.findAll();
            if (meetings.isEmpty()) {
                // Si la liste est vide, retournez une erreur
                throw new EmptyObjectList("La liste des demandes de réunions est vide.");
            } else {
                return ResponseEntity.ok(meetings);
            }
        } catch (EmptyObjectList e) {
            // Si la liste est vide, lever une exception et le code d'état approprié
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            // Si une autre erreur se produit, retournez le message d'erreur et le code d'état approprié
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur s'est produite : " + e.getMessage());
        }
    }

    @PutMapping("/updateMeeting/{id}")
    public ResponseEntity<?> updateMeeting(@RequestBody Meeting meetingUpdated, @PathVariable Integer id) {
        try {
            return repoMeeting.findById(id).map(meeting -> {
                meeting.setName(meetingUpdated.getName());
                meeting.setStartTime(meetingUpdated.getStartTime());
                meeting.setEndTime(meetingUpdated.getEndTime());
                meeting.setNumberOfpeople(meetingUpdated.getNumberOfpeople());
                meeting.setMeetingType(meetingUpdated.getMeetingType());
                repoMeeting.saveAndFlush(meeting);
                return ResponseEntity.ok("l'élément avec l'ID"+ id +" bien été modifié");
            }).orElseThrow(() -> new NotFoundObject("Réunion non trouvée avec l'ID : " + id));
        } catch (NotFoundObject e) {
            // Si la réunion avec l'ID donné n'existe pas, lever une exception et le code d'état approprié
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            // Si une autre erreur se produit, retournez le message d'erreur et le code d'état approprié
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur s'est produite : " + e.getMessage());
        }
    }

    @DeleteMapping("/deleteMeeting/{id}")
    public ResponseEntity<?> deleteMeeting(@PathVariable Integer id) {
        try {
            if (repoMeeting.findById(id).isPresent()) {
                repoMeeting.deleteById(id);
                return ResponseEntity.ok("L'élément avec l'ID "+id+" a été supprimé avec succès");
            } else {
                throw new NotFoundObject("Réunion non trouvée avec l'ID : " + id);
            }
        } catch (NotFoundObject e) {
            // Si la réunion avec l'ID donné n'existe pas, retournez une exception et le code d'état approprié
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            // Si une autre erreur se produit, retournez le message d'erreur et le code d'état approprié
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur s'est produite : " + e.getMessage());
        }
    }


    //Now let's call reservation service and implements some method
    @PostMapping("/reserveRoom")
    public ResponseEntity<?> reserveRoom(@RequestBody ReservationRequestService reservationRequestService) {
        try {
            Room room = reservationRequestService.getRoom();
            Meeting meeting = reservationRequestService.getMeeting();
            if(repoRoom.existsById(room.getId())) {
                boolean reservationStatus = reservationService.reserveRoom(room, meeting);
                if(reservationStatus) {
                    return ResponseEntity.ok().body("La salle a été réservée avec succès.");
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("La réservation de la salle a échoué pour une raison inconnue");
                }
            } else {
                throw new NotFoundObject("Cette salle est introuvable dans notre liste de salles disponible");
            }
        } catch (NotFoundObject e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/reservations")
    public ResponseEntity<?> displayReservations() {
        List<Room> reservations = repoRoom.findAllByAvailability(false);
        if (reservations.isEmpty()) {
            throw new NotFoundObject("Aucune réservation n'a été trouvée.");
        }
        return ResponseEntity.ok(reservations);
    }

}
