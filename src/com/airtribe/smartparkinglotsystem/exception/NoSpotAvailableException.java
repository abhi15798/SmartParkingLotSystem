package com.airtribe.smartparkinglotsystem.exception;

public class NoSpotAvailableException extends ParkingLotException {
    public NoSpotAvailableException(String detail) {
        super("No parking spot available for this vehicle: " + detail);
    }
}
