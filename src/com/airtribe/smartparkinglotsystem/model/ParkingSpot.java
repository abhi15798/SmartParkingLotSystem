package com.airtribe.smartparkinglotsystem.model;

import java.util.concurrent.atomic.AtomicReference;

public class ParkingSpot {
    private final int parkingSpotId;
    private final int floorNumber;
    private final SpotType spotType;
    // null = free, non-null = occupied by that vehicle (single source of truth)
    private final AtomicReference<Vehicle> vehicle = new AtomicReference<>();

    public ParkingSpot(int parkingSpotId, int floorNumber, SpotType spotType) {
        this.parkingSpotId = parkingSpotId;
        this.floorNumber = floorNumber;
        this.spotType = spotType;
    }

    /** True if this spot is large enough for the vehicle (same size or bigger). */
    public boolean canFit(Vehicle v) {
        return spotType.compareTo(v.getVehicleType().getMinimumSpotType()) >= 0;
    }

    /** Atomic claim: exactly one thread wins when several race for the same spot. */
    public boolean tryOccupy(Vehicle v) {
        return vehicle.compareAndSet(null, v);
    }

    /** Frees the spot only if this exact vehicle object is the occupant (CAS compares by reference). */
    public boolean release(Vehicle v) {
        return vehicle.compareAndSet(v, null);
    }

    public boolean isAvailable() { return vehicle.get() == null; }
    public int getParkingSpotId() { return parkingSpotId; }
    public int getFloorNumber() { return floorNumber; }
    public SpotType getSpotType() { return spotType; }
}