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

    @Inject
    private Logger logger;

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

    // separate helper method to actually create the events
    // to keep checkstyle from complaining
    protected List<EventInstance> createEventInstances(final XWikiDocument event, final Date startDate,
        final long duration, final Date dateFrom, final Date dateTo)
    {
        Calendar cal = Calendar.getInstance();
    
        cal.setTime(startDate);
    
        // stupid, inefficient, but should work
        while (cal.getTimeInMillis() + duration < dateFrom.getTime()) {
            incrementCalendarByOnePeriod(cal);
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
        }
    
        return eventInstances;
    }

}
