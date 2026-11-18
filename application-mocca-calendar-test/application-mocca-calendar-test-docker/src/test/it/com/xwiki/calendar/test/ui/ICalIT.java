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

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.calendar.test.po.CalendarEditPage;
import com.xwiki.calendar.test.po.CalendarElement;
import com.xwiki.calendar.test.po.CalendarPage;
import com.xwiki.calendar.test.po.EventDates;
import com.xwiki.calendar.test.po.EventEditModal.Frequency;
import com.xwiki.calendar.test.po.EventViewModal;
import com.xwiki.calendar.test.po.ICalFile;
import com.xwiki.calendar.test.po.MoccaCalendarHomePage;
import com.xwiki.calendar.test.po.SpaceCleaner;

import static com.xwiki.calendar.test.po.CalendarElement.View.MONTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the iCalendar support: the export of a calendar, the subscription to an iCalendar feed (the export of
 * another calendar), and the import of an {@code .ics} file.
 *
 * @version $Id$
 */
@UITest
class ICalIT
{
    private static final String SOURCE_CALENDAR = "HR Calendar";

    private static final String SUBSCRIBED_CALENDAR = "Team Holidays";

    private static final String IMPORT_CALENDAR = "Imported Calendar";

    private static final String CALENDAR_DESCRIPTION = "The holidays of the team, from the HR calendar.";

    private static final String BACKGROUND = "#6f42c1";

    private static final String TEXT = "#fff3cd";

    private static final String MEETING = "Quarterly review";

    private static final String MEETING_DESCRIPTION = "Results of the quarter";

    private static final String HOLIDAY = "Team retreat";

    private static final String IMPORTED_MEETING = "Imported meeting";

    private static final String IMPORTED_HOLIDAY = "Imported holiday";

    private static final String IMPORTED_STANDUP = "Imported standup";

    private static final String GLOBAL_DISPLAY = "Display the events inside the global calendar";

    private static final String ALL_DAY = "All day";

    private static final String TITLE = "Title";

    /**
     * The host name and port through which the XWiki server reaches itself, to fetch the subscribed iCal URLs.
     */
    private String serverHost;

    private int serverPort;

    private DocumentReference sourceCalendarReference;

    private DocumentReference subscribedCalendarReference;

    /**
     * The number of events with the titles of the exported events that the calendar of the home page displays before
     * the tests. The tests clean only their own space, which is named after the running class: when the tests are run
     * again on the same XWiki instance (e.g. with an external servlet engine), the events created by a run under
     * another class name ({@code NestedICalIT} inside {@code AllIT}, or {@code ICalIT} alone) are still there.
     */
    private long otherMeetings;

    private long otherHolidays;

    @BeforeAll
    void beforeAll(TestUtils setup, TestConfiguration testConfiguration)
    {
        setup.loginAsSuperAdmin();
        ServletEngine servletEngine = testConfiguration.getServletEngine();
        this.serverHost = servletEngine.isOutsideDocker() ? "localhost" : servletEngine.getInternalIP();
        this.serverPort = servletEngine.isOutsideDocker() ? servletEngine.getPort() : servletEngine.getInternalPort();
        CalendarElement.forgetSelectedView();
    }

