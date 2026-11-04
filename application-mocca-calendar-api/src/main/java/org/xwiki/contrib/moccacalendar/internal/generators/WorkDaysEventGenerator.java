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
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * A generator for events happening every working day.
 *
 * @version $Id: $
 * @since 2.7
 */
@Component
@Singleton
@Named("workdays")
public class WorkDaysEventGenerator extends AbstractRecurrentEventGenerator
{
    /**
     * increment the calendar by one day, skipping weekends.
     * TODO: configure which days are weekdays
     */
    protected void incrementCalendarByOnePeriod(Calendar cal, int... pos)
    {
        do {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        } while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
            || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);
    }

    @Override
    protected void jumpToFirstEventAfterDate(Calendar cal, long duration, Date dateFrom)
    {
        // Jumping whole weeks can skip the work days between dateFrom and the landing day, and would land on a weekend
        // if the event started on one. So stop more than a week before dateFrom (8 days, in case of a DST change) and
        // increment the remaining days one by one, which only stops on work days.
        jumpByWholePeriods(cal, ChronoUnit.WEEKS, 1, duration,
            new Date(dateFrom.getTime() - TimeUnit.DAYS.toMillis(8)));
        super.jumpToFirstEventAfterDate(cal, duration, dateFrom);
    }
}
