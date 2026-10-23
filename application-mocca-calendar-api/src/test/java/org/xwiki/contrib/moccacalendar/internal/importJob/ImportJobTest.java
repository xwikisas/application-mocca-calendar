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
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobRequest;
import org.xwiki.fullcalendar.FullCalendarManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ImportJob}.
 *
 * @version $Id$
 */
@ComponentTest
class ImportJobTest
{
    private static final String PARENT_CALENDAR = "Space.Calendar.WebHome";

    private static final String CALENDAR = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR";

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private ImportJob importJob;

    @MockComponent
    private FullCalendarManager fullCalendarManager;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @BeforeEach
    void setUp()
    {
        when(this.wikiContextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
    }

    private ImportJobRequest initialize(String content)
    {
        ImportJobRequest request = new ImportJobRequest(List.of("moccacalendar", "import", PARENT_CALENDAR),
            content.getBytes(StandardCharsets.UTF_8), PARENT_CALENDAR, null);
        this.importJob.initialize(request);
        return request;
    }

    @Test
    void runInternal() throws Exception
    {
        ImportJobRequest request = initialize("garbage before\r\n" + CALENDAR + "\r\ngarbage after");
        when(this.fullCalendarManager.getICalEventsFromFile(any(), any(), any(), any(Boolean.class)))
            .thenReturn(List.of());

        this.importJob.runInternal();

        verify(this.fullCalendarManager).getICalEventsFromFile(CALENDAR.getBytes(StandardCharsets.UTF_8), null,
            null, true);
        // The imported content must not be kept in memory with the job status once processed.
        assertNull(request.getFile());
    }

    @Test
    void runInternalWithInvalidFile() throws Exception
    {
        ImportJobRequest request = initialize("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n");

        assertThrows(RuntimeException.class, () -> this.importJob.runInternal());

        verify(this.fullCalendarManager, never()).getICalEventsFromFile(any(), any(), any(), any(Boolean.class));
        assertNull(request.getFile());
        assertEquals("Import .ics file job failed. Root cause is: "
            + "[IllegalArgumentException: The imported file is not a valid .ics file.]", this.logCapture.getMessage(0));
    }

    @Test
    void requestFileIsNotSerialized()
    {
        ImportJobRequest request = new ImportJobRequest(List.of("moccacalendar", "import", PARENT_CALENDAR),
            CALENDAR.getBytes(StandardCharsets.UTF_8), PARENT_CALENDAR, null);

        ImportJobRequest deserializedRequest = SerializationUtils.roundtrip(request);

        assertNotNull(request.getFile());
        assertNull(deserializedRequest.getFile());
        assertEquals(PARENT_CALENDAR, deserializedRequest.getParentRef());
        assertArrayEquals(request.getId().toArray(), deserializedRequest.getId().toArray());
    }
}
