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

import java.util.ArrayList;
import java.util.List;

/**
 * A list of often reused constants.
 *
 * @version $Id: $
 * @since 2.7
 */
public final class EventConstants
{

    /** the class name for calendars. */
    public static final String MOCCA_CALENDAR_CLASS_NAME = "MoccaCalendar.MoccaCalendarClass";

    /**
     * the class name for events.
     */
    public static final String MOCCA_CALENDAR_EVENT_CLASS_NAME = "MoccaCalendar.MoccaCalendarEventClass";

    /**
     * the class name for event recurrency information.
     */
    public static final String MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME
        = "MoccaCalendar.Code.MoccaCalendarEventRecurrencyClass";

    /**
     * the class name for exceptions when a recurrent event instance is skipped.
     */
    public static final String MOCCA_CALENDAR_EVENT_DELETION_CLASS_NAME
        = "MoccaCalendar.Code.MoccaCalendarEventDeletionClass";

    /**
     * the class name for exceptions when a recurrent event instance is modified.
     */
    public static final String MOCCA_CALENDAR_EVENT_MODIFICATION_CLASS_NAME
        = "MoccaCalendar.Code.MoccaCalendarEventModificationClass";

    /**
     * the name of the startDate property for event objects.
     */
    public static final String PROPERTY_STARTDATE_NAME = "startDate";

    /**
     * the name of the endDate property for event objects.
     */
    public static final String PROPERTY_ENDDATE_NAME = "endDate";

    /**
     * the name of the allDay property for event objects.
     */
    public static final String PROPERTY_ALLDAY_NAME = "allDay";

    /**
     * the name of the title property for event objects.
     */
    public static final String PROPERTY_TITLE_NAME = "title";

    /**
     * the name of the description property for event objects.
     */
    public static final String PROPERTY_DESCRIPTION_NAME = "description";

    /**
     * the name of the recurrency flag for event objects.
     */
    public static final String PROPERTY_RECURRENT_NAME = "recurrent";

    /**
     * the name of the first instance date property for event recurrency objects.
     */
    public static final String PROPERTY_FIRSTINSTANCE_NAME = "firstInstance";

    /**
     * the name of the last instance date property for event recurrency objects.
     */
    public static final String PROPERTY_LASTINSTANCE_NAME = "lastInstance";

    /**
     * the name of the start date property for event deletion objects.
     */
    public static final String PROPERTY_STARTDATE_OF_DELETED_NAME = "eventOrigStartDate";

    /**
     * the name of the original start date property for event modification objects.
     */
    public static final String PROPERTY_ORIG_STARTDATE_OF_MODIFIED_NAME = PROPERTY_STARTDATE_OF_DELETED_NAME;
    // public static final String PROPERTY__NAME = "";

    private EventConstants()
    {

    }

    public static List<String> getClassFields()
    {
        List<String> classFields = new ArrayList<>();
        classFields.add(PROPERTY_TITLE_NAME);
        classFields.add(PROPERTY_STARTDATE_NAME);
        classFields.add(PROPERTY_DESCRIPTION_NAME);

        return classFields;
    }
}
