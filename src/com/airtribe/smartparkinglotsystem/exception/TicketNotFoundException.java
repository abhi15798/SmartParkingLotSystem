package com.airtribe.smartparkinglotsystem.exception;

public class TicketNotFoundException extends ParkingLotException {
    public TicketNotFoundException(String detail) {
        super("Ticket not found: " + detail);
    }
}
