package com.airtribe.smartparkinglotsystem.model;

public class Vehicle {
    private final String registrationNo;
    private final VehicleType vehicleType;

    public Vehicle(String registrationNo, VehicleType vehicleType) {
        if (registrationNo == null || registrationNo.trim().isEmpty() || vehicleType == null) {
            throw new IllegalArgumentException("Registration number and vehicle type are required");
        }
        this.registrationNo = registrationNo;
        this.vehicleType = vehicleType;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

}
