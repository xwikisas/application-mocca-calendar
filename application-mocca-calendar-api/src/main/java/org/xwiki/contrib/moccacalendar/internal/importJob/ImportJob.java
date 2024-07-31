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

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobRequest;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobStatus;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.fullcalendar.FullCalendarManager;
import org.xwiki.fullcalendar.model.MoccaCalendarEvent;
import org.xwiki.fullcalendar.model.RecurrentEventModification;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.fortuna.ical4j.data.ParserException;

/**
 * The Mocca calendar import job.
 *
 * @version $Id$
 * @since 2.14
 */
@Unstable
@Component
@Named(ImportJob.JOB_TYPE)
public class ImportJob extends AbstractJob<ImportJobRequest, ImportJobStatus> implements GroupedJob
{
    /**
     * Mocca calendar import job type.
     */
    public static final String JOB_TYPE = "moccacalendar.import";

    private static final String NEW_EVENT_HOME = "WebHome";

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private FullCalendarManager fullCalendarManager;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    public JobGroupPath getGroupPath()
    {
        List<String> path = new ArrayList<>();
        path.add("moccacalendar");
        path.add("import");
        path.add(request.getParentRef());
        return new JobGroupPath(path);
    }

    @Override
    protected ImportJobStatus createNewStatus(ImportJobRequest request)
    {
        return new ImportJobStatus(JOB_TYPE, request, observationManager, loggerManager);
    }

    @Override
    protected void runInternal()
    {
        try {
            List<MoccaCalendarEvent> calendarEventsJson =
                fullCalendarManager.getICalEventsFromFile(getICSFileContent(request.getFile()), true);
            List<XWikiDocument> eventDocuments = new ArrayList<>();
            this.progressManager.pushLevelProgress(calendarEventsJson.size() + 1, this);

            for (MoccaCalendarEvent calendarEvent : calendarEventsJson) {
                progressManager.startStep(this);
                if (!status.isCanceled()) {
                    progressManager.endStep(this);
                    break;
                }

                XWikiDocument eventDoc =
                    getUniqueEventName(calendarEvent.getTitle().trim(), request.getParentRef(), eventDocuments);
                createCalendarObjects(eventDoc, calendarEvent);
                eventDocuments.add(eventDoc);

                progressManager.endStep(this);
                Thread.yield();
            }
            batchSave(eventDocuments);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void batchSave(List<XWikiDocument> eventDocuments) throws XWikiException, ParseException
    {
        if (!status.isCanceled()) {
            XWikiContext wikiContext = wikiContextProvider.get();
            XWiki wiki = wikiContext.getWiki();
            progressManager.startStep(this);
            for (XWikiDocument calendarEvent : eventDocuments) {
                wiki.saveDocument(calendarEvent, wikiContext);
            }
            progressManager.endStep(this);
        }
    }

    private void createCalendarObjects(XWikiDocument eventDoc, MoccaCalendarEvent component) throws XWikiException
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        DocumentReference eventClassRef =
            documentReferenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME);
        BaseObject eventObj = eventDoc.newXObject(eventClassRef, wikiContext);

        eventObj.set(EventConstants.PROPERTY_TITLE_NAME, component.getTitle(), wikiContext);
        eventObj.set(EventConstants.PROPERTY_DESCRIPTION_NAME, component.getDescription(), wikiContext);

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
            processRecurrence(eventDoc, component, wikiContext);
        }
    }

    private void processRecurrence(XWikiDocument eventDoc, MoccaCalendarEvent component, XWikiContext wikiContext)
        throws XWikiException
    {
        DocumentReference eventRecClassRef =
            documentReferenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME);
        BaseObject eventRecObj = eventDoc.newXObject(eventRecClassRef, wikiContext);

        eventRecObj.set(EventConstants.PROPERTY_FIRSTINSTANCE_NAME, component.getStart(), wikiContext);
        eventRecObj.set(EventConstants.PROPERTY_LASTINSTANCE_NAME, component.getRecEndDate(), wikiContext);
        eventRecObj.set(EventConstants.PROPERTY_FREQUENCY_NAME, component.getRecurrenceFreq().toLowerCase(),
            wikiContext);

        List<RecurrentEventModification> modList = component.getModificationList();
        if (!modList.isEmpty()) {
            for (RecurrentEventModification eventModification : modList) {
                DocumentReference eventRecModifiedRef =
                    documentReferenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_MODIFICATION_CLASS_NAME);
                BaseObject eventModObj = eventDoc.newXObject(eventRecModifiedRef, wikiContext);
                eventModObj.set(EventConstants.PROPERTY_ORIG_STARTDATE_OF_MODIFIED_NAME,
                    eventModification.getOriginalDate(), wikiContext);
                eventModObj.set(EventConstants.PROPERTY_STARTDATE_NAME, eventModification.getModifiedStartDate(),
                    wikiContext);
                eventModObj.set(EventConstants.PROPERTY_ENDDATE_NAME, eventModification.getModifiedEndDate(),
                    wikiContext);
                eventModObj.set(EventConstants.PROPERTY_TITLE_NAME, eventModification.getModifiedTitle(), wikiContext);
                eventModObj.set(EventConstants.PROPERTY_DESCRIPTION_NAME, eventModification.getModifiedDescription(),
                    wikiContext);
            }
        }
    }

    private byte[] getICSFileContent(byte[] importedFileContent) throws ParserException
    {
        String importedFileString = new String(importedFileContent);
        int beginIndex = importedFileString.indexOf(CalendarKeys.ICS_CALENDAR_CALENDAR_BEGIN);
        int endIndex = importedFileString.indexOf(CalendarKeys.ICS_CALENDAR_CALENDAR_END)
            + CalendarKeys.ICS_CALENDAR_CALENDAR_END.length();
        String filteredCalendarContent = importedFileString.substring(beginIndex, endIndex);

        return filteredCalendarContent.getBytes();
    }

    private XWikiDocument getUniqueEventName(String eventName, String parentRef, List<XWikiDocument> eventDocuments)
        throws XWikiException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();

        DocumentReference parentDocRef = documentReferenceResolver.resolve(parentRef);
        SpaceReference documentSpaceRef = new SpaceReference(eventName, parentDocRef.getLastSpaceReference());
        DocumentReference eventRef = new DocumentReference(NEW_EVENT_HOME, documentSpaceRef);
        wikiContext.getWiki().getDocument(eventRef, wikiContext);
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
