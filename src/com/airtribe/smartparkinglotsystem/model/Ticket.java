package com.airtribe.smartparkinglotsystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Ticket {
    public enum Status { ACTIVE, CLOSED }

    private final int ticketNo;
    private final Vehicle vehicle;
    private final ParkingSpot parkingSpot;
    private final LocalDateTime arrivalTime;
    private volatile LocalDateTime exitTime;
    private volatile BigDecimal fee;
    private volatile Status status = Status.ACTIVE;

    public Ticket(int ticketNo, Vehicle vehicle, ParkingSpot parkingSpot, LocalDateTime arrivalTime) {
        this.ticketNo = ticketNo;
        this.vehicle = vehicle;
        this.parkingSpot = parkingSpot;
        this.arrivalTime = arrivalTime;
    }

    /** Returns false if already closed, so a repeated checkout can't charge twice. */
    public synchronized boolean close(LocalDateTime exitTime, BigDecimal fee) {
        if (status == Status.CLOSED) return false;
        if (exitTime.isBefore(arrivalTime)) {
            throw new IllegalArgumentException("Exit time is before arrival time");
        }
        this.exitTime = exitTime;
        this.fee = fee;
        this.status = Status.CLOSED;
        return true;
    }

    public int getTicketNo() { return ticketNo; }
    public Vehicle getVehicle() { return vehicle; }
    public ParkingSpot getParkingSpot() { return parkingSpot; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public BigDecimal getFee() { return fee; }
    public Status getStatus() { return status; }
}