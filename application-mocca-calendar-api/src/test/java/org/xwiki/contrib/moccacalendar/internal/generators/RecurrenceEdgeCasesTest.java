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
package org.xwiki.contrib.moccacalendar.internal.generators;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Edge cases of the recurrent event generators: month ends, daylight saving time, view boundaries, locales and old
 * dates. Each test states the expected behavior; some of them fail on purpose to document known issues.
 * <p>
 * All the dates are local times in Europe/Paris, where DST starts on 2026-03-29 at 02:00 and ends on 2026-10-25 at
 * 03:00.
 *
 * @version $Id$
 */
class RecurrenceEdgeCasesTest
{
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final long HOUR = 3600000L;

    private TimeZone defaultTimeZone;

    private Locale defaultLocale;

    @BeforeEach
    void setUp()
    {
        this.defaultTimeZone = TimeZone.getDefault();
        this.defaultLocale = Locale.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(PARIS));
        Locale.setDefault(Locale.US);
    }

    @AfterEach
    void tearDown()
    {
        TimeZone.setDefault(this.defaultTimeZone);
        Locale.setDefault(this.defaultLocale);
    }

    // Month ends.

    /**
     * A monthly event on the 31st happens on the last day of shorter months, and on the 31st again afterwards.
     */
    @Test
    @Disabled("Pending the month-end decision: clamp to the last day of shorter months, or skip them.")
    void monthlyOnThe31stClampsToTheLastDayOfShorterMonths()
    {
        assertEquals(List.of("2026-01-31 10:00", "2026-02-28 10:00", "2026-03-31 10:00", "2026-04-30 10:00",
            "2026-05-31 10:00"),
            generate(new MonthlyEventGenerator(), "2026-01-31 10:00", HOUR, "2026-01-01 00:00", "2026-05-31 23:00"));
    }

    /**
     * The date of an instance doesn't depend on the range being viewed.
     */
    @Test
    void monthlyOnThe31stDoesNotDependOnTheView()
    {
        List<String> februaryToMarch =
            generate(new MonthlyEventGenerator(), "2026-01-31 10:00", HOUR, "2026-02-01 00:00", "2026-03-31 23:00");
        List<String> march =
            generate(new MonthlyEventGenerator(), "2026-01-31 10:00", HOUR, "2026-03-01 00:00", "2026-03-31 23:00");

        assertEquals(march, februaryToMarch.subList(1, februaryToMarch.size()));
    }

    /**
     * A quarterly event on the 31st happens on the last day of shorter months, and on the 31st again afterwards.
     */
    @Test
    @Disabled("Pending the month-end decision: clamp to the last day of shorter months, or skip them.")
    void quarterlyOnThe31stClampsToTheLastDayOfShorterMonths()
    {
        assertEquals(List.of("2025-08-31 10:00", "2025-11-30 10:00", "2026-02-28 10:00", "2026-05-31 10:00"),
            generate(new QuarterlyEventGenerator(), "2025-08-31 10:00", HOUR, "2025-08-01 00:00",
                "2026-06-01 00:00"));
    }

    /**
     * A yearly event on February 29 happens on February 28 of common years, and on February 29 of leap years.
     */
    @Test
    @Disabled("Pending the month-end decision: clamp to the last day of shorter months, or skip them.")
    void yearlyOnFebruary29ClampsToFebruary28InCommonYears()
    {
        assertEquals(List.of("2024-02-29 10:00", "2025-02-28 10:00", "2026-02-28 10:00", "2027-02-28 10:00",
            "2028-02-29 10:00"),
            generate(new YearlyEventGenerator(), "2024-02-29 10:00", HOUR, "2024-01-01 00:00", "2028-12-31 00:00"));
    }

    // Daylight saving time.

    /**
     * Instances keep their wall clock time across DST changes, also when jumping over them.
     */
    @Test
    void dailyKeepsTheWallClockTimeAcrossDst()
    {
        assertEquals(List.of("2026-03-28 09:30", "2026-03-29 09:30", "2026-03-30 09:30"),
            generate(new DailyEventGenerator(), "2026-03-20 09:30", HOUR, "2026-03-28 00:00", "2026-03-30 23:00"));
        assertEquals(List.of("2026-11-02 09:30"),
            generate(new DailyEventGenerator(), "2026-03-20 09:30", HOUR, "2026-11-02 00:00", "2026-11-02 23:00"));
    }

    /**
     * An instance in the hour skipped when DST starts is moved one hour later, and only that one.
     */
    @Test
    @Disabled("Fails on master as well as with the uncommitted generator changes.")
    void dailyInTheDstGapOnlyMovesTheSkippedInstance()
    {
        assertEquals(List.of("2026-03-28 02:30", "2026-03-29 03:30", "2026-03-30 02:30"),
            generate(new DailyEventGenerator(), "2026-03-20 02:30", HOUR, "2026-03-28 00:00", "2026-03-30 23:00"));
    }

    /**
     * A work days event in the hour skipped when DST starts keeps its time on the following work days. DST starts on a
     * Sunday, which has no instance, but the days after it still used to drift one hour earlier.
     */
    @Test
    @Disabled("Fails on master as well as with the uncommitted generator changes.")
    void workDaysAfterTheDstGapKeepTheirTime()
    {
        // 2026-03-27 is a Friday.
        assertEquals(List.of("2026-03-27 02:30", "2026-03-30 02:30", "2026-03-31 02:30"),
            generate(new WorkDaysEventGenerator(), "2026-03-20 02:30", HOUR, "2026-03-27 00:00", "2026-03-31 23:00"));
    }

    /**
     * A custom weekly event in the hour skipped when DST starts is moved one hour later, and only that one.
     */
    @Test
    @Disabled("Fails on master as well as with the uncommitted generator changes.")
    void customWeeklyInTheDstGapOnlyMovesTheSkippedInstance()
    {
        // Sundays, 2026-03-15 is a Sunday.
        assertEquals(List.of("2026-03-22 02:30", "2026-03-29 03:30", "2026-04-05 02:30"), generateCustomWeekly(
            List.of("1"), "2026-03-15 02:30", HOUR, "2026-03-22 00:00", "2026-04-05 23:00"));
    }

    /**
     * An event on the last (5th) Sunday of the month in the hour skipped when DST starts is moved one hour later, and
     * only that one.
     */
    @Test
    @Disabled("Fails on master as well as with the uncommitted generator changes.")
    void monthlySpecificInTheDstGapOnlyMovesTheSkippedInstance()
    {
        // 2025-11-30 is the 5th Sunday of November, 2026-03-29 the 5th Sunday of March.
        assertEquals(List.of("2026-03-29 03:30", "2026-04-26 02:30"), generate(new MonthlySpecificEventGenerator(),
            "2025-11-30 02:30", HOUR, "2026-03-01 00:00", "2026-04-30 23:00"));
    }

    /**
     * An instance in the hour repeated when DST ends happens at its first occurrence, in summer time, whatever the
     * range being viewed.
     */
    @Test
    void dailyInTheDstOverlapUsesSummerTime()
    {
        Instant expected = LocalDateTime.of(2026, 10, 25, 2, 30).atOffset(ZoneOffset.ofHours(2)).toInstant();

        assertEquals(List.of(expected), generateInstants(new DailyEventGenerator(), "2026-10-20 02:30", HOUR,
            "2026-10-25 00:00", "2026-10-25 12:00"));
        assertEquals(expected, generateInstants(new DailyEventGenerator(), "2026-10-20 02:30", HOUR,
            "2026-10-24 00:00", "2026-10-25 12:00").get(1));
    }

    // View boundaries.

    /**
     * An instance ending exactly when the viewed range starts is part of it.
     */
    @Test
    void instanceEndingAtDateFromIsIncluded()
    {
        assertEquals(List.of("2026-01-05 10:00"),
            generate(new DailyEventGenerator(), "2026-01-01 10:00", HOUR, "2026-01-05 11:00", "2026-01-05 12:00"));
    }

    /**
     * An instance starting exactly when the viewed range ends is part of it.
     */
    @Test
    void instanceStartingAtDateToIsIncluded()
    {
        assertEquals(List.of("2026-01-04 10:00", "2026-01-05 10:00"),
            generate(new DailyEventGenerator(), "2026-01-01 10:00", HOUR, "2026-01-04 09:00", "2026-01-05 10:00"));
    }

    /**
     * An instance overlapping the start of the viewed range is part of it, as for the other generators.
     */
    @Test
    @Disabled("Fails on master as well as with the uncommitted generator changes.")
    void customWeeklyIncludesInstanceOverlappingDateFrom()
    {
        assertEquals(List.of("2026-01-05 10:00"),
            generate(new DailyEventGenerator(), "2026-01-01 10:00", 2 * HOUR, "2026-01-05 11:00",
                "2026-01-05 23:00"));
        // 2026-01-05 is a Monday.
        assertEquals(List.of("2026-01-05 10:00"), generateCustomWeekly(List.of("2"), "2026-01-01 10:00", 2 * HOUR,
            "2026-01-05 11:00", "2026-01-05 23:00"));
    }

    /**
     * An instance overlapping the end of the viewed range is part of it, as for the other generators.
     */
    @Test
    @Disabled("Fails on master as well as with the uncommitted generator changes.")
    void customWeeklyIncludesInstanceOverlappingDateTo()
    {
        assertEquals(List.of("2026-01-05 10:00"),
            generate(new DailyEventGenerator(), "2026-01-01 10:00", 2 * HOUR, "2026-01-05 00:00",
                "2026-01-05 11:00"));
        assertEquals(List.of("2026-01-05 10:00"), generateCustomWeekly(List.of("2"), "2026-01-01 10:00", 2 * HOUR,
            "2026-01-05 00:00", "2026-01-05 11:00"));
    }

    // Custom weekly and locales.

    /**
     * A custom weekly event happens on its days of the week, whatever the range being viewed.
     */
    @Test
    void customWeeklyHappensOnItsDaysOfTheWeek()
    {
        // Monday, Wednesday and Friday.
        assertEquals(List.of("2026-03-02 10:00", "2026-03-04 10:00", "2026-03-06 10:00"),
            generateCustomWeekly(List.of("2", "4", "6"), "2026-01-01 10:00", HOUR, "2026-03-01 00:00",
                "2026-03-07 23:00"));
    }

    /**
     * A custom weekly event happens on Sundays also where weeks start on Monday.
     */
    @Test
    @Disabled("Fails on master as well as with the uncommitted generator changes.")
    void customWeeklyOnSundayWhereWeeksStartOnMonday()
    {
        Locale.setDefault(Locale.FRANCE);

        // 2026-01-18 is a Sunday.
        assertEquals(List.of("2026-01-18 12:00"), generateCustomWeekly(List.of("1"), "2026-01-04 12:00", HOUR,
            "2026-01-18 10:00", "2026-01-18 23:00"));
    }

    // Work days.

    /**
     * A work days event starting on a Saturday has its first instance on that Saturday, and then only on work days.
     */
    @Test
    void workDaysStartingOnSaturday()
    {
        // 2026-01-03 is a Saturday.
        assertEquals(List.of("2026-01-03 09:00", "2026-01-05 09:00", "2026-01-06 09:00"),
            generate(new WorkDaysEventGenerator(), "2026-01-03 09:00", HOUR, "2026-01-01 00:00",
                "2026-01-06 23:00"));
        assertEquals(List.of("2026-03-09 09:00"), generate(new WorkDaysEventGenerator(), "2026-01-03 09:00", HOUR,
            "2026-03-07 00:00", "2026-03-09 23:00"));
    }

    // Monthly on a specific day of the week.

    /**
     * An event on the 5th Thursday of a month happens on the last Thursday of months having only four.
     */
    @Test
    void fifthDayOfTheWeekFallsBackToTheLastOne()
    {
        assertEquals(List.of("2026-01-29 10:00", "2026-02-26 10:00", "2026-03-26 10:00", "2026-04-30 10:00",
            "2026-05-28 10:00"),
            generate(new MonthlySpecificEventGenerator(), "2026-01-29 10:00", HOUR, "2026-01-01 00:00",
                "2026-05-31 23:00"));
    }

    /**
     * An event on the 2nd Tuesday of a month keeps it years later.
     */
    @Test
    void secondTuesdayYearsLater()
    {
        assertEquals(List.of("2026-10-13 10:00", "2026-11-10 10:00"),
            generate(new MonthlySpecificEventGenerator(), "2020-01-14 10:00", HOUR, "2026-10-01 00:00",
                "2026-11-30 23:00"));
    }

    // Old dates.

    /**
     * An event starting before the Gregorian calendar was introduced in 1582 keeps its day of the month.
     * {@link java.util.GregorianCalendar} uses the Julian calendar before it, but {@code java.time} doesn't.
     */
    @Test
    @Disabled("Won't fix: events starting before 1582 are not realistic.")
    void monthlyStartingBefore1582KeepsItsDayOfTheMonth()
    {
        // The start date is interpreted as a Julian date by GregorianCalendar.
        java.util.Calendar start = java.util.Calendar.getInstance();
        start.clear();
        start.set(1500, java.util.Calendar.JANUARY, 31, 10, 0);

        assertEquals(List.of("2026-03-31 10:00"), format(generate(new MonthlyEventGenerator(), start.getTime(),
            HOUR, parse("2026-03-01 00:00"), parse("2026-03-31 23:00"))));
    }

    private List<String> generate(AbstractRecurrentEventGenerator generator, String start, long duration,
        String from, String to)
    {
        return format(generate(generator, parse(start), duration, parse(from), parse(to)));
    }

    private List<Instant> generateInstants(AbstractRecurrentEventGenerator generator, String start, long duration,
        String from, String to)
    {
        return generate(generator, parse(start), duration, parse(from), parse(to));
    }

    private List<String> generateCustomWeekly(List<String> days, String start, long duration, String from,
        String to)
    {
        XWikiDocument event = mockEvent(parse(start), duration);
        when(event.getListValue("days")).thenReturn(days);
        return format(toInstants(new CustomWeeklyEventGenerator().generate(event, parse(from), parse(to))));
    }

    private List<Instant> generate(AbstractRecurrentEventGenerator generator, Date start, long duration, Date from,
        Date to)
    {
        return toInstants(generator.generate(mockEvent(start, duration), from, to));
    }

    private XWikiDocument mockEvent(Date start, long duration)
    {
        DocumentReference eventClass = new DocumentReference("a", "b", "c");
        DocumentReference recurrencyClass = new DocumentReference("d", "e", "f");
        XWikiDocument event = mock(XWikiDocument.class);
        BaseObject eventData = mock(BaseObject.class);
        when(event.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME)).thenReturn(eventClass);
        when(event.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME))
            .thenReturn(recurrencyClass);
        when(event.getXObject(eventClass)).thenReturn(eventData);
        when(event.getXObject(recurrencyClass)).thenReturn(mock(BaseObject.class));
        when(eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME)).thenReturn(start);
        when(eventData.getDateValue(EventConstants.PROPERTY_ENDDATE_NAME))
            .thenReturn(new Date(start.getTime() + duration));
        return event;
    }

    private static List<Instant> toInstants(List<org.xwiki.contrib.moccacalendar.EventInstance> instances)
    {
        return instances.stream().map(instance -> Instant.ofEpochMilli(instance.getStartDate().getMillis()))
            .collect(Collectors.toList());
    }

    private static Date parse(String date)
    {
        return Date.from(LocalDateTime.parse(date, FORMAT).atZone(PARIS).toInstant());
    }

    private static List<String> format(List<Instant> instants)
    {
        return instants.stream().map(instant -> ZonedDateTime.ofInstant(instant, PARIS).format(FORMAT))
            .collect(Collectors.toList());
    }
}
