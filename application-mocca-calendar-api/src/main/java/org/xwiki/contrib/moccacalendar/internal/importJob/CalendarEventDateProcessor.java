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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.stability.Unstable;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;

@Unstable
public class CalendarEventDateProcessor
{
    private Date startDate;

    private Date endDate;

    private Date recEndDate;

    private int isRecurrent;

    private String recurrenceFreq;

    private int allDay;

    private String timeZone;

    public CalendarEventDateProcessor()
    {
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public int getIsRecurrent()
    {
        return isRecurrent;
    }

    public String getRecurrenceFreq()
    {
        return recurrenceFreq;
    }

    public int getAllDay()
    {
        return allDay;
    }

    public Date getRecEndDate()
    {
        return recEndDate;
    }

    public void processEventDate(CalendarComponent component, String timeZone)
    {
        this.timeZone = timeZone;
        this.resetValues();
        processEventDates(component);
        checkRecurrence(component);
    }

    private void processEventDates(CalendarComponent component)
    {
        String dateStartValue = component.getProperty("DtStart").getValue();
        DateTimeFormatter dateTimeFormatter = getDateFormat(dateStartValue);
        if (dateStartValue.length() > 8) {
            processTimedEventDates(component, dateTimeFormatter);
        } else {
            processAllDayEventDates(component, dateTimeFormatter);
        }
    }

    private DateTimeFormatter getDateFormat(String dateValue)
    {
        DateTimeFormatter formatter;
        if (dateValue.length() > 8) {
            if (dateValue.endsWith("Z")) {
                formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");
            } else {
                formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
            }
        } else {
            formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        }
        return formatter;
    }

    private void processTimedEventDates(CalendarComponent component, DateTimeFormatter formatter)
    {
        Parameter timeZoneParameter = component.getProperty("DtStart").getParameter("TZID");
        String usedTimeZone;
        if(timeZoneParameter != null){
            usedTimeZone = timeZoneParameter.getValue();
        } else {
            usedTimeZone = this.timeZone;
        }
        LocalDateTime startDateTime = LocalDateTime.parse(component.getProperty("DtStart").getValue(), formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(component.getProperty("DtEnd").getValue(), formatter);
        this.startDate = Date.from(startDateTime.atZone(ZoneId.of(usedTimeZone)).toInstant());
        this.endDate = Date.from(endDateTime.atZone(ZoneId.of(usedTimeZone)).toInstant());
        this.allDay = 0;
    }

    private void processAllDayEventDates(CalendarComponent component, DateTimeFormatter formatter)
    {
        LocalDate startLocalDate = LocalDate.parse(component.getProperty("DtStart").getValue(), formatter);
        LocalDate endLocalDate = LocalDate.parse(component.getProperty("DtEnd").getValue(), formatter);
        this.startDate = Date.from(startLocalDate.atStartOfDay(ZoneId.of(this.timeZone)).toInstant());
        this.endDate = Date.from(endLocalDate.minusDays(1).atStartOfDay(ZoneId.of(this.timeZone)).toInstant());
        this.allDay = 1;
    }

    private void checkRecurrence(CalendarComponent component)
    {
        Property recProperty = component.getProperty("rrule");
        if (recProperty != null) {
            this.isRecurrent = 1;
            String recurrenceRule = recProperty.getValue();
            String untilPattern = "(?<=UNTIL=)[^;]+";
            String freqPattern = "(?<=FREQ=)[^;]+";
            String untilValue = extractValue(recurrenceRule, untilPattern);
            if (untilValue != null) {
                extractRecEndValue(untilValue);
            }
            String freqValue = extractValue(recurrenceRule, freqPattern);
            if (freqValue != null) {
                setRecurrenceInfo(freqValue, recurrenceRule);
            }
        }
    }

    private void extractRecEndValue(String untilValue)
    {
        DateTimeFormatter recDateTimeFormatter = getDateFormat(untilValue);
        if (untilValue.length() > 8) {
            LocalDateTime endRecDateTime = LocalDateTime.parse(untilValue, recDateTimeFormatter);
            this.recEndDate = Date.from(endRecDateTime.atZone(ZoneId.of(this.timeZone)).toInstant());
        } else {
            LocalDate endRecDateTime = LocalDate.parse(untilValue, recDateTimeFormatter);
            this.startDate = Date.from(endRecDateTime.atStartOfDay(ZoneId.of(this.timeZone)).toInstant());
        }
    }

    private void setRecurrenceInfo(String freqValue, String recurrenceRule)
    {
        if (freqValue.equals("WEEKLY")) {
            this.recurrenceFreq = freqValue.toLowerCase();
            checkWeeklyFrequency(recurrenceRule);
        } else {
            this.recurrenceFreq = freqValue.toLowerCase();
        }
    }

    private void checkWeeklyFrequency(String recurrenceRule)
    {
        String intervalPattern = "(?<=INTERVAL=)[^;]+";
        String bydayPattern = "(?<=BYDAY=)[^;]+";
        String bydayValue = extractValue(recurrenceRule, bydayPattern);
        String intervalValue = extractValue(recurrenceRule, intervalPattern);

        if (bydayValue != null && bydayValue.contains("MO") && bydayValue.contains("TU") && bydayValue.contains("WE")
            && bydayValue.contains("TH") && bydayValue.contains("FR"))
        {
            this.recurrenceFreq = "workdays";
        } else if (intervalValue != null && intervalValue.equals("2")) {
            this.recurrenceFreq = "biweekly";
        } else if (bydayValue != null && bydayValue.length() > 2) {
            this.isRecurrent = 0;
            this.recurrenceFreq = "";
        }
    }

    private void resetValues()
    {
        this.isRecurrent = 0;
        this.allDay = 0;
        this.endDate = null;
        this.startDate = null;
        this.recEndDate = null;
        this.recurrenceFreq = "";
    }

    private String extractValue(String rule, String pattern)
    {
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(rule);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
