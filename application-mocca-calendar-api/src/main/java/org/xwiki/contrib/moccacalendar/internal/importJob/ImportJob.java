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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobRequest;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobStatus;
import org.xwiki.fullcalendar.FullCalendarManager;
import org.xwiki.fullcalendar.model.CalendarEvent;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

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

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> resolver;

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
            byte[] icsFileContent;
            try {
                icsFileContent = getICSFileContent(this.request.getFile());
            } finally {
                // Don't keep the imported content in memory with the job status.
                this.request.clearFile();
            }
            List<CalendarEvent> calendarEventsJson =
                this.fullCalendarManager.getICalEventsFromFile(icsFileContent, null, null, true);
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
            UserReference userReference = this.resolver.resolve(this.request.getUserReference());
            for (XWikiDocument calendarEvent : eventDocuments) {
                calendarEvent.getAuthors().setCreator(userReference);
                calendarEvent.getAuthors().setEffectiveMetadataAuthor(userReference);
                wiki.saveDocument(calendarEvent, wikiContext);
            }
            progressManager.endStep(this);
        }
    }

    private byte[] getICSFileContent(byte[] importedFileContent)
    {
        if (importedFileContent == null) {
            throw new IllegalArgumentException("No .ics file content to import.");
        }
        // ISO-8859-1 maps each byte to one char, so indexes in the string are byte offsets in the original content
        // (and the ASCII delimiters are found as-is in UTF-8 content).
        String content = new String(importedFileContent, StandardCharsets.ISO_8859_1);
        int beginIndex = content.indexOf(CalendarKeys.ICS_CALENDAR_CALENDAR_BEGIN);
        int endIndex = beginIndex < 0 ? -1 : content.indexOf(CalendarKeys.ICS_CALENDAR_CALENDAR_END, beginIndex);
        if (endIndex < 0) {
            throw new IllegalArgumentException("The imported file is not a valid .ics file.");
        }
        endIndex += CalendarKeys.ICS_CALENDAR_CALENDAR_END.length();
        if (beginIndex == 0 && endIndex == importedFileContent.length) {
            return importedFileContent;
        }

        return Arrays.copyOfRange(importedFileContent, beginIndex, endIndex);
    }
}
