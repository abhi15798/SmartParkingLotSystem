package com.airtribe.smartparkinglotsystem.exception;

public class VehicleAlreadyParkedException extends ParkingLotException {
    public VehicleAlreadyParkedException(String detail) {
        super("Vehicle is already parked: " + detail);
    }
}
