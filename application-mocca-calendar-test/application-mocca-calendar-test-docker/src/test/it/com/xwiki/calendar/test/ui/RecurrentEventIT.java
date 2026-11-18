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
package com.xwiki.calendar.test.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.calendar.test.po.CalendarElement;
import com.xwiki.calendar.test.po.CalendarPage;
import com.xwiki.calendar.test.po.DeleteEventModal;
import com.xwiki.calendar.test.po.EventDates;
import com.xwiki.calendar.test.po.EventEditModal;
import com.xwiki.calendar.test.po.EventEditModal.Frequency;
import com.xwiki.calendar.test.po.EventViewModal;
import com.xwiki.calendar.test.po.SpaceCleaner;

import static com.xwiki.calendar.test.po.CalendarElement.View.MONTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for recurrent events: creation, modification of a single instance and of all the instances, deletion of a
 * single instance and of all the instances.
 *
 * @version $Id$
 */
@UITest
class RecurrentEventIT
{
    private static final String CALENDAR_TITLE = "Recurrent Calendar";

    private static final String EVENT_TITLE = "Daily standup";

    private static final String MODIFIED_INSTANCE_TITLE = "Daily standup (moved)";

    private static final String RENAMED_EVENT_TITLE = "Daily sync";

    private static final String WEEKLY_EVENT_TITLE = "Weekly review";

