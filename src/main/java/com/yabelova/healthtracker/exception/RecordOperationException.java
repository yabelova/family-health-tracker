package com.yabelova.healthtracker.exception;

import lombok.Getter;

@Getter
public class RecordOperationException extends RuntimeException {

    private final Error error;

    public RecordOperationException(Error error) {
        super(messageOf(error));
        this.error = error;
    }

    public enum Error {
        RECORD_NOT_FOUND, RECORD_NOT_LINKED, PROFILE_ACCESS_DENIED
    }

    private static String messageOf(Error error) {
        return switch (error) {
            case RECORD_NOT_FOUND -> ErrorTexts.RECORD_NOT_FOUND;
            case RECORD_NOT_LINKED -> ErrorTexts.RECORD_NOT_LINKED;
            case PROFILE_ACCESS_DENIED -> ErrorTexts.RECORD_PROFILE_ACCESS_DENIED;
        };
    }
}
