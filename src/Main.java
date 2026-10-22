import com.airtribe.smartparkinglotsystem.exception.NoSpotAvailableException;
import com.airtribe.smartparkinglotsystem.exception.ParkingLotException;
import com.airtribe.smartparkinglotsystem.model.ParkingFloor;
import com.airtribe.smartparkinglotsystem.model.ParkingLot;
import com.airtribe.smartparkinglotsystem.model.ParkingSpot;
import com.airtribe.smartparkinglotsystem.model.SpotType;
import com.airtribe.smartparkinglotsystem.model.Ticket;
import com.airtribe.smartparkinglotsystem.model.Vehicle;
import com.airtribe.smartparkinglotsystem.model.VehicleType;
import com.airtribe.smartparkinglotsystem.observer.ConsoleDisplayPanel;
import com.airtribe.smartparkinglotsystem.repository.InMemoryTicketRepository;
import com.airtribe.smartparkinglotsystem.service.ParkingService;
import com.airtribe.smartparkinglotsystem.strategy.HourlyFeeStrategy;
import com.airtribe.smartparkinglotsystem.strategy.BestFitSpotAllocationStrategy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class Main {

    /** Clock we can move forward, so the fee demo doesn't need to wait for real hours. */
    static class DemoClock extends Clock {
        private Instant now = Instant.parse("2026-10-15T09:00:00Z");

        void advance(Duration d) { now = now.plus(d); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    public static void main(String[] args) {
        // ---- Build the lot: 2 floors ----
        // Floor 0: 1 small, 1 medium, 1 large   |   Floor 1: 1 medium, 1 large
        ParkingFloor floor0 = new ParkingFloor(0);
        floor0.addSpot(new ParkingSpot(1, 0, SpotType.SMALL));
        floor0.addSpot(new ParkingSpot(2, 0, SpotType.MEDIUM));
        floor0.addSpot(new ParkingSpot(3, 0, SpotType.LARGE));
        ParkingFloor floor1 = new ParkingFloor(1);
        floor1.addSpot(new ParkingSpot(4, 1, SpotType.MEDIUM));
        floor1.addSpot(new ParkingSpot(5, 1, SpotType.LARGE));
        ParkingLot lot = new ParkingLot(1, Arrays.asList(floor0, floor1));

        // ---- Wire the service ----
        Map<VehicleType, BigDecimal> rates = new EnumMap<>(VehicleType.class);
        rates.put(VehicleType.BIKE, new BigDecimal("10"));   // per hour
        rates.put(VehicleType.CAR, new BigDecimal("20"));
        rates.put(VehicleType.BUS, new BigDecimal("50"));
        DemoClock clock = new DemoClock();
        ParkingService service = new ParkingService(lot,
                new BestFitSpotAllocationStrategy(lot),   // build the lot BEFORE this line
                new HourlyFeeStrategy(rates),
                new InMemoryTicketRepository(),
                clock);

        // The display board subscribes once; after that it updates itself on every check-in and check-out.
        service.addListener(new ConsoleDisplayPanel(System.out));

        System.out.println("1) Empty lot");
        System.out.println("   free spots: " + service.getAvailability());

        System.out.println("\n2) Vehicles arrive (each gets the smallest spot that fits, lowest floor first)");
        Ticket bike = service.checkIn(new Vehicle("KA01-BIKE", VehicleType.BIKE));
        Ticket car1 = service.checkIn(new Vehicle("KA01-CAR1", VehicleType.CAR));
        Ticket car2 = service.checkIn(new Vehicle("KA01-CAR2", VehicleType.CAR));
        print(bike);
        print(car1);
        print(car2);

        System.out.println("\n3) Availability updates as vehicles enter");
        System.out.println("   free spots:     " + service.getAvailability());
        System.out.println("   free by floor:  " + service.getAvailabilityByFloor());

        System.out.println("\n4) Both MEDIUM spots are taken, so the next car falls back to a LARGE spot");
        Ticket car3 = service.checkIn(new Vehicle("KA01-CAR3", VehicleType.CAR));
        print(car3);

        System.out.println("\n5) A bus fits only a LARGE spot");
        Ticket bus = service.checkIn(new Vehicle("KA01-BUS", VehicleType.BUS));
        print(bus);

        System.out.println("\n6) Lot is full for another bus");
        try {
            service.checkIn(new Vehicle("KA01-BUS2", VehicleType.BUS));
        } catch (NoSpotAvailableException e) {
            System.out.println("   rejected: " + e.getMessage());
        }

        System.out.println("\n7) The same vehicle cannot enter twice");
        try {
            service.checkIn(new Vehicle("KA01-CAR1", VehicleType.CAR));
        } catch (ParkingLotException e) {
            System.out.println("   rejected: " + e.getMessage());
        }

        System.out.println("\n8) car1 leaves after 2h 10m (billed as 3 hours x 20)");
        clock.advance(Duration.ofHours(2).plusMinutes(10));
        Ticket closed = service.checkOut(car1.getTicketNo());
        System.out.println("   entry: " + closed.getArrivalTime() + "   exit: " + closed.getExitTime());
        System.out.println("   fee:   " + closed.getFee());
        System.out.println("   free spots: " + service.getAvailability());

        System.out.println("\n9) Checking out the same ticket again is rejected");
        try {
            service.checkOut(car1.getTicketNo());
        } catch (ParkingLotException e) {
            System.out.println("   rejected: " + e.getMessage());
        }

        System.out.println("\n10) The freed spot is reused by the next car");
        Ticket car4 = service.checkIn(new Vehicle("KA01-CAR4", VehicleType.CAR));
        print(car4);

        System.out.println("\n11) car4 leaves only 20 minutes after entering (minimum 1 hour x 20)");
        clock.advance(Duration.ofMinutes(20));
        System.out.println("   fee: " + service.checkOut(car4.getTicketNo()).getFee());
        System.out.println("   free spots: " + service.getAvailability());

        System.out.println("\n12) Bike leaves after 2h 30m in total (billed as 3 hours x 10)");
        System.out.println("   fee: " + service.checkOut(bike.getTicketNo()).getFee());
        System.out.println("   free spots: " + service.getAvailability());
    }

    private static void print(Ticket t) {
        System.out.println("   ticket " + t.getTicketNo() + ": " + t.getVehicle().getRegistrationNo()
                + " (" + t.getVehicle().getVehicleType() + ") -> spot " + t.getParkingSpot().getParkingSpotId()
                + " [floor " + t.getParkingSpot().getFloorNumber() + ", " + t.getParkingSpot().getSpotType() + "]");
    }
}