    private DocumentReference calendarReference;

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        CalendarElement.forgetSelectedView();
    }

    @Test
    @Order(1)
    void createRecurrentEvent(TestUtils setup, TestReference testReference)
    {
        SpaceCleaner.deleteSpace(setup, testReference.getLastSpaceReference().getParent());
        SpaceReference parent = new SpaceReference(testReference.getLastSpaceReference().getParent());
        this.calendarReference = new DocumentReference("WebHome", new SpaceReference(CALENDAR_TITLE, parent));
        CalendarPage.createCalendar(this.calendarReference, CALENDAR_TITLE);

        CalendarElement calendar = CalendarPage.gotoToday(this.calendarReference, MONTH);
        EventEditModal eventModal = calendar.clickCreateEvent().setTitle(EVENT_TITLE)
            .setStartDate(EventDates.at(0, "09:00")).setEndDate(EventDates.at(0, "09:30")).setRecurrent(true);

        // Once the start date is known, a monthly recurrence on the same week day is offered too.
        assertThat(eventModal.getFrequencyOptions(),
            hasItems("daily", "workdays", "weekly", "biweekly", "monthly", "quarterly", "yearly", "custom week"));
        assertTrue(eventModal.getFrequencyOptions().stream().anyMatch(option -> option.startsWith("Every ")));
        // The week days are chosen only for a custom weekly recurrence.
        assertFalse(eventModal.isRecurrenceDaysDisplayed());
        eventModal.setFrequency(Frequency.CUSTOM_WEEKLY);
        assertTrue(eventModal.isRecurrenceDaysDisplayed());
        eventModal.setFrequency(Frequency.DAILY);
        assertFalse(eventModal.isRecurrenceDaysDisplayed());

        eventModal.setLastInstance(EventDates.at(2, "09:00")).save();

        // One instance per day, from today to the last instance.
        assertEquals(3, calendar.getEventCount(EVENT_TITLE));
        EventViewModal viewModal = calendar.clickEvent(EVENT_TITLE);
        assertEquals("Yes", viewModal.getFieldValue("Recurrent"));
        viewModal.cancel();
    }

    @Test
    @Order(2)
    void editSingleInstance()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.calendarReference, MONTH);
        // The first displayed instance is the one of today.
        EventEditModal eventModal = calendar.clickEvent(EVENT_TITLE).clickEdit();
        assertTrue(eventModal.hasInstanceEditSwitch());
        eventModal.editThisInstanceOnly();
        // The fields of the instance are pre-filled with the values of the instance.
        assertEquals(EVENT_TITLE, eventModal.getTitle());
        assertEquals(EventDates.at(0, "09:00"), eventModal.getStartDate());
        assertEquals(EventDates.at(0, "09:30"), eventModal.getEndDate());
        // Move the instance later in the day.
        eventModal.setTitle(MODIFIED_INSTANCE_TITLE).setStartDate(EventDates.at(0, "11:00"))
            .setEndDate(EventDates.at(0, "11:30")).save();

        assertEquals(2, calendar.getEventCount(EVENT_TITLE));
        assertEquals(1, calendar.getEventCount(MODIFIED_INSTANCE_TITLE));
        EventViewModal viewModal = calendar.clickEvent(MODIFIED_INSTANCE_TITLE);
        assertEquals(EventDates.at(0, "11:00"), viewModal.getFieldValue("Start date"));
        assertEquals(EventDates.at(0, "11:30"), viewModal.getFieldValue("End date"));
        viewModal.cancel();

        // The other instances are not modified.
        viewModal = calendar.clickEvent(EVENT_TITLE);
        assertEquals(EventDates.at(1, "09:00"), viewModal.getFieldValue("Start date"));
        assertEquals("", viewModal.getFieldValue("Description"));
        viewModal.cancel();
    }

    @Test
    @Order(3)
    void editAllInstances()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.calendarReference, MONTH);
        EventEditModal eventModal = calendar.clickEvent(EVENT_TITLE).clickEdit().editAllInstances();
        // The fields of the whole recurrent event are displayed, starting with its first instance, even though the
        // clicked instance is the one of tomorrow.
        assertEquals(EVENT_TITLE, eventModal.getTitle());
        assertEquals(EventDates.at(0, "09:00"), eventModal.getStartDate());
        eventModal.setTitle(RENAMED_EVENT_TITLE).setDescription("Shared progress").save();

        // All the instances are modified, except the one that was modified on its own.
        assertEquals(0, calendar.getEventCount(EVENT_TITLE));
        assertEquals(2, calendar.getEventCount(RENAMED_EVENT_TITLE));
        assertEquals(1, calendar.getEventCount(MODIFIED_INSTANCE_TITLE));
        EventViewModal viewModal = calendar.clickEvent(RENAMED_EVENT_TITLE);
        assertEquals("Shared progress", viewModal.getFieldValue("Description"));
        assertEquals("Yes", viewModal.getFieldValue("Recurrent"));
        viewModal.cancel();
        viewModal = calendar.clickEvent(MODIFIED_INSTANCE_TITLE);
        assertEquals(EventDates.at(0, "11:00"), viewModal.getFieldValue("Start date"));
        viewModal.cancel();
    }

    @Test
    @Order(4)
    @Disabled("https://github.com/xwikisas/application-mocca-calendar/issues/263")
    void editModifiedInstance()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.calendarReference, MONTH);
        // Editing the modified instance again starts from its modified values, and does not create a new instance.
        EventEditModal eventModal = calendar.clickEvent(MODIFIED_INSTANCE_TITLE).clickEdit().editThisInstanceOnly();
        assertEquals(MODIFIED_INSTANCE_TITLE, eventModal.getTitle());
        assertEquals(EventDates.at(0, "11:00"), eventModal.getStartDate());
        eventModal.setDescription("Moved because of the release").save();

        assertEquals(2, calendar.getEventCount(RENAMED_EVENT_TITLE));
        assertEquals(1, calendar.getEventCount(MODIFIED_INSTANCE_TITLE));
        EventViewModal viewModal = calendar.clickEvent(MODIFIED_INSTANCE_TITLE);
        assertEquals("Moved because of the release", viewModal.getFieldValue("Description"));
        assertEquals(EventDates.at(0, "11:00"), viewModal.getFieldValue("Start date"));
        viewModal.cancel();
    }

    @Test
    @Order(5)
    void deleteSingleInstance()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.calendarReference, MONTH);
        DeleteEventModal deleteModal = calendar.clickEvent(RENAMED_EVENT_TITLE).clickDelete();
        assertThat(deleteModal.getMessage(), containsString("«" + RENAMED_EVENT_TITLE + "»"));
        // Recurrent events can be deleted all at once.
        assertTrue(deleteModal.canDeleteAllInstances());
        deleteModal.clickYes();

        assertEquals(1, calendar.getEventCount(RENAMED_EVENT_TITLE));
        assertEquals(1, calendar.getEventCount(MODIFIED_INSTANCE_TITLE));
        // The deleted instance is the clicked one, i.e. the first displayed one, of tomorrow.
        EventViewModal viewModal = calendar.clickEvent(RENAMED_EVENT_TITLE);
        assertEquals(EventDates.at(2, "09:00"), viewModal.getFieldValue("Start date"));
        viewModal.cancel();
    }

    @Test
    @Order(6)
    @Disabled("https://github.com/xwikisas/application-mocca-calendar/issues/264")
    void deleteModifiedInstance()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.calendarReference, MONTH);
        calendar.clickEvent(MODIFIED_INSTANCE_TITLE).clickDelete().clickYes();

        assertEquals(0, calendar.getEventCount(MODIFIED_INSTANCE_TITLE));
        assertEquals(1, calendar.getEventCount(RENAMED_EVENT_TITLE));
        // The deletion is saved.
        assertEquals(0, CalendarPage.gotoToday(this.calendarReference, MONTH).getEventCount(MODIFIED_INSTANCE_TITLE));
    }

    @Test
    @Order(7)
    void deleteAllInstances()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.calendarReference, MONTH);
        calendar.clickEvent(RENAMED_EVENT_TITLE).clickDelete().clickDeleteAllInstances();

        assertTrue(calendar.getEventTitles().isEmpty());
    }

    @Test
    @Order(8)
    void weeklyEvent()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.calendarReference, MONTH);
        calendar.clickCreateEvent().setTitle(WEEKLY_EVENT_TITLE).setStartDate(EventDates.at(0, "16:00"))
            .setEndDate(EventDates.at(0, "17:00")).setRecurrent(true).setFrequency(Frequency.WEEKLY)
                // The last instance is included.
                .setLastInstance(EventDates.at(14, "16:00")).save();

        // Go through the next 3 weeks, one day at a time: there is one instance every 7 days, on the same time.
        calendar.showToday(CalendarElement.View.DAY);
        for (int day = 0; day <= 21; day++) {
            long expectedCount = (day % 7 == 0 && day <= 14) ? 1 : 0;
            assertEquals(expectedCount, calendar.getEventCount(WEEKLY_EVENT_TITLE), "Instances on day " + day);
            if (expectedCount == 1) {
                assertEquals("16:00 - 17:00", calendar.getEventTime(WEEKLY_EVENT_TITLE));
            }
            calendar.clickNext();
        }

        // Clean up.
        calendar.showToday(MONTH);
        calendar.clickEvent(WEEKLY_EVENT_TITLE).clickDelete().clickDeleteAllInstances();
        assertTrue(calendar.getEventTitles().isEmpty());
    }

    @Test
    @Order(9)
    void workdaysEvent()
    {
        String title = "Workdays check";
        LocalDate today = EventDates.date(0);
        LocalDate lastInstance = today.plusDays(13);
        createRecurrentEvent(title, 0, Frequency.WORKDAYS, lastInstance).save();

        // The first instance is on the start day, the next ones on the following week days, without the week-ends.
        List<LocalDate> expectedDays = new ArrayList<>(List.of(today));
        for (LocalDate day = today.plusDays(1); !day.isAfter(lastInstance); day = day.plusDays(1)) {
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                expectedDays.add(day);
            }
        }
        assertInstancesAndDelete(title, expectedDays);
    }

    @Test
    @Order(10)
    void customWeeklyEvent()
    {
        String title = "Gym";
        // The event starts on a Monday and takes place on Mondays and Wednesdays.
        LocalDate start = EventDates.date(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        int startDay = daysFromToday(start);
        // The last instance is set on the Thursday after the last Wednesday because of a bug: with a custom weekly
        // frequency, the instance starting exactly at the date of the last instance is not generated (it is with the
        // other frequencies).
        LocalDate lastWednesday = start.plusDays(16);
        createRecurrentEvent(title, startDay, Frequency.CUSTOM_WEEKLY, lastWednesday.plusDays(1))
            .setRecurrenceDays(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
            .save();

        assertInstancesAndDelete(title, List.of(start, start.plusDays(2), start.plusDays(7), start.plusDays(9),
            start.plusDays(14), lastWednesday));
    }

    @Test
    @Order(11)
    void biweeklyEvent()
    {
        String title = "Sprint review";
        LocalDate today = EventDates.date(0);
        createRecurrentEvent(title, 0, Frequency.BIWEEKLY, today.plusDays(28)).save();

        assertInstancesAndDelete(title, List.of(today, today.plusDays(14), today.plusDays(28)));
    }

    @Test
    @Order(12)
    void monthlyEvent()
    {
        String title = "Monthly report";
        // The same day of the following months. The event starts on the 15th, which all the months have (see
        // monthlyEventOnTheLastDayOfTheMonth for the shorter months).
        LocalDate start = EventDates.date(0).plusMonths(1).withDayOfMonth(15);
        createRecurrentEvent(title, daysFromToday(start), Frequency.MONTHLY, start.plusMonths(2)).save();

        assertInstancesAndDelete(title, List.of(start, start.plusMonths(1), start.plusMonths(2)));
    }

    @Test
    @Order(13)
    @Disabled("https://github.com/xwikisas/application-mocca-calendar/issues/265")
    void monthlySpecificEvent()
    {
        String title = "Town hall";
        // The same week day of the month, e.g. the 2nd Thursday, or the last one when the month has fewer.
        LocalDate today = EventDates.date(0);
        int occurrence = (today.getDayOfMonth() - 1) / 7 + 1;
        List<LocalDate> expectedDays = new ArrayList<>(List.of(today));
        for (int month = 1; month <= 2; month++) {
            LocalDate firstDay = today.plusMonths(month).withDayOfMonth(1);
            LocalDate day = firstDay.with(TemporalAdjusters.dayOfWeekInMonth(occurrence, today.getDayOfWeek()));
            if (day.getMonth() != firstDay.getMonth()) {
                day = firstDay.with(TemporalAdjusters.lastInMonth(today.getDayOfWeek()));
            }
            expectedDays.add(day);
        }
        createRecurrentEvent(title, 0, Frequency.MONTHLY_SPECIFIC, expectedDays.get(2)).save();

        assertInstancesAndDelete(title, expectedDays);
    }

    @Test
    @Order(14)
    void quarterlyEvent()
    {
        String title = "Quarterly planning";
        // The event starts on the 15th, which all the months have.
        LocalDate start = EventDates.date(0).plusMonths(1).withDayOfMonth(15);
        createRecurrentEvent(title, daysFromToday(start), Frequency.QUARTERLY, start.plusMonths(6)).save();

        assertInstancesAndDelete(title, List.of(start, start.plusMonths(3), start.plusMonths(6)));
    }

    @Test
    @Order(15)
    void yearlyEvent()
    {
        String title = "Anniversary";
        LocalDate today = EventDates.date(0);
        createRecurrentEvent(title, 0, Frequency.YEARLY, today.plusYears(1)).save();

        assertInstancesAndDelete(title, List.of(today, today.plusYears(1)));
    }

    @Test
    @Order(16)
    @Disabled("https://github.com/xwikisas/application-mocca-calendar/issues/266")
    void monthlyEventOnTheLastDayOfTheMonth()
    {
        String title = "Month-end closing";
        // The next 31st followed by a shorter month.
        LocalDate start = EventDates.date(1);
        while (start.getDayOfMonth() != 31 || start.plusMonths(1).getDayOfMonth() == 31) {
            start = start.plusDays(1);
        }
        createRecurrentEvent(title, daysFromToday(start), Frequency.MONTHLY, start.plusMonths(2)).save();

        // The last day of the shorter month, then the 31st again (or the 30th).
        assertInstancesAndDelete(title, List.of(start, start.plusMonths(1), start.plusMonths(2)));
    }

    @Test
    @Order(17)
    void firstInstanceEvent()
    {
        String title = "Onboarding";
        // The instances before the first instance are not displayed, even though the event starts today.
        createRecurrentEvent(title, 0, Frequency.DAILY, EventDates.date(4))
            .setFirstInstance(EventDates.at(2, "08:00"))
            .save();

        assertInstancesAndDelete(title, List.of(EventDates.date(2), EventDates.date(3), EventDates.date(4)));
    }

    @Test
    @Order(18)
    @Disabled("https://github.com/xwikisas/application-mocca-calendar/issues/267")
    void moveSingleInstanceKeepsDuration()
    {
        String title = "Coffee break";
        createRecurrentEvent(title, 0, Frequency.DAILY, EventDates.date(2)).save();

        // The instance of tomorrow, edited for the first time: moving its start moves its end too.
        EventEditModal eventModal = CalendarPage.gotoToday(this.calendarReference, MONTH).clickEvent(title, 1)
            .clickEdit().editThisInstanceOnly();
        assertEquals(EventDates.at(1, "08:00"), eventModal.getStartDate());
        eventModal.setStartDate(EventDates.at(1, "11:00"));
        assertEquals(EventDates.at(1, "11:30"), eventModal.getEndDate());
        eventModal.cancel();

        deleteEvent(title);
    }

    /**
     * Creates a recurrent event.
     *
     * @param startDay the start day of the event, as a number of days from today
     * @param lastInstance the day of the last instance
     */
    private EventEditModal createRecurrentEvent(String title, int startDay, Frequency frequency, LocalDate lastInstance)
    {
        int lastInstanceDay = daysFromToday(lastInstance);
        return CalendarPage.gotoToday(this.calendarReference, MONTH).clickCreateEvent()
            .setTitle(title)
            .setStartDate(EventDates.at(startDay, "08:00"))
            .setEndDate(EventDates.at(startDay, "08:30"))
            .setRecurrent(true)
            .setFrequency(frequency)
            .setLastInstance(EventDates.at(lastInstanceDay, "08:00"));
    }

    /**
     * Checks the days of the instances of the event, by going through the months from the current one to the one of
     * the last expected instance, then deletes the event.
     */
    private void assertInstancesAndDelete(String title, List<LocalDate> expectedDays)
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.calendarReference, MONTH);
        // The month view also displays a few days of the previous and next months.
        Set<LocalDate> days = new TreeSet<>(calendar.getEventDays(title));
        long months = ChronoUnit.MONTHS.between(YearMonth.from(EventDates.date(0)),
            YearMonth.from(expectedDays.get(expectedDays.size() - 1)));
        for (int month = 0; month < months; month++) {
            days.addAll(calendar.clickNext().getEventDays(title));
        }
        assertEquals(expectedDays, new ArrayList<>(days), title);

        deleteEvent(title);
    }

    /**
     * Deletes all the instances of an event, from the first month displaying one of them.
     */
    private void deleteEvent(String title)
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.calendarReference, MONTH);
        for (int month = 0; month < 12 && calendar.getEventCount(title) == 0; month++) {
            calendar.clickNext();
        }
        calendar.clickEvent(title).clickDelete().clickDeleteAllInstances();
        assertEquals(0, calendar.getEventCount(title));
    }

    private static int daysFromToday(LocalDate day)
    {
        return (int) ChronoUnit.DAYS.between(EventDates.date(0), day);
    }
}
