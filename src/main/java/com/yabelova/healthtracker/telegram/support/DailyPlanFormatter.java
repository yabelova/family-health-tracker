package com.yabelova.healthtracker.telegram.support;

import com.yabelova.healthtracker.service.DailyPlanBuilder.CourseLine;
import com.yabelova.healthtracker.service.DailyPlanBuilder.DailyPlan;
import com.yabelova.healthtracker.service.DailyPlanBuilder.DailyPlanProfile;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Форматирование плана на сегодня в текст сообщения.
 */
@Component
public class DailyPlanFormatter {

    public String format(DailyPlan plan) {
        String body = plan.profiles().stream()
                .map(this::formatProfile)
                .collect(Collectors.joining("\n"));
        return BotTexts.DAILY_PLAN_HEADER + "\n\n" + body;
    }

    private String formatProfile(DailyPlanProfile profile) {
        String courseLines = profile.courseLines().stream()
                .map(this::formatCourse)
                .collect(Collectors.joining("\n  "));
        return BotTexts.DAILY_PLAN_PROFILE.formatted(
                HtmlUtils.bold(profile.profileName()),
                courseLines);
    }

    private String formatCourse(CourseLine course) {
        if (course.dosesPerDay() == null) {
            return HtmlUtils.escape(course.medication());
        }
        return BotTexts.DAILY_PLAN_COURSE_LINE.formatted(
                HtmlUtils.escape(course.medication()),
                course.dosesPerDay());
    }
}
