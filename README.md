# Smart Parking Lot System (Low-Level Design)

A low-level design and working Java implementation of a smart parking lot backend: vehicle check-in and
check-out, automatic spot allocation by vehicle size, fee calculation by duration and vehicle type, and
real-time availability updates, with safe handling of vehicles entering and leaving at the same time.

Plain Java 8, no frameworks, in-memory. The database schema is a design deliverable (`docs/schema.sql`).

## Features

| Requirement | How it is handled |
|---|---|
| Spot allocation by vehicle size | BIKE needs SMALL, CAR needs MEDIUM, BUS needs LARGE. A vehicle may use a larger spot when its own size is full, always preferring the smallest spot that fits, then the lowest floor |
| Check-in and check-out | Every entry creates a `Ticket` with an arrival time; checkout records the exit time and the fee |
| Fee calculation | Hourly rate per vehicle type, duration rounded up to the next full hour, minimum 1 hour, `BigDecimal` money |
| Real-time availability | Live counts per lot and per floor, plus an observer-based display panel that updates on every change |
| Data model | ER diagram, DDL and class-to-table mapping in `docs/` |
| Concurrency | Atomic spot reservation, one active ticket per vehicle, idempotent checkout |

## Project structure

```
SmartParkingLotSystem/
├── docs/
│   ├── DESIGN.md                  design write-up, diagrams, concurrency and limitations
│   └── schema.sql                 database schema (PostgreSQL-style DDL)
└── src/
    ├── Main.java                  demo of the normal scenarios
    └── com/airtribe/smartparkinglotsystem/
        ├── model/                 ParkingLot, ParkingFloor, ParkingSpot, Vehicle, VehicleType,
        │                          SpotType, Ticket
        ├── strategy/              SpotAllocationStrategy, BestFitSpotAllocationStrategy,
        │                          FeeCalculationStrategy, HourlyFeeStrategy
        ├── repository/            TicketRepository, InMemoryTicketRepository
        ├── observer/              AvailabilityListener, AvailabilityEvent, ConsoleDisplayPanel
        ├── service/               ParkingService
        └── exception/             ParkingLotException and four specific exceptions
```

## Running it

Requires Java 8 or later.

**IntelliJ:** open the project and run `Main`.

**Command line (macOS and Linux):**
```bash
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out Main
```

**Command line (Windows):**
```bat
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
java -cp out Main
```

## Quick usage

```java
ParkingFloor floor0 = new ParkingFloor(0);
floor0.addSpot(new ParkingSpot(1, 0, SpotType.SMALL));
floor0.addSpot(new ParkingSpot(2, 0, SpotType.MEDIUM));
ParkingLot lot = new ParkingLot(1, Arrays.asList(floor0));

Map<VehicleType, BigDecimal> rates = new EnumMap<>(VehicleType.class);
rates.put(VehicleType.BIKE, new BigDecimal("10"));
rates.put(VehicleType.CAR, new BigDecimal("20"));
rates.put(VehicleType.BUS, new BigDecimal("50"));

ParkingService service = new ParkingService(lot,
        new BestFitSpotAllocationStrategy(lot),   // build the lot BEFORE this line
        new HourlyFeeStrategy(rates),
        new InMemoryTicketRepository(),
        Clock.systemDefaultZone());

service.addListener(new ConsoleDisplayPanel());          // display board subscribes once

Ticket ticket = service.checkIn(new Vehicle("KA01-CAR1", VehicleType.CAR));
Ticket closed = service.checkOut(ticket.getTicketNo());
System.out.println(closed.getFee());                     // 20 (minimum 1 hour)
```

Sample display output:
```
[DISPLAY #1] PARKED KA01-BIKE (floor 0, spot 1) | free: 4 | F0 S:0 M:1 L:1 | F1 S:0 M:1 L:1
[DISPLAY #2] PARKED KA01-CAR1 (floor 0, spot 2) | free: 3 | F0 S:0 M:0 L:1 | F1 S:0 M:1 L:1
[DISPLAY #6] EXITED KA01-CAR1 (floor 0, spot 2) | free: 1 | F0 S:0 M:1 L:0 | F1 S:0 M:0 L:0
```

