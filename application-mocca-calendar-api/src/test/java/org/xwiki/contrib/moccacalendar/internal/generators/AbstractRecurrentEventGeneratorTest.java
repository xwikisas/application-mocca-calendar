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

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link AbstractRecurrentEventGenerator#jumpToFirstEventAfterDate(Calendar, long, Date)} variants of the
 * generators.
 *
 * @version $Id$
 */
class AbstractRecurrentEventGeneratorTest
{
    private static final long HOUR = TimeUnit.HOURS.toMillis(1);

    @Test
    void jumpToFirstEventAfterDateFindsTheSameInstanceAsIncrementing()
    {
        List<AbstractRecurrentEventGenerator> generators = List.of(new DailyEventGenerator(),
            new WorkDaysEventGenerator(), new WeeklyEventGenerator(), new BiWeeklyEventGenerator(),
            new CustomWeeklyEventGenerator(), new MonthlyEventGenerator(), new MonthlySpecificEventGenerator(),
            new QuarterlyEventGenerator(), new YearlyEventGenerator());
        Calendar start = Calendar.getInstance();
        start.clear();
        // A Saturday, so that the work days generator starts on a weekend.
        start.set(2021, Calendar.MARCH, 13, 9, 30);
        Calendar otherStart = (Calendar) start.clone();
        // A Wednesday late in the evening, so that instances lasting a day overlap the next one.
        otherStart.set(2019, Calendar.OCTOBER, 2, 23, 45);

        for (AbstractRecurrentEventGenerator generator : generators) {
            for (Calendar startDate : List.of(start, otherStart)) {
                for (long duration : new long[] { HOUR, 26 * HOUR }) {
                    // Every 29 hours over more than a year, to hit all the days of the week and times of the day.
                    for (long from = startDate.getTimeInMillis() - 50 * HOUR;
                        from < startDate.getTimeInMillis() + 10000 * HOUR; from += 29 * HOUR)
                    {
                        Date dateFrom = new Date(from);
                        Calendar expected = (Calendar) startDate.clone();
                        // Days of the month 2 and 13 are the 1st and 2nd occurrence of their day of the week.
                        int occurrence = (expected.get(Calendar.DAY_OF_MONTH) - 1) / 7 + 1;
                        while (expected.getTimeInMillis() + duration < from) {
                            generator.incrementCalendarByOnePeriod(expected, occurrence);
                        }
                        Calendar actual = (Calendar) startDate.clone();
                        generator.jumpToFirstEventAfterDate(actual, duration, dateFrom);

                        assertEquals(expected.getTime(), actual.getTime(),
                            () -> generator.getClass().getSimpleName() + " from " + dateFrom);
                    }
                }
            }
        }
    }

    @Test
    void jumpByWholePeriodsNeedsTwoExtraPeriodsWhenTheDayIsClamped()
    {
        // The whole months between January 31 10:00 and the target (February 28 11:00) round down to 0, and one
        // more month lands on February 28 10:00, still before the target.
        assertEquals(date(2026, Calendar.MARCH, 31),
            jump(1, date(2026, Calendar.JANUARY, 31), date(2026, Calendar.FEBRUARY, 28)));
        // Same with quarters: August 31 + 1 quarter is November 30, + 2 is February 28 10:00.
        assertEquals(date(2026, Calendar.MAY, 31),
            jump(3, date(2025, Calendar.AUGUST, 31), date(2026, Calendar.FEBRUARY, 28)));
    }

    @Test
    void jumpToFirstEventAfterDateGivesUpAfterMaxSkippedInstances()
    {
        // A daily generator without a jump of its own, so that it goes through the default loop.
        AbstractRecurrentEventGenerator generator = new AbstractRecurrentEventGenerator()
        {
            @Override
            protected void incrementCalendarByOnePeriod(Calendar cal, int... pos)
            {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        };
        Date start = date(2000, Calendar.JANUARY, 1);
        Calendar lastReachable = Calendar.getInstance();
        lastReachable.setTime(start);
        lastReachable.add(Calendar.DAY_OF_YEAR, AbstractRecurrentEventGenerator.MAX_SKIPPED_INSTANCES);

        // Reaching the instance at dateFrom takes exactly MAX_SKIPPED_INSTANCES increments.
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        generator.jumpToFirstEventAfterDate(cal, HOUR, lastReachable.getTime());
        assertEquals(lastReachable.getTime(), cal.getTime());

        // The instance at dateFrom ends before it, so the next one would take one more increment.
        cal.setTime(start);
        Date tooFar = new Date(lastReachable.getTimeInMillis() + 2 * HOUR);
        assertThrows(IllegalArgumentException.class, () -> generator.jumpToFirstEventAfterDate(cal, HOUR, tooFar));
    }

    /**
     * @return the start of the first instance of a one hour event starting at 10:00 on {@code start} and repeating
     *     every {@code months} months, ending at or after 12:00 on {@code from}
     */
    private static Date jump(int months, Date start, Date from)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        AbstractRecurrentEventGenerator.jumpByWholePeriods(cal, ChronoUnit.MONTHS, months, HOUR,
            new Date(from.getTime() + 2 * HOUR));
        return cal.getTime();
    }

    /**
     * @return the given day at 10:00
     */
    private static Date date(int year, int month, int day)
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month, day, 10, 0);
        return cal.getTime();
    }
}
