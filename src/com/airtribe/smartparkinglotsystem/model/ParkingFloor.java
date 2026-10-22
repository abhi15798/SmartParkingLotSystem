package com.airtribe.smartparkinglotsystem.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ParkingFloor {
    private final int floorNumber;
    // Keys are fixed in the constructor and never change; only the lists mutate.
    private final Map<SpotType, List<ParkingSpot>> spotsByType = new EnumMap<>(SpotType.class);

    public ParkingFloor(int floorNumber) {
        this.floorNumber = floorNumber;
        for (SpotType type : SpotType.values()) {
            spotsByType.put(type, new CopyOnWriteArrayList<>());
        }
    }

    public void addSpot(ParkingSpot spot) {
        if (spot == null) {
            throw new IllegalArgumentException("Spot is required");
        }
        if (spot.getFloorNumber() != floorNumber) {
            throw new IllegalArgumentException(
                    "Spot " + spot.getParkingSpotId() + " does not belong to floor " + floorNumber);
        }
        spotsByType.get(spot.getSpotType()).add(spot);
    }

    public List<ParkingSpot> getSpotsForType(SpotType spotType) {
        return Collections.unmodifiableList(spotsByType.get(spotType));
    }

    /** Needed by ParkingLot.getAllSpots(). */
    public List<ParkingSpot> getAllSpots() {
        List<ParkingSpot> all = new ArrayList<>();
        for (List<ParkingSpot> bucket : spotsByType.values()) {
            all.addAll(bucket);
        }
        return all;
    }

    public int getAvailableSpotCount(SpotType spotType) {
        int count = 0;
        for (ParkingSpot spot : spotsByType.get(spotType)) {
            if (spot.isAvailable()) count++;
        }
        return count;
    }

    public int getFloorNumber() { return floorNumber; }
}