## Design overview

**Patterns used**
- **Strategy:** `SpotAllocationStrategy` and `FeeCalculationStrategy` can be swapped without touching the service.
- **Observer:** `ParkingService` notifies `AvailabilityListener`s (such as `ConsoleDisplayPanel`) on every change.
- **Repository:** `TicketRepository` hides storage; the in-memory version maps onto the SQL schema.

**Spot allocation**
- The strategy keeps one sorted set of free spots per spot type, ordered by (floor, spot id).
- Allocation takes the first entry from the smallest suitable type, falling back to larger types.
- Taking an entry is atomic, so two threads can never receive the same spot.
- Cost is O(types x log n) instead of scanning every spot.

**Check-in flow:** reject a vehicle that is already parked, reserve a spot, create and atomically save the ticket,
and roll the spot back if saving fails.

**Check-out flow:** find the ticket, compute the fee from the clock, close the ticket (idempotent), remove it from
the active index, then free the spot.

**Concurrency**

| Risk | Handling |
|---|---|
| Two vehicles get the same spot | Atomic removal from the free-spot index plus a compare-and-set on the spot |
| Same vehicle parked twice | `putIfAbsent` on the active-ticket index; the loser releases its spot |
| Same ticket charged or freed twice | `Ticket.close` is synchronized and returns false if already closed |
| A stale release frees someone else's spot | `release(vehicle)` only succeeds for the current occupant |
| Display shows stale or out-of-order data | Notifications are serialized with sequence numbers |

There is no global lock, so contention is per spot.

**Database design:** see `docs/schema.sql` and `docs/DESIGN.md`. Two partial unique indexes enforce at most one
active ticket per spot and per vehicle, the database equivalent of the in-memory guarantees.

| Code | Table |
|---|---|
| `ParkingLot` | `parking_lot` |
| `ParkingFloor` | `floor` |
| `ParkingSpot` | `parking_spot` |
| `Vehicle` | `vehicle` |
| `Ticket` | `ticket` |
| rates in `HourlyFeeStrategy` | `rate_card` |

## Verification

The demo scenarios and the concurrency checks were compiled with `javac --release 8` and run repeatedly:
- 20 cars racing for 5 spots: exactly 5 park, no spot is given twice.
- The same vehicle checking in from 10 threads: exactly one succeeds and the others roll back.
- The same ticket checked out from 10 threads: one succeeds, the fee is charged once, the spot is freed once.
- 16 threads checking in and out repeatedly: every spot is free at the end and the free-spot index is intact.
- Display events arrive in order and the last one matches the real final state; a failing listener cannot break parking.
- The indexed allocation picked the same spot as a plain scan in 800,000 random operations.

## Extending

- **New pricing:** implement `FeeCalculationStrategy` (for example, tiered or flat-per-day).
- **New allocation policy:** implement `SpotAllocationStrategy` (for example, least-loaded floor).
- **Persistence:** implement `TicketRepository` with JDBC or JPA against `docs/schema.sql`.
- **Another display:** implement `AvailabilityListener` (for example, a web dashboard) and register it with `addListener`.

## Assumptions and limitations

- The free-spot index is built once from the lot, so build the whole lot before creating the strategy.
- A check-in can briefly see "full" while another thread's check-in is about to roll back and free its spot.
- Tickets and vehicles live in memory only, and ticket numbers restart at 1001 on every run.
- The fee depends on the vehicle type, not the spot type, and uses the rate configured at exit time.
- There is no payment step, REST layer, reservation system or special spot types (EV, handicapped).
- Listener callbacks run on the thread that performed the check-in or check-out, so slow listeners should hand
  events to their own queue.