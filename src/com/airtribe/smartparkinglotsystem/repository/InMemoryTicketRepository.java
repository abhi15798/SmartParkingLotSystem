package com.airtribe.smartparkinglotsystem.repository;

import com.airtribe.smartparkinglotsystem.model.Ticket;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTicketRepository implements TicketRepository {
    private final ConcurrentHashMap<Integer, Ticket> tickets = new ConcurrentHashMap<>();
    // registrationNo -> ticketNo of its active ticket (in-memory equivalent of the unique index on active tickets)
    private final ConcurrentHashMap<String, Integer> activeByRegistrationNo = new ConcurrentHashMap<>();

    @Override
    public boolean saveActive(Ticket ticket) {
        String reg = ticket.getVehicle().getRegistrationNo();
        if (activeByRegistrationNo.putIfAbsent(reg, ticket.getTicketNo()) != null) {
            return false;
        }
        tickets.put(ticket.getTicketNo(), ticket);
        return true;
    }

    @Override
    public Optional<Ticket> findByTicketNo(int ticketNo) {
        return Optional.ofNullable(tickets.get(ticketNo));
    }

    @Override
    public Optional<Ticket> findActiveByRegistrationNo(String registrationNo) {
        Integer ticketNo = activeByRegistrationNo.get(registrationNo);
        return ticketNo == null ? Optional.<Ticket>empty() : Optional.ofNullable(tickets.get(ticketNo));
    }

    @Override
    public void markClosed(Ticket ticket) {
        activeByRegistrationNo.remove(ticket.getVehicle().getRegistrationNo(), ticket.getTicketNo());
    }
}