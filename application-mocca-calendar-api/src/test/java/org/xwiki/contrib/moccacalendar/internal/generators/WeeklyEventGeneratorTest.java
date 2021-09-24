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
package org.xwiki.contrib.moccacalendar.internal.generators;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.moccacalendar.EventInstance;
import org.xwiki.contrib.moccacalendar.RecurrentEventGenerator;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WeeklyEventGeneratorTest
{

    @Rule
    public MockitoComponentMockingRule<RecurrentEventGenerator> mocker = new MockitoComponentMockingRule<>(
        WeeklyEventGenerator.class);

    protected BaseObject eventData;
    protected BaseObject eventRecurrentData;
    protected XWikiDocument eventDoc;

    @Before
    public void setUpMocks()
    {
        DocumentReference eventClass = new DocumentReference("a", "b", "c");
        DocumentReference recurrencyClass = new DocumentReference("d", "e", "f");

        eventDoc = mock(XWikiDocument.class);
        eventData = mock(BaseObject.class);
        eventRecurrentData = mock(BaseObject.class);

        when(eventDoc.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME)).thenReturn(eventClass);
        when(eventDoc.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME))
            .thenReturn(recurrencyClass);

        when(eventDoc.getXObject(eventClass)).thenReturn(eventData);
        when(eventDoc.getXObject(recurrencyClass)).thenReturn(eventRecurrentData);
    }

    @Test
    public void testEventGeneration() throws Exception
    {
        Calendar cal = Calendar.getInstance();
        cal.set(1987, Calendar.MAY, 1, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date dateFrom = cal.getTime();

        cal.set(Calendar.MONTH, Calendar.JUNE);
        Date dateTo = cal.getTime();

        cal.set(Calendar.YEAR, 1982);
        cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 20);
        Date startDate = cal.getTime();

        when(eventData.getDateValue("startDate")).thenReturn(startDate);
        when(eventData.getIntValue("allDay")).thenReturn(1);

        when(eventRecurrentData.getDateValue("firstInstance")).thenReturn(startDate);
        when(eventRecurrentData.getDateValue("lastInstance")).thenReturn(null);

        //
        // now the actual test
        //

        List<DateTime> expectedStartDates = new ArrayList<>();
        List<DateTime> expectedEndDates = new ArrayList<>();
        cal.set(1987, Calendar.MAY, 4, 0, 0, 0);
        for (int i = 1; i <= 4; i++) {
            expectedStartDates.add(new DateTime(cal.getTime().getTime()));
            expectedEndDates.add(new DateTime(cal.getTime().getTime()));
            cal.set(Calendar.DAY_OF_MONTH, 5 + 7 * (i - 1));
            // FIXME: should we expect the event to last till the start of the next day + 30 min?
            // currently this is the (backwards compatible) behaviour
            cal.set(Calendar.MINUTE, 30);
            //expectedEndDates.add(new DateTime(cal.getTime().getTime()));
            cal.set(Calendar.DAY_OF_MONTH, 4 + 7 * i);
            cal.set(Calendar.MINUTE, 0);
        }

        List<EventInstance> eventInstances = mocker.getComponentUnderTest().generate(eventDoc, dateFrom, dateTo);

        Assert.assertNotNull("should return some values", eventInstances);

        for (int i = 0; i < 4; i++) {
            Assert.assertThat("expected four events", eventInstances.size(), Matchers.greaterThan(i));
            EventInstance event = eventInstances.get(i);
            DateTime expectedStartDate = expectedStartDates.get(i);
            DateTime expectedEndDate = expectedEndDates.get(i);
            Assert.assertEquals("should get monday", expectedStartDate, event.getStartDate());
            // Assert.assertEquals("should get monday as end", expectedDate, event.getEndDate());
            Assert.assertEquals("should end on tuesday", expectedEndDate, event.getEndDate());
        }
    }

    @Test
    public void testEventEndsBeforeDateEnd() throws Exception
    {
        Calendar cal = Calendar.getInstance();
        cal.set(1987, Calendar.MAY, 1, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date dateFrom = cal.getTime();

        cal.set(Calendar.MONTH, Calendar.JUNE);
        Date dateTo = cal.getTime();

        cal.set(Calendar.YEAR, 1982);
        cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 20);
        Date startDate = cal.getTime();

        when(eventData.getDateValue("startDate")).thenReturn(startDate);
        when(eventData.getIntValue("allDay")).thenReturn(1);

        when(eventRecurrentData.getDateValue("firstInstance")).thenReturn(startDate);
        cal.set(Calendar.YEAR, 1987);
        cal.set(Calendar.MONTH, Calendar.MAY);
        cal.set(Calendar.DAY_OF_MONTH, 18);
        Date lastInstanceDate = cal.getTime();

        when(eventRecurrentData.getDateValue("lastInstance")).thenReturn(lastInstanceDate);

        //
        // now the actual test
        //

        List<DateTime> expectedStartDates = new ArrayList<>();
        List<DateTime> expectedEndDates = new ArrayList<>();
        cal.set(1987, Calendar.MAY, 4, 0, 0, 0);
        for (int i = 1; i <= 3; i++) {
            expectedStartDates.add(new DateTime(cal.getTime().getTime()));
            expectedEndDates.add(new DateTime(cal.getTime().getTime()));
            cal.set(Calendar.DAY_OF_MONTH, 5 + 7 * (i - 1));
            // FIXME: see above
            cal.set(Calendar.MINUTE, 30);
            //expectedEndDates.add(new DateTime(cal.getTime().getTime()));
            cal.set(Calendar.DAY_OF_MONTH, 4 + 7 * i);
            cal.set(Calendar.MINUTE, 0);
        }

        List<EventInstance> eventInstances = mocker.getComponentUnderTest().generate(eventDoc, dateFrom, dateTo);

        Assert.assertNotNull("should return some values", eventInstances);

        for (int i = 0; i < 3; i++) {
            Assert.assertThat("expected three events", eventInstances.size(), Matchers.greaterThan(i));
            EventInstance event = eventInstances.get(i);
            DateTime expectedStartDate = expectedStartDates.get(i);
            DateTime expectedEndDate = expectedEndDates.get(i);
            Assert.assertEquals("should start on monday", expectedStartDate, event.getStartDate());
            Assert.assertEquals("should end on tuesday", expectedEndDate, event.getEndDate());
        }
    }

    @Test
    public void testEventEndsBeforeDateStart() throws Exception
    {
        Calendar cal = Calendar.getInstance();
        cal.set(1987, Calendar.MAY, 1, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date dateFrom = cal.getTime();

        cal.set(Calendar.MONTH, Calendar.JUNE);
        Date dateTo = cal.getTime();

        cal.set(Calendar.YEAR, 1982);
        cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 20);
        Date startDate = cal.getTime();

        when(eventData.getDateValue("startDate")).thenReturn(startDate);
        when(eventData.getIntValue("allDay")).thenReturn(1);

        when(eventRecurrentData.getDateValue("firstInstance")).thenReturn(startDate);
        cal.set(Calendar.YEAR, 1987);
        cal.set(Calendar.MONTH, Calendar.APRIL);
        cal.set(Calendar.DAY_OF_MONTH, 27);
        Date lastInstanceDate = cal.getTime();

        when(eventRecurrentData.getDateValue("lastInstance")).thenReturn(lastInstanceDate);

        //
        // now the actual test
        //

        List<EventInstance> eventInstances = mocker.getComponentUnderTest().generate(eventDoc, dateFrom, dateTo);

        Assert.assertNotNull("should return some values", eventInstances);
        Assert.assertThat("should return empty list", eventInstances, Matchers.empty());
    }

    @Test
    public void testMultipleDayEvenCrossesDateFromAndDateTo() throws Exception
    {
        Calendar cal = Calendar.getInstance();
        cal.set(1987, Calendar.APRIL, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date dateFrom = cal.getTime();

        cal.set(Calendar.MONTH, Calendar.MAY);
        Date dateTo = cal.getTime();

        cal.set(Calendar.YEAR, 1982);
        cal.set(Calendar.MONTH, Calendar.NOVEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 2);
        Date startDate = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, 5);
        Date endDate = cal.getTime();

        when(eventData.getDateValue("startDate")).thenReturn(startDate);
        when(eventData.getDateValue("endDate")).thenReturn(endDate);
        when(eventData.getIntValue("allDay")).thenReturn(1);

        when(eventRecurrentData.getDateValue("firstInstance")).thenReturn(startDate);

        cal.set(Calendar.YEAR, 1987);
        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 7);
        Date lastInstanceDate = cal.getTime();

        when(eventRecurrentData.getDateValue("lastInstance")).thenReturn(lastInstanceDate);

        //
        // now the actual test
        //

        List<DateTime> expectedStartDates = new ArrayList<>();
        List<DateTime> expectedEndDates = new ArrayList<>();
        cal.set(1987, Calendar.MARCH, 31, 0, 0, 0);
        for (int i = 1; i <= 5; i++) {
            expectedStartDates.add(new DateTime(cal.getTime().getTime()));
            if (i < 5) {
                cal.set(Calendar.MONTH, Calendar.APRIL);
                cal.set(Calendar.DAY_OF_MONTH, 3 + 7 * (i - 1));
            } else {
                cal.set(Calendar.MONTH, Calendar.MAY);
                cal.set(Calendar.DAY_OF_MONTH, 1);
            }
            expectedEndDates.add(new DateTime(cal.getTime().getTime()));
            cal.set(Calendar.DAY_OF_MONTH, 7 * i);
        }

        List<EventInstance> eventInstances = mocker.getComponentUnderTest().generate(eventDoc, dateFrom, dateTo);

        Assert.assertNotNull("should return some values", eventInstances);

        for (int i = 0; i < 5; i++) {
            Assert.assertThat("expected five events", eventInstances.size(), Matchers.greaterThan(i));
            EventInstance event = eventInstances.get(i);
            DateTime expectedStartDate = expectedStartDates.get(i);
            DateTime expectedEndDate = expectedEndDates.get(i);
            Assert.assertEquals("should start on tuesday", expectedStartDate, event.getStartDate());
            Assert.assertEquals("should end on friday", expectedEndDate, event.getEndDate());
        }

    }

}
