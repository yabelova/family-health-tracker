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
        boolean hasName = StringUtils.hasText(participant.firstName());
        boolean hasUsername = StringUtils.hasText(participant.username());

        if (hasName && hasUsername) {
            return participant.firstName() + " @" + participant.username();
        }
        if (hasName) {
            return participant.firstName();
        }
        if (hasUsername) {
            return "@" + participant.username();
        }
        return BotTexts.PARTICIPANT_UNKNOWN;
    }
}
