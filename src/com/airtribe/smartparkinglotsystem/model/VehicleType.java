package com.airtribe.smartparkinglotsystem.model;

public enum VehicleType {
    CAR(SpotType.MEDIUM),
    BUS(SpotType.LARGE),
    BIKE(SpotType.SMALL);
    private final SpotType minimumSpotType;
    VehicleType(SpotType minimumSpotType) {
        this.minimumSpotType = minimumSpotType;
    }
    public SpotType getMinimumSpotType() {
        return minimumSpotType;
    }
}
