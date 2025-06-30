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
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.EventInstance;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A generator for weekly events on specific days.
 *
 * @version $Id: $
 * @since 2.16
 */
@Named("customWeekly")
@Singleton
@Component
public class CustomWeeklyEventGenerator extends AbstractRecurrentEventGenerator
{
    /**
     * increment the calendar by one week.
     */
    protected void incrementCalendarByOnePeriod(Calendar cal, int... pos)
    {
        cal.add(Calendar.WEEK_OF_YEAR, 1);
    }

    @Override
    protected List<EventInstance> createEventInstances(final XWikiDocument event, final Date startDate,
        final long duration, final Date dateFrom, final Date dateTo)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.set(Calendar.DAY_OF_WEEK, 7);
        while (cal.getTimeInMillis() + duration <= dateFrom.getTime()) {
            incrementCalendarByOnePeriod(cal);
        }
        List<Object> rawDays = (List<Object>) event.getListValue("days");
        List<Integer> days =
            rawDays.stream().map(Object::toString).map(Integer::parseInt).sorted().collect(Collectors.toList());
        List<EventInstance> eventInstances = new ArrayList<>();
        while (cal.getTime().compareTo(dateTo) <= 0) {
            for (int day : days) {
                cal.set(Calendar.DAY_OF_WEEK, day);
                long recurrenceTime = cal.getTimeInMillis();
                if (recurrenceTime >= dateFrom.getTime() && recurrenceTime + duration <= dateTo.getTime()) {
                    EventInstance instance = new EventInstance();
                    instance.setStartDate(new DateTime(recurrenceTime));
                    instance.setEndDate(new DateTime(recurrenceTime + duration));
                    eventInstances.add(instance);
                }
            }
            if (eventInstances.size() >= MAX_INSTANCES) {
                break;
            }
            cal.set(Calendar.DAY_OF_WEEK, 1);
            incrementCalendarByOnePeriod(cal);
        }

        return eventInstances;
    }
}

