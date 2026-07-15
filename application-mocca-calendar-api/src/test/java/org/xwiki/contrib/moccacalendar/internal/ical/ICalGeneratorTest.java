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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.util.FixedUidGenerator;
import net.fortuna.ical4j.util.SimpleHostInfo;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import javax.inject.Named;
import javax.inject.Provider;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ICalGenerator}.
 *
 * @version $Id: $
 */
@ComponentTest
class ICalGeneratorTest
{
    private static final String EVENT_TIME = "2026-01-15T09:00:00Z";

    private static final String CALENDAR_REFERENCE = "Space.Calendar";

    private static final String CALENDAR_TITLE = "Test Calendar";

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private ICalGenerator iCalGenerator;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("document")
    private QueryFilter documentFilter;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private ICalEventGenerator eventGenerator;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private Query query;

    @Mock
    private XWikiDocument calendarDoc;

    @Mock
    private DocumentReference calendarRef;

    @Mock
    private XWikiDocument eventDoc1;

    @Mock
    private XWikiDocument eventDoc2;

    @Mock
    private XWikiDocument eventDoc3;

    @Mock
    private XWikiDocument eventDoc4;

    @Mock
    private XWikiDocument eventDoc6;

    @Mock
    private DocumentReference eventRef1;

    @Mock
    private DocumentReference eventRef2;

    @Mock
    private DocumentReference eventRef3;

    @Mock
    private DocumentReference eventRef4;

    @Mock
    private DocumentReference eventRef5;

    @Mock
    private DocumentReference eventRef6;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.referenceResolver.resolve(CALENDAR_REFERENCE)).thenReturn(this.calendarRef);
        mockDocument(this.calendarDoc, this.calendarRef);
        when(this.calendarDoc.getRenderedTitle(Syntax.PLAIN_1_0, this.context)).thenReturn(CALENDAR_TITLE);
        mockDocument(this.eventDoc1, this.eventRef1);
        mockDocument(this.eventDoc2, this.eventRef2);
        mockDocument(this.eventDoc3, this.eventRef3);
        mockDocument(this.eventDoc4, this.eventRef4);
        generateVEvent(this.eventDoc1, Instant.parse(EVENT_TIME), Instant.parse(EVENT_TIME).plusSeconds(3600),
            "Event" + " 1");
        generateVEvent(this.eventDoc2, Instant.parse(EVENT_TIME), Instant.parse(EVENT_TIME).plusSeconds(5400),
            "Event 2");
        generateVEvent(this.eventDoc3, Instant.parse(EVENT_TIME), Instant.parse(EVENT_TIME).plusSeconds(7200),
            "Event 3");
        when(this.eventGenerator.createEvent(this.eventDoc4)).thenReturn(null);
        when(this.wiki.getDocument(this.eventRef5, this.context)).thenReturn(null);
        when(this.wiki.getDocument(this.eventRef6, this.context)).thenReturn(this.eventDoc6);
        when(this.eventDoc6.isNew()).thenReturn(true);
        when(this.queryManager.createQuery(anyString(), eq(Query.XWQL))).thenReturn(this.query);
        when(this.query.bindValue(eq("parent"), eq(CALENDAR_REFERENCE))).thenReturn(this.query);
        when(this.query.addFilter(eq(this.documentFilter))).thenReturn(this.query);
    }

    @Test
    void generateCalendarBlankReference()
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThrows(FileNotFoundException.class, () -> this.iCalGenerator.generateCalendar("", outputStream));
    }

    @Test
    void generateCalendarDocumentNotFound()
    {
        when(this.calendarDoc.isNew()).thenReturn(true);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThrows(FileNotFoundException.class,
            () -> this.iCalGenerator.generateCalendar("Space.Missing", outputStream));
    }

    @Test
    void generateCalendarAccessDenied() throws Exception
    {
        doThrow(AccessDeniedException.class).when(this.contextualAuthorizationManager)
            .checkAccess(Right.VIEW, this.calendarRef);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThrows(AccessDeniedException.class,
            () -> this.iCalGenerator.generateCalendar(CALENDAR_REFERENCE, outputStream));
    }

    @Test
    void generateCalendarTimedEvents() throws Exception
    {
        when(this.query.execute()).thenReturn(
            List.of(this.eventRef1, this.eventRef2, this.eventRef3, this.eventRef4, this.eventRef5, this.eventRef6));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        this.iCalGenerator.generateCalendar(CALENDAR_REFERENCE, outputStream);
        String output = outputStream.toString(StandardCharsets.UTF_8);

        // Calendar metadata
        assertTrue(output.contains("PRODID:-//XWiki//iCal4j 1.0//EN"));
        assertTrue(output.contains("VERSION:2.0"));
        assertTrue(output.contains("CALSCALE:GREGORIAN"));
        assertTrue(output.contains("X-WR-CALNAME:" + CALENDAR_TITLE));
        assertTrue(output.contains("DTSTART:20260115T090000Z"));

        assertTrue(output.contains("SUMMARY:Event 1"));
        assertTrue(output.contains("DTEND:20260115T100000Z"));
        assertTrue(output.contains("SUMMARY:Event 2"));
        assertTrue(output.contains("DTEND:20260115T103000Z"));
        assertTrue(output.contains("DTEND:20260115T110000Z"));
        assertTrue(output.contains("SUMMARY:Event 3"));
        assertFalse(output.contains("SUMMARY:Event 4"));
        assertEquals(3, StringUtils.countMatches(output, "BEGIN:VEVENT"));
    }

    @Test
    void generateCalendarEmptyEvents() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        this.iCalGenerator.generateCalendar(CALENDAR_REFERENCE, outputStream);
        assertEquals(
            "One ore more components have errors: ValidationResult{entries=[ValidationEntry{message='Calendar must " + "contain at least one component', level=ERROR, context='VCALENDAR'}]}",
            this.logCapture.getMessage(0));
    }

    private void mockDocument(XWikiDocument doc, DocumentReference ref) throws Exception
    {
        when(doc.getDocumentReference()).thenReturn(ref);
        when(doc.isNew()).thenReturn(false);
        when(this.wiki.getDocument(ref, this.context)).thenReturn(doc);
    }

    private void generateVEvent(XWikiDocument doc, Instant start, Instant end, String title)
    {
        VEvent event = new VEvent(start, end, title);
        event.add(new FixedUidGenerator(new SimpleHostInfo("test"), end.toString()).generateUid());
        when(this.eventGenerator.createEvent(doc)).thenReturn(event);
    }
}
