package com.airtribe.smartparkinglotsystem.observer;

import com.airtribe.smartparkinglotsystem.model.SpotType;

import java.io.PrintStream;
import java.util.Map;

/** Concrete observer: the display board at the entrance. Prints one line per availability change. */
public class ConsoleDisplayPanel implements AvailabilityListener {
    private final PrintStream out;
    private volatile AvailabilityEvent latest;

    public ConsoleDisplayPanel() {
        this(System.out);
    }

    public ConsoleDisplayPanel(PrintStream out) {
        this.out = out;
    }

    @Override
    public void onAvailabilityChanged(AvailabilityEvent event) {
        latest = event;
        out.println(render(event));
    }

    /** The most recent board state, or null if nothing has happened yet. */
    public AvailabilityEvent getLatestEvent() {
        return latest;
    }

    private String render(AvailabilityEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append("[DISPLAY #").append(e.getSequence()).append("] ")
                .append(describe(e)).append(" | free: ").append(e.getTotalAvailable());
        for (Map.Entry<Integer, Map<SpotType, Integer>> floor : e.getAvailabilityByFloor().entrySet()) {
            Map<SpotType, Integer> c = floor.getValue();
            sb.append(" | F").append(floor.getKey())
                    .append(" S:").append(c.get(SpotType.SMALL))
                    .append(" M:").append(c.get(SpotType.MEDIUM))
                    .append(" L:").append(c.get(SpotType.LARGE));
        }
        return sb.toString();
    }

    private String describe(AvailabilityEvent e) {
        String where = "floor " + e.getSpot().getFloorNumber() + ", spot " + e.getSpot().getParkingSpotId();
        String reg = e.getVehicle().getRegistrationNo();
        switch (e.getType()) {
            case VEHICLE_PARKED:
                return "PARKED " + reg + " (" + where + ")";
            case VEHICLE_EXITED:
                return "EXITED " + reg + " (" + where + ")";
            default:
                return "CANCELLED " + reg + " (" + where + ")";
        }
    }
}