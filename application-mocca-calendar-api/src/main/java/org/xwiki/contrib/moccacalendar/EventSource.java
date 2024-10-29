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
package org.xwiki.contrib.moccacalendar;

import java.util.Date;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.moccacalendar.internal.DefaultSourceConfigurationClassInitializer;
import org.xwiki.contrib.moccacalendar.script.MoccaCalendarScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.api.Document;

/**
 *
 * @version $Id: $
 * @since 2.11
 */
@Role
@Unstable
public interface EventSource
{

    /**
     * Check if the source is available in the given context.
     * To get the context, use a context provider.
     * @return true if the source is available to create events
     */
    default boolean isAvailable()
    {
        return true;
    };

    /**
     * Get all events that this source can find for the given constraints.
     *
     * If the "dateTo" is null, then assume a search for all events where the "dateFrom" is
     * between the start date and the end date of the event.
     * Otherwise return all events where the time interval between start date and end date (both inclusive)
     * overlaps with the (inclusive) interval of dateFrom to dateTo. It can be assumed that
     * dateTo is equal or after dateFrom, otherwise return an empty list.
     *
     * The filter parameter is a hint how to limit the the results relative to the given "parentRef" document.
     * If the filter is null or equals to "wiki", all events independent of their location should be returned.
     * The "parentRef" parameter can be ignored in that case.
     * If the filter is "page", then return only direct sub pages of the given parentRef.
     * If the filter is "space", then return all events whose pages are nested within the parentRefs space
     * (i.e. they would also show up in that pages "Children" or "Siblings" view.
     * If the filter is unknown or not supported, then return an empty list.
     *
     * @param dateFrom the start date of the date search window, never null
     * @param dateTo the end date of the date search window, might be null
     * @param filter textual hint how to filter the events, might be null
     * @param parentRef the root document for the filter. might be null
     * @param sortAscending if the events should be sorted by start date
     * @return a list of event instances, should not be null, and should not contain nulls.
     * @see MoccaCalendarScriptService#queryEvents(Date, Date, String, String, boolean)
     */
    List<EventInstance> getEvents(Date dateFrom, Date dateTo, String filter, DocumentReference parentRef,
        boolean sortAscending);

    /**
     * Return the given document as an {@link EventInstance} with the given start date.
     * If the document contains only information about one event, then the start date can be ignored.
     * If there is no matching information that this source can create from the document,
     * then return null.
     *
     * @param eventDoc the document storing the information about this event
     * @param eventStartDate the start date of the event, might be null if it does not matter
     * @return an event instance or null, if not event data found
     */
    EventInstance getEventInstance(Document eventDoc, Date eventStartDate);

    /**
     * The configuration class to use for this event source.
     * The configuration source allows to enable or disable the source for different calendars.
     * It might also add more variables to configure the event source; if these should be used
     * then the event source is responsible to get an instance of that configuration class from the current document.
     * The default implementation returns {@code null} which means no special configuration class.
     * In that case a default class will be used.
     * @return a class name, or null. In the latter case a default implementation will be used.
     * @see DefaultSourceConfigurationClassInitializer
     */
    default LocalDocumentReference getConfigurationClass()
    {
        return null;
    }
}
