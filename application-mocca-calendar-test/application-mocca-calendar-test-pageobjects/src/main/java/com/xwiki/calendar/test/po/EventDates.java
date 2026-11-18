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
package com.xwiki.calendar.test.po;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Computes the event dates typed in the event form, relative to a reference day: the day the tests started, in UTC (the
 * time zone of the browser container). The reference day doesn't change during the tests, even if they run past
 * midnight; the calendars show it with {@link CalendarElement#showToday(CalendarElement.View)}.
 * <p>
 * The tests assume that the browser and the XWiki server both use UTC, which is the case when both run in Docker
 * containers (the default). With a servlet engine outside Docker, on a machine in another time zone, the dates typed
 * in the browser and the dates stored by the server can be on different days, e.g. for events close to midnight, and
 * so can the times of the {@code .ics} files built by {@link ICalFile}, which are in UTC.
 *
 * @version $Id$
 * @since 2.22.1
 */
public final class EventDates
{
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final LocalDate REFERENCE_DAY = LocalDate.now(ZoneOffset.UTC);

    private EventDates()
    {
    }

    /**
     * @param daysFromToday the number of days to add to the reference day
     * @param time the time, in the {@code HH:mm} format
     * @return the date in the {@code yyyy/MM/dd HH:mm} format expected by the event form
     */
    public static String at(int daysFromToday, String time)
    {
        return day(daysFromToday) + ' ' + time;
    }

    /**
     * @param daysFromToday the number of days to add to the reference day
     * @return the day in the {@code yyyy/MM/dd} format expected by the event form for all day events
     */
    public static String day(int daysFromToday)
    {
        return date(daysFromToday).format(DAY_FORMAT);
    }

    /**
     * @param daysFromToday the number of days to add to the reference day
     * @return the date
     */
    public static LocalDate date(int daysFromToday)
    {
        return REFERENCE_DAY.plusDays(daysFromToday);
    }
}
