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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.moccacalendar.EventInstance;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MonthlyEventGenerator}.
 *
 * @version $Id$
 */
@ComponentTest
class MonthlyEventGeneratorTest
{
    @InjectMockComponents
    private MonthlyEventGenerator generator;

    @Test
    @Disabled("Pending the month-end decision: clamp to the last day of shorter months, or skip them.")
    void firstInstanceKeepsTheDayOfMonthWhenJumpingOverFebruary()
    {
        DocumentReference eventClass = new DocumentReference("a", "b", "c");
        DocumentReference recurrencyClass = new DocumentReference("d", "e", "f");
        XWikiDocument eventDoc = mock(XWikiDocument.class);
        BaseObject eventData = mock(BaseObject.class);
        when(eventDoc.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME)).thenReturn(eventClass);
        when(eventDoc.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME))
            .thenReturn(recurrencyClass);
        when(eventDoc.getXObject(eventClass)).thenReturn(eventData);
        when(eventDoc.getXObject(recurrencyClass)).thenReturn(mock(BaseObject.class));

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2000, Calendar.JANUARY, 31);
        when(eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME)).thenReturn(cal.getTime());
        cal.set(Calendar.HOUR_OF_DAY, 1);
        when(eventData.getDateValue(EventConstants.PROPERTY_ENDDATE_NAME)).thenReturn(cal.getTime());

        // The instance of March 31 ends exactly at dateFrom. Incrementing month by month would have clamped the day
        // to the 28th since February 2001.
        cal.set(2026, Calendar.MARCH, 31, 1, 0);
        Date dateFrom = cal.getTime();
        cal.set(Calendar.HOUR_OF_DAY, 12);
        List<EventInstance> instances = this.generator.generate(eventDoc, dateFrom, cal.getTime());

        cal.set(Calendar.HOUR_OF_DAY, 0);
        assertEquals(1, instances.size());
        assertEquals(new DateTime(cal.getTimeInMillis()), instances.get(0).getStartDate());
    }
}
