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
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.EventInstance;
import org.xwiki.contrib.moccacalendar.EventSource;
import org.xwiki.contrib.moccacalendar.internal.utils.DefaultEventAssembly;
import org.xwiki.contrib.moccacalendar.internal.utils.EventQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.QueryException;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Allows to show meetings as calendar entries.
 * @version $Id: $
 * @since 2.11
 */
@Named("meetings")
@Singleton
@Component
public class MeetingEventSource implements EventSource
{

    private static final String MEETING_ENTRY_CLASS_NAME = "Meeting.Code.MeetingClass";
    private static final String MEETING_TEMPLATE_PAGE = "Meeting.Code.MeetingTemplate";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> stringDocRefResolver;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    private DefaultEventAssembly eventAssembly;

    @Inject
    private Logger logger;

    /**
     * Get a list of meetings as events.
     *
     * These events are currently not editable.
     * The filtering parameter might need some work. too.
     *
     * {@inheritDoc}
     */
    @Override
    public List<EventInstance> getEvents(Date dateFrom, Date dateTo, String filter, DocumentReference parentRef,
        boolean sortAscending)
    {
        List<EventInstance> events = new ArrayList<EventInstance>();

        try {
            EventQuery evQ = new EventQuery(MEETING_ENTRY_CLASS_NAME, MEETING_TEMPLATE_PAGE);

            // filter by date range
            evQ.addDateLimits(dateFrom, dateTo);

            // filter by event location
            evQ.addLocationFilter(filter, parentRef);

            // finally the ordering
            evQ.setAscending(sortAscending);

            List<DocumentReference> meetingDocRefs = eventAssembly.executeQuery(evQ);

            for (DocumentReference ref : meetingDocRefs) {
                EventInstance event = convertToEventInstance(evQ, ref);
                if (event != null) {
                    events.add(event);
                }
            }
        } catch (QueryException qe) {
            logger.error("unexpected query error while fetching meetings", qe);
        }

        return events;
    }

    private EventInstance convertToEventInstance(EventQuery evQ, DocumentReference meetingDocRef)
    {
        EventInstance event = null;
        try {
            XWikiContext context = xcontextProvider.get();
            XWikiDocument eventDoc = context.getWiki().getDocument(meetingDocRef, context);
            BaseObject eventData = eventDoc.getXObject(stringDocRefResolver.resolve(evQ.getClassName()));
            if (eventData == null) {
                logger.error("data inconsistency: query returned [{}] which contains no object for [{}]", meetingDocRef,
                    evQ.getClassName());
                return null;
            }

            event = new EventInstance();
            event.setEventDocRef(meetingDocRef);

            Date startDate = eventData.getDateValue(evQ.getStartDateName());
            DateTime startDateTime = new DateTime(startDate.getTime());
            event.setStartDate(startDateTime);

            Date endDate = Utils.fetchOrGuessEndDate(eventData, evQ.getStartDateName(), evQ.getEndDateName(), null);
            DateTime endDateTime = new DateTime(endDate.getTime());
            event.setEndDate(endDateTime);
            event.setEndDateExclusive(endDateTime);

            if (null == event.getTitle()) {
                event.setTitle(eventDoc.getRenderedTitle(Syntax.PLAIN_1_0, context));
            }

            if (null == event.getDescription()) {
                Utils.fillDescription(eventData, "description", context, event);
            }
        } catch (XWikiException e) {
            logger.warn("cannot find meeting event data [{}]", meetingDocRef, e);
        }

        return event;
    }

    /**
     * Get the meeting of the given document as event.
     *
     * {@inheritDoc}
     */
    @Override
    public EventInstance getEventInstance(Document eventDoc, Date eventStartDate)
    {
        // FIXME: it is odd that we use the event query only to transfer information
        // about the startDate/endDate fields
        EventQuery evQ = new EventQuery(MEETING_ENTRY_CLASS_NAME, MEETING_TEMPLATE_PAGE);

        return convertToEventInstance(evQ, eventDoc.getDocumentReference());
    }

}
