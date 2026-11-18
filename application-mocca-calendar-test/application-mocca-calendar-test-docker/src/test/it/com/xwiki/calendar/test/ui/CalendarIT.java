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
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import com.xwiki.calendar.test.po.CalendarAdministrationSectionPage;
import com.xwiki.calendar.test.po.CalendarElement;
import com.xwiki.calendar.test.po.CalendarPage;
import com.xwiki.calendar.test.po.EventDates;
import com.xwiki.calendar.test.po.EventEditModal;
import com.xwiki.calendar.test.po.EventViewModal;
import com.xwiki.calendar.test.po.MoccaCalendarHomePage;
import com.xwiki.calendar.test.po.SpaceCleaner;

import static com.xwiki.calendar.test.po.CalendarElement.View.MONTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI Tests for the Calendar Application: home page, calendar creation and administration.
 * 
 * @since 2.11
 */
@UITest
class CalendarIT
{
    private static final String TEAM_CALENDAR = "Team Calendar";

    private DocumentReference teamCalendarReference;

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        // The browser remembers the calendar view: start from the default one, whatever the previous tests did.
        CalendarElement.forgetSelectedView();
    }

    @Test
    @Order(1)
    void calendarApplicationPanelEntry()
    {
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("Calendar");
        assertEquals("MoccaCalendar", vp.getMetaDataValue("space"));
        assertEquals("WebHome", vp.getMetaDataValue("page"));

        MoccaCalendarHomePage homePage = new MoccaCalendarHomePage();
        assertEquals("Calendar", homePage.getDocumentTitle());
        assertTrue(homePage.canCreateCalendar());
        // The default calendar is listed in the overview of all calendars.
        assertTrue(homePage.hasCalendar("Other Events"));

        // The calendar of all events offers all the views, the month being the default one.
        CalendarElement calendar = homePage.getCalendar();
        assertTrue(calendar.canCreateEvents());
        assertEquals(MONTH, calendar.getCurrentView());
        for (CalendarElement.View view : CalendarElement.View.values()) {
            assertEquals(view, calendar.switchToView(view).getCurrentView());
        }
        assertEquals("Upcoming Events", calendar.getTitle());
        assertEquals("Past Events", calendar.clickPrevious().getTitle());
        calendar.switchToView(MONTH);
    }

    @Test
    @Order(2)
    void createCalendar(TestUtils setup, TestReference testReference)
    {
        SpaceCleaner.deleteSpace(setup, testReference.getLastSpaceReference().getParent());
        this.teamCalendarReference =
            new DocumentReference("WebHome", new SpaceReference(TEAM_CALENDAR, testReference.getLastSpaceReference()));
        CalendarPage calendarPage = MoccaCalendarHomePage.gotoPage()
            .createCalendar(TEAM_CALENDAR, testReference.getLastSpaceReference()).setShowAuthor(true)
            .clickSaveAndView();

        assertEquals(TEAM_CALENDAR, calendarPage.getDocumentTitle());
        assertEquals("Yes", calendarPage.getPropertyValue("Display the event author"));
        assertFalse(calendarPage.canAddCalendarObject());
        CalendarElement calendar = calendarPage.getCalendar();
        assertTrue(calendar.canCreateEvents());
        assertTrue(calendar.getEventTitles().isEmpty());

        // The new calendar is listed on the home page.
        assertTrue(MoccaCalendarHomePage.gotoPage().hasCalendar(TEAM_CALENDAR));
    }

    @Test
    @Order(3)
    void createEventFromHomePage()
    {
        // The calendar of the home page shows all the calendars: the calendar of a new event can be chosen, and is
        // the default calendar, "Other Events", unless another one is chosen.
        String title = "Kick-off";
        EventEditModal eventModal = MoccaCalendarHomePage.gotoPage().getCalendar().showToday(MONTH).clickCreateEvent();
        assertTrue(eventModal.canSelectCalendar());
        assertEquals("Other Events", eventModal.getSelectedCalendar());
        assertThat(eventModal.getCalendarOptions(), hasItems("Other Events", TEAM_CALENDAR));
        eventModal.selectCalendar(this.teamCalendarReference).setTitle(title).setStartDate(EventDates.at(0, "17:00"))
            .setEndDate(EventDates.at(0, "18:00")).save();

        assertEquals(List.of(title), CalendarPage.gotoToday(this.teamCalendarReference, MONTH).getEventTitles());
        EventViewModal viewModal = CalendarPage.gotoToday(this.teamCalendarReference, MONTH).clickEvent(title);
        assertEquals(TEAM_CALENDAR, viewModal.getFieldValue("Calendar"));
        viewModal.cancel();
    }

    @Test
    @Order(4)
    void administration()
    {
        CalendarAdministrationSectionPage administration = CalendarAdministrationSectionPage.gotoPage();
        // The Meeting Application is not installed, so its event source can only be installed.
        assertTrue(administration.getAvailableSources().isEmpty());
        assertEquals(List.of("Meeting Application (Pro)"), administration.getInstallableSources());
        assertTrue(administration.hasInstallLink("Meeting Application (Pro)"));
    }
}
