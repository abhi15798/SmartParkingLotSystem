package com.airtribe.smartparkinglotsystem.strategy;

import com.airtribe.smartparkinglotsystem.model.Ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface FeeCalculationStrategy {
    BigDecimal calculateFee(Ticket ticket, LocalDateTime exitTime);
}