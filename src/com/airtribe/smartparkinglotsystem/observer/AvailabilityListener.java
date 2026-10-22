package com.airtribe.smartparkinglotsystem.observer;

/** Observer: gets told whenever the lot's availability changes. */
public interface AvailabilityListener {
    void onAvailabilityChanged(AvailabilityEvent event);
}