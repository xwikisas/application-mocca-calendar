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
package org.xwiki.contrib.moccacalendar.internal.ical;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.util.FixedUidGenerator;
import net.fortuna.ical4j.util.SimpleHostInfo;
import net.fortuna.ical4j.util.UidGenerator;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

/**
 * Generates a {@link VEvent} based on the given XWikiDocument.
 *
 * @version $Id:$
 * @since 2.20
 */
@Component(roles = ICalEventGenerator.class)
@Singleton
public class ICalEventGenerator
{
    private static final String HOST_NAME = "atelier-medias.org";

    @Inject
    private Logger logger;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private ICalRecurrenceGenerator recurrenceGenerator;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Creates a VEvent from the given event document.
     *
     * @param eventDocument the XWiki document containing event data
     * @return the created VEvent, or null if the event could not be created
     */
    public VEvent createEvent(XWikiDocument eventDocument)
    {
        DocumentReference eventClassRef =
            this.referenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME);
        BaseObject eventData = eventDocument.getXObject(eventClassRef);
        if (eventData == null) {
            this.logger.warn("Event document [{}] has no event data object.", eventDocument.getDocumentReference());
            return null;
        }
        if (eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME) == null) {
            this.logger.warn("Event [{}] has no start date.", eventDocument.getDocumentReference());
            return null;
        }
        VEvent event = initializeEvent(eventData, eventDocument.getTitle());
        addEventDescription(event, eventData, eventDocument);
        addEventRecurrence(eventDocument, event);
        UidGenerator ug =
            new FixedUidGenerator(new SimpleHostInfo(HOST_NAME), eventDocument.getDocumentReference().toString());
        event.add(ug.generateUid());
        return event;
    }

    private void addEventRecurrence(XWikiDocument eventDocument, VEvent event)
    {
        Optional<RRule> rRule = this.recurrenceGenerator.getRecurrenceRule(eventDocument);
        rRule.ifPresent(event::add);
    }

    private VEvent initializeEvent(BaseObject eventData, String title)
    {
        Date startDate = eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME);
        Date endDate = eventData.getDateValue(EventConstants.PROPERTY_ENDDATE_NAME);
        int allDay = eventData.getIntValue(EventConstants.PROPERTY_ALLDAY_NAME);
        VEvent event;
        if (allDay != 1) {
            Instant endInstant;
            Instant startInstant = startDate.toInstant();
            if (endDate == null) {
                endInstant = startInstant.plusSeconds(3600);
            } else {
                endInstant = endDate.toInstant();
            }
            event = new VEvent(startInstant, endInstant, title);
        } else {
            LocalDate startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (endDate != null) {
                // We add one more day to the end date as the all day events are rendered as end date - 1 day in
                // Google calendar.
                LocalDate endLocalDate = endDate.toInstant().atZone(ZoneId.systemDefault()).plusDays(1).toLocalDate();
                event = new VEvent(startLocalDate, endLocalDate, title);
            } else {
                event = new VEvent(startLocalDate, title);
            }
        }
        return event;
    }

    private void addEventDescription(VEvent event, BaseObject eventData, XWikiDocument eventDocument)
    {
        String propertyDescription = eventData.getStringValue(EventConstants.PROPERTY_DESCRIPTION_NAME);
        if (propertyDescription != null) {
            // Normalize line endings: \n to \r\n (iCal requires \r\n)
            XWikiContext wikiContext = this.xcontextProvider.get();
            propertyDescription = propertyDescription.replaceAll("([^\r])\\n", "$1\r\n");
            String description = propertyDescription + "\r\n\r\n" + eventDocument.getExternalURL("view", wikiContext);
            event.add(new Description(description));
        }
    }
}
