package com.airtribe.smartparkinglotsystem.strategy;

import com.airtribe.smartparkinglotsystem.model.ParkingSpot;
import com.airtribe.smartparkinglotsystem.model.Vehicle;

import java.util.Optional;

public interface SpotAllocationStrategy {
    /**
     * Atomically reserves the best free spot that fits the vehicle and returns it
     * already occupied by that vehicle, or empty if nothing fits.
     */
    Optional<ParkingSpot> allocateSpot(Vehicle vehicle);

    /**
     * Frees the spot and makes it allocatable again. Only the first call for a
     * given occupant has any effect, so a repeated release is harmless.
     */
    void releaseSpot(ParkingSpot spot, Vehicle vehicle);
}