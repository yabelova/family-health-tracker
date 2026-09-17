package com.yabelova.healthtracker.telegram;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface UpdateHandler {
    void dispatch(Update update);
}
