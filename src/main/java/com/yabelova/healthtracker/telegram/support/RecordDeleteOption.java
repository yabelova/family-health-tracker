package com.yabelova.healthtracker.telegram.support;

/**
 * Опция для списка удаляемых записей: id записи и подпись кнопки
 */
public record RecordDeleteOption(Integer id, String label) {
}
