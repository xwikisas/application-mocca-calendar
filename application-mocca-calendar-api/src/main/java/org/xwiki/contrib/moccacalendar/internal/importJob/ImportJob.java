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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobRequest;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobStatus;
import org.xwiki.contrib.moccacalendar.importJob.result.MoccaCalendarEventResult;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
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

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.CalendarComponent;

/**
 * A list of used ical constants.
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
    private Provider<CalendarEventProcessor> eventProcessorProvider;

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
            List<CalendarComponent> sortedComponents = getCalendarComponents(new String(request.getFile()));
            List<XWikiDocument> eventDocuments = new ArrayList<>();
            this.progressManager.pushLevelProgress(sortedComponents.size() + 1, this);

            for (CalendarComponent component : sortedComponents) {
                progressManager.startStep(this);
                String eventUID = component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_UID).getValue();
                if (status.isCanceled()) {
                    progressManager.endStep(this);
                    break;
                } else if (!this.status.isDuplicate(eventUID)) {
                    XWikiDocument eventDoc = getUniqueEventName(
                        component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_SUMMARY).getValue().trim(),
                        request.getParentRef(), eventDocuments);
                    createCalendarObjects(eventDoc, component);
                    eventDoc.setAuthorReference(request.getUserReference());
                    eventDoc.setCreatorReference(request.getUserReference());
                    this.status.storeUID(eventUID);
                    eventDocuments.add(eventDoc);
                }
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

    private void batchSave(List<XWikiDocument> eventDocuments) throws XWikiException
    {
        if (!status.isCanceled()) {
            XWikiContext wikiContext = wikiContextProvider.get();
            XWiki wiki = wikiContext.getWiki();
            progressManager.startStep(this);
            for (XWikiDocument eventDocument : eventDocuments) {
                wiki.saveDocument(eventDocument, "Created new event.", wikiContext);
            }
            progressManager.endStep(this);
        }
    }

    private void createCalendarObjects(XWikiDocument eventDoc, CalendarComponent component) throws XWikiException
    {
        MoccaCalendarEventResult eventResult = eventProcessorProvider.get().processEvent(component);
        XWikiContext wikiContext = wikiContextProvider.get();
        DocumentReference eventClassRef =
            documentReferenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME);
        DocumentReference eventRecClassRef =
            documentReferenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME);
        BaseObject eventObj = eventDoc.newXObject(eventClassRef, wikiContext);
        BaseObject eventRecObj = eventDoc.newXObject(eventRecClassRef, wikiContext);

        eventObj.set(EventConstants.PROPERTY_TITLE_NAME, eventResult.getTitle(), wikiContext);
        eventObj.set(EventConstants.PROPERTY_DESCRIPTION_NAME, eventResult.getDescription(), wikiContext);
        Date startDate = eventResult.getStartDate();
        eventObj.set(EventConstants.PROPERTY_STARTDATE_NAME, startDate, wikiContext);
        eventObj.set(EventConstants.PROPERTY_ENDDATE_NAME, eventResult.getEndDate(), wikiContext);
        eventObj.set(EventConstants.PROPERTY_ALLDAY_NAME, eventResult.getAllDay(), wikiContext);
        int recurrenceValue = eventResult.getIsRecurrent();
        eventObj.set(EventConstants.PROPERTY_RECURRENT_NAME, recurrenceValue, wikiContext);
        if (recurrenceValue == 1) {
            eventRecObj.set(EventConstants.PROPERTY_FIRSTINSTANCE_NAME, startDate, wikiContext);
            eventRecObj.set(EventConstants.PROPERTY_LASTINSTANCE_NAME, eventResult.getRecEndDate(), wikiContext);
            eventRecObj.set(EventConstants.PROPERTY_FREQUENCY_NAME, eventResult.getRecurrenceFreq(), wikiContext);
        }
    }

    private List<CalendarComponent> getCalendarComponents(String importedFileContent)
        throws IOException, ParserException
    {
        int beginIndex = importedFileContent.indexOf(CalendarKeys.ICS_CALENDAR_CALENDAR_BEGIN);
        int endIndex = importedFileContent.indexOf(CalendarKeys.ICS_CALENDAR_CALENDAR_END)
            + CalendarKeys.ICS_CALENDAR_CALENDAR_END.length();
        String filteredCalendarContent = importedFileContent.substring(beginIndex, endIndex);

        StringReader stringReader = new StringReader(filteredCalendarContent);
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = builder.build(stringReader);
        return calendar.getComponents(CalendarKeys.ICS_CALENDAR_CALENDAR_EVENT).stream().sorted((c1, c2) -> {
            String dtStartStr1 = c1.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_START_DATE).getValue();
            String dtStartStr2 = c2.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_START_DATE).getValue();
            return dtStartStr1.compareTo(dtStartStr2);
        }).collect(Collectors.toList());
    }

    private XWikiDocument getUniqueEventName(String eventName, String parentRef, List<XWikiDocument> eventDocuments)
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();

        DocumentReference parentDocRef = documentReferenceResolver.resolve(parentRef);
        SpaceReference documentSpaceRef = new SpaceReference(eventName, parentDocRef.getLastSpaceReference());
        DocumentReference eventRef = new DocumentReference(NEW_EVENT_HOME, documentSpaceRef);
        XWikiDocument eventDoc;

        if (wikiContext.getWiki().exists(eventRef, wikiContext) || sameNameEvent(eventRef, eventDocuments)) {
            String newEventName = eventName + "_" + System.nanoTime();
            SpaceReference newDocumentSpaceRef = new SpaceReference(newEventName, parentDocRef.getLastSpaceReference());
            DocumentReference newEventRef = new DocumentReference(NEW_EVENT_HOME, newDocumentSpaceRef);
            eventDoc = new XWikiDocument(newEventRef);
        } else {
            eventDoc = new XWikiDocument(eventRef);
        }
        eventDoc.setTitle(eventName);
        eventDoc.setParentReference(parentDocRef);
        return eventDoc;
    }

    private boolean sameNameEvent(DocumentReference eventRef, List<XWikiDocument> eventDocuments)
    {
        for (XWikiDocument document : eventDocuments) {
            if (document.getDocumentReference().equals(eventRef)) {
                return true;
            }
        }
        return false;
    }
}
