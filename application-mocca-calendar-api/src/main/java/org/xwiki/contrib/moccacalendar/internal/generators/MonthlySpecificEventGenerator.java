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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.EventInstance;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A generator for monthly events that take place on a specific day.
 *
 * @version $Id: $
 * @since 2.16
 */
@Component
@Singleton
@Named("monthlySpecific")
public class MonthlySpecificEventGenerator extends AbstractRecurrentEventGenerator
{
    /**
     * increment the calendar by one month, to a specific day.
     */
    protected void incrementCalendarByOnePeriod(Calendar cal, int... pos)
    {
        int originalDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, 1);
        int newMonth = cal.get(Calendar.MONTH);

        int weekdayCount = 0;
        int lastOccurrenceDay = 1;
        while (cal.get(Calendar.MONTH) == newMonth) {
            if (cal.get(Calendar.DAY_OF_WEEK) == originalDayOfWeek) {
                lastOccurrenceDay = cal.get(Calendar.DAY_OF_MONTH);
                weekdayCount++;
                if (weekdayCount == pos[0]) {
                    return;
                }
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.DAY_OF_MONTH, lastOccurrenceDay);
    }

    @Override
    protected List<EventInstance> createEventInstances(final XWikiDocument event, final Date startDate,
        final long duration, final Date dateFrom, final Date dateTo)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        int position = getPosition(cal);
        while (cal.getTimeInMillis() + duration < dateFrom.getTime()) {
            incrementCalendarByOnePeriod(cal, position);
        }
        List<EventInstance> eventInstances = new ArrayList<>();
        while (cal.getTime().compareTo(dateTo) <= 0) {
            EventInstance instance = new EventInstance();
            instance.setStartDate(new DateTime(cal.getTimeInMillis()));
            instance.setEndDate(new DateTime(cal.getTimeInMillis() + duration));

            eventInstances.add(instance);

            if (eventInstances.size() >= MAX_INSTANCES) {
                break;
            }

            incrementCalendarByOnePeriod(cal, position);
        }

        return eventInstances;
    }

    private int getPosition(Calendar cal)
    {
        int originalDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int originalDay = cal.get(Calendar.DAY_OF_MONTH);
        int originalMonth = cal.get(Calendar.MONTH);

        Calendar temp = (Calendar) cal.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);
        int occurrence = 0;
        while (temp.get(Calendar.MONTH) == originalMonth && temp.get(Calendar.DAY_OF_MONTH) <= originalDay) {
            if (temp.get(Calendar.DAY_OF_WEEK) == originalDayOfWeek) {
                occurrence++;
            }
            temp.add(Calendar.DAY_OF_MONTH, 1);
        }
        return occurrence;
    }
}
