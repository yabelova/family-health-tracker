package com.yabelova.healthtracker.exception;

import lombok.Getter;

@Getter
public class ProfileOperationException extends RuntimeException {

    private final Error error;

    public ProfileOperationException(Error error) {
        super(messageOf(error));
        this.error = error;
    }

    public enum Error {
        INVITE_NOT_FOUND, INVITE_USED, INVITE_EXPIRED, OWN_PROFILE, ALREADY_LINKED, NOT_OWNER,
        PROFILE_HAS_PARTICIPANTS, NOT_FOUND_PARTICIPANT
    }

    private static String messageOf(Error error) {
        return switch (error) {
            case INVITE_NOT_FOUND -> ErrorTexts.INVITE_NOT_FOUND;
            case INVITE_USED -> ErrorTexts.INVITE_USED;
            case INVITE_EXPIRED -> ErrorTexts.INVITE_EXPIRED;
            case OWN_PROFILE -> ErrorTexts.OWN_PROFILE;
            case ALREADY_LINKED -> ErrorTexts.ALREADY_LINKED;
            case NOT_OWNER -> ErrorTexts.NOT_OWNER;
            case PROFILE_HAS_PARTICIPANTS -> ErrorTexts.PROFILE_HAS_PARTICIPANTS;
            case NOT_FOUND_PARTICIPANT -> ErrorTexts.NOT_FOUND_PARTICIPANT;
        };
    }
}