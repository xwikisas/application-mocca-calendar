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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobRequest;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.contrib.moccacalendar.internal.ical.ICalGenerator;
import org.xwiki.contrib.moccacalendar.internal.importJob.ImportJob;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultMoccaCalendarResource}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultMoccaCalendarResourceTest
{
    private static final String CALENDAR_REFERENCE = "Space.Calendar.WebHome";

    private static final String ABSOLUTE_CALENDAR_REFERENCE = "xwiki:Space.Calendar.WebHome";

    private static final DocumentReference CALENDAR_REF =
        new DocumentReference("xwiki", List.of("Space", "Calendar"), "WebHome");

    private static final SpaceReference CALENDAR_SPACE_REF = CALENDAR_REF.getLastSpaceReference();

    private static final DocumentReference CALENDAR_CLASS_REF =
        new DocumentReference("xwiki", "MoccaCalendar", "MoccaCalendarClass");

    private static final List<String> JOB_ID = List.of("moccacalendar", "import", CALENDAR_REFERENCE);

    private static final long MAX_SIZE = 10;

    private static final String TOKEN_HEADER = "xwiki-form-token";

    private static final String TOKEN = "token";

    private static final String IMPORT_AUTHORIZATION_ERROR =
        "Authorization error while attempting to import calendar file.";

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private DefaultMoccaCalendarResource resource;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private CSRFToken csrf;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private ICalGenerator iCalGenerator;

    @MockComponent
    private JobExecutor jobExecutor;

    private XWikiContext context = mock(XWikiContext.class);

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiRequest request;

    @Mock
    private XWikiDocument calendarDoc;

    @BeforeComponent
    void configure()
    {
        // The resource initialization reads the current user from the context.
        when(this.xcontextProvider.get()).thenReturn(this.context);
    }

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.request.getHeader(TOKEN_HEADER)).thenReturn(TOKEN);
        when(this.csrf.isTokenValid(TOKEN)).thenReturn(true);
        when(this.referenceResolver.resolve(CALENDAR_REFERENCE)).thenReturn(CALENDAR_REF);
        when(this.serializer.serialize(CALENDAR_REF)).thenReturn(ABSOLUTE_CALENDAR_REFERENCE);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.context.getRequest()).thenReturn(this.request);
        when(this.request.getContentLengthLong()).thenReturn(-1L);
        when(this.wiki.getSpacePreference(FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER, CALENDAR_SPACE_REF, "",
            this.context)).thenReturn(String.valueOf(MAX_SIZE));
        when(this.wiki.getDocument(CALENDAR_REF, this.context)).thenReturn(this.calendarDoc);
        when(this.calendarDoc.resolveClassReference(EventConstants.MOCCA_CALENDAR_CLASS_NAME))
            .thenReturn(CALENDAR_CLASS_REF);
        when(this.calendarDoc.getXObject(CALENDAR_CLASS_REF)).thenReturn(mock(BaseObject.class));
    }

    @Test
    void getICalContent() throws Exception
    {
        Response response = this.resource.getICalContent(CALENDAR_REFERENCE);

        assertEquals(200, response.getStatus());
        verify(this.iCalGenerator).generateCalendar(eq(CALENDAR_REFERENCE), any(OutputStream.class));
    }

    @Test
    void getICalContentWithoutCSRFToken() throws Exception
    {
        // The export is a read-only request, so it doesn't need any CSRF token.
        when(this.request.getHeader(TOKEN_HEADER)).thenReturn(null);

        Response response = this.resource.getICalContent(CALENDAR_REFERENCE);

        assertEquals(200, response.getStatus());
        verify(this.csrf, never()).isTokenValid(any());
    }

    @Test
    void getICalContentAccessDenied() throws Exception
    {
        // The generator checks the view right on the calendar.
        doThrow(AccessDeniedException.class).when(this.iCalGenerator)
            .generateCalendar(eq(CALENDAR_REFERENCE), any(OutputStream.class));

        WebApplicationException exception =
            assertThrows(WebApplicationException.class, () -> this.resource.getICalContent(CALENDAR_REFERENCE));

        assertEquals(401, exception.getResponse().getStatus());
        assertEquals("Authorization error while attempting to generate ICal content.",
            this.logCapture.getMessage(0));
    }

    @Test
    void getICalContentNotFound() throws Exception
    {
        doThrow(FileNotFoundException.class).when(this.iCalGenerator)
            .generateCalendar(eq(CALENDAR_REFERENCE), any(OutputStream.class));

        WebApplicationException exception =
            assertThrows(WebApplicationException.class, () -> this.resource.getICalContent(CALENDAR_REFERENCE));

        assertEquals(404, exception.getResponse().getStatus());
        assertEquals("Failed to look up the given calendar.", this.logCapture.getMessage(0));
    }

    @Test
    void importCalendarFile() throws Exception
    {
        byte[] content = "0123456789".getBytes(StandardCharsets.UTF_8);

        Response response = this.resource.importCalendarFile(CALENDAR_REFERENCE, new ByteArrayInputStream(content));

        assertEquals(202, response.getStatus());
        verify(this.contextualAuthorizationManager).checkAccess(Right.EDIT, CALENDAR_SPACE_REF);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(this.jobExecutor).execute(eq(ImportJob.JOB_TYPE), requestCaptor.capture());
        ImportJobRequest importJobRequest = (ImportJobRequest) requestCaptor.getValue();
        assertEquals(JOB_ID, importJobRequest.getId());
        // The job must create the events where the rights were checked.
        assertEquals(ABSOLUTE_CALENDAR_REFERENCE, importJobRequest.getParentRef());
        assertArrayEquals(content, importJobRequest.getFile());
    }

    @Test
    void importCalendarFileWithoutEditRightOnCalendarSpace() throws Exception
    {
        doThrow(AccessDeniedException.class).when(this.contextualAuthorizationManager)
            .checkAccess(Right.EDIT, CALENDAR_SPACE_REF);
        InputStream file = mock(InputStream.class);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
            () -> this.resource.importCalendarFile(CALENDAR_REFERENCE, file));

        assertEquals(401, exception.getResponse().getStatus());
        verifyNoInteractions(file);
        verify(this.jobExecutor, never()).execute(any(), any());
        assertEquals(IMPORT_AUTHORIZATION_ERROR, this.logCapture.getMessage(0));
    }

    @Test
    void importCalendarFileNotInCalendar() throws Exception
    {
        when(this.calendarDoc.getXObject(CALENDAR_CLASS_REF)).thenReturn(null);
        InputStream file = mock(InputStream.class);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
            () -> this.resource.importCalendarFile(CALENDAR_REFERENCE, file));

        assertEquals(404, exception.getResponse().getStatus());
        verifyNoInteractions(file);
        verify(this.jobExecutor, never()).execute(any(), any());
        assertEquals("Failed to look up the given calendar.", this.logCapture.getMessage(0));
    }

    @Test
    void importCalendarFileInvalidCSRFToken() throws Exception
    {
        when(this.csrf.isTokenValid(TOKEN)).thenReturn(false);
        InputStream file = mock(InputStream.class);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
            () -> this.resource.importCalendarFile(CALENDAR_REFERENCE, file));

        assertEquals(401, exception.getResponse().getStatus());
        verifyNoInteractions(file);
        verify(this.jobExecutor, never()).execute(any(), any());
        assertEquals(IMPORT_AUTHORIZATION_ERROR, this.logCapture.getMessage(0));
    }

    @Test
    void importCalendarFileWithoutRequest() throws Exception
    {
        when(this.context.getRequest()).thenReturn(null);
        InputStream file = mock(InputStream.class);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
            () -> this.resource.importCalendarFile(CALENDAR_REFERENCE, file));

        assertEquals(401, exception.getResponse().getStatus());
        verify(this.csrf).isTokenValid(null);
        verifyNoInteractions(file);
        verify(this.jobExecutor, never()).execute(any(), any());
        assertEquals(IMPORT_AUTHORIZATION_ERROR, this.logCapture.getMessage(0));
    }

    @Test
    void importCalendarFileTooLargeResponse() throws Exception
    {
        byte[] content = "0123456789X".getBytes(StandardCharsets.UTF_8);

        Response response = this.resource.importCalendarFile(CALENDAR_REFERENCE, new ByteArrayInputStream(content));

        assertEquals(413, response.getStatus());
        verify(this.jobExecutor, never()).execute(anyString(), any());
    }

    @Test
    void importCalendarFileWithDefaultMaxSize() throws Exception
    {
        when(this.wiki.getSpacePreference(FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER, CALENDAR_SPACE_REF, "",
            this.context)).thenReturn("");
        byte[] content = "0123456789X".getBytes(StandardCharsets.UTF_8);

        Response response = this.resource.importCalendarFile(CALENDAR_REFERENCE, new ByteArrayInputStream(content));

        assertEquals(202, response.getStatus());
        verify(this.jobExecutor).execute(eq(ImportJob.JOB_TYPE), any());
    }

    @Test
    void importCalendarFileWithTooLargeContentLength() throws Exception
    {
        when(this.request.getContentLengthLong()).thenReturn(MAX_SIZE + 1);
        InputStream file = mock(InputStream.class);

        Response response = this.resource.importCalendarFile(CALENDAR_REFERENCE, file);

        assertEquals(413, response.getStatus());
        verifyNoInteractions(file);
        verify(this.jobExecutor, never()).execute(anyString(), any());
    }

    @Test
    void importCalendarFileWhenJobAlreadyRunning() throws Exception
    {
        when(this.jobExecutor.getJob(JOB_ID)).thenReturn(mock(Job.class));
        InputStream file = mock(InputStream.class);

        Response response = this.resource.importCalendarFile(CALENDAR_REFERENCE, file);

        assertEquals(304, response.getStatus());
        verifyNoInteractions(file);
        verify(this.jobExecutor, never()).execute(anyString(), any());
    }
}
