package com.airtribe.smartparkinglotsystem.model;

import java.util.List;
import java.util.stream.Collectors;

public class ParkingLot {
    private final int parkingLotNumber;
    private final List<ParkingFloor> parkingFloors;

    public ParkingLot(int parkingLotNumber, List<ParkingFloor> parkingFloors) {
        this.parkingLotNumber = parkingLotNumber;
        this.parkingFloors = parkingFloors;
    }

    public int getParkingLotNumber() { return parkingLotNumber; }
    public List<ParkingFloor> getParkingFloors() { return parkingFloors; }

    /** Flattened view across floors. */
    public List<ParkingSpot> getAllSpots() {
        return parkingFloors.stream()
                .flatMap(f -> f.getAllSpots().stream())
                .collect(Collectors.toList());
    }
}