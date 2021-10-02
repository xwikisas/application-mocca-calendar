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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.EventInstance;
import org.xwiki.contrib.moccacalendar.internal.utils.DefaultEventAssembly;
import org.xwiki.contrib.moccacalendar.internal.utils.EventQuery;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.QueryException;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 *
 * @version $Id: $
 * @since 2.11
 */
@Named("calendar.source.meeting")
@Singleton
@Component(roles = { MeetingEventSource.class })
public class MeetingEventSource
{

    private static String MEETING_ENTRY_CLASS_NAME = "Meeting.Code.MeetingClass";

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

    public boolean isAvailable()
    {
        // FIXME: unimplemented stub
        return false;
    }

    public List<EventInstance> getEvents(Date dateFrom, Date dateTo, String filter, String parentReference)
    {
        List<EventInstance> events = new ArrayList<EventInstance>();

        try {
            EventQuery evQ = new EventQuery(MEETING_ENTRY_CLASS_NAME, "Meeting.Code.MeetingTemplate");

            //
            // filter by date range
            //
            evQ.addDateLimits(dateFrom, dateTo);

            //
            // filter by event location
            //
            DocumentReference parentRef = (parentReference == null) ? null
                : stringDocRefResolver.resolve(parentReference);
            evQ.addLocationFilter(filter, parentRef);

            // finally the ordering: we do not care for now
            // evQ.setAscending(sortAscending);

            List<DocumentReference> meetingDocRefs = eventAssembly.executeQuery(evQ);

            convertToEventInstances(meetingDocRefs, events);
        } catch (QueryException qe) {
            // duh
            logger.error("what, me query error?", qe);
        }

        return events;
    }

    private void convertToEventInstances(List<DocumentReference> meetingDocRefs, final List<EventInstance> events)
    {
        // this is silly. instead use streams or whatever?
/*        meetingDocRefs.stream().map((DocumentReference ref) -> {
            return convertToEventInstance(ref);
        })./* here filter nulls * / collect(Collectors.toCollection(() -> {
            return events;
        }));
*/
        for (DocumentReference ref : meetingDocRefs) {
            EventInstance event = convertToEventInstance(ref);
            if (event != null) {
                events.add(event);
            }
        }
    }

    private EventInstance convertToEventInstance(DocumentReference meetingDocRef)
    {
        EventInstance event = null;
        try {
            XWikiContext context = xcontextProvider.get();

            // XXX: created copy of all try ... block
            XWikiDocument eventDoc = context.getWiki().getDocument(meetingDocRef, context);
            BaseObject eventData = eventDoc.getXObject(stringDocRefResolver.resolve(MEETING_ENTRY_CLASS_NAME));
            if (eventData == null) {
                logger.error("data inconsistency: query returned [{}] which contains no object for [{}]", meetingDocRef,
                    MEETING_ENTRY_CLASS_NAME);
                return null;
            }

            event = new EventInstance();
            event.setEventDocRef(meetingDocRef);

            Date startDate = eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME); // it is the same !
            DateTime startDateTime = new DateTime(startDate.getTime());
            event.setStartDate(startDateTime);

            Date endDate = Utils.fetchOrGuessEndDate(eventData);
            DateTime endDateTime = new DateTime(endDate.getTime());
            event.setEndDate(endDateTime);

            completeEventData(event, eventDoc, eventData);

            // events.add(event);
        } catch (XWikiException e) {
            logger.warn("cannot find meeting event data [{}]", meetingDocRef, e);
        }

        return event;
    }

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    // XXX complete copy
    @SuppressWarnings("unused") // only for now!
    private void completeEventData(EventInstance event, XWikiDocument eventDoc, BaseObject eventData)
        throws XWikiException
    {
        final XWikiContext context = xcontextProvider.get();
        final String defaultPageName = defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT)
            .getName();
        final DocumentReference eventDocRef = eventDoc.getDocumentReference();

        boolean isAllDay = false; // modified here !
        event.setAllDay(isAllDay);

        DateTime endDateExclusive = event.getEndDate();
        if (isAllDay) {
            // as end date is actually treated exclusive by the calendar
            // but inclusive by the input data:
            endDateExclusive = endDateExclusive.plusDays(1);
        }

        event.setEndDateExclusive(endDateExclusive);

        if (null == event.getTitle()) {
            // FIXME: do we always want the title?
            event.setTitle(eventDoc.getRenderedTitle(Syntax.PLAIN_1_0, context));
        }

        if (null == event.getDescription()) {
            // FIXME: here wrong property name
            Utils.fillDescription(eventData, EventConstants.PROPERTY_DESCRIPTION_NAME, context, event);
        }

        event.setEventDocRef(eventDocRef);

        /*
         * the corresponding --calendar-- meetings page should be the default page of the parent space.
         * this is the space of the page if the event page is terminal, and the parent
         * of the events page space, if the page is non-terminal
         */
        BaseObject calendarData = null;
        /*
         * SpaceReference parentSpaceRef = null;
         * if (defaultPageName.equals(eventDocRef.getName())) {
         * EntityReference parentRef = eventDocRef.getLastSpaceReference().getParent();
         * if ((parentRef != null) && (parentRef instanceof SpaceReference)) {
         * parentSpaceRef = (SpaceReference) parentRef;
         * }
         * } else {
         * parentSpaceRef = eventDocRef.getLastSpaceReference();
         * }
         * 
         * /*if (parentSpaceRef != null) {
         * DocumentReference parentDoc = new DocumentReference(defaultPageName, parentSpaceRef);
         * XWikiDocument calendarDoc = context.getWiki().getDocument(parentDoc, context);
         * calendarData = calendarDoc
         * .getXObject(stringDocRefResolver.resolve(EventConstants.MOCCA_CALENDAR_CLASS_NAME));
         * }
         */

        // XXX: this always null now
        if (calendarData == null) {
            // some arbitrary defaults
            event.setBackgroundColor("#888");
            // text color can be missing
            event.setTextColor("");
        } else {
            // XXX here different names
            event.setTextColor(calendarData.getStringValue("textColor"));
            event.setBackgroundColor(calendarData.getStringValue("color"));
        }

    }

}
