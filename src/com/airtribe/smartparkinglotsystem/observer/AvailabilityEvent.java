package com.airtribe.smartparkinglotsystem.observer;

import com.airtribe.smartparkinglotsystem.model.ParkingSpot;
import com.airtribe.smartparkinglotsystem.model.SpotType;
import com.airtribe.smartparkinglotsystem.model.Vehicle;

import java.util.Collections;
import java.util.Map;

/** Immutable notification: what happened, plus a snapshot of free spots per floor and spot type. */
public class AvailabilityEvent {
    public enum Type { VEHICLE_PARKED, VEHICLE_EXITED, RESERVATION_CANCELLED }

    private final long sequence;
    private final Type type;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final Map<Integer, Map<SpotType, Integer>> availabilityByFloor;

    public AvailabilityEvent(long sequence, Type type, Vehicle vehicle, ParkingSpot spot,
                             Map<Integer, Map<SpotType, Integer>> availabilityByFloor) {
        this.sequence = sequence;
        this.type = type;
        this.vehicle = vehicle;
        this.spot = spot;
        this.availabilityByFloor = Collections.unmodifiableMap(availabilityByFloor);
    }

    /** Increases by 1 with every event, so a listener can detect missed or out-of-order updates. */
    public long getSequence() { return sequence; }
    public Type getType() { return type; }
    public Vehicle getVehicle() { return vehicle; }
    public ParkingSpot getSpot() { return spot; }
    public Map<Integer, Map<SpotType, Integer>> getAvailabilityByFloor() { return availabilityByFloor; }

    public int getTotalAvailable() {
        int total = 0;
        for (Map<SpotType, Integer> perType : availabilityByFloor.values()) {
            for (int count : perType.values()) {
                total += count;
            }
        }
        return total;
    }
}