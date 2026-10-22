package com.airtribe.smartparkinglotsystem.strategy;

import com.airtribe.smartparkinglotsystem.model.ParkingLot;
import com.airtribe.smartparkinglotsystem.model.ParkingSpot;
import com.airtribe.smartparkinglotsystem.model.SpotType;
import com.airtribe.smartparkinglotsystem.model.Vehicle;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Best-fit allocation backed by an index of FREE spots: one sorted set per spot type,
 * ordered by (floor, spot id).
 *
 * allocate: try the smallest type that fits, then larger types (fallback). pollFirst() is atomic,
 * so two threads can never receive the same spot. Cost is O(types x log n) instead of scanning every spot.
 * release:  free the spot, and only if that really freed it, put it back in the index.
 *
 * Invariant: a spot is in the index only while it is free. The index is built once from the lot,
 * so create this strategy after the lot is fully built.
 */
public class BestFitSpotAllocationStrategy implements SpotAllocationStrategy {

    private static final Comparator<ParkingSpot> FLOOR_THEN_ID =
            Comparator.comparingInt(ParkingSpot::getFloorNumber)
                    .thenComparingInt(ParkingSpot::getParkingSpotId);

    // Keys are fixed in the constructor; only the sets mutate.
    private final Map<SpotType, ConcurrentSkipListSet<ParkingSpot>> freeSpots = new EnumMap<>(SpotType.class);

    public BestFitSpotAllocationStrategy(ParkingLot parkingLot) {
        for (SpotType type : SpotType.values()) {
            freeSpots.put(type, new ConcurrentSkipListSet<>(FLOOR_THEN_ID));
        }
        for (ParkingSpot spot : parkingLot.getAllSpots()) {
            if (spot.isAvailable()) {
                freeSpots.get(spot.getSpotType()).add(spot);
            }
        }
    }

    @Override
    public Optional<ParkingSpot> allocateSpot(Vehicle vehicle) {
        SpotType[] types = SpotType.values();   // declared small to large
        for (int i = vehicle.getVehicleType().getMinimumSpotType().ordinal(); i < types.length; i++) {
            ConcurrentSkipListSet<ParkingSpot> bucket = freeSpots.get(types[i]);
            ParkingSpot spot;
            while ((spot = bucket.pollFirst()) != null) {
                if (spot.tryOccupy(vehicle)) {
                    return Optional.of(spot);
                }
                // Defensive: a polled spot that was not actually free is dropped from the index;
                // it is re-added when its occupant releases it.
            }
        }
        return Optional.empty();
    }

    @Override
    public void releaseSpot(ParkingSpot spot, Vehicle vehicle) {
        if (spot.release(vehicle)) {
            freeSpots.get(spot.getSpotType()).add(spot);
        }
    }
}