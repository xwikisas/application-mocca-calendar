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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import javax.inject.Named;
import javax.inject.Provider;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ICalEventGenerator}.
 *
 * @version $Id: $
 */
@ComponentTest
public class ICalEventGeneratorTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private ICalEventGenerator eventGenerator;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext context;

    @Mock
    private BaseObject eventData;

    @Mock
    private XWikiDocument eventDoc;

    @Mock
    private DocumentReference eventRef;

    @Mock
    private DocumentReference classRef;

    @BeforeEach
    void setUp()
    {
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.referenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME)).thenReturn(this.classRef);
        when(this.eventDoc.getXObject(this.classRef)).thenReturn(this.eventData);
        when(eventDoc.getDocumentReference()).thenReturn(eventRef);
        when(eventDoc.getTitle()).thenReturn("Event 1");
    }

    @Test
    void createEventNoObject()
    {
        when(this.eventDoc.getXObject(this.classRef)).thenReturn(null);
        assertNull(this.eventGenerator.createEvent(this.eventDoc));
        assertEquals("Event document [eventRef] has no event data object.", this.logCapture.getMessage(0));
    }

    @Test
    void generateCalendarTimedEventsNoStartDate()
    {
        // Event: timed event with both start and end date
        createEventData(null, null, false);
        assertNull(this.eventGenerator.createEvent(this.eventDoc));
        assertEquals("Event [eventRef] has no start date.", this.logCapture.getMessage(0));
    }

    @Test
    void generateCalendarTimedEventsNoEnd()
    {
        // Event: timed event without end date (should default to start + 1h)
        createEventData(Instant.parse("2026-02-20T14:00:00Z"), null, false);
        VEvent event = this.eventGenerator.createEvent(this.eventDoc);
        assertEquals("20260220T140000Z", event.getDateTimeStart().getValue());
        assertEquals("20260220T150000Z", event.getDateTimeEnd().getValue());
        assertEquals("Event 1", event.getSummary().getValue());
        assertTrue(event.getUid().get().getValue().contains("atelier-medias.org"));
    }

    @Test
    void generateCalendarTimedEvents()
    {
        createEventData(Instant.parse("2026-02-20T14:00:00Z"), Instant.parse("2026-02-20T19:00:00Z"), false);
        VEvent event = this.eventGenerator.createEvent(this.eventDoc);
        assertEquals("20260220T140000Z", event.getDateTimeStart().getValue());
        assertEquals("20260220T190000Z", event.getDateTimeEnd().getValue());
        assertTrue(event.getUid().get().getValue().contains("atelier-medias.org"));
    }

    @Test
    void generateCalendarAllDayEvents() throws Exception
    {
        // Event: all-day event with end date (end date should be +1 day in output)
        Instant allDayStart = Instant.parse("2026-03-10T12:00:00Z");
        Instant allDayEnd = Instant.parse("2026-03-11T12:00:00Z");
        createEventData(allDayStart, allDayEnd, true);
        VEvent event = this.eventGenerator.createEvent(this.eventDoc);
        assertEquals("20260310", event.getDateTimeStart().getValue());
        assertEquals("20260312", event.getDateTimeEnd().getValue());
    }

    @Test
    void generateCalendarAllDayEventsNoEnd() throws Exception
    {
        // Event: all-day event with end date (end date should be +1 day in output)
        Instant allDayStart = Instant.parse("2026-03-10T12:00:00Z");
        createEventData(allDayStart, null, true);
        VEvent event = this.eventGenerator.createEvent(this.eventDoc);
        assertEquals("20260310", event.getDateTimeStart().getValue());
        assertNull(event.getDateTimeEnd());
    }

    private void createEventData(Instant start, Instant end, boolean allDay)
    {
        Date startDate = start != null ? Date.from(start) : null;
        Date endDate = end != null ? Date.from(end) : null;
        when(this.eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME)).thenReturn(startDate);
        when(this.eventData.getDateValue(EventConstants.PROPERTY_ENDDATE_NAME)).thenReturn(endDate);
        when(this.eventData.getIntValue(EventConstants.PROPERTY_ALLDAY_NAME)).thenReturn(allDay ? 1 : 0);
        when(this.eventData.getIntValue(EventConstants.PROPERTY_RECURRENT_NAME)).thenReturn(0);
        when(this.eventData.getStringValue(EventConstants.PROPERTY_DESCRIPTION_NAME)).thenReturn(null);
    }
}
