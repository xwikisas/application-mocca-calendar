/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.moccacalendar.internal.importJob;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.importJob.result.MoccaCalendarEventResult;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;

/**
 * ics file import helper class used for processing the data related to the event duration from a
 * {@link CalendarComponent} extracted from an ical file, and mapping it to a {@link MoccaCalendarEventResult}.
 *
 * @version $Id$
 * @since 2.14
 */
@Component(roles = CalendarEventDurationProcessor.class)
@Singleton
public class CalendarEventDurationProcessor
{
    private static final String DEFAULT_TIME_ZONE = ZoneId.of("UTC").getId();

    private static final String WEEKLY_FREQUENCY = "weekly";

    /**
     * Check if an event takes all day.
     *
     * @param date the checked date.
     * @return {@code 1} if the event takes all day, or {@code 0} otherwise.
     */
    public int isAllDay(String date)
    {
        // In an ics file there is no flag to mark an event that takes all day. Instead, if an event takes all day,
        // the start and end date will only have the date and not also the time. This means the maximum length of
        // the date parameter will be 8 characters.
        if (date.length() > 8) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Extract the formatted date from the event, for a given event property.
     *
     * @param component the ical event.
     * @param propertyName the property for which the date is processed.
     * @return the {@link Date} of the event given property.
     */
    public Date getEventDate(CalendarComponent component, String propertyName)
    {
        String dateStartValue = component.getProperty(propertyName).getValue();
        DateTimeFormatter dateTimeFormatter = getDateFormat(dateStartValue);
        if (isAllDay(component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_START_DATE).getValue()) == 0) {
            return processTimedEventDates(component, dateTimeFormatter, propertyName);
        } else {
            return processAllDayEventDates(component, dateTimeFormatter, propertyName);
        }
    }

    /**
     * Check if an event is recurrent.
     *
     * @param component the ical event.
     * @return {@code 1} if the event is recurrent, or {@code 0} otherwise.
     */
    public int isRecurrentEvent(CalendarComponent component)
    {
        Property recProperty = component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_RECURRENCE_RULE);
        if (recProperty != null) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Process the date of the event recurrence end.
     *
     * @param component the ical event.
     * @return the {@link Date} of the event recurrence end.
     */
    public Date getRecurrenceEndDate(CalendarComponent component)
    {
        String recurrenceRule = component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_RECURRENCE_RULE).getValue();
        String untilPattern = "(?<=UNTIL=)[^;]+";
        String untilValue = extractValueByPattern(recurrenceRule, untilPattern);
        if (untilValue != null) {
            return extractRecEndValue(untilValue);
        } else {
            LocalDate localDateStartDate =
                this.getEventDate(component, CalendarKeys.ICS_CALENDAR_PROPERTY_START_DATE).toInstant()
                    .atZone(ZoneId.of(DEFAULT_TIME_ZONE)).toLocalDate();
            LocalDate futureLocalDate = localDateStartDate.plusYears(5);
            return Date.from(futureLocalDate.atStartOfDay(ZoneId.of(DEFAULT_TIME_ZONE)).toInstant());
        }
    }

    /**
     * Process the frequency of the event recurrence.
     *
     * @param component the ical event.
     * @return the frequency of the recurrence.
     */
    public Optional<String> getRecurrenceFrequency(CalendarComponent component)
    {
        if (isRecurrentEvent(component) == 1) {
            return Optional.of(processRecurrenceFrequency(
                component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_RECURRENCE_RULE).getValue()));
        }
        return Optional.empty();
    }

    private DateTimeFormatter getDateFormat(String dateValue)
    {
        DateTimeFormatter formatter;

        if (isAllDay(dateValue) == 0) {
            if (dateValue.endsWith("Z")) {
                // Used format for those date formats that already include the time zone.
                formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");
            } else {
                // Used format for those date formats that must have the time zone computed.
                formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
            }
        } else {
            // Used format for those events that take all day.
            formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        }
        return formatter;
    }

    private Date processTimedEventDates(CalendarComponent component, DateTimeFormatter formatter, String propertyName)
    {
        Parameter timeZoneParameter = component.getProperty(propertyName).getParameter("TZID");
        String usedTimeZone = DEFAULT_TIME_ZONE;
        if (timeZoneParameter != null) {
            usedTimeZone = timeZoneParameter.getValue();
        }
        LocalDateTime dateTime = LocalDateTime.parse(component.getProperty(propertyName).getValue(), formatter);
        return Date.from(dateTime.atZone(ZoneId.of(usedTimeZone)).toInstant());
    }

    private Date processAllDayEventDates(CalendarComponent component, DateTimeFormatter formatter, String propertyName)
    {
        LocalDate localDate = LocalDate.parse(component.getProperty(propertyName).getValue(), formatter);
        if (propertyName.equals(CalendarKeys.ICS_CALENDAR_PROPERTY_END_DATE)) {
            return Date.from(localDate.minusDays(1).atStartOfDay(ZoneId.of(DEFAULT_TIME_ZONE)).toInstant());
        } else {
            return Date.from(localDate.atStartOfDay(ZoneId.of(DEFAULT_TIME_ZONE)).toInstant());
        }
    }

    private Date extractRecEndValue(String untilValue)
    {
        DateTimeFormatter recDateTimeFormatter = getDateFormat(untilValue);
        if (isAllDay(untilValue) == 0) {
            LocalDateTime endRecDateTime = LocalDateTime.parse(untilValue, recDateTimeFormatter);
            return Date.from(endRecDateTime.atZone(ZoneId.of(DEFAULT_TIME_ZONE)).toInstant());
        } else {
            LocalDate endRecDateTime = LocalDate.parse(untilValue, recDateTimeFormatter);
            return Date.from(endRecDateTime.atStartOfDay(ZoneId.of(DEFAULT_TIME_ZONE)).toInstant());
        }
    }

    private String processRecurrenceFrequency(String recurrenceRule)
    {
        String freqValue = extractValueByPattern(recurrenceRule, "(?<=FREQ=)[^;]+");
        if (freqValue != null) {
            if (freqValue.equalsIgnoreCase(WEEKLY_FREQUENCY)) {
                return checkWeeklyFrequency(recurrenceRule);
            } else {
                return freqValue.toLowerCase();
            }
        }
        return "";
    }

    private String checkWeeklyFrequency(String recurrenceRule)
    {
        String byDayValue = extractValueByPattern(recurrenceRule, "(?<=BYDAY=)[^;]+");
        String intervalValue = extractValueByPattern(recurrenceRule, "(?<=INTERVAL=)[^;]+");

        if (isEveryWeekday(byDayValue)) {
            return "workdays";
        } else if (intervalValue != null && intervalValue.equals("2")) {
            return "biweekly";
        } else if (byDayValue != null && byDayValue.length() > 2) {
            return "";
        } else {
            return WEEKLY_FREQUENCY;
        }
    }

    private static boolean isEveryWeekday(String bydayValue)
    {
        if (bydayValue == null) {
            return false;
        }
        Set<String> requiredDays = new HashSet<>();
        requiredDays.add("MO");
        requiredDays.add("TU");
        requiredDays.add("WE");
        requiredDays.add("TH");
        requiredDays.add("FR");
        for (String day : requiredDays) {
            if (!bydayValue.contains(day)) {
                return false;
            }
        }

        return true;
    }

    private String extractValueByPattern(String rule, String pattern)
    {
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(rule);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
