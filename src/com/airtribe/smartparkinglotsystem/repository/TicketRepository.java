package com.airtribe.smartparkinglotsystem.repository;

import com.airtribe.smartparkinglotsystem.model.Ticket;

import java.util.Optional;

public interface TicketRepository {
    /** Atomically stores an ACTIVE ticket. Returns false if the vehicle already has an active ticket. */
    boolean saveActive(Ticket ticket);

    Optional<Ticket> findByTicketNo(int ticketNo);

    Optional<Ticket> findActiveByRegistrationNo(String registrationNo);

    /** Removes the ticket from the active index (the ticket itself stays for history). */
    void markClosed(Ticket ticket);
}