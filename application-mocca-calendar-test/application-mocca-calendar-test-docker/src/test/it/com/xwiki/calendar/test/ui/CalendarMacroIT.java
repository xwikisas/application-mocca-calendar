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

import com.xwiki.calendar.test.po.CalendarElement;
import com.xwiki.calendar.test.po.CalendarPage;
import com.xwiki.calendar.test.po.EventDates;
import com.xwiki.calendar.test.po.EventEditModal;
import com.xwiki.calendar.test.po.EventPage;
import com.xwiki.calendar.test.po.EventViewModal;
import com.xwiki.calendar.test.po.MoccaCalendarHomePage;
import com.xwiki.calendar.test.po.SpaceCleaner;
import com.xwiki.calendar.test.po.UpcomingEventsElement;

import static com.xwiki.calendar.test.po.CalendarElement.View.AGENDA;
import static com.xwiki.calendar.test.po.CalendarElement.View.MONTH;
import static com.xwiki.calendar.test.po.CalendarElement.View.WEEK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the calendar macros.
 *
 * @version $Id$
 */
@UITest
class CalendarMacroIT
{
    private static final String CALENDAR_A = "Calendar A";

    private static final String CALENDAR_B = "Calendar B";

    private static final String EVENT_A = "Event A";

    private static final String EVENT_B = "Event B";

    private static final String EVENT_C = "Event C";

    private static final String MACRO_CALENDAR = "Macro calendar";

    private static final String MACRO_EVENT = "Macro event";

    private static final String READER_EVENT = "Reader event";

    private static final String SCRIPT_RESULT = "The answer is 42.";

    private static final String CALENDAR_FIELD = "Calendar";

    private DocumentReference calendarAReference;

    private DocumentReference calendarBReference;

