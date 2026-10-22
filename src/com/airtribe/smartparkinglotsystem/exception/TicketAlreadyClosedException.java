package com.airtribe.smartparkinglotsystem.exception;

public class TicketAlreadyClosedException extends ParkingLotException {
    public TicketAlreadyClosedException(String detail) {
        super("Ticket is already closed: " + detail);
    }
}
