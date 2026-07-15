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

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import net.fortuna.ical4j.model.property.RRule;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ICalRecurrenceGenerator}.
 *
 * @version $Id: $
 */
@ComponentTest
public class ICalRecurrenceGeneratorTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @InjectMockComponents
    private ICalRecurrenceGenerator recurrenceGenerator;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Mock
    private BaseObject eventData;
    @Mock
    private BaseObject recData;

    @Mock
    private XWikiDocument eventDoc;

    @Mock
    private DocumentReference eventRef;

    @Mock
    private DocumentReference recClassRef;

    @Mock
    private DocumentReference classRef;

    @BeforeEach
    void setUp()
    {
        when(this.referenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME)).thenReturn(this.classRef);
        when(this.referenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME)).thenReturn(
            this.recClassRef);
        when(this.eventDoc.getDocumentReference()).thenReturn(this.eventRef);
        when(this.eventDoc.getTitle()).thenReturn("title");
        when(this.eventDoc.getXObject(this.recClassRef)).thenReturn(this.recData);
        when(this.eventDoc.getXObject(this.classRef)).thenReturn(this.eventData);
        when(this.eventData.getIntValue(EventConstants.PROPERTY_RECURRENT_NAME)).thenReturn(1);
        when(this.eventData.getIntValue(EventConstants.PROPERTY_ALLDAY_NAME)).thenReturn(0);
    }

    @Test
    void getRecurrenceRuleNoFlag()
    {
        when(this.eventData.getIntValue(EventConstants.PROPERTY_RECURRENT_NAME)).thenReturn(0);
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertEquals("Event [eventRef] is not marked at recurrent. Skipping rrule generation.",
            this.logCapture.getMessage(0));
        assertTrue(rule.isEmpty());
    }

    @Test
    void getRecurrenceRuleNoObject()
    {
        when(this.eventDoc.getXObject(this.recClassRef)).thenReturn(null);
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertEquals("Event [eventRef] is marked as recurrent but has no recurrency object.",
            this.logCapture.getMessage(0));
        assertTrue(rule.isEmpty());
    }

    @Test
    void getRecurrenceRuleNoFreq()
    {
        when(this.recData.getStringValue(EventConstants.PROPERTY_FREQUENCY_NAME)).thenReturn(null);
        when(this.recData.getDocumentReference()).thenReturn(this.eventRef);
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertEquals("Missing recurrence frequency for event [eventRef].", this.logCapture.getMessage(0));
        assertTrue(rule.isEmpty());
    }

    @Test
    void getRecurrenceRuleDaily()
    {
        Date untilDate = Date.from(Instant.parse("2026-06-30T23:59:59Z"));
        setupRecurrence("daily", untilDate);
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertTrue(rule.isPresent());
        assertEquals("FREQ=DAILY;UNTIL=20260630T235959Z", rule.get().getValue());
    }

    @Test
    void getRecurrenceRuleWeekly()
    {
        Date untilDate = Date.from(Instant.parse("2026-06-30T23:59:59Z"));
        when(this.eventData.getIntValue(EventConstants.PROPERTY_ALLDAY_NAME)).thenReturn(1);
        setupRecurrence("weekly", untilDate);
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertTrue(rule.isPresent());
        assertEquals("FREQ=WEEKLY;UNTIL=20260630", rule.get().getValue());
    }

    @Test
    void getRecurrenceRuleWorkdays()
    {
        setupRecurrence("workdays", null);
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertTrue(rule.isPresent());
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", rule.get().getValue());
    }

    @Test
    void getRecurrenceRuleBiweekly()
    {
        Date untilDate = Date.from(Instant.parse("2026-06-30T23:59:59Z"));
        when(this.eventData.getIntValue(EventConstants.PROPERTY_ALLDAY_NAME)).thenReturn(1);
        setupRecurrence("biweekly", untilDate);
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertTrue(rule.isPresent());
        assertEquals("FREQ=WEEKLY;UNTIL=20260630;INTERVAL=2", rule.get().getValue());
    }

    @Test
    void getRecurrenceRuleMonthly()
    {
        Date untilDate = Date.from(Instant.parse("2026-06-30T23:59:59Z"));
        setupRecurrence("monthly", untilDate);
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertTrue(rule.isPresent());
        assertEquals("FREQ=MONTHLY;UNTIL=20260630T235959Z", rule.get().getValue());
    }

    @Test
    void getRecurrenceRuleQuarterly()
    {
        when(this.eventData.getIntValue(EventConstants.PROPERTY_ALLDAY_NAME)).thenReturn(1);
        setupRecurrence("quarterly", null);
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertTrue(rule.isPresent());
        assertEquals("FREQ=MONTHLY;INTERVAL=3", rule.get().getValue());
    }

    @Test
    void getRecurrenceRuleYearly()
    {
        Date untilDate = Date.from(Instant.parse("2026-06-30T23:59:59Z"));
        setupRecurrence("yearly", untilDate);
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertTrue(rule.isPresent());
        assertEquals("FREQ=YEARLY;UNTIL=20260630T235959Z", rule.get().getValue());
    }

    @Test
    void getRecurrenceRuleUnknownFreq()
    {
        Date untilDate = Date.from(Instant.parse("2026-06-30T23:59:59Z"));
        setupRecurrence("unknown", untilDate);
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertTrue(rule.isEmpty());
        assertEquals("Unknown recurrence frequency [unknown].", this.logCapture.getMessage(0));
    }

    @Test
    void customWeeklyRecurrenceDays()
    {
        setupRecurrence("customWeekly", null);
        when(this.recData.getListValue("days")).thenReturn(List.of("2", "4", "6"));
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertTrue(rule.isPresent());
        assertEquals("FREQ=WEEKLY;BYDAY=MO,WE,FR", rule.get().getValue());
    }

    @Test
    void customWeeklyRecurrenceNoDays() throws Exception
    {
        setupRecurrence("customWeekly", null);
        when(this.recData.getListValue("days")).thenReturn(new ArrayList<>());
        Optional<RRule> rule = this.recurrenceGenerator.getRecurrenceRule(this.eventDoc);
        assertTrue(rule.isPresent());
        assertEquals("FREQ=WEEKLY", rule.get().getValue());
    }

    private void setupRecurrence(String frequency, Date lastInstance)
    {
        when(this.recData.getStringValue(EventConstants.PROPERTY_FREQUENCY_NAME)).thenReturn(frequency);
        when(this.recData.getDateValue(EventConstants.PROPERTY_LASTINSTANCE_NAME)).thenReturn(lastInstance);
    }
}