    @Test
    @Order(1)
    void subscribeToExportedCalendar(TestUtils setup, TestReference testReference) throws Exception
    {
        SpaceCleaner.deleteSpace(setup, testReference.getLastSpaceReference().getParent());
        SpaceReference parent = new SpaceReference(testReference.getLastSpaceReference().getParent());
        CalendarElement globalCalendar = MoccaCalendarHomePage.gotoPage().getCalendar().showToday(MONTH);
        this.otherMeetings = globalCalendar.getEventCount(MEETING);
        this.otherHolidays = globalCalendar.getEventCount(HOLIDAY);

        // The calendar to export: one event with hours and one all day event, both today. The event with hours is at
        // noon so that it stays on the same day whatever the time zone it is exported and displayed in.
        this.sourceCalendarReference = new DocumentReference("WebHome", new SpaceReference(SOURCE_CALENDAR, parent));
        CalendarPage.createCalendar(this.sourceCalendarReference, SOURCE_CALENDAR);
        CalendarPage.createEvent(this.sourceCalendarReference, MEETING, EventDates.at(0, "12:00"),
            EventDates.at(0, "13:00"), MEETING_DESCRIPTION);
        // The all day event is created with the dialog, which converts the dates of the all day events.
        CalendarElement sourceCalendar = CalendarPage.gotoToday(this.sourceCalendarReference, MONTH);
        sourceCalendar.clickCreateEvent().setTitle(HOLIDAY).setAllDay(true).setStartDate(EventDates.day(0))
            .setEndDate(EventDates.day(0)).save();

        // The overview of all calendars offers the export of each calendar.
        String exportURL =
            MoccaCalendarHomePage.gotoPage().getICalExportURL(this.sourceCalendarReference, SOURCE_CALENDAR);

        CalendarPage calendarPage = MoccaCalendarHomePage.gotoPage().subscribeToCalendar(SUBSCRIBED_CALENDAR, parent)
            .setICalURL(ICalFile.withServer(exportURL, this.serverHost, this.serverPort))
            .setBackgroundColor(BACKGROUND)
            .setTextColor(TEXT)
            .setDescription(CALENDAR_DESCRIPTION)
            .clickSaveAndView();
        this.subscribedCalendarReference =
            new DocumentReference("WebHome", new SpaceReference(SUBSCRIBED_CALENDAR, parent));

        assertEquals(SUBSCRIBED_CALENDAR, calendarPage.getDocumentTitle());
        assertEquals(CALENDAR_DESCRIPTION, calendarPage.getPropertyValue("Description"));
        assertEquals("No", calendarPage.getPropertyValue(GLOBAL_DISPLAY));

        // The calendar shows the exported events, with the colors of the subscribed calendar.
        CalendarElement calendar = calendarPage.getCalendar().showToday(MONTH);
        assertThat(calendar.getEventTitles(), containsInAnyOrder(MEETING, HOLIDAY));
        assertEquals(BACKGROUND, calendar.getEventBackgroundColor(MEETING));
        assertEquals(TEXT, calendar.getEventTextColor(MEETING));

        // The subscribed events can be viewed, but not edited, deleted or opened in a page.
        EventViewModal eventModal = calendar.clickEvent(MEETING);
        assertEquals(MEETING, eventModal.getFieldValue(TITLE));
        assertEquals(MEETING_DESCRIPTION, eventModal.getFieldValue("Description"));
        assertThat(eventModal.getFieldValue("Start date"), startsWith(EventDates.day(0)));
        assertEquals("No", eventModal.getFieldValue(ALL_DAY));
        assertFalse(eventModal.canEdit());
        assertFalse(eventModal.canDelete());
        assertFalse(eventModal.canView());
        eventModal.cancel();

        eventModal = calendar.clickEvent(HOLIDAY);
        assertEquals(HOLIDAY, eventModal.getFieldValue(TITLE));
        assertEquals("Yes", eventModal.getFieldValue(ALL_DAY));
        eventModal.cancel();
    }

    @Test
    @Order(2)
    void exportedFile()
    {
        // A weekly event, to check the export of the recurrence.
        String weeklyEvent = "Weekly sync";
        CalendarPage.gotoToday(this.sourceCalendarReference, MONTH).clickCreateEvent()
            .setTitle(weeklyEvent)
            .setStartDate(EventDates.at(0, "15:00"))
            .setEndDate(EventDates.at(0, "15:30"))
            .setRecurrent(true)
            .setFrequency(Frequency.WEEKLY)
            .setLastInstance(EventDates.at(14, "15:00"))
            .save();

        String exportedFile =
            MoccaCalendarHomePage.gotoPage().getICalExport(this.sourceCalendarReference, SOURCE_CALENDAR);
        assertThat(exportedFile, startsWith("BEGIN:VCALENDAR"));
        assertThat(exportedFile, containsString("SUMMARY:" + MEETING));
        assertThat(exportedFile, containsString("DESCRIPTION:" + MEETING_DESCRIPTION));
        // The all day event is exported with dates without hours.
        assertThat(exportedFile, containsString("SUMMARY:" + HOLIDAY));
        assertThat(exportedFile,
            containsString("DTSTART;VALUE=DATE:" + EventDates.date(0).format(DateTimeFormatter.BASIC_ISO_DATE)));
        // The recurrence is exported, up to its last instance.
        assertThat(exportedFile, containsString("SUMMARY:" + weeklyEvent));
        assertThat(exportedFile, containsString(
            "RRULE:FREQ=WEEKLY;UNTIL=" + EventDates.date(14).format(DateTimeFormatter.BASIC_ISO_DATE)));
    }

