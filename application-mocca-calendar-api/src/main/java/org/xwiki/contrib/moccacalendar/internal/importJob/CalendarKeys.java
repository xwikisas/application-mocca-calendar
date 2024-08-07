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

/**
 * A list of often used constants in an ical calendar.
 *
 * @version $Id$
 * @since 2.14
 */
public final class CalendarKeys
{
    /**
     * Calendar summary propriety.
     */
    public static final String ICS_CALENDAR_PROPERTY_SUMMARY = "SUMMARY";

    /**
     * Calendar description propriety.
     */
    public static final String ICS_CALENDAR_PROPERTY_DESCRIPTION = "DESCRIPTION";

    /**
     * Calendar uid propriety.
     */
    public static final String ICS_CALENDAR_PROPERTY_UID = "UID";

    /**
     * Calendar location propriety.
     */
    public static final String ICS_CALENDAR_PROPERTY_LOCATION = "LOCATION";

    /**
     * Calendar date start propriety.
     */
    public static final String ICS_CALENDAR_PROPERTY_START_DATE = "DTSTART";

    /**
     * Calendar date end propriety.
     */
    public static final String ICS_CALENDAR_PROPERTY_END_DATE = "DTEND";

    /**
     * Calendar organizer propriety.
     */
    public static final String ICS_CALENDAR_PROPERTY_ORGANIZER = "ORGANIZER";

    /**
     * Calendar attendee propriety.
     */
    public static final String ICS_CALENDAR_PROPERTY_ATTENDEE = "ATTENDEE";

    /**
     * Calendar recurrence rule propriety.
     */
    public static final String ICS_CALENDAR_PROPERTY_RECURRENCE_RULE = "RRULE";

    /**
     * Calendar end key.
     */
    public static final String ICS_CALENDAR_CALENDAR_END = "END:VCALENDAR";

    /**
     * Calendar event key.
     */
    public static final String ICS_CALENDAR_CALENDAR_EVENT = "VEVENT";

    /**
     * Calendar begin key.
     */
    public static final String ICS_CALENDAR_CALENDAR_BEGIN = "BEGIN:VCALENDAR";

    private CalendarKeys()
    {

    }
}
