package com.pvk.cinemas.common.exceptions;

public class OutOfCalendarRangeException extends BadRequestException {

    public OutOfCalendarRangeException(String message) {
        super("OUT_OF_CALENDAR_RANGE", message);
    }
}
