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
package org.xwiki.contrib.moccacalendar.internal.importJob;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.fullcalendar.model.CalendarEvent;
import org.xwiki.fullcalendar.model.RecurrentEventModification;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;
import org.xwiki.wysiwyg.converter.HTMLConverter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Helper class for processing a {@link CalendarEvent} and importing it in XWiki as a Mocca Calendar Event, by creating
 * needed calendar objects.
 *
 * @version $Id$
 * @since 2.14
 */
@Unstable
@Component(roles = CalendarEventImporter.class)
@Singleton
public class CalendarEventImporter
{
    private static final String NEW_EVENT_HOME = "WebHome";

    private static final Map<String, String> PROPERTY_MAP = Map.of(
        "X-MOCCA-TEXT-COLOR", EventConstants.PROPERTY_TEXTCOLOR_NAME,
        "X-MOCCA-SHOW-AUTHOR", EventConstants.PROPERTY_SHOW_AUTHOR
        );

    @Inject
    private HTMLConverter htmlConverter;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Creates the class objects specific to a Mocca calendar event and adds them to the given {@link XWikiDocument}.
     *
     * @param eventDoc the {@link XWikiDocument} where the objects will be added.
     * @param component contains the data used to populate the objects.
     * @throws XWikiException if there are any issues when creating the {@link BaseObject} for the given document.
     */
    public void importCalendarEvent(XWikiDocument eventDoc, CalendarEvent component) throws XWikiException
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        DocumentReference eventClassRef =
            documentReferenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME);
        BaseObject eventObj = eventDoc.newXObject(eventClassRef, wikiContext);

        setBasicProperties(component, eventObj);
        int allDay = component.isAllDay() ? 1 : 0;
        eventObj.set(EventConstants.PROPERTY_ALLDAY_NAME, allDay, wikiContext);
        if (allDay == 1) {
            LocalDateTime end = component.getEnd().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime().minusDays(1);
            eventObj.set(EventConstants.PROPERTY_ENDDATE_NAME, Date.from(end.atZone(ZoneOffset.UTC).toInstant()),
                wikiContext);
        } else {
            eventObj.set(EventConstants.PROPERTY_ENDDATE_NAME, component.getEnd(), wikiContext);
        }
        eventObj.set(EventConstants.PROPERTY_STARTDATE_NAME, component.getStart(), wikiContext);

        int recurrenceValue = component.isRecurrent();
        eventObj.set(EventConstants.PROPERTY_RECURRENT_NAME, recurrenceValue, wikiContext);
        if (recurrenceValue == 1) {
            setRecurrence(eventDoc, component);
        }
    }

    /**
     * Checks if a {@link XWikiDocument} with the given name exists at the given parent reference or is to be processed
     * and generate a unique name for it.
     *
     * @param eventName name of the event to be added.
     * @param parentRef reference to the parent of the event.
     * @param eventDocuments {@link List} with the events document that are to be processed and created.
     * @return a {@link XWikiDocument} with the title the given event name and a unique identifier.
     * @throws XWikiException if there are any exceptions while getting the {@link XWikiDocument} for the event.
     */
    public XWikiDocument getUniqueEventName(String eventName, String parentRef, List<XWikiDocument> eventDocuments)
        throws XWikiException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();

        DocumentReference parentDocRef = documentReferenceResolver.resolve(parentRef);
        SpaceReference documentSpaceRef = new SpaceReference(eventName, parentDocRef.getLastSpaceReference());
        DocumentReference eventRef = new DocumentReference(NEW_EVENT_HOME, documentSpaceRef);

        XWikiDocument eventDoc = wikiContext.getWiki().getDocument(eventRef, wikiContext);

        if (!eventDoc.isNew() || sameNameEventExists(eventRef, eventDocuments)) {
            String newEventName = eventName + "_" + System.nanoTime();
            SpaceReference newDocumentSpaceRef = new SpaceReference(newEventName, parentDocRef.getLastSpaceReference());
            DocumentReference newEventRef = new DocumentReference(NEW_EVENT_HOME, newDocumentSpaceRef);
            eventDoc = new XWikiDocument(newEventRef);
        }
        eventDoc.setTitle(eventName);
        eventDoc.setParentReference(parentDocRef);
        return eventDoc;
    }

    private void setBasicProperties(CalendarEvent component, BaseObject eventObj)
    {
        addConvertedPropertyToObject(component.getDescription(), eventObj, EventConstants.PROPERTY_DESCRIPTION_NAME);
        addConvertedPropertyToObject(component.getTitle(), eventObj, EventConstants.PROPERTY_TITLE_NAME);
        addConvertedPropertyToObject(component.getColor(), eventObj, EventConstants.PROPERTY_BACKGROUNDCOLOR_NAME);
        setMetadataProperties(component.getMeta(), eventObj);
    }

    private void addConvertedPropertyToObject(String htmlContent, BaseObject eventObj, String property)
    {
        // Sanitize the given HTML content and convert it to XWiki syntax prior to adding it to the given object field.
        // This is done in order to offer the user a less technical way to edit the content.
        if (!StringUtils.isBlank(htmlContent)) {
            String htmlContentWithBreaks = htmlContent.replace("\n", "<br>");
            String cleanHTMLContent = Jsoup.clean(htmlContentWithBreaks, Safelist.basic());
            String convertedContent = this.htmlConverter.fromHTML(cleanHTMLContent, Syntax.XWIKI_2_1.toIdString());
            eventObj.set(property, convertedContent, this.wikiContextProvider.get());
        }
    }

    private void setMetadataProperties(Map<String, Object> meta, BaseObject eventObj)
    {
        if (meta == null) {
            return;
        }

        for (Map.Entry<String, String> entry : PROPERTY_MAP.entrySet()) {
            addConvertedPropertyToObject(meta.getOrDefault(entry.getKey(), "").toString(), eventObj, entry.getValue());
        }
    }

    private void setRecurrence(XWikiDocument eventDoc, CalendarEvent component) throws XWikiException
    {
        // Creates the MoccaCalendarEventRecurrencyClass object and populates it with the fields from the CalendarEvent.
        XWikiContext wikiContext = wikiContextProvider.get();
        DocumentReference eventRecClassRef =
            documentReferenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME);
        BaseObject eventRecObj = eventDoc.newXObject(eventRecClassRef, wikiContext);

        eventRecObj.set(EventConstants.PROPERTY_FIRSTINSTANCE_NAME, component.getStart(), wikiContext);
        eventRecObj.set(EventConstants.PROPERTY_LASTINSTANCE_NAME, component.getRecEndDate(), wikiContext);
        eventRecObj.set(EventConstants.PROPERTY_FREQUENCY_NAME, component.getRecurrenceFreq().toLowerCase(),
            wikiContext);

        DocumentReference eventRecModifiedRef =
            documentReferenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_MODIFICATION_CLASS_NAME);
        // For every modified recurrence instance, creates a new MoccaCalendarEventModificationClass and populates it
        // with the required fields.
        for (RecurrentEventModification eventModification : component.getModificationList()) {
            BaseObject eventModObj = eventDoc.newXObject(eventRecModifiedRef, wikiContext);
            eventModObj.set(EventConstants.PROPERTY_ORIG_STARTDATE_OF_MODIFIED_NAME,
                eventModification.getOriginalDate(), wikiContext);
            eventModObj.set(EventConstants.PROPERTY_STARTDATE_NAME, eventModification.getModifiedStartDate(),
                wikiContext);
            eventModObj.set(EventConstants.PROPERTY_ENDDATE_NAME, eventModification.getModifiedEndDate(), wikiContext);
            addConvertedPropertyToObject(eventModification.getModifiedTitle(), eventModObj,
                EventConstants.PROPERTY_TITLE_NAME);

            addConvertedPropertyToObject(eventModification.getModifiedDescription(), eventModObj,
                EventConstants.PROPERTY_DESCRIPTION_NAME);
        }
    }

    private boolean sameNameEventExists(DocumentReference eventRef, List<XWikiDocument> eventDocuments)
    {
        for (XWikiDocument document : eventDocuments) {
            if (document.getDocumentReference().equals(eventRef)) {
                return true;
            }
        }
        return false;
    }
}
