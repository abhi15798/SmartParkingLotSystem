package com.airtribe.smartparkinglotsystem.service;

import com.airtribe.smartparkinglotsystem.exception.NoSpotAvailableException;
import com.airtribe.smartparkinglotsystem.exception.TicketAlreadyClosedException;
import com.airtribe.smartparkinglotsystem.exception.TicketNotFoundException;
import com.airtribe.smartparkinglotsystem.exception.VehicleAlreadyParkedException;
import com.airtribe.smartparkinglotsystem.model.ParkingFloor;
import com.airtribe.smartparkinglotsystem.model.ParkingLot;
import com.airtribe.smartparkinglotsystem.model.ParkingSpot;
import com.airtribe.smartparkinglotsystem.model.SpotType;
import com.airtribe.smartparkinglotsystem.model.Ticket;
import com.airtribe.smartparkinglotsystem.model.Vehicle;
import com.airtribe.smartparkinglotsystem.observer.AvailabilityEvent;
import com.airtribe.smartparkinglotsystem.observer.AvailabilityListener;
import com.airtribe.smartparkinglotsystem.repository.TicketRepository;
import com.airtribe.smartparkinglotsystem.strategy.FeeCalculationStrategy;
import com.airtribe.smartparkinglotsystem.strategy.SpotAllocationStrategy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ParkingService {
    private final ParkingLot parkingLot;
    private final SpotAllocationStrategy spotAllocationStrategy;
    private final FeeCalculationStrategy feeCalculationStrategy;
    private final TicketRepository ticketRepository;
    private final Clock clock;
    private final AtomicInteger ticketCounter = new AtomicInteger(1000);

    // Observer pattern: this service is the subject; listeners (e.g. a display panel) subscribe to availability changes.
    private final List<AvailabilityListener> listeners = new CopyOnWriteArrayList<>();
    private final Object notifyLock = new Object();
    private long eventSequence;   // guarded by notifyLock

    public ParkingService(ParkingLot parkingLot,
                          SpotAllocationStrategy spotAllocationStrategy,
                          FeeCalculationStrategy feeCalculationStrategy,
                          TicketRepository ticketRepository,
                          Clock clock) {
        this.parkingLot = parkingLot;
        this.spotAllocationStrategy = spotAllocationStrategy;
        this.feeCalculationStrategy = feeCalculationStrategy;
        this.ticketRepository = ticketRepository;
        this.clock = clock;
    }

    public Ticket checkIn(Vehicle vehicle) {
        String reg = vehicle.getRegistrationNo();

        // Fast-path rejection; the authoritative check is the atomic saveActive below.
        if (ticketRepository.findActiveByRegistrationNo(reg).isPresent()) {
            throw new VehicleAlreadyParkedException(reg);
        }

        // Reserves the spot: it comes back already occupied by this vehicle.
        ParkingSpot spot = spotAllocationStrategy.allocateSpot(vehicle)
                .orElseThrow(() -> new NoSpotAvailableException(vehicle.getVehicleType().name()));

        Ticket ticket = new Ticket(ticketCounter.incrementAndGet(), vehicle, spot, LocalDateTime.now(clock));
        boolean saved;
        try {
            saved = ticketRepository.saveActive(ticket);
        } catch (RuntimeException e) {
            spotAllocationStrategy.releaseSpot(spot, vehicle);   // roll back the reservation
            publish(AvailabilityEvent.Type.RESERVATION_CANCELLED, vehicle, spot);
            throw e;
        }
        if (!saved) {
            spotAllocationStrategy.releaseSpot(spot, vehicle);   // same vehicle checked in concurrently, so roll back
            publish(AvailabilityEvent.Type.RESERVATION_CANCELLED, vehicle, spot);
            throw new VehicleAlreadyParkedException(reg);
        }
        publish(AvailabilityEvent.Type.VEHICLE_PARKED, vehicle, spot);
        return ticket;
    }

    public Ticket checkOut(int ticketNo) {
        Ticket ticket = ticketRepository.findByTicketNo(ticketNo)
                .orElseThrow(() -> new TicketNotFoundException(String.valueOf(ticketNo)));
        if (ticket.getStatus() == Ticket.Status.CLOSED) {
            throw new TicketAlreadyClosedException(String.valueOf(ticketNo));
        }

        LocalDateTime exitTime = LocalDateTime.now(clock);
        BigDecimal fee = feeCalculationStrategy.calculateFee(ticket, exitTime);   // before any state change

        if (!ticket.close(exitTime, fee)) {
            throw new TicketAlreadyClosedException(String.valueOf(ticketNo));   // lost a checkout race
        }
        ticketRepository.markClosed(ticket);
        // free the spot only after the ticket is closed
        spotAllocationStrategy.releaseSpot(ticket.getParkingSpot(), ticket.getVehicle());
        publish(AvailabilityEvent.Type.VEHICLE_EXITED, ticket.getVehicle(), ticket.getParkingSpot());
        return ticket;
    }

    public void addListener(AvailabilityListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener is required");
        }
        listeners.add(listener);
    }

    public void removeListener(AvailabilityListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies listeners with a fresh availability snapshot. Runs under a lock so events are delivered one at a
     * time, in sequence order, and every snapshot is taken after the previous one: the last event a listener sees
     * therefore always reflects the final state. A failing listener must never break check-in or check-out.
     */
    private void publish(AvailabilityEvent.Type type, Vehicle vehicle, ParkingSpot spot) {
        if (listeners.isEmpty()) {
            return;
        }
        synchronized (notifyLock) {
            AvailabilityEvent event = new AvailabilityEvent(++eventSequence, type, vehicle, spot,
                    getAvailabilityByFloor());
            for (AvailabilityListener listener : listeners) {
                try {
                    listener.onAvailabilityChanged(event);
                } catch (RuntimeException e) {
                    System.err.println("Availability listener failed: " + e);
                }
            }
        }
    }

    /** Free spots per spot type across the whole lot, read from live spot state. */
    public Map<SpotType, Integer> getAvailability() {
        Map<SpotType, Integer> result = new EnumMap<>(SpotType.class);
        for (SpotType type : SpotType.values()) {
            result.put(type, 0);
        }
        for (ParkingFloor floor : parkingLot.getParkingFloors()) {
            for (SpotType type : SpotType.values()) {
                result.merge(type, floor.getAvailableSpotCount(type), Integer::sum);
            }
        }
        return result;
    }

    /** Free spots per floor and spot type, read from live spot state. */
    public Map<Integer, Map<SpotType, Integer>> getAvailabilityByFloor() {
        Map<Integer, Map<SpotType, Integer>> result = new TreeMap<>();
        for (ParkingFloor floor : parkingLot.getParkingFloors()) {
            Map<SpotType, Integer> perType = new EnumMap<>(SpotType.class);
            for (SpotType type : SpotType.values()) {
                perType.put(type, floor.getAvailableSpotCount(type));
            }
            result.put(floor.getFloorNumber(), perType);
        }
        return result;
    }

    public int getTotalAvailableSpots() {
        int total = 0;
        for (int count : getAvailability().values()) {
            total += count;
        }
        return total;
    }
}