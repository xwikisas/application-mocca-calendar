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

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.calendar.test.po.CalendarEditPage;
import com.xwiki.calendar.test.po.CalendarElement;
import com.xwiki.calendar.test.po.CalendarPage;
import com.xwiki.calendar.test.po.DeleteEventModal;
import com.xwiki.calendar.test.po.EventDates;
import com.xwiki.calendar.test.po.EventEditModal;
import com.xwiki.calendar.test.po.EventEditModal.Frequency;
import com.xwiki.calendar.test.po.EventPage;
import com.xwiki.calendar.test.po.EventViewModal;
import com.xwiki.calendar.test.po.MoccaCalendarHomePage;
import com.xwiki.calendar.test.po.SpaceCleaner;

import static com.xwiki.calendar.test.po.CalendarElement.View.DAY;
import static com.xwiki.calendar.test.po.CalendarElement.View.MONTH;
import static com.xwiki.calendar.test.po.CalendarElement.View.WEEK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the calendar events.
 *
 * @version $Id$
 */
@UITest
class CalendarEventIT
{
    private static final String EVENT_CALENDAR = "Event Calendar";

    private static final String COLORED_CALENDAR = "Colored Calendar";

    private static final String TIME_CALENDAR = "Time Calendar";

    private static final String DRAG_CALENDAR = "Drag Calendar";

    private static final String START_DATE = "Start date";

    private static final String END_DATE = "End date";

    private static final String EVENT_TITLE = "Team meeting";

    private static final String UPDATED_EVENT_TITLE = "Team meeting (updated)";

    private static final String ALL_DAY_EVENT_TITLE = "Company holiday";

    private static final String DESCRIPTION = "Weekly sync of the team";

    private static final String UPDATED_DESCRIPTION = "Weekly sync of the team, in the second meeting room";

    private static final String SPECIAL_TITLE = "Q&A <b>\"R&D\"</b> 'review' 1/2. 50% #1 ?x=y café ü 日本 €";

    private static final String SPECIAL_DESCRIPTION =
        "<i>Tom & Jerry</i> <script>alert('x')</script> \"quoted\" 'single' 100% café ü 日本 €";

    private static final String CALENDAR_BACKGROUND = "#1e7b34";

    private static final String CALENDAR_TEXT = "#ffe066";

    private static final String NEW_CALENDAR_BACKGROUND = "#c0392b";

    private static final String DEFAULT_COLORS_EVENT = "Planning";

    private static final String CUSTOM_COLORS_EVENT = "Demo";

    private static final String CUSTOM_BACKGROUND = "#8e44ad";

    private static final String CUSTOM_TEXT = "#000000";

    private static final String NEW_CUSTOM_BACKGROUND = "#2980b9";

    private static final String NO_AUTHOR_EVENT = "Anonymous feedback";

    private static final String AUTHOR = "superadmin";

    private static final String BACKGROUND_COLOR = "Background color";

    private static final String CALENDAR_DEFAULT = "Calendar Default";

    private static final String AUTHOR_FIELD = "Author";

    private static final String MIDNIGHT_EVENT = "Night shift handover";

    private static final String LATE_EVENT = "Late deployment";

    private static final String NOON_EVENT = "Lunch";

    private static final String OVERNIGHT_EVENT = "Overnight maintenance";

    private static final List<CalendarElement.View> TIME_GRID_VIEWS =
        List.of(WEEK, DAY);

    private static final String WORKSHOP = "Workshop";

    private static final String SYNC = "Team sync";

    private static final String DAILY_CHECK = "Daily check";

    private DocumentReference eventCalendarReference;

    private DocumentReference coloredCalendarReference;

    private DocumentReference timeCalendarReference;

