package com.yabelova.healthtracker.telegram.support;

import com.yabelova.healthtracker.repository.ProfileRepository.ProfileParticipant;
import org.springframework.util.StringUtils;

/**
 * Отображаемое имя участника: «Имя @username», либо только часть, либо фолбэк.
 * Живет в UI-слое, чтобы данные не зависели от пользовательских строк.
 */
public final class ParticipantName {

    private ParticipantName() {
    }

    public static String of(ProfileParticipant participant) {
        boolean hasName = StringUtils.hasText(participant.telegramFirstName());
        boolean hasUsername = StringUtils.hasText(participant.telegramUsername());

        if (hasName && hasUsername) {
            return participant.telegramFirstName() + " @" + participant.telegramUsername();
        }
        if (hasName) {
            return participant.telegramFirstName();
        }
        if (hasUsername) {
            return "@" + participant.telegramUsername();
        }
        return BotTexts.PARTICIPANT_UNKNOWN;
    }
}
