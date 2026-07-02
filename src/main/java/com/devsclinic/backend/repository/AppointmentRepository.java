package com.devsclinic.backend.repository;

import com.devsclinic.backend.model.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository layer for Appointment documents in MongoDB.
 * Extends MongoRepository to get CRUD operations out of the box,
 * plus custom finder methods using Spring Data's method-name query derivation.
 */
@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {

    List<Appointment> findByPhoneNumber(String phoneNumber);

    List<Appointment> findByEmailAddress(String emailAddress);

    List<Appointment> findByPreferredDate(LocalDate preferredDate);

    List<Appointment> findByStatus(Appointment.AppointmentStatus status);

    List<Appointment> findByPreferredDateAndPreferredTime(LocalDate preferredDate, String preferredTime);
}
