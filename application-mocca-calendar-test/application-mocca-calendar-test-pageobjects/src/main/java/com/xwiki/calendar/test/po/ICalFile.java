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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Builds an iCalendar ({@code .ics}) file, e.g. to import its events in a calendar.
 *
 * @version $Id$
 * @since 2.22.1
 */
public class ICalFile
{
    private static final String CRLF = "\r\n";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringBuilder events = new StringBuilder();

    private int eventCount;

    /**
     * Adds an event with a start and end time.
     *
     * @param title the title of the event
     * @param description the description of the event
     * @param start the start of the event, in UTC
     * @param end the end of the event, in UTC
     * @return this file
     */
    public ICalFile addEvent(String title, String description, LocalDateTime start, LocalDateTime end)
    {
        return addEvent(title, description, "DTSTART:" + start.format(DATE_TIME), "DTEND:" + end.format(DATE_TIME),
            null);
    }

    /**
     * Adds a recurrent event.
     *
     * @param title the title of the event
     * @param start the start of the first instance, in UTC
     * @param end the end of the first instance, in UTC
     * @param frequency the iCalendar frequency of the instances, e.g. {@code DAILY}
     * @param lastStart the start of the last instance, in UTC
     * @return this file
     */
    public ICalFile addRecurrentEvent(String title, LocalDateTime start, LocalDateTime end, String frequency,
        LocalDateTime lastStart)
    {
        return addEvent(title, null, "DTSTART:" + start.format(DATE_TIME), "DTEND:" + end.format(DATE_TIME),
            "FREQ=" + frequency + ";UNTIL=" + lastStart.format(DATE_TIME));
    }

    /**
     * Adds an event lasting the whole day.
     *
     * @param title the title of the event
     * @param day the day of the event
     * @return this file
     */
    public ICalFile addAllDayEvent(String title, LocalDate day)
    {
        return addEvent(title, null, "DTSTART;VALUE=DATE:" + day.format(DATE),
            "DTEND;VALUE=DATE:" + day.plusDays(1).format(DATE), null);
    }

    /**
     * @return the content of the file, in the iCalendar format
     */
    @Override
    public String toString()
    {
        return "BEGIN:VCALENDAR" + CRLF + "VERSION:2.0" + CRLF + "PRODID:-//XWiki//Calendar Application tests//EN"
            + CRLF + this.events + "END:VCALENDAR" + CRLF;
    }

    /**
     * Changes the server of a URL, e.g. to give the XWiki server a URL of its own that it can reach: the browser and
     * the XWiki server may reach XWiki through different host names and ports, depending on where they run.
     *
     * @param url a URL
     * @param host the new host name
     * @param port the new port
     * @return the same URL, on the given server
     * @throws URISyntaxException if the URL is invalid
     */
    public static String withServer(String url, String host, int port) throws URISyntaxException
    {
        URI uri = new URI(url);
        return new URI(uri.getScheme(), null, host, port, uri.getPath(), uri.getQuery(), null).toString();
    }

    /**
     * Writes the iCalendar content to a file, e.g. to import it.
     *
     * @param directory the directory where to create the file
     * @param fileName the name of the file, e.g. {@code events.ics}
     * @return the {@code .ics} file
     * @throws IOException if the file cannot be written
     */
    public File writeTo(Path directory, String fileName) throws IOException
    {
        Path file = directory.resolve(fileName);
        Files.writeString(file, toString(), StandardCharsets.UTF_8);
        return file.toFile();
    }

    private ICalFile addEvent(String title, String description, String start, String end, String recurrenceRule)
    {
        this.eventCount++;
        this.events.append("BEGIN:VEVENT").append(CRLF)
            .append("UID:event-").append(this.eventCount).append("@calendar.test").append(CRLF)
            .append("DTSTAMP:").append(LocalDateTime.now(ZoneOffset.UTC).format(DATE_TIME)).append(CRLF)
            .append(start).append(CRLF)
            .append(end).append(CRLF)
            .append("SUMMARY:").append(title).append(CRLF);
        if (recurrenceRule != null) {
            this.events.append("RRULE:").append(recurrenceRule).append(CRLF);
        }
        if (description != null) {
            this.events.append("DESCRIPTION:").append(description).append(CRLF);
        }
        this.events.append("END:VEVENT").append(CRLF);
        return this;
    }
}