    /**
     * A page displaying the events of the calendars A and B, using the "space" filter.
     */
    private DocumentReference spacePageReference;

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        CalendarElement.forgetSelectedView();
    }

    @Test
    @Order(1)
    void eventFilters(TestUtils setup, TestReference testReference)
    {
        SpaceCleaner.deleteSpace(setup, testReference.getLastSpaceReference().getParent());
        SpaceReference space = testReference.getLastSpaceReference();
        this.calendarAReference = new DocumentReference("WebHome", new SpaceReference(CALENDAR_A, space));
        this.calendarBReference = new DocumentReference("WebHome", new SpaceReference(CALENDAR_B, space));

        CalendarPage.createCalendar(this.calendarAReference, CALENDAR_A);
        CalendarPage.createCalendar(this.calendarBReference, CALENDAR_B);
        // The events are tomorrow: the upcoming events macro, tested below, lists only the events that are not
        // finished yet, so events of today would disappear from it during the day.
        CalendarPage.createEvent(this.calendarAReference, EVENT_A, EventDates.at(1, "10:00"),
            EventDates.at(1, "11:00"));
        CalendarPage.createEvent(this.calendarBReference, EVENT_B, EventDates.at(1, "14:00"),
            EventDates.at(1, "15:00"));

        // A calendar page shows only its own events (the "page" filter).
        assertEquals(List.of(EVENT_A), CalendarPage.gotoToday(this.calendarAReference, MONTH).getEventTitles());
        assertEquals(List.of(EVENT_B), CalendarPage.gotoToday(this.calendarBReference, MONTH).getEventTitles());

        // The "space" filter shows the events of all the calendars from the space of the page.
        this.spacePageReference = testReference;
        CalendarPage spacePage =
            CalendarPage.create(testReference, "Space calendar", "{{moccacalendar filter=\"space\"/}}");
        // Only a calendar macro using the "page" filter can be turned into a calendar.
        assertFalse(spacePage.canAddCalendarObject());
        CalendarElement spaceCalendar = spacePage.getCalendar().showToday(MONTH);
        assertThat(spaceCalendar.getEventTitles(), containsInAnyOrder(EVENT_A, EVENT_B));

        // An event created from a calendar showing several calendars can be added to any of them.
        EventEditModal eventModal = spaceCalendar.clickCreateEvent();
        assertTrue(eventModal.canSelectCalendar());
        assertThat(eventModal.getCalendarOptions(), hasItems(CALENDAR_A, CALENDAR_B));
        eventModal.selectCalendar(this.calendarBReference).setTitle(EVENT_C).setStartDate(EventDates.at(1, "16:00"))
            .setEndDate(EventDates.at(1, "17:00")).save();
        assertThat(spaceCalendar.getEventTitles(), containsInAnyOrder(EVENT_A, EVENT_B, EVENT_C));
        assertThat(CalendarPage.gotoToday(this.calendarBReference, MONTH).getEventTitles(),
            containsInAnyOrder(EVENT_B, EVENT_C));

        // The "wiki" filter, used on the home page of the application, shows the events of all the calendars.
        CalendarElement wikiCalendar = MoccaCalendarHomePage.gotoPage().getCalendar().showToday(MONTH);
        assertThat(wikiCalendar.getEventTitles(), hasItems(EVENT_A, EVENT_B, EVENT_C));
    }

    @Test
    @Order(2)
    void addCalendarObject(TestReference testReference)
    {
        CalendarPage calendarPage = CalendarPage.create(testReference, MACRO_CALENDAR, "{{moccacalendar/}}");
        assertTrue(calendarPage.canAddCalendarObject());

        calendarPage = calendarPage.clickAddCalendarObject();
        assertFalse(calendarPage.canAddCalendarObject());
        // The events created from the page now belong to the page, which became a calendar.
        CalendarElement calendar = calendarPage.getCalendar().showToday(MONTH);
        calendar.createEvent(MACRO_EVENT, EventDates.at(1, "18:00"), EventDates.at(1, "19:00"));
        assertEquals(List.of(MACRO_EVENT), calendar.getEventTitles());
        EventViewModal viewModal = calendar.clickEvent(MACRO_EVENT);
        assertEquals(MACRO_CALENDAR, viewModal.getFieldValue("Calendar"));
        viewModal.cancel();
        assertTrue(MoccaCalendarHomePage.gotoPage().hasCalendar(MACRO_CALENDAR));
    }

    @Test
    @Order(3)
    void upcomingEventsMacro(TestReference testReference)
    {
        CalendarPage.create(testReference, "Upcoming events", "{{moccacalendar.upcomingEvents limit=\"50\"/}}");
        UpcomingEventsElement upcomingEvents = UpcomingEventsElement.inContent();
        // The events are listed in chronological order.
        assertThat(upcomingEvents.getEventTitles(), containsInRelativeOrder(EVENT_A, EVENT_B, EVENT_C));
        assertEquals("10:00 - 11:00", upcomingEvents.getSchedule(EVENT_A));

        EventPage eventPage = upcomingEvents.clickEvent(EVENT_A);
        assertEquals(EVENT_A, eventPage.getDocumentTitle());

        // The number of listed events can be limited.
        CalendarPage.create(testReference, "Upcoming events", "{{moccacalendar.upcomingEvents limit=\"1\"/}}");
        assertEquals(1, UpcomingEventsElement.inContent().getEventTitles().size());
    }

    @Test
    @Order(4)
    void upcomingEventsPanel(TestUtils setup, TestReference testReference)
    {
        // Display the panel on the pages of the test space only.
        setup.addObject(new DocumentReference("WebPreferences", testReference.getLastSpaceReference()),
            "XWiki.XWikiPreferences", "rightPanels", "MoccaCalendar.Code.UpcomingEventsPanel", "showRightPanels", "1");

        // Make sure that there are more upcoming events than the panel can list: the previous methods of this class
        // created 4 (events A, B and C, and the event of the macro calendar), and the other test classes may have left
        // some more.
        for (String title : List.of("Event D", "Event E")) {
            CalendarPage.createEvent(this.calendarBReference, title, EventDates.at(1, "20:00"),
                EventDates.at(1, "21:00"));
        }

        CalendarPage.create(testReference, "Upcoming events panel", "Some content.");
        UpcomingEventsElement panel = UpcomingEventsElement.inPanel();
        assertEquals("Upcoming Events", panel.getPanelHeader());
        // The panel lists at most 5 events.
        assertEquals(5, panel.getEventTitles().size());
    }

    @Test
    @Order(5)
    void calendarWithoutEditRight(TestUtils setup)
    {
        String userName = "CalendarReader";
        setup.createUser(userName, userName, null);
        setup.setRightsOnSpace(this.calendarAReference.getLastSpaceReference(), "", "XWiki." + userName, "edit,delete",
            false);
        setup.login(userName, userName);

        try {
            CalendarElement calendarA = CalendarPage.gotoToday(this.calendarAReference, MONTH);
            assertFalse(calendarA.canCreateEvents());
            // The events cannot be moved with the mouse either.
            assertFalse(calendarA.canMoveEvent(EVENT_A));
            EventViewModal viewModal = calendarA.clickEvent(EVENT_A);
            assertFalse(viewModal.canEdit());
            assertFalse(viewModal.canDelete());
            assertTrue(viewModal.canView());
            viewModal.cancel();

            // The other calendars can still be edited.
            CalendarElement calendarB = CalendarPage.gotoToday(this.calendarBReference, MONTH);
            calendarB.createEvent(READER_EVENT, EventDates.at(1, "22:00"), EventDates.at(1, "23:00"));
            assertThat(calendarB.getEventTitles(), hasItem(READER_EVENT));
        } finally {
            setup.loginAsSuperAdmin();
        }
    }

    @Test
    @Order(6)
    void macroParameters(TestUtils setup, TestReference testReference)
    {
        // The default view of the macro applies only if the user did not choose another view.
        CalendarElement.forgetSelectedView();
        String calendarA =
            setup.serializeReference(this.calendarAReference.removeParent(this.calendarAReference.getWikiReference()));
        CalendarPage page = CalendarPage.create(testReference, "Calendar A, by week", String.format(
            "{{moccacalendar calendarDoc=\"%s\" defaultView=\"agendaWeek\" firstDay=\"1\"/}}", calendarA));
        CalendarElement calendar = page.getCalendar();
        assertEquals(WEEK, calendar.getCurrentView());

        // The weeks start on Monday instead of Sunday.
        calendar.showToday(MONTH);
        assertEquals("Mon", calendar.getWeekDayNames().get(0));

        // The macro displays the events of the given calendar, and creates the new events in it.
        assertEquals(List.of(EVENT_A), calendar.getEventTitles());
        String title = "Delegated event";
        calendar.createEvent(title, EventDates.at(1, "19:00"), EventDates.at(1, "20:00"));
        EventViewModal viewModal = calendar.clickEvent(title);
        assertEquals(CALENDAR_A, viewModal.getFieldValue(CALENDAR_FIELD));
        viewModal.cancel();
        assertThat(CalendarPage.gotoToday(this.calendarAReference, MONTH).getEventTitles(), hasItem(title));

        // Don't leave the remembered view to the next tests.
        CalendarElement.forgetSelectedView();
    }

    @Test
    @Order(7)
    void calendarWithoutViewRight(TestUtils setup, TestReference testReference)
    {
        String userName = "CalendarOutsider";
        setup.createUser(userName, userName, null);
        setup.setRightsOnSpace(this.calendarAReference.getLastSpaceReference(), "",
            "XWiki." + userName + ",XWiki.XWikiGuest", "view", false);
        DocumentReference upcomingEventsPage = new DocumentReference("Upcoming", testReference.getLastSpaceReference());
        CalendarPage.create(upcomingEventsPage, "Upcoming events", "{{moccacalendar.upcomingEvents limit=\"50\"/}}");

        try {
            // A user who cannot view the calendar A, and a guest.
            setup.login(userName, userName);
            assertEventsOfCalendarAAreHidden(upcomingEventsPage);
            setup.forceGuestUser();
            assertEventsOfCalendarAAreHidden(upcomingEventsPage);
        } finally {
            setup.loginAsSuperAdmin();
        }
    }

    @Test
    @Order(8)
    void scriptInDescription(TestUtils setup)
    {
        String userName = "CalendarWriter";
        setup.createUser(userName, userName, null);
        setup.setGlobalRights("", "XWiki." + userName, "script", false);
        String title = "Scripted event";
        String description = "{{velocity}}#set ($answer = 6 * 7)The answer is $answer.{{/velocity}}";

        setup.login(userName, userName);
        try {
            CalendarElement calendar = CalendarPage.gotoToday(this.calendarBReference, MONTH);
            calendar.clickCreateEvent().setTitle(title).setStartDate(EventDates.at(1, "12:00"))
                .setEndDate(EventDates.at(1, "13:00")).setDescription(description).save();

            // The description is displayed, but not executed, since its author is not allowed to run scripts: the
            // script or an error about it is displayed instead of its result.
            EventViewModal viewModal = calendar.clickEvent(title);
            String displayedDescription = viewModal.getFieldValue("Description");
            assertThat(displayedDescription, not(containsString(SCRIPT_RESULT)));
            assertThat(displayedDescription, containsString("velocity"));
            viewModal.cancel();
            calendar.switchToView(AGENDA);
            String agendaText = calendar.getAgendaEventText(title);
            assertThat(agendaText, not(containsString(SCRIPT_RESULT)));
            assertThat(agendaText, containsString("velocity"));
        } finally {
            setup.loginAsSuperAdmin();
        }

        // Once saved by a user allowed to run scripts, the description is executed.
        CalendarElement calendar = CalendarPage.gotoToday(this.calendarBReference, MONTH);
        calendar.clickEvent(title).clickEdit().setDescription(description).save();
        calendar.switchToView(AGENDA);
        assertThat(calendar.getAgendaEventText(title), containsString(SCRIPT_RESULT));
    }

    @Test
    @Order(9)
    void moveEventToAnotherCalendar()
    {
        // The calendar of an event can be changed from a page displaying several calendars.
        EventEditModal eventModal =
            CalendarPage.gotoToday(this.spacePageReference, MONTH).clickEvent(EVENT_C).clickEdit();
        assertTrue(eventModal.canSelectCalendar());
        eventModal.selectCalendar(this.calendarAReference).save();

        assertThat(CalendarPage.gotoToday(this.calendarAReference, MONTH).getEventTitles(), hasItem(EVENT_C));
        assertThat(CalendarPage.gotoToday(this.calendarBReference, MONTH).getEventTitles(), not(hasItem(EVENT_C)));
        // The page of the event is moved under its new calendar.
        EventPage eventPage = CalendarPage.gotoToday(this.calendarAReference, MONTH).clickEvent(EVENT_C).clickView();
        assertEquals("Back to calendar «" + CALENDAR_A + "»", eventPage.getBackToCalendarText());
        assertThat(eventPage.getMetaDataValue("space"), containsString(CALENDAR_A));
    }

    private void assertEventsOfCalendarAAreHidden(DocumentReference upcomingEventsPage)
    {
        List<String> titles = MoccaCalendarHomePage.gotoPage().getCalendar().showToday(MONTH).getEventTitles();
        assertThat(titles, hasItem(EVENT_B));
        assertThat(titles, not(hasItem(EVENT_A)));

        titles = CalendarPage.gotoToday(this.spacePageReference, MONTH).getEventTitles();
        assertThat(titles, hasItem(EVENT_B));
        assertThat(titles, not(hasItem(EVENT_A)));

        CalendarPage.gotoPage(upcomingEventsPage);
        titles = UpcomingEventsElement.inContent().getEventTitles();
        assertThat(titles, hasItem(EVENT_B));
        assertThat(titles, not(hasItem(EVENT_A)));
    }
}
