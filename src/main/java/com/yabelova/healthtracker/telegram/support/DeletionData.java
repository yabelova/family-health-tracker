package com.yabelova.healthtracker.telegram.support;

/**
 * Данные о записи из списка удаления: id записи и подпись для кнопки.
 */
public record DeletionData(Integer id, String label) {
}