    private DocumentReference dragCalendarReference;

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        CalendarElement.forgetSelectedView();
    }

    @Test
    @Order(1)
    void createAndViewEvent(TestUtils setup, TestReference testReference)
    {
        SpaceCleaner.deleteSpace(setup, testReference.getLastSpaceReference().getParent());
        SpaceReference parent = new SpaceReference(testReference.getLastSpaceReference().getParent());
        this.eventCalendarReference = new DocumentReference("WebHome", new SpaceReference(EVENT_CALENDAR, parent));
        CalendarPage calendarPage = CalendarPage.createCalendar(this.eventCalendarReference, EVENT_CALENDAR);
        CalendarElement calendar = calendarPage.getCalendar().switchToView(MONTH);

        EventEditModal eventModal = calendar.clickCreateEvent();
        assertEquals("Create new event", eventModal.getDialogTitle());
        // The calendar macro of a calendar page shows only the events of that calendar, so the calendar of the new
        // event cannot be changed.
        assertFalse(eventModal.canSelectCalendar());
        assertFalse(eventModal.isRecurrenceDisplayed());
        // The calendar does not display the event authors, so neither does a new event by default.
        assertFalse(eventModal.isShowAuthor());
        // A new event can be made recurrent.
        assertTrue(eventModal.canSetRecurrent());
        eventModal.setTitle(EVENT_TITLE).setStartDate(EventDates.at(0, "10:00")).setEndDate(EventDates.at(0, "11:00"))
            .setDescription(DESCRIPTION).setShowAuthor(true).save();

        assertEquals(List.of(EVENT_TITLE), calendar.getEventTitles());
        assertEquals(AUTHOR, calendar.getEventAuthor(EVENT_TITLE));

        EventViewModal viewModal = calendar.clickEvent(EVENT_TITLE);
        assertEquals("Event", viewModal.getDialogTitle());
        assertEquals(EVENT_CALENDAR, viewModal.getFieldValue("Calendar"));
        assertEquals(EVENT_TITLE, viewModal.getFieldValue("Title"));
        assertEquals(EventDates.at(0, "10:00"), viewModal.getFieldValue(START_DATE));
        assertEquals(EventDates.at(0, "11:00"), viewModal.getFieldValue(END_DATE));
        assertEquals(DESCRIPTION, viewModal.getFieldValue("Description"));
        assertEquals(AUTHOR, viewModal.getFieldValue(AUTHOR_FIELD));
        assertTrue(viewModal.canEdit());
        assertTrue(viewModal.canDelete());
        assertTrue(viewModal.canView());

        // The event has its own page.
        EventPage eventPage = viewModal.clickView();
        assertEquals(EVENT_TITLE, eventPage.getDocumentTitle());
        assertEquals(EventDates.at(0, "10:00"), eventPage.getFieldValue(START_DATE));
        assertEquals(DESCRIPTION, eventPage.getFieldValue("Description"));
        assertEquals("Back to calendar «" + EVENT_CALENDAR + "»", eventPage.getBackToCalendarText());
        calendarPage = eventPage.clickBackToCalendar();
        assertEquals(EVENT_CALENDAR, calendarPage.getDocumentTitle());
        assertThat(calendarPage.getCalendar().getEventTitles(), hasItem(EVENT_TITLE));
    }

    @Test
    @Order(2)
    void eventValidation()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.eventCalendarReference, MONTH);
        EventEditModal eventModal = calendar.clickCreateEvent();

        assertThat(eventModal.saveWithErrors(), hasItem("Title field is required."));

        eventModal.setTitle("Invalid event").setStartDate(EventDates.at(0, "12:00"))
            .setEndDate(EventDates.at(0, "11:00"));
        assertThat(eventModal.saveWithErrors(), hasItem("End date must be after start date."));

        // Nothing is saved when the dialog is canceled: reload the calendar to get the events from the server.
        eventModal.cancel();
        assertEquals(List.of(EVENT_TITLE), CalendarPage.gotoToday(this.eventCalendarReference, MONTH).getEventTitles());
    }

    @Test
    @Order(3)
    void editEvent()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.eventCalendarReference, MONTH);
        EventEditModal eventModal = calendar.clickEvent(EVENT_TITLE).clickEdit();
        assertEquals(EVENT_TITLE, eventModal.getTitle());
        assertEquals(EventDates.at(0, "10:00"), eventModal.getStartDate());
        assertEquals(EventDates.at(0, "11:00"), eventModal.getEndDate());
        assertEquals(DESCRIPTION, eventModal.getDescription());
        // An existing event cannot be made recurrent, so it has no "this instance only / all instances" switch.
        assertFalse(eventModal.canSetRecurrent());
        assertFalse(eventModal.hasInstanceEditSwitch());

        eventModal.setTitle(UPDATED_EVENT_TITLE).setEndDate(EventDates.at(0, "11:30"))
            .setDescription(UPDATED_DESCRIPTION).save();

        assertEquals(List.of(UPDATED_EVENT_TITLE), calendar.getEventTitles());
        EventViewModal viewModal = calendar.clickEvent(UPDATED_EVENT_TITLE);
        assertEquals(EventDates.at(0, "10:00"), viewModal.getFieldValue(START_DATE));
        assertEquals(EventDates.at(0, "11:30"), viewModal.getFieldValue(END_DATE));
        assertEquals(UPDATED_DESCRIPTION, viewModal.getFieldValue("Description"));
        // The event page shows the new description too.
        EventPage eventPage = viewModal.clickView();
        assertEquals(UPDATED_DESCRIPTION, eventPage.getFieldValue("Description"));

        // The description can be removed.
        calendar = eventPage.clickBackToCalendar().getCalendar().showToday(MONTH);
        calendar.clickEvent(UPDATED_EVENT_TITLE).clickEdit().setDescription("").save();
        viewModal = calendar.clickEvent(UPDATED_EVENT_TITLE);
        assertEquals("", viewModal.getFieldValue("Description"));
        viewModal.cancel();
    }

    @Test
    @Order(4)
    void allDayEvent()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.eventCalendarReference, MONTH);
        calendar.clickCreateEvent().setTitle(ALL_DAY_EVENT_TITLE).setAllDay(true).setStartDate(EventDates.day(1))
            .setEndDate(EventDates.day(1)).save();

        assertThat(calendar.getEventTitles(), hasItem(ALL_DAY_EVENT_TITLE));
        EventViewModal viewModal = calendar.clickEvent(ALL_DAY_EVENT_TITLE);
        assertEquals("Yes", viewModal.getFieldValue("All day"));
        // The time is not displayed for all day events.
        assertEquals(EventDates.day(1), viewModal.getFieldValue(START_DATE));
        viewModal.cancel();
    }

    @Test
    @Order(5)
    void calendarViews()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.eventCalendarReference, MONTH);
        assertThat(calendar.getEventTitles(), hasItem(UPDATED_EVENT_TITLE));

        calendar.switchToView(WEEK);
        assertThat(calendar.getEventTitles(), hasItem(UPDATED_EVENT_TITLE));

        calendar.switchToView(DAY);
        assertEquals(List.of(UPDATED_EVENT_TITLE), calendar.getEventTitles());
        // The all day event is displayed on the next day.
        assertEquals(List.of(ALL_DAY_EVENT_TITLE), calendar.clickNext().getEventTitles());

        // The agenda view lists the events from the current day: the reference day of the tests, or the next day if
        // the tests run past midnight. Only the event of the next day is upcoming in both cases.
        calendar.switchToView(CalendarElement.View.AGENDA);
        assertEquals("Upcoming Events", calendar.getTitle());
        List<String> upcomingEvents = calendar.getEventTitles();
        // The events are listed in chronological order.
        assertEquals(ALL_DAY_EVENT_TITLE, upcomingEvents.get(upcomingEvents.size() - 1));
        calendar.clickPrevious();
        assertEquals("Past Events", calendar.getTitle());
        assertThat(calendar.getEventTitles(), not(hasItem(ALL_DAY_EVENT_TITLE)));
    }

    @Test
    @Order(6)
    void deleteEvent()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.eventCalendarReference, MONTH);

        DeleteEventModal deleteModal = calendar.clickEvent(UPDATED_EVENT_TITLE).clickDelete();
        assertThat(deleteModal.getMessage(), containsString("«" + UPDATED_EVENT_TITLE + "»"));
        assertFalse(deleteModal.canDeleteAllInstances());
        // Canceling the deletion goes back to the event.
        EventViewModal viewModal = deleteModal.clickNo();
        assertEquals(UPDATED_EVENT_TITLE, viewModal.getFieldValue("Title"));
        assertThat(calendar.getEventTitles(), hasItem(UPDATED_EVENT_TITLE));

        viewModal.clickDelete().clickYes();
        assertThat(calendar.getEventTitles(), not(hasItem(UPDATED_EVENT_TITLE)));
        assertThat(calendar.getEventTitles(), hasItem(ALL_DAY_EVENT_TITLE));

        calendar.clickEvent(ALL_DAY_EVENT_TITLE).clickDelete().clickYes();
        assertTrue(calendar.getEventTitles().isEmpty());
        calendar.switchToView(CalendarElement.View.AGENDA);
        assertEquals("No upcoming events found.", calendar.getNoEventsMessage());
        assertEquals("No past events found.", calendar.clickPrevious().getNoEventsMessage());
    }

    @Test
    @Order(7)
    void specialCharacters()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.eventCalendarReference, MONTH);
        // The event is tomorrow, so that the agenda view lists it even if the tests run past midnight.
        calendar.clickCreateEvent().setTitle(SPECIAL_TITLE).setStartDate(EventDates.at(1, "14:00"))
            .setEndDate(EventDates.at(1, "15:00")).setDescription(SPECIAL_DESCRIPTION).save();

        // The title and the description are displayed as they were typed, without being interpreted.
        assertEquals(List.of(SPECIAL_TITLE), calendar.getEventTitles());
        EventViewModal viewModal = calendar.clickEvent(SPECIAL_TITLE);
        assertEquals(SPECIAL_TITLE, viewModal.getFieldValue("Title"));
        assertEquals(SPECIAL_DESCRIPTION, viewModal.getFieldValue("Description"));

        // The edit dialog is pre-filled with the unaltered values.
        EventEditModal eventModal = viewModal.clickEdit();
        assertEquals(SPECIAL_TITLE, eventModal.getTitle());
        assertEquals(SPECIAL_DESCRIPTION, eventModal.getDescription());
        eventModal.cancel();

        calendar.switchToView(CalendarElement.View.AGENDA);
        assertEquals(List.of(SPECIAL_TITLE), calendar.getEventTitles());
        calendar.switchToView(MONTH);

        EventPage eventPage = calendar.clickEvent(SPECIAL_TITLE).clickView();
        assertEquals(SPECIAL_TITLE, eventPage.getDocumentTitle());
        assertEquals(SPECIAL_TITLE, eventPage.getFieldValue("Title"));
        assertEquals(SPECIAL_DESCRIPTION, eventPage.getFieldValue("Description"));
        assertEquals("Back to calendar «" + EVENT_CALENDAR + "»", eventPage.getBackToCalendarText());

        calendar = eventPage.clickBackToCalendar().getCalendar().showToday(MONTH);
        DeleteEventModal deleteModal = calendar.clickEvent(SPECIAL_TITLE).clickDelete();
        assertThat(deleteModal.getMessage(), containsString("«" + SPECIAL_TITLE + "»"));
        deleteModal.clickYes();
        assertTrue(calendar.getEventTitles().isEmpty());
    }

    @Test
    @Order(8)
    void multiDayAllDayEvent()
    {
        String title = "Conference";
        CalendarElement calendar = CalendarPage.gotoToday(this.eventCalendarReference, MONTH);
        calendar.clickCreateEvent().setTitle(title).setAllDay(true).setStartDate(EventDates.day(1))
            .setEndDate(EventDates.day(3)).save();

        // The end date is the last day of the event.
        EventViewModal viewModal = calendar.clickEvent(title);
        assertEquals(EventDates.day(1), viewModal.getFieldValue(START_DATE));
        assertEquals(EventDates.day(3), viewModal.getFieldValue(END_DATE));
        viewModal.cancel();

        // The event is displayed on each of its days, and only on them.
        calendar.showToday(DAY);
        assertThat(calendar.getEventTitles(), not(hasItem(title)));
        for (int day = 1; day <= 3; day++) {
            assertEquals(List.of(title), calendar.clickNext().getEventTitles(), "Day " + day);
        }
        assertThat(calendar.clickNext().getEventTitles(), not(hasItem(title)));

        // Clean up.
        calendar = CalendarPage.gotoToday(this.eventCalendarReference, MONTH);
        calendar.clickEvent(title).clickDelete().clickYes();
        assertTrue(calendar.getEventTitles().isEmpty());
    }

    @Test
    @Order(9)
    void sameTitle()
    {
        String title = "Retrospective";
        CalendarElement calendar = CalendarPage.gotoToday(this.eventCalendarReference, MONTH);
        calendar.clickCreateEvent().setTitle(title).setStartDate(EventDates.at(0, "09:00"))
            .setEndDate(EventDates.at(0, "10:00")).setDescription("First").save();
        calendar.clickCreateEvent().setTitle(title).setStartDate(EventDates.at(0, "15:00"))
            .setEndDate(EventDates.at(0, "16:00")).setDescription("Second").save();

        // Both events are saved, each in its own page.
        assertEquals(2, calendar.getEventCount(title));
        EventPage firstEvent = CalendarPage.gotoToday(this.eventCalendarReference, MONTH).clickEvent(title).clickView();
        assertEquals("First", firstEvent.getFieldValue("Description"));
        String firstSpace = firstEvent.getMetaDataValue("space");
        // The events of a day are displayed by start time.
        EventPage secondEvent =
            CalendarPage.gotoToday(this.eventCalendarReference, MONTH).clickEvent(title, 1).clickView();
        assertEquals("Second", secondEvent.getFieldValue("Description"));
        String secondSpace = secondEvent.getMetaDataValue("space");
        assertNotEquals(firstSpace, secondSpace);
        // The name of the second page gets a suffix.
        assertThat(secondSpace, startsWith(firstSpace + "_"));
        assertEquals(title, secondEvent.getDocumentTitle());

        // Clean up.
        calendar = CalendarPage.gotoToday(this.eventCalendarReference, MONTH);
        calendar.clickEvent(title).clickDelete().clickYes();
        calendar.clickEvent(title).clickDelete().clickYes();
        assertTrue(calendar.getEventTitles().isEmpty());
    }

    @Test
    @Order(10)
    void createFromSelection()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.eventCalendarReference, MONTH);
        // Clicking an empty day opens the creation dialog for an all day event on that day.
        EventEditModal eventModal = calendar.selectDay(EventDates.date(2));
        assertTrue(eventModal.isAllDay());
        assertThat(eventModal.getStartDate(), startsWith(EventDates.day(2)));
        eventModal.cancel();

        // Selecting hours in the week view opens the creation dialog for these hours, up to the end of the last
        // selected slot.
        String title = "Selected hours";
        calendar.showToday(WEEK);
        eventModal = calendar.selectTimeRange(EventDates.date(0), "14:00", "15:30");
        assertFalse(eventModal.isAllDay());
        assertEquals(EventDates.at(0, "14:00"), eventModal.getStartDate());
        assertEquals(EventDates.at(0, "16:00"), eventModal.getEndDate());
        eventModal.setTitle(title).save();
        assertEquals(List.of(title), calendar.getEventTitles());
        assertEquals("14:00 - 16:00", calendar.getEventTime(title));

        // Clean up.
        calendar.clickEvent(title).clickDelete().clickYes();
        assertTrue(calendar.getEventTitles().isEmpty());
    }

    @Test
    @Order(11)
    void calendarColors(TestReference testReference)
    {
        SpaceReference parent = new SpaceReference(testReference.getLastSpaceReference().getParent());
        MoccaCalendarHomePage.gotoPage().createCalendar(COLORED_CALENDAR, parent)
            .setBackgroundColor(CALENDAR_BACKGROUND).setTextColor(CALENDAR_TEXT).setShowAuthor(true).clickSaveAndView();
        this.coloredCalendarReference = new DocumentReference("WebHome", new SpaceReference(COLORED_CALENDAR, parent));

        CalendarElement calendar = CalendarPage.gotoToday(this.coloredCalendarReference, MONTH);
        EventEditModal eventModal = calendar.clickCreateEvent();
        // The calendar displays the event authors, so a new event displays its author by default.
        assertTrue(eventModal.isShowAuthor());
        eventModal.setTitle(DEFAULT_COLORS_EVENT).setStartDate(EventDates.at(0, "09:00"))
            .setEndDate(EventDates.at(0, "10:00")).save();

        // An event without colors of its own uses the colors of its calendar.
        assertEquals(CALENDAR_BACKGROUND, calendar.getEventBackgroundColor(DEFAULT_COLORS_EVENT));
        assertEquals(CALENDAR_TEXT, calendar.getEventTextColor(DEFAULT_COLORS_EVENT));
        EventViewModal viewModal = calendar.clickEvent(DEFAULT_COLORS_EVENT);
        assertThat(viewModal.getFieldValue(BACKGROUND_COLOR), containsString(CALENDAR_DEFAULT));
        assertThat(viewModal.getFieldValue("Text color"), containsString(CALENDAR_DEFAULT));
        viewModal.cancel();
    }

    @Test
    @Order(12)
    void eventColors()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.coloredCalendarReference, MONTH);
        calendar.clickCreateEvent().setTitle(CUSTOM_COLORS_EVENT).setStartDate(EventDates.at(0, "14:00"))
            .setEndDate(EventDates.at(0, "15:00")).setBackgroundColor(CUSTOM_BACKGROUND).setTextColor(CUSTOM_TEXT)
            .save();

        // The colors of the event replace the colors of its calendar.
        assertEquals(CUSTOM_BACKGROUND, calendar.getEventBackgroundColor(CUSTOM_COLORS_EVENT));
        assertEquals(CUSTOM_TEXT, calendar.getEventTextColor(CUSTOM_COLORS_EVENT));
        EventViewModal viewModal = calendar.clickEvent(CUSTOM_COLORS_EVENT);
        assertThat(viewModal.getFieldValue(BACKGROUND_COLOR), not(containsString(CALENDAR_DEFAULT)));
        assertThat(viewModal.getFieldValue("Text color"), not(containsString(CALENDAR_DEFAULT)));

        // The colors of an event can be modified.
        viewModal.clickEdit().setBackgroundColor(NEW_CUSTOM_BACKGROUND).save();
        assertEquals(NEW_CUSTOM_BACKGROUND, calendar.getEventBackgroundColor(CUSTOM_COLORS_EVENT));
        assertEquals(CUSTOM_TEXT, calendar.getEventTextColor(CUSTOM_COLORS_EVENT));

        // Changing the colors of the calendar changes only the events without colors of their own.
        CalendarEditPage.gotoPage(this.coloredCalendarReference).setBackgroundColor(NEW_CALENDAR_BACKGROUND)
            .clickSaveAndView();
        calendar = CalendarPage.gotoToday(this.coloredCalendarReference, MONTH);
        assertEquals(NEW_CALENDAR_BACKGROUND, calendar.getEventBackgroundColor(DEFAULT_COLORS_EVENT));
        assertEquals(CALENDAR_TEXT, calendar.getEventTextColor(DEFAULT_COLORS_EVENT));
        assertEquals(NEW_CUSTOM_BACKGROUND, calendar.getEventBackgroundColor(CUSTOM_COLORS_EVENT));

        // The colors are the same in the other views.
        for (CalendarElement.View view : TIME_GRID_VIEWS) {
            calendar.showToday(view);
            assertEquals(NEW_CALENDAR_BACKGROUND, calendar.getEventBackgroundColor(DEFAULT_COLORS_EVENT), view.name());
            assertEquals(NEW_CUSTOM_BACKGROUND, calendar.getEventBackgroundColor(CUSTOM_COLORS_EVENT), view.name());
            assertEquals(CUSTOM_TEXT, calendar.getEventTextColor(CUSTOM_COLORS_EVENT), view.name());
        }
    }

    @Test
    @Order(13)
    void displayAuthor()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.coloredCalendarReference, MONTH);
        // Both events were created with the default value of the calendar.
        assertEquals(AUTHOR, calendar.getEventAuthor(DEFAULT_COLORS_EVENT));
        assertEquals(AUTHOR, calendar.getEventAuthor(CUSTOM_COLORS_EVENT));

        // The author of an event can be hidden even if the calendar displays the authors by default. The event is
        // tomorrow, so that the agenda view lists it even if the tests run past midnight.
        calendar.clickCreateEvent().setTitle(NO_AUTHOR_EVENT).setStartDate(EventDates.at(1, "16:00"))
            .setEndDate(EventDates.at(1, "17:00")).setShowAuthor(false).save();
        assertNull(calendar.getEventAuthor(NO_AUTHOR_EVENT));
        EventViewModal viewModal = calendar.clickEvent(NO_AUTHOR_EVENT);
        assertEquals("No", viewModal.getFieldValue("Display the event author"));
        assertFalse(viewModal.hasField(AUTHOR_FIELD));
        viewModal.cancel();

        // The agenda view hides the author too.
        calendar.switchToView(CalendarElement.View.AGENDA);
        assertNull(calendar.getEventAuthor(NO_AUTHOR_EVENT));
        calendar.switchToView(MONTH);

        // The author can be hidden after the event is created.
        EventEditModal eventModal = calendar.clickEvent(DEFAULT_COLORS_EVENT).clickEdit();
        assertTrue(eventModal.isShowAuthor());
        eventModal.setShowAuthor(false).save();
        assertNull(calendar.getEventAuthor(DEFAULT_COLORS_EVENT));
        viewModal = calendar.clickEvent(DEFAULT_COLORS_EVENT);
        assertFalse(viewModal.hasField(AUTHOR_FIELD));
        viewModal.cancel();

        // And displayed again.
        calendar.clickEvent(NO_AUTHOR_EVENT).clickEdit().setShowAuthor(true).save();
        assertEquals(AUTHOR, calendar.getEventAuthor(NO_AUTHOR_EVENT));
        viewModal = calendar.clickEvent(NO_AUTHOR_EVENT);
        assertEquals("Yes", viewModal.getFieldValue("Display the event author"));
        assertEquals(AUTHOR, viewModal.getFieldValue(AUTHOR_FIELD));
        viewModal.cancel();

        // The author is displayed in the agenda view too.
        calendar.switchToView(CalendarElement.View.AGENDA);
        assertEquals(AUTHOR, calendar.getEventAuthor(NO_AUTHOR_EVENT));
        calendar.switchToView(MONTH);
    }

    @Test
    @Order(14)
    void defaultTimeRange(TestReference testReference)
    {
        // The calendar macro of the next test shows the events of the calendars from the space of the page: give the
        // calendar a space of its own, without the calendars of the other tests.
        SpaceReference parent = testReference.getLastSpaceReference();
        this.timeCalendarReference = new DocumentReference("WebHome", new SpaceReference(TIME_CALENDAR, parent));
        CalendarPage.createCalendar(this.timeCalendarReference, TIME_CALENDAR);
        // The first and the last minutes of the day.
        CalendarPage.createEvent(this.timeCalendarReference, MIDNIGHT_EVENT, EventDates.at(0, "00:00"),
            EventDates.at(0, "00:30"));
        CalendarPage.createEvent(this.timeCalendarReference, LATE_EVENT, EventDates.at(0, "23:00"),
            EventDates.at(0, "23:59"));
        CalendarPage.createEvent(this.timeCalendarReference, NOON_EVENT, EventDates.at(0, "12:00"),
            EventDates.at(0, "13:00"));

        CalendarElement calendar = CalendarPage.gotoToday(this.timeCalendarReference, MONTH);

        // The month view shows the start time of the events.
        assertThat(calendar.getEventTitles(), containsInAnyOrder(MIDNIGHT_EVENT, LATE_EVENT, NOON_EVENT));
        assertEquals("00:00", calendar.getEventTime(MIDNIGHT_EVENT));
        assertEquals("23:00", calendar.getEventTime(LATE_EVENT));

        EventViewModal viewModal = calendar.clickEvent(MIDNIGHT_EVENT);
        assertEquals(EventDates.at(0, "00:00"), viewModal.getFieldValue(START_DATE));
        assertEquals(EventDates.at(0, "00:30"), viewModal.getFieldValue(END_DATE));
        viewModal.cancel();
        viewModal = calendar.clickEvent(LATE_EVENT);
        assertEquals(EventDates.at(0, "23:00"), viewModal.getFieldValue(START_DATE));
        assertEquals(EventDates.at(0, "23:59"), viewModal.getFieldValue(END_DATE));
        viewModal.cancel();

        // By default, the week and day views display the whole day, from 00:00 to 24:00.
        for (CalendarElement.View view : TIME_GRID_VIEWS) {
            calendar.showToday(view);
            List<String> timeSlots = calendar.getTimeSlots();
            assertEquals("00:00", timeSlots.get(0), view.name());
            assertEquals("23:30", timeSlots.get(timeSlots.size() - 1), view.name());
            assertThat(view.name(), calendar.getEventTitles(),
                containsInAnyOrder(MIDNIGHT_EVENT, LATE_EVENT, NOON_EVENT));
            assertEquals("00:00 - 00:30", calendar.getEventTime(MIDNIGHT_EVENT), view.name());
            assertEquals("23:00 - 23:59", calendar.getEventTime(LATE_EVENT), view.name());
        }
    }

    @Test
    @Order(15)
    void customTimeRange()
    {
        // The whole day, set explicitly.
        CalendarElement calendar = gotoMacroCalendar("minTime=\"00:00\" maxTime=\"24:00\"");
        for (CalendarElement.View view : TIME_GRID_VIEWS) {
            calendar.showToday(view);
            List<String> timeSlots = calendar.getTimeSlots();
            assertEquals("00:00", timeSlots.get(0), view.name());
            assertEquals("23:30", timeSlots.get(timeSlots.size() - 1), view.name());
            assertThat(view.name(), calendar.getEventTitles(),
                containsInAnyOrder(MIDNIGHT_EVENT, LATE_EVENT, NOON_EVENT));
        }

        // Working hours, using the hour only (backward compatible format). The events outside of the displayed hours
        // are not visible in the week and day views.
        calendar = gotoMacroCalendar("minTime=\"8\" maxTime=\"18\"");
        for (CalendarElement.View view : TIME_GRID_VIEWS) {
            calendar.showToday(view);
            List<String> timeSlots = calendar.getTimeSlots();
            assertEquals("08:00", timeSlots.get(0), view.name());
            assertEquals("17:30", timeSlots.get(timeSlots.size() - 1), view.name());
            assertEquals(List.of(NOON_EVENT), calendar.getEventTitles(), view.name());
        }
        // The month view is not limited to these hours.
        calendar.showToday(MONTH);
        assertThat(calendar.getEventTitles(), containsInAnyOrder(MIDNIGHT_EVENT, LATE_EVENT, NOON_EVENT));
    }

    @Test
    @Order(16)
    void overnightEvent()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.timeCalendarReference, MONTH);
        calendar.createEvent(OVERNIGHT_EVENT, EventDates.at(0, "22:00"), EventDates.at(1, "02:00"));

        EventViewModal viewModal = calendar.clickEvent(OVERNIGHT_EVENT);
        assertEquals(EventDates.at(0, "22:00"), viewModal.getFieldValue(START_DATE));
        assertEquals(EventDates.at(1, "02:00"), viewModal.getFieldValue(END_DATE));
        viewModal.cancel();

        // The day view displays the event on both days.
        calendar.showToday(DAY);
        assertThat(calendar.getEventTitles(), hasItem(OVERNIGHT_EVENT));
        assertEquals(List.of(OVERNIGHT_EVENT), calendar.clickNext().getEventTitles());
    }

    @Test
    @Order(17)
    void moveStartTimeKeepsDuration()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.timeCalendarReference, MONTH);
        EventEditModal eventModal = calendar.clickEvent(NOON_EVENT).clickEdit();

        // Moving the start of the event moves its end too, keeping the duration of one hour.
        eventModal.setStartDate(EventDates.at(0, "14:00"));
        assertEquals(EventDates.at(0, "15:00"), eventModal.getEndDate());

        // Even when the event ends on the next day.
        eventModal.setStartDate(EventDates.at(0, "23:30"));
        assertEquals(EventDates.at(1, "00:30"), eventModal.getEndDate());
        eventModal.save();

        EventViewModal viewModal = calendar.clickEvent(NOON_EVENT);
        assertEquals(EventDates.at(0, "23:30"), viewModal.getFieldValue(START_DATE));
        assertEquals(EventDates.at(1, "00:30"), viewModal.getFieldValue(END_DATE));
        viewModal.cancel();

        // The day view displays the event on both days.
        calendar.showToday(DAY);
        assertThat(calendar.getEventTitles(), hasItem(NOON_EVENT));
        assertThat(calendar.clickNext().getEventTitles(), hasItem(NOON_EVENT));
    }

    @Test
    @Order(18)
    void sameStartAndEnd()
    {
        String title = "Reminder";
        CalendarElement calendar = CalendarPage.gotoToday(this.timeCalendarReference, MONTH);
        // The end date may be equal to the start date.
        calendar.createEvent(title, EventDates.at(0, "07:15"), EventDates.at(0, "07:15"));

        EventViewModal viewModal = calendar.clickEvent(title);
        assertEquals(EventDates.at(0, "07:15"), viewModal.getFieldValue(START_DATE));
        assertEquals(EventDates.at(0, "07:15"), viewModal.getFieldValue(END_DATE));
        viewModal.cancel();

        calendar.showToday(DAY);
        assertThat(calendar.getEventTitles(), hasItem(title));
        // It is not displayed the day after.
        assertThat(calendar.clickNext().getEventTitles(), not(hasItem(title)));
    }

    @Test
    @Order(19)
    void moveToAnotherDay(TestReference testReference)
    {
        SpaceReference parent = new SpaceReference(testReference.getLastSpaceReference().getParent());
        this.dragCalendarReference = new DocumentReference("WebHome", new SpaceReference(DRAG_CALENDAR, parent));
        CalendarPage.createCalendar(this.dragCalendarReference, DRAG_CALENDAR);
        CalendarPage.createEvent(this.dragCalendarReference, WORKSHOP, EventDates.at(0, "10:00"),
            EventDates.at(0, "11:00"));

        CalendarElement calendar = CalendarPage.gotoToday(this.dragCalendarReference, MONTH);
        assertTrue(calendar.canMoveEvent(WORKSHOP));

        // The event keeps its hours on the new day. The month view always displays the 2 next days.
        calendar.dragEventToDay(WORKSHOP, EventDates.date(2));
        assertDragEventDates(WORKSHOP, MONTH, EventDates.at(2, "10:00"), EventDates.at(2, "11:00"));
    }

    @Test
    @Order(20)
    void moveToAnotherTime()
    {
        CalendarPage.createEvent(this.dragCalendarReference, SYNC, EventDates.at(0, "10:00"),
            EventDates.at(0, "11:00"));
        CalendarElement calendar = CalendarPage.gotoToday(this.dragCalendarReference, WEEK);

        // The event keeps its duration.
        calendar.dragEventToTime(SYNC, "13:00");
        assertEquals("13:00 - 14:00", calendar.getEventTime(SYNC));
        assertDragEventDates(SYNC, DAY, EventDates.at(0, "13:00"), EventDates.at(0, "14:00"));
    }

    @Test
    @Order(21)
    void resize()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.dragCalendarReference, WEEK);

        // Only the end of the event changes.
        calendar.resizeEventTo(SYNC, "15:00");
        assertEquals("13:00 - 15:00", calendar.getEventTime(SYNC));
        assertDragEventDates(SYNC, DAY, EventDates.at(0, "13:00"), EventDates.at(0, "15:00"));
    }

    @Test
    @Order(22)
    void recurrentEventCannotBeMoved()
    {
        CalendarElement calendar = CalendarPage.gotoToday(this.dragCalendarReference, MONTH);
        calendar.clickCreateEvent().setTitle(DAILY_CHECK).setStartDate(EventDates.at(0, "16:00"))
            .setEndDate(EventDates.at(0, "16:30")).setRecurrent(true).setFrequency(Frequency.DAILY)
            .setLastInstance(EventDates.at(2, "16:00")).save();

        // An instance is dragged to a day without instance.
        assertThat(calendar.dragEventToDayRefused(DAILY_CHECK, EventDates.date(4)),
            containsString("Recurrent events cannot be moved."));

        // Nothing is saved. In the month view, the instance of today is not necessarily the first displayed one (the
        // events are displayed one row at a time, and today has other events), so check it in the day view.
        assertEquals(3, CalendarPage.gotoToday(this.dragCalendarReference, MONTH).getEventCount(DAILY_CHECK));
        assertDragEventDates(DAILY_CHECK, DAY, EventDates.at(0, "16:00"), EventDates.at(0, "16:30"));
    }

    /**
     * Checks the dates of an event of the drag calendar, after reloading the calendar, i.e. as saved.
     */
    private void assertDragEventDates(String title, CalendarElement.View view, String expectedStart,
        String expectedEnd)
    {
        EventViewModal viewModal = CalendarPage.gotoToday(this.dragCalendarReference, view).clickEvent(title);
        assertEquals(expectedStart, viewModal.getFieldValue(START_DATE), title);
        assertEquals(expectedEnd, viewModal.getFieldValue(END_DATE), title);
        viewModal.cancel();
    }

    /**
     * Displays the events of the test calendar in a wiki page using the calendar macro with the given parameters.
     */
    private CalendarElement gotoMacroCalendar(String macroParameters)
    {
        // The "space" filter of a page displays the events of the calendars from the space of that page.
        DocumentReference pageReference =
            new DocumentReference("WebHome",
                new SpaceReference(this.timeCalendarReference.getLastSpaceReference().getParent()));
        return CalendarPage.create(pageReference, TIME_CALENDAR, "{{moccacalendar filter=\"space\" " + macroParameters
            + "/}}").getCalendar();
    }
}