    @Test
    @Order(3)
    void listedOnHomePage()
    {
        MoccaCalendarHomePage homePage = MoccaCalendarHomePage.gotoPage();
        // The calendar of the home page shows the events of the exported calendar, but not their copies from the
        // subscribed calendar, by default.
        CalendarElement globalCalendar = homePage.getCalendar().showToday(MONTH);
        assertEquals(this.otherMeetings + 1, globalCalendar.getEventCount(MEETING));
        assertEquals(this.otherHolidays + 1, globalCalendar.getEventCount(HOLIDAY));
        assertTrue(homePage.hasSubscribedCalendar(SUBSCRIBED_CALENDAR));
    }

    @Test
    @Order(4)
    void displayInGlobalCalendar()
    {
        CalendarPage calendarPage = CalendarEditPage.gotoPage(this.subscribedCalendarReference)
            .setDisplayInGlobalCalendar(true)
            .clickSaveAndView();
        assertEquals("Yes", calendarPage.getPropertyValue(GLOBAL_DISPLAY));

        // The calendar of the home page shows the subscribed events too, with the colors of the subscribed calendar.
        CalendarElement globalCalendar = MoccaCalendarHomePage.gotoPage().getCalendar().showToday(MONTH);
        assertEquals(this.otherMeetings + 2, globalCalendar.getEventCount(MEETING));
        assertEquals(this.otherHolidays + 2, globalCalendar.getEventCount(HOLIDAY));
        assertThat(globalCalendar.getEventBackgroundColors(MEETING), hasItem(BACKGROUND));
    }

    @Test
    @Order(5)
    void importICalFile(TestUtils setup, TestReference testReference, @TempDir Path directory) throws Exception
    {
        // FIXME: the import checks the file against the maximum upload size of the wiki preferences, and fails when
        //  that size is not set (the default) instead of falling back to the default limit. Set it.
        setup.updateObject("XWiki", "XWikiPreferences", "XWiki.XWikiPreferences", 0, "upload_maxsize", "10485760");

        SpaceReference parent = new SpaceReference(testReference.getLastSpaceReference().getParent());
        CalendarPage importPage = CalendarPage.createCalendar(
            new DocumentReference("WebHome", new SpaceReference(IMPORT_CALENDAR, parent)), IMPORT_CALENDAR);
        CalendarElement calendar = importPage.getCalendar().showToday(MONTH);

        // An event with hours (at noon, to stay on the same day whatever the time zone), an all day event and a daily
        // event with 3 instances.
        // FIXME: The last instance is given as a date (UNTIL) because a recurrence given as a number of instances
        //  (COUNT) is imported with one instance too many (RecurrenceProcessor of macro-fullcalendar).
        LocalDate today = EventDates.date(0);
        File file = new ICalFile()
            .addEvent(IMPORTED_MEETING, MEETING_DESCRIPTION, today.atTime(12, 0), today.atTime(13, 0))
            .addAllDayEvent(IMPORTED_HOLIDAY, today.plusDays(1))
            .addRecurrentEvent(IMPORTED_STANDUP, today.atTime(12, 0), today.atTime(12, 30), "DAILY",
                today.plusDays(2).atTime(12, 0))
            .writeTo(directory, "events.ics");
        calendar.importICalFile(file);

        // The imported events belong to the calendar, and can be edited like the other events.
        assertEquals(1, calendar.getEventCount(IMPORTED_MEETING));
        EventViewModal eventModal = calendar.clickEvent(IMPORTED_MEETING);
        assertEquals(IMPORT_CALENDAR, eventModal.getFieldValue("Calendar"));
        assertEquals(MEETING_DESCRIPTION, eventModal.getFieldValue("Description"));
        assertThat(eventModal.getFieldValue("Start date"), startsWith(EventDates.day(0)));
        assertTrue(eventModal.canEdit());
        eventModal.cancel();
        eventModal = calendar.clickEvent(IMPORTED_HOLIDAY);
        assertEquals("Yes", eventModal.getFieldValue(ALL_DAY));
        assertEquals(EventDates.day(1), eventModal.getFieldValue("Start date"));
        eventModal.cancel();
        // The recurrence is imported too.
        assertEquals(List.of(today, today.plusDays(1), today.plusDays(2)), calendar.getEventDays(IMPORTED_STANDUP));

        // Importing the same file again creates new events, even though events with the same names exist.
        calendar.importICalFile(file);
        assertEquals(2, calendar.getEventCount(IMPORTED_MEETING));
        assertEquals(2, calendar.getEventCount(IMPORTED_HOLIDAY));
    }
}
