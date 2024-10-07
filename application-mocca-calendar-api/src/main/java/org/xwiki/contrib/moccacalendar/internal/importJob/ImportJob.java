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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobRequest;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobStatus;
import org.xwiki.fullcalendar.FullCalendarManager;
import org.xwiki.fullcalendar.model.CalendarEvent;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

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

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private FullCalendarManager fullCalendarManager;

    @Inject
    private CalendarEventImporter calendarEventImporter;

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
            List<CalendarEvent> calendarEventsJson =
                fullCalendarManager.getICalEventsFromFile(getICSFileContent(request.getFile()), null, null, true);
            List<XWikiDocument> eventDocuments = new ArrayList<>();
            this.progressManager.pushLevelProgress(calendarEventsJson.size() + 1, this);

            for (CalendarEvent calendarEvent : calendarEventsJson) {
                progressManager.startStep(this);
                if (status.isCanceled()) {
                    progressManager.endStep(this);
                    break;
                }
                String eventName = calendarEvent.getTitle().trim();
                if (eventName.isEmpty()) {
                    continue;
                }
                XWikiDocument eventDoc =
                    calendarEventImporter.getUniqueEventName(eventName, request.getParentRef(), eventDocuments);
                calendarEventImporter.importCalendarEvent(eventDoc, calendarEvent);
                eventDocuments.add(eventDoc);

                progressManager.endStep(this);
                Thread.yield();
            }
            batchSave(eventDocuments);
        } catch (Exception e) {
            logger.warn("Import .ics file job failed. Root cause is: [{}]", ExceptionUtils.getRootCauseMessage(e));
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
            for (XWikiDocument calendarEvent : eventDocuments) {
                wiki.saveDocument(calendarEvent, wikiContext);
            }
            progressManager.endStep(this);
        }
    }

    private byte[] getICSFileContent(byte[] importedFileContent)
    {
        String importedFileString = new String(importedFileContent);
        int beginIndex = importedFileString.indexOf(CalendarKeys.ICS_CALENDAR_CALENDAR_BEGIN);
        int endIndex = importedFileString.indexOf(CalendarKeys.ICS_CALENDAR_CALENDAR_END)
            + CalendarKeys.ICS_CALENDAR_CALENDAR_END.length();
        String filteredCalendarContent = importedFileString.substring(beginIndex, endIndex);

        return filteredCalendarContent.getBytes();
    }
}
