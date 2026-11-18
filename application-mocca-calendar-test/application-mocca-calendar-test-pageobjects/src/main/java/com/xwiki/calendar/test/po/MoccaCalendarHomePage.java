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
package com.xwiki.calendar.test.po;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.openqa.selenium.By;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.ui.po.DocumentPicker;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.PageTypePicker;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The home page of the Calendar application.
 *
 * @version $Id$
 * @since 2.22.1
 */
public class MoccaCalendarHomePage extends ViewPage
{
    private static final String CALENDARS_LIVETABLE = "moccacalendar";

    private static final String TITLE_FILTER = "xwiki-livetable-" + CALENDARS_LIVETABLE + "-filter-1";

    private static final By PAGE_TYPE_PICKER = By.cssSelector(".xwiki-select.page-type");

    private static final By CREATE_BUTTON = By.cssSelector("#xwikicontent a.button[href*='MoccaCalendarTemplate']");

    private static final By SUBSCRIBE_BUTTON =
        By.cssSelector("#xwikicontent a.button[href*='MoccaCalendarSubscribeTemplate']");

    private static final String CALENDAR_COLUMN = "Calendar";

    /**
     * Opens the home page of the Calendar application.
     *
     * @return the home page
     */
    public static MoccaCalendarHomePage gotoPage()
    {
        getUtil().gotoPage("MoccaCalendar", "WebHome");
        return new MoccaCalendarHomePage();
    }

    /**
     * @return the calendar showing the events of all the calendars of the wiki
     */
    public CalendarElement getCalendar()
    {
        return new CalendarElement();
    }

    /**
     * @return {@code true} if the current user can create calendars
     */
    public boolean canCreateCalendar()
    {
        return getDriver().hasElementWithoutWaiting(CREATE_BUTTON);
    }

    /**
     * Creates a new calendar, using the "Create new calendar" button.
     *
     * @param title the title of the calendar
     * @param parent the space where the calendar page is created
     * @return the edit form of the new calendar
     */
    public CalendarEditPage createCalendar(String title, SpaceReference parent)
    {
        create(CREATE_BUTTON, "MoccaCalendar.MoccaCalendarTemplateProvider", title, parent);
        return new CalendarEditPage();
    }

    /**
     * Creates a calendar showing the events of an external calendar, using the "Subscribe to a calendar" button.
     *
     * @param title the title of the subscribed calendar
     * @param parent the space where the subscribed calendar page is created
     * @return the edit form of the new subscribed calendar
     */
    public CalendarEditPage subscribeToCalendar(String title, SpaceReference parent)
    {
        create(SUBSCRIBE_BUTTON, "MoccaCalendar.MoccaCalendarSubscribeTemplateProvider", title, parent);
        return new CalendarEditPage();
    }

    /**
     * @param calendarReference the reference of the page of a calendar holding its own events
     * @param title the title of the calendar, used to filter the overview of all calendars
     * @return the URL of the iCalendar export of the calendar, offered by the "iCal" action of the overview of all
     *     calendars
     */
    public String getICalExportURL(DocumentReference calendarReference, String title)
    {
        LiveTableElement calendars = new LiveTableElement(CALENDARS_LIVETABLE);
        calendars.waitUntilReady();
        calendars.filterColumn(TITLE_FILTER, title);
        // Several calendars can have the same title: find the action of the given calendar page, whose full name is
        // passed in the URL of the export. The action is added to the rows once they are displayed.
        String fullName =
            getUtil().serializeReference(calendarReference.removeParent(calendarReference.getWikiReference()));
        By exportLinks = By.cssSelector("table#" + CALENDARS_LIVETABLE + " a.actionical");
        return getDriver().waitUntilCondition(driver -> getDriver().findElementsWithoutWaiting(exportLinks).stream()
            .map(link -> link.getAttribute("href")).filter(url -> fullName.equals(getCalendarParameter(url)))
            .findFirst().orElse(null));
    }

    /**
     * Downloads the iCalendar export of a calendar, from the browser, i.e. with the rights of the current user.
     *
     * @param calendarReference the reference of the page of a calendar holding its own events
     * @param title the title of the calendar
     * @return the content of the iCalendar export of the calendar
     */
    public String getICalExport(DocumentReference calendarReference, String title)
    {
        return (String) getDriver().executeAsyncScript("const done = arguments[arguments.length - 1];"
            + "fetch(arguments[0]).then(response => response.ok ? response.text()"
            + "  : 'Failed to download the export: ' + response.status).then(done, error => done(String(error)));",
            getICalExportURL(calendarReference, title));
    }

    /**
     * @param title the title of a subscribed calendar
     * @return {@code true} if the calendar is listed in the "Subscribed calendars" table
     */
    public boolean hasSubscribedCalendar(String title)
    {
        TableLayoutElement subscribedCalendars = new LiveDataElement("subscribedCalendars").getTableLayout();
        subscribedCalendars.filterColumn(CALENDAR_COLUMN, title);
        return subscribedCalendars.getAllCells(CALENDAR_COLUMN).stream()
            .anyMatch(cell -> title.equals(cell.getText().trim()));
    }

    /**
     * @param title the title of a calendar
     * @return {@code true} if the calendar is listed in the overview of all calendars
     */
    public boolean hasCalendar(String title)
    {
        LiveTableElement calendars = new LiveTableElement(CALENDARS_LIVETABLE);
        calendars.waitUntilReady();
        calendars.filterColumn(TITLE_FILTER, title);
        if (calendars.hasRow(CALENDAR_COLUMN, title)) {
            return true;
        }
        // The livetable filters on the stored title, so a computed title (such as the translated title of the default
        // calendar) never matches the filter. Such a title is stored as a script starting with "$", which comes before
        // any letter in the default ascending order on the title, so it is listed on the first page without filter.
        calendars.filterColumn(TITLE_FILTER, "");
        return calendars.hasRow(CALENDAR_COLUMN, title);
    }

    private void create(By button, String templateProvider, String title, SpaceReference parent)
    {
        getDriver().findElement(button).click();

        // The button passes the template in the URL, so XWiki hides the page type picker and the platform
        // CreatePagePage, which requires that picker, cannot be used.
        DocumentPicker documentPicker = new DocumentPicker(getDriver().findElement(By.className("location-picker")));
        documentPicker.setTitle(title);
        documentPicker.waitForName(title);
        documentPicker.setParent(getUtil().serializeReference(parent.removeParent(parent.getWikiReference())));
        // Depending on the XWiki version, the page type can still be chosen even if the button specifies the template.
        if (getDriver().hasElementWithoutWaiting(PAGE_TYPE_PICKER)) {
            new PageTypePicker().selectTemplateByValue(templateProvider);
        }

        // The form can be submitted by JavaScript after its asynchronous validation, so wait for the reload.
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElement(By.cssSelector("form#create input[type='submit']")).click();
        getDriver().waitUntilPageIsReloaded();
    }

    private static String getCalendarParameter(String url)
    {
        String query = URI.create(url).getRawQuery();
        if (query != null) {
            for (String parameter : query.split("&")) {
                String[] nameAndValue = parameter.split("=", 2);
                if (nameAndValue.length == 2 && "calendar".equals(nameAndValue[0])) {
                    return URLDecoder.decode(nameAndValue[1], StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }
}
