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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.xwiki.contrib.moccacalendar.EventInstance;
import org.xwiki.contrib.moccacalendar.RecurrentEventGenerator;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.contrib.moccacalendar.internal.Utils;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class to create recurrent events if the frequency is sufficient regular allow this.
 * Concrete subclasses must implement the {@link #incrementCalendarByOnePeriod(Calendar)} method.
 *
 * @version $Id: $
 * @since 2.7
 */
public abstract class AbstractRecurrentEventGenerator implements RecurrentEventGenerator
{

    /**
     * a limit for the maximum of generated instance.
     * this is mainly used to avoid crashes due to a runaway generator.
     */
    protected static final int MAX_INSTANCES = 1000;

    /**
     * A limit for how many event instances to generate to reach the requested calendar view. Used to avoid excessive
     * computation of unrealistic calendar views (e.g. viewing events starting in year 2300) 100.000 daily events ~= 273
     * years
     */
    protected static final int MAX_SKIPPED_INSTANCES = 100_000;

    @Inject
    protected Logger logger;

    /**
     * increment the calendar by the period of the event.
     * E.g. in case of weekly events increment it by one week.
     * @param cal the calendar to increment; should never be null
     */
    protected abstract void incrementCalendarByOnePeriod(Calendar cal, int... pos);
    
    /**
     * generate a list of event instances for the given date range
     * by incrementing the calendar from the start date until it has covered
     * the complete date range.
     * @see {@link RecurrentEventGenerator#generate(XWikiDocument, Date, Date)}
     */
    @Override
    public List<EventInstance> generate(final XWikiDocument event, final Date dateFrom, final Date dateTo)
    {
        BaseObject eventData = event
            .getXObject(event.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME));
        BaseObject eventRecData = event
            .getXObject(event.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME));
    
        Date startDate = eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME);
        Date endDate = eventData.getDateValue(EventConstants.PROPERTY_ENDDATE_NAME);
        final boolean allDay = eventData.getIntValue(EventConstants.PROPERTY_ALLDAY_NAME) == 1;
        if (allDay) {
            // we need to cut out the start time:
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            startDate = cal.getTime();
        }
        if (endDate == null) {
            endDate = Utils.guessEndDate(startDate, allDay);
        }
        final long duration = endDate.getTime() - startDate.getTime();
    
        Date firstInstance = eventRecData.getDateValue(EventConstants.PROPERTY_FIRSTINSTANCE_NAME);
        Date lastInstance = eventRecData.getDateValue(EventConstants.PROPERTY_LASTINSTANCE_NAME);
    
        Date actualDateFrom = dateFrom;
        if (firstInstance != null && firstInstance.after(dateFrom)) {
            actualDateFrom = firstInstance;
        }
        Date actualDateTo = dateTo;
        // FIXME: this is likely not to be correct
        if (lastInstance != null && lastInstance.before(dateTo)) {
            actualDateTo = lastInstance;
        }
    
        if (dateTo.before(actualDateFrom)) {
            return Collections.emptyList();
        }
    
        return createEventInstances(event, startDate, duration, actualDateFrom, actualDateTo);
    }

    /**
     * Implementation helper for {@link #jumpToFirstEventAfterDate(Calendar, long, Date)} for events repeating every
     * {@code amount} {@code unit}s. The instance is counted from the start of the event, so that e.g. a monthly event
     * on the 31st still lands on March 31 after jumping over February.
     *
     * @param cal the calendar, set to the start of the first instance of the event
     * @param unit the unit of the period of the event
     * @param amount the number of units in the period of the event
     * @param duration the duration of an instance in milliseconds
     * @param dateFrom the date at or after which the instance should end
     */
    protected static void jumpByWholePeriods(Calendar cal, ChronoUnit unit, int amount, long duration, Date dateFrom)
    {
        ZonedDateTime start = cal.toInstant().atZone(cal.getTimeZone().toZoneId());
        ZonedDateTime target = Instant.ofEpochMilli(dateFrom.getTime() - duration).atZone(start.getZone());
        // Adding the whole periods between them to the start doesn't go past the target.
        long periods = Math.max(unit.between(start, target) / amount, 0);
        ZonedDateTime first = start.plus(periods * amount, unit);
        // The periods computed above are rounded down, so reaching the target date can take one extra period. When the
        // date gets clamped (e.g. January 31 + 1 month = February 28), a second extra period is needed, otherwise it
        // can still fall short.
        for (int extra = 1; extra <= 2 && first.isBefore(target); extra++) {
            first = start.plus((periods + extra) * amount, unit);
        }
        cal.setTimeInMillis(first.toInstant().toEpochMilli());
    }

    /**
     * Set the time of day of the calendar back to the one of the start of the event. Moving a calendar through the hour
     * skipped when DST starts shifts its time, and the shift would otherwise carry over to the next instances.
     *
     * @param cal the calendar to fix
     * @param start the start of the event
     */
    protected static void resetTimeOfDay(Calendar cal, Calendar start)
    {
        // Only the fields that changed: setting the same time again would move an instance in the hour repeated when
        // DST ends from its first occurrence (summer time) to its second one.
        for (int field : new int[] { Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND }) {
            if (cal.get(field) != start.get(field)) {
                cal.set(field, start.get(field));
            }
        }
    }

    // separate helper method to actually create the events
    // to keep checkstyle from complaining
    protected List<EventInstance> createEventInstances(final XWikiDocument event, final Date startDate,
        final long duration, final Date dateFrom, final Date dateTo)
    {
        Calendar cal = Calendar.getInstance();
    
        cal.setTime(startDate);
        Calendar start = (Calendar) cal.clone();

        try {
            jumpToFirstEventAfterDate(cal, duration, dateFrom);
        } catch (IllegalArgumentException e) {
            this.logger.warn("Failed to find the first instance of [{}] after [{}]: [{}]", event, dateFrom,
                e.getMessage());
            return Collections.emptyList();
        }

        List<EventInstance> eventInstances = new ArrayList<>();
        // FIXME: isn't it endDate instead of the start date that should be after dateFrom instead ?
        while (cal.getTime().compareTo(dateTo) <= 0) {
            EventInstance instance = new EventInstance();
            instance.setStartDate(new DateTime(cal.getTimeInMillis()));
            instance.setEndDate(new DateTime(cal.getTimeInMillis() + duration));
    
            eventInstances.add(instance);
    
            if (eventInstances.size() >= MAX_INSTANCES) {
                logger.info("maximal number of events generated for [{}]; stopping", event);
                break;
            }

            incrementCalendarByOnePeriod(cal);
            resetTimeOfDay(cal, start);
        }
    
        return eventInstances;
    }

    /**
     * Move the calendar from the start of the first instance of the recurrent event to the start of the first instance
     * ending at or after {@code dateFrom}. This implementation increments it one period at a time; subclasses can
     * override it to jump there directly.
     *
     * @param cal the calendar, set to the start of the first instance of the event
     * @param duration the duration of an instance in milliseconds
     * @param dateFrom the date at or after which the instance should end
     * @throws IllegalArgumentException if reaching that instance takes more than a million periods
     */
    protected void jumpToFirstEventAfterDate(Calendar cal, final long duration, final Date dateFrom)
    {
        Calendar start = (Calendar) cal.clone();
        // Give up after a million periods (about 2,740 years of daily events) instead of looping for a long time.
        for (int increments = 0; cal.getTimeInMillis() + duration < dateFrom.getTime(); increments++) {
            if (increments >= 1_000_000) {
                throw new IllegalArgumentException("Recurrent event starts too far in the past.");
            }
            incrementCalendarByOnePeriod(cal);
            resetTimeOfDay(cal, start);
        }
    }
}
