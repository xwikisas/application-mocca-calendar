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
package org.xwiki.contrib.moccacalendar.internal.rest;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobRequest;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.contrib.moccacalendar.internal.ical.ICalGenerator;
import org.xwiki.contrib.moccacalendar.internal.importJob.ImportJob;
import org.xwiki.contrib.moccacalendar.rest.MoccaCalendarResource;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.resources.pages.ModifiablePageResource;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;

/**
 * Default implementation of {@link MoccaCalendarResource}.
 *
 * @version $Id$
 * @since 2.14
 */
@Unstable
@Component
@Named("org.xwiki.contrib.moccacalendar.internal.rest.DefaultMoccaCalendarResource")
@Singleton
public class DefaultMoccaCalendarResource extends ModifiablePageResource implements MoccaCalendarResource
{
    private static final String CALENDAR_NOT_FOUND = "Failed to look up the given calendar.";

    @Inject
    private Logger logger;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private ICalGenerator iCalGenerator;

    @Inject
    private CSRFToken csrf;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public Response importCalendarFile(String parentCalendar, InputStream file)
    {
        try {
            validateCSRF();
            DocumentReference calendarRef = getEditableCalendar(parentCalendar);
            List<String> jobId = new ArrayList<>();
            jobId.add("moccacalendar");
            jobId.add("import");
            jobId.add(parentCalendar);
            if (this.jobExecutor.getJob(jobId) == null) {
                XWikiContext wikiContext = this.xcontextProvider.get();
                // Read the limit from the calendar space, like the UI does, since there's no relevant context document.
                String maxSizeValue = wikiContext.getWiki().getSpacePreference(
                    FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER, calendarRef.getLastSpaceReference(), "", wikiContext);
                long maxSize = NumberUtils.toLong(maxSizeValue, FileUploadPlugin.UPLOAD_DEFAULT_MAXSIZE);
                // Fail fast when the client announces a body that is too large.
                if (wikiContext.getRequest() != null && wikiContext.getRequest().getContentLengthLong() > maxSize) {
                    return fileTooLargeResponse();
                }
                // The announced length can't be trusted (or can be missing): read at most one byte more than allowed,
                // which is enough to detect a file that is too large without loading it entirely.
                byte[] fileContent = new BoundedInputStream(file, maxSize + 1).readAllBytes();
                if (fileContent.length > maxSize) {
                    return fileTooLargeResponse();
                }
                // Pass the absolute reference so that the job creates the events where the rights were checked.
                ImportJobRequest importJobRequest =
                    new ImportJobRequest(jobId, fileContent, this.serializer.serialize(calendarRef),
                        wikiContext.getUserReference());
                this.jobExecutor.execute(ImportJob.JOB_TYPE, importJobRequest);
                return Response.status(202).type(MediaType.TEXT_PLAIN_TYPE).build();
            } else {
                return Response.notModified().type(MediaType.TEXT_PLAIN_TYPE).build();
            }
        } catch (AccessDeniedException | SecurityException e) {
            this.logger.warn("Authorization error while attempting to import calendar file.", e);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } catch (FileNotFoundException e) {
            this.logger.warn(CALENDAR_NOT_FOUND, e);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (Exception e) {
            this.logger.warn("Failed to import .ics file data. Root cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response getICalContent(String calendarReference) throws XWikiRestException
    {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            this.iCalGenerator.generateCalendar(calendarReference, buffer);
            return Response.ok(buffer.toByteArray()).type("text/calendar")
                .header("Content-Disposition", "attachment; filename=\"" + calendarReference + ".ics\"").build();
        } catch (AccessDeniedException e) {
            this.logger.warn("Authorization error while attempting to generate ICal content.", e);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } catch (FileNotFoundException e) {
            this.logger.warn(CALENDAR_NOT_FOUND, e);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (Exception e) {
            this.logger.error("Failed to generate .ics file. Root cause: [{}]", ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Events are imported only under an existing calendar, so the edit right is checked on the calendar space, where
     * the events are created.
     */
    private DocumentReference getEditableCalendar(String calendarReference) throws Exception
    {
        DocumentReference calendarRef = this.referenceResolver.resolve(calendarReference);
        this.contextualAuthorizationManager.checkAccess(Right.VIEW, calendarRef);
        this.contextualAuthorizationManager.checkAccess(Right.EDIT, calendarRef.getLastSpaceReference());
        XWikiContext context = this.xcontextProvider.get();
        XWikiDocument calendarDoc = context.getWiki().getDocument(calendarRef, context);
        DocumentReference calendarClassRef =
            calendarDoc.resolveClassReference(EventConstants.MOCCA_CALENDAR_CLASS_NAME);
        if (calendarDoc.getXObject(calendarClassRef) == null) {
            throw new FileNotFoundException(String.format("No calendar found at [%s].", calendarRef));
        }
        return calendarRef;
    }

    private void validateCSRF()
    {
        XWikiContext context = this.xcontextProvider.get();
        String token = context.getRequest() != null ? context.getRequest().getHeader("xwiki-form-token") : null;
        if (!this.csrf.isTokenValid(token)) {
            throw new SecurityException("Invalid CSRF token.");
        }
    }

    private Response fileTooLargeResponse()
    {
        return Response.status(413).type(MediaType.TEXT_PLAIN_TYPE).build();
    }
}
