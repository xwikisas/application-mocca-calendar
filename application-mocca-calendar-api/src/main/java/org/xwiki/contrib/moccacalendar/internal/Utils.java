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
package org.xwiki.contrib.moccacalendar.internal;

import java.util.Date;

import org.xwiki.contrib.moccacalendar.EventInstance;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Some internal helper methods that did not fit in elsewhere. Do not expect
 * these helpers or the complete class to form a stable API; they might go away sooner or later.
 *
 * @version $Id: $
 * @since 2.7
 */
@Unstable
public final class Utils
{

    /*
     * the hard wired guess of the duration of an event where the end date is missing.
     */
    private static final int EVENT_DURATION_MIN = 30;

    private Utils()
    {
        // no instances, please
    }

    /**
     * return the end date of the event (or event modification). this either gets the date from the &quot;endDate&quot;
     * property or calculates it from the &quot;startDate&quot; property
     *
     * @param eventData
     *            the object describing the data. should have a 'startDate', 'allDay' and optional 'endDate' property.
     * @return the end date of the event
     */
    public static Date fetchOrGuessEndDate(BaseObject eventData)
    {
        return fetchOrGuessEndDate(eventData, EventConstants.PROPERTY_STARTDATE_NAME,
            EventConstants.PROPERTY_ENDDATE_NAME, EventConstants.PROPERTY_ALLDAY_NAME);
    }

    public static Date fetchOrGuessEndDate(BaseObject eventData, String startDateName, String endDateName,
        String allDayName)
    {
        Date endDate = eventData.getDateValue(endDateName);
        if (endDate == null) {
            final boolean allDay = (allDayName != null) && (eventData.getIntValue(allDayName) == 1);
            final Date startDate = eventData.getDateValue(startDateName);
            endDate = guessEndDate(startDate, allDay);
        }

        return endDate;
    }

    /**
     * Guess the end date of an event, given a start date.
     *
     * @param startDate the start date of the event.
     * @param allDay flag, if the event lasts all day.
     * @return the end date of the event
     */
    public static Date guessEndDate(Date startDate, boolean allDay)
    {
        Date endDate;
        if (allDay) {
            // if we have an all day event, then the hours and minutes, etc. are irrelevant.
            // assume the event lasts one day by setting the (inclusive) end date to the
            // start date
            endDate = new Date(startDate.getTime());
        } else {
            // FIXME: hardwired default event duration of 30 minutes.
            endDate = new Date(startDate.getTime() + EVENT_DURATION_MIN * 60 * 1000L);
        }
        return endDate;
    }

    /**
     * fill the description of the event instance from the value in the given base object.
     * The base object should either be a MoccaCalendarEvent or a MoccaCalendarEventModification.
     *
     * @param eventData the xwiki object to get the information from
     * @param descriptionPropertyName then name of the property storing the description
     * @param context the current context
     * @param event the event instance whose description will be set
     */
    public static void fillDescription(BaseObject eventData, String descriptionPropertyName, XWikiContext context,
        EventInstance event)
    {
        XWikiDocument eventDoc = eventData.getOwnerDocument();
        String idString = eventDoc.getSyntax().toIdString();
        String description = eventData.getStringValue(descriptionPropertyName);
        event
            .setDescription(eventDoc.getRenderedContent(description, idString, Syntax.PLAIN_1_0.toIdString(), context));
        event.setDescriptionHtml(
            eventDoc.getRenderedContent(description, idString, Syntax.HTML_5_0.toIdString(), context));
    }

}
