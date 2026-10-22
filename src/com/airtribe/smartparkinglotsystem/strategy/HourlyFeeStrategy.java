package com.airtribe.smartparkinglotsystem.strategy;

import com.airtribe.smartparkinglotsystem.model.Ticket;
import com.airtribe.smartparkinglotsystem.model.VehicleType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/** Fee = hourly rate of the vehicle type x hours parked, rounded up, minimum 1 hour. */
public class HourlyFeeStrategy implements FeeCalculationStrategy {
    private final Map<VehicleType, BigDecimal> hourlyRates;

    public HourlyFeeStrategy(Map<VehicleType, BigDecimal> rates) {
        EnumMap<VehicleType, BigDecimal> copy = new EnumMap<>(VehicleType.class);
        for (VehicleType type : VehicleType.values()) {
            BigDecimal rate = rates.get(type);
            if (rate == null || rate.signum() < 0) {
                throw new IllegalArgumentException("Missing or invalid hourly rate for " + type);
            }
            copy.put(type, rate);
        }
        this.hourlyRates = copy;
    }

    @Override
    public BigDecimal calculateFee(Ticket ticket, LocalDateTime exitTime) {
        long seconds = Duration.between(ticket.getArrivalTime(), exitTime).getSeconds();
        if (seconds < 0) {
            throw new IllegalArgumentException("Exit time is before arrival time");
        }
        long hours = Math.max(1, (seconds + 3599) / 3600);
        BigDecimal rate = hourlyRates.get(ticket.getVehicle().getVehicleType());
        return rate.multiply(BigDecimal.valueOf(hours));
    }
}