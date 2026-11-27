package com.smartpark.common.exception;

/** Nedozvoljen prelaz stanja (FR-12) - vraca 409 sa jasnom porukom. */
public class InvalidStateTransitionException extends ConflictException {

    public InvalidStateTransitionException(Enum<?> from, Enum<?> to) {
        super("Nedozvoljen prelaz stanja: " + from + " -> " + to);
    }
}
