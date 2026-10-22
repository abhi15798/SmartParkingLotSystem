CREATE TABLE parking_lot (
                             id   BIGINT PRIMARY KEY,
                             name VARCHAR(100) NOT NULL
);

CREATE TABLE floor (
                       id           BIGINT PRIMARY KEY,
                       lot_id       BIGINT NOT NULL REFERENCES parking_lot(id),
                       floor_number INT NOT NULL,
                       UNIQUE (lot_id, floor_number)
);

CREATE TABLE parking_spot (
                              id          BIGINT PRIMARY KEY,
                              floor_id    BIGINT NOT NULL REFERENCES floor(id),
                              spot_number VARCHAR(20) NOT NULL,
                              spot_type   VARCHAR(10) NOT NULL CHECK (spot_type IN ('SMALL', 'MEDIUM', 'LARGE')),
                              status      VARCHAR(15) NOT NULL DEFAULT 'AVAILABLE'
                                  CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'OUT_OF_SERVICE')),
                              UNIQUE (floor_id, spot_number)
);
CREATE INDEX idx_spot_alloc ON parking_spot (spot_type, status);

CREATE TABLE vehicle (
                         id              BIGINT PRIMARY KEY,
                         registration_no VARCHAR(20) NOT NULL UNIQUE,
                         vehicle_type    VARCHAR(10) NOT NULL CHECK (vehicle_type IN ('BIKE', 'CAR', 'BUS'))
);

CREATE TABLE rate_card (
                           id             BIGINT PRIMARY KEY,
                           vehicle_type   VARCHAR(10) NOT NULL CHECK (vehicle_type IN ('BIKE', 'CAR', 'BUS')),
                           hourly_rate    DECIMAL(10,2) NOT NULL CHECK (hourly_rate >= 0),
                           effective_from TIMESTAMP NOT NULL,
                           UNIQUE (vehicle_type, effective_from)
);

CREATE TABLE ticket (
                        id         BIGINT PRIMARY KEY,
                        vehicle_id BIGINT NOT NULL REFERENCES vehicle(id),
                        spot_id    BIGINT NOT NULL REFERENCES parking_spot(id),
                        entry_time TIMESTAMP NOT NULL,
                        exit_time  TIMESTAMP NULL,
                        fee        DECIMAL(10,2) NULL,
                        status     VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CLOSED')),
                        CHECK (exit_time IS NULL OR exit_time >= entry_time)
);

-- At most one ACTIVE ticket per spot and per vehicle, enforced by the database itself.
CREATE UNIQUE INDEX uq_ticket_active_spot    ON ticket (spot_id)    WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uq_ticket_active_vehicle ON ticket (vehicle_id) WHERE status = 'ACTIVE';