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

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.FileDetector;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.support.Color;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents a calendar displayed by the {@code moccacalendar} macro.
 *
 * @version $Id$
 * @since 2.22.1
 */
public class CalendarElement extends BaseElement
{
    /**
     * The views offered by the calendar toolbar.
     */
    public enum View
    {
        /**
         * The month view.
         */
        MONTH("month"),

        /**
         * The week view.
         */
        WEEK("agendaWeek"),

        /**
         * The day view.
         */
        DAY("agendaDay"),

        /**
         * The agenda view, listing the upcoming or the past events.
         */
        AGENDA("plainList");

        private final String id;

        View(String id)
        {
            this.id = id;
        }

        /**
         * @return the identifier used by FullCalendar for this view
         */
        String getId()
        {
            return this.id;
        }
    }

    private static final String CLASS_ATTRIBUTE = "class";

    private static final String TEXT_CONTENT = "textContent";

    private static final int DRAG_START_DISTANCE = 5;

    private static final Duration DRAG_PAUSE = Duration.ofMillis(200);

    private static final By EVENT_TITLE = By.cssSelector(".fc-title");

    private static final By AGENDA_EVENT_TITLE = By.cssSelector(".fc-timeline-box .moccaevent h5");

    private static final By DAY_CELL = By.cssSelector(".fc-bg td.fc-day[data-date]");

    private static final By ERROR_NOTIFICATION = By.cssSelector(".xnotification-error");

    /**
     * The cookie in which the calendars remember the last view selected by the user, for all the calendars.
     */
    private static final String SELECTED_VIEW_COOKIE = "MoccaCalendar_view";

    private final String calendarId;

    /**
     * Types a color in a color picker field, of the calendar or event forms. The color picker popup is opened only on
     * click, so typing the value keeps it closed; the field accepts typed values.
     *
     * @param input the color picker input
     * @param color the color, e.g. {@code #ff0000}
     */
    static void setColor(WebElement input, String color)
    {
        input.clear();
        input.sendKeys(color);
    }

    /**
     * Makes the calendars forget the last view selected by the user, so that they display their default view again.
     * The browser must be on a page of the wiki, which is the case after logging in.
     */
    public static void forgetSelectedView()
    {
        getUtil().getDriver().manage().deleteCookieNamed(SELECTED_VIEW_COOKIE);
    }

    /**
     * Creates the page object for the first calendar displayed in the page.
     */
    public CalendarElement()
    {
        this(0);
    }

    /**
     * @param index the index of the calendar in the page, starting from 0
     */
    public CalendarElement(int index)
    {
        this.calendarId = "calendar" + index;
        waitUntilReady();
    }

    /**
     * Waits until the calendar is displayed and its events are loaded and rendered.
     *
     * @return this calendar
     */
    CalendarElement waitUntilReady()
    {
        // The agenda view shows a loading image until its events are displayed.
        getDriver().waitUntilJavascriptCondition("return !!document.querySelector('#' + arguments[0] + ' .fc-view') "
            + "&& !!window.jQuery && jQuery.active === 0 && (!window.Ajax || Ajax.activeRequestCount === 0) "
            + "&& !document.querySelector('#' + arguments[0] + ' .fc-timeline-box > img[src*=\"ajax-loader\"]')",
            this.calendarId);
        // FullCalendar renders the fetched events in promise callbacks, which jQuery runs asynchronously (with
        // setTimeout) once the requests are done: let the pending callbacks run before reading the calendar.
        getDriver().executeAsyncScript("const done = arguments[arguments.length - 1];"
            + "setTimeout(() => setTimeout(done, 0), 0);");
        return this;
    }

    /**
     * @return {@code true} if the current user is allowed to create events in this calendar
     */
    public boolean canCreateEvents()
    {
        return getDriver().hasElementWithoutWaiting(By.id(this.calendarId + "-btn"));
    }

    /**
     * Opens the dialog used to create a new event.
     *
     * @return the event creation dialog
     */
    public EventEditModal clickCreateEvent()
    {
        getDriver().findElement(By.id(this.calendarId + "-btn")).click();
        return new EventEditModal(this);
    }

    /**
     * Switches the calendar to the given view.
     *
     * @param view the view to display
     * @return this calendar
     */
    public CalendarElement switchToView(View view)
    {
        WebElement button = getContainer().findElement(By.className("fc-" + view.getId() + "-button"));
        if (!button.getAttribute(CLASS_ATTRIBUTE).contains("fc-state-active")) {
            button.click();
            getDriver().waitUntilCondition(
                driver -> getContainer().findElement(By.className("fc-" + view.getId() + "-view")).isDisplayed());
        }
        return waitUntilReady();
    }

    /**
     * @return the view currently displayed
     */
    public View getCurrentView()
    {
        String viewClass = getContainer().findElement(By.className("fc-view")).getAttribute(CLASS_ATTRIBUTE);
        for (View view : View.values()) {
            if (viewClass.contains("fc-" + view.getId() + "-view")) {
                return view;
            }
        }
        return null;
    }

    /**
     * @return the title shown in the calendar toolbar (e.g. the displayed month or "Upcoming Events")
     */
    public String getTitle()
    {
        return getContainer().findElement(By.cssSelector(".fc-center h2")).getText();
    }

    /**
     * Displays the next period (next month, week, day or the upcoming events).
     *
     * @return this calendar
     */
    public CalendarElement clickNext()
    {
        getContainer().findElement(By.className("fc-next-button")).click();
        return waitUntilReady();
    }

    /**
     * Displays the previous period (previous month, week, day or the past events).
     *
     * @return this calendar
     */
    public CalendarElement clickPrevious()
    {
        getContainer().findElement(By.className("fc-prev-button")).click();
        return waitUntilReady();
    }

    /**
     * Displays the period containing today.
     *
     * @return this calendar
     */
    public CalendarElement clickToday()
    {
        WebElement today = getContainer().findElement(By.className("fc-today-button"));
        if (today.isEnabled()) {
            today.click();
        }
        return waitUntilReady();
    }

    /**
     * Shows the current day (or the week, month containing it) in the given view.
     *
     * @param view the view to show
     * @return this calendar
     */
    public CalendarElement showToday(View view)
    {
        // The agenda view always lists the events from the current time: use its "today" button.
        if (view == View.AGENDA) {
            return switchToView(view).clickToday();
        }
        // The other views show the reference day of the tests, which stays the same even if the tests run past
        // midnight, unlike the "today" button.
        return showDate(EventDates.date(0), view);
    }

    /**
     * Shows the given day (or the week, month containing it) in the given view (not the agenda view).
     *
     * @param day the day to show
     * @param view the view to show
     * @return this calendar
     */
    public CalendarElement showDate(LocalDate day, View view)
    {
        switchToView(view);
        getDriver().executeScript("jQuery('#' + arguments[0]).fullCalendar('gotoDate', arguments[1]);",
            this.calendarId, day.toString());
        return waitUntilReady();
    }

    /**
     * Creates an event with the "Create new event" dialog, and waits for the calendar to display it.
     *
     * @param title the title of the event
     * @param startDate the start date, in the {@code yyyy/MM/dd HH:mm} format, see {@link EventDates}
     * @param endDate the end date, in the same format
     * @return this calendar
     */
    public CalendarElement createEvent(String title, String startDate, String endDate)
    {
        clickCreateEvent().setTitle(title).setStartDate(startDate).setEndDate(endDate).save();
        return this;
    }

    /**
     * Clicks on an empty part of a day, in the month view, which opens the dialog to create an event on that day.
     *
     * @param day the day, displayed in the current view
     * @return the event creation dialog
     */
    public EventEditModal selectDay(LocalDate day)
    {
        getDriver().createActions().moveToElement(getDayCell(day)).click().perform();
        return new EventEditModal(this);
    }

    /**
     * Selects a time range of a day with the mouse, in the week and day views, which opens the dialog to create an
     * event at that time.
     *
     * @param day the day, displayed in the current view
     * @param startTime the start of the first selected slot, in the {@code HH:mm} format, on a 30 minutes slot
     * @param lastSlotTime the start of the last selected slot, in the same format
     * @return the event creation dialog
     */
    public EventEditModal selectTimeRange(LocalDate day, String startTime, String lastSlotTime)
    {
        WebElement firstSlot = scrollToSlot(startTime);
        WebElement dayColumn = getContainer().findElement(By.cssSelector(
            ".fc-time-grid .fc-bg td.fc-day[data-date='" + day + "']"));
        // From the middle of the first slot, in the column of the day, to the middle of the last slot. The mouse is
        // positioned relative to the center of the visible part of an element: use the row of the first slot, which is
        // fully displayed, rather than the column of the day, which is taller than the scrollable area of the hours.
        int offsetFromSlotCenter = getCenterX(dayColumn) - getCenterX(firstSlot);
        int distance = getSlotTop(lastSlotTime) - getSlotTop(startTime);
        drag(getDriver().createActions().moveToElement(firstSlot, offsetFromSlotCenter, 0), distance);
        return new EventEditModal(this);
    }

    /**
     * @return the titles of the events displayed in the current view, one entry per event instance
     */
    public List<String> getEventTitles()
    {
        By locator = getCurrentView() == View.AGENDA ? AGENDA_EVENT_TITLE : EVENT_TITLE;
        return getDriver().findElementsWithoutWaiting(getContainer(), locator).stream()
            .map(element -> element.getAttribute(TEXT_CONTENT).trim()).collect(Collectors.toList());
    }

    /**
     * @param title an event title
     * @return the number of instances of the event with the given title displayed in the current view
     */
    public long getEventCount(String title)
    {
        return getEventTitles().stream().filter(title::equals).count();
    }

    /**
     * @param title the title of an event displayed in the current view (including the agenda view)
     * @return the author displayed with the event, or {@code null} if the author is not displayed
     */
    public String getEventAuthor(String title)
    {
        WebElement event = getCurrentView() == View.AGENDA ? getAgendaEvent(title) : getEvent(title);
        List<WebElement> author = getDriver().findElementsWithoutWaiting(event, By.className("mocca-event-author"));
        return author.isEmpty() ? null : author.get(0).getAttribute(TEXT_CONTENT).trim();
    }

    /**
     * @param title the title of an event displayed in the agenda view
     * @return the text displayed for the event in the agenda view: its title, dates, author and description
     */
    public String getAgendaEventText(String title)
    {
        return getAgendaEvent(title).getText();
    }

    /**
     * @param title the title of an event displayed in the current view (not available in the agenda view)
     * @return the background color of the first displayed event with the given title, in the {@code #rrggbb} format
     */
    public String getEventBackgroundColor(String title)
    {
        return toHex(getEvent(title).getCssValue("background-color"));
    }

    /**
     * @param title an event title
     * @return the background colors of all the displayed events with the given title (not available in the agenda
     *     view), in the {@code #rrggbb} format
     */
    public List<String> getEventBackgroundColors(String title)
    {
        return getEvents(title).stream().map(event -> toHex(event.getCssValue("background-color")))
            .collect(Collectors.toList());
    }

    /**
     * @param title the title of an event displayed in the current view (not available in the agenda view)
     * @return the text color of the first displayed event with the given title, in the {@code #rrggbb} format
     */
    public String getEventTextColor(String title)
    {
        return toHex(getEvent(title).findElement(EVENT_TITLE).getCssValue("color"));
    }

    /**
     * @param title the title of an event displayed in the current view (not available in the agenda view)
     * @return the time displayed for the first event with the given title: the start time in the month view (e.g.
     *     {@code 10:00}), the start and end times in the week and day views (e.g. {@code 10:00 - 11:00}), or an
     *     empty string for an all day event
     */
    public String getEventTime(String title)
    {
        List<WebElement> time = getDriver().findElementsWithoutWaiting(getEvent(title), By.className("fc-time"));
        return time.isEmpty() ? "" : time.get(0).getAttribute(TEXT_CONTENT).trim();
    }

    /**
     * @return the times of the rows displayed by the week and day views, in the {@code HH:mm} format, from the
     *     first one to the last one (e.g. {@code 00:00, 00:30, ..., 23:30})
     */
    public List<String> getTimeSlots()
    {
        return getDriver().findElementsWithoutWaiting(getContainer(), By.cssSelector(".fc-slats tr[data-time]"))
            .stream().map(row -> row.getAttribute("data-time").substring(0, 5)).collect(Collectors.toList());
    }

    /**
     * @return the message displayed by the agenda view when there are no events to list
     */
    public String getNoEventsMessage()
    {
        return getContainer().findElement(By.cssSelector(".fc-timeline-box > p")).getText();
    }

    /**
     * Only available in the month view.
     *
     * @param title an event title
     * @return the start days of the instances of the event with the given title displayed in the current view
     *     (including the days of the previous and next months displayed by the month view), in chronological order
     */
    public List<LocalDate> getEventDays(String title)
    {
        // The day of an event is the day cell it is displayed in. The days are drawn in a background layer, below the
        // layer of the events, so find the cell under the start of the event.
        List<WebElement> dayCells = getDriver().findElementsWithoutWaiting(getContainer(), DAY_CELL);
        List<Rectangle> dayRectangles = dayCells.stream().map(WebElement::getRect).collect(Collectors.toList());
        // An event spanning several weeks is displayed in several parts: keep the part of its start.
        return getEvents(title).stream().filter(event -> event.getAttribute(CLASS_ATTRIBUTE).contains("fc-start"))
            .map(event -> {
                Rectangle eventRectangle = event.getRect();
                int x = eventRectangle.getX() + 1;
                int y = eventRectangle.getY() + eventRectangle.getHeight() / 2;
                for (int i = 0; i < dayRectangles.size(); i++) {
                    if (contains(dayRectangles.get(i), x, y)) {
                        return LocalDate.parse(dayCells.get(i).getAttribute("data-date"));
                    }
                }
                throw new IllegalStateException(String.format("No day displayed under the event [%s]", title));
            }).sorted().collect(Collectors.toList());
    }

    /**
     * @return the names of the days of the week, as displayed in the header of the month and week views, e.g.
     *     {@code Sun}, from the first column to the last one
     */
    public List<String> getWeekDayNames()
    {
        return getDriver().findElementsWithoutWaiting(getContainer(), By.cssSelector("th.fc-day-header")).stream()
            .map(header -> header.getAttribute(TEXT_CONTENT).trim()).collect(Collectors.toList());
    }

    /**
     * Imports the events of an iCalendar file in this calendar, using the "Import .ics file" button, and waits for the
     * imported events to be displayed.
     *
     * @param file the {@code .ics} file to import
     * @return this calendar
     */
    public CalendarElement importICalFile(File file)
    {
        // The import dialog and button are numbered like the calendar.
        String modalId = "import-calendar-file-" + this.calendarId.substring("calendar".length());
        getDriver().findElement(By.cssSelector("button[data-target='#" + modalId + "']")).click();
        WebElement modal = getDriver().findElement(By.id(modalId));
        getDriver().waitUntilCondition(driver -> modal.isDisplayed());

        // The browser runs in a container: send it the file.
        FileDetector fileDetector = getDriver().getFileDetector();
        getDriver().setFileDetector(new LocalFileDetector());
        try {
            modal.findElement(By.cssSelector("input[type='file']")).sendKeys(file.getAbsolutePath());
        } finally {
            getDriver().setFileDetector(fileDetector);
        }
        modal.findElement(By.className("import-calendar-file-button")).click();
        // The import is done by a job, which refreshes the calendar once finished.
        waitForNotificationSuccessMessage("Calendar successfully imported!");
        modal.findElement(By.cssSelector(".modal-footer .btn-default")).click();
        getDriver().waitUntilCondition(driver -> !modal.isDisplayed());
        return waitUntilReady();
    }

    /**
     * @param title the title of an event displayed in the current view (not available in the agenda view)
     * @return {@code true} if the current user can move the event by dragging it
     */
    public boolean canMoveEvent(String title)
    {
        return getEvent(title).getAttribute(CLASS_ATTRIBUTE).contains("fc-draggable");
    }

    /**
     * Drags the first displayed event with the given title to another day, and waits for the calendar to be updated.
     * Only available in the month view.
     *
     * @param title the event title
     * @param day the day to move the event to, displayed in the current view
     * @return this calendar
     */
    public CalendarElement dragEventToDay(String title, LocalDate day)
    {
        dropEventOnDay(title, day);
        return waitUntilReady();
    }

    /**
     * Drags the first displayed event with the given title to another day, when the move is expected to be refused,
     * e.g. for a recurrent event. Only available in the month view.
     *
     * @param title the event title
     * @param day the day to move the event to, displayed in the current view
     * @return the error message displayed, the event being moved back
     */
    public String dragEventToDayRefused(String title, LocalDate day)
    {
        // Read the error displayed for this move, not an older one that would still be displayed.
        List<WebElement> previousErrors = getDriver().findElementsWithoutWaiting(ERROR_NOTIFICATION);
        dropEventOnDay(title, day);
        String message = getDriver().waitUntilCondition(driver -> getDriver()
            .findElementsWithoutWaiting(ERROR_NOTIFICATION).stream()
            .filter(error -> !previousErrors.contains(error) && error.isDisplayed())
            .map(WebElement::getText).findFirst().orElse(null));
        waitUntilReady();
        return message;
    }

    /**
     * Drags the first displayed event with the given title to another time of the same day, and waits for the
     * calendar to be updated. Only available in the week and day views.
     *
     * @param title the event title
     * @param startTime the new start time of the event, in the {@code HH:mm} format, on a 30 minutes slot
     * @return this calendar
     */
    public CalendarElement dragEventToTime(String title, String startTime)
    {
        // The hours are displayed in a scrollable area: show the target before moving the mouse.
        scrollToSlot(startTime);
        WebElement event = getEvent(title);
        // The event is moved by the time between the slot it is grabbed in and the slot it is dropped in: grab it
        // inside its first slot, i.e. just below its top.
        int grabOffset = DRAG_START_DISTANCE - event.getRect().getHeight() / 2;
        drag(getDriver().createActions().moveToElement(event, 0, grabOffset),
            getSlotTop(startTime) - getSlotTop(getEventHours(title)[0]));
        return waitUntilReady();
    }

    /**
     * Changes the end time of the first displayed event with the given title by dragging its bottom edge, and waits
     * for the calendar to be updated. Only available in the week and day views.
     *
     * @param title the event title
     * @param endTime the new end time of the event, in the {@code HH:mm} format, on a 30 minutes slot
     * @return this calendar
     */
    public CalendarElement resizeEventTo(String title, String endTime)
    {
        // The hours are displayed in a scrollable area: show the target before moving the mouse.
        scrollToSlot(endTime);
        WebElement event = getEvent(title);
        // The resize handle is displayed only while the mouse is over the event.
        getDriver().createActions().moveToElement(event).perform();
        WebElement resizer = event.findElement(By.className("fc-end-resizer"));
        // The end is moved by the time between the slot the handle is grabbed in (the last slot of the event) and
        // the slot it is dropped in.
        drag(getDriver().createActions().moveToElement(resizer),
            getSlotTop(endTime) - getSlotTop(getEventHours(title)[1]));
        return waitUntilReady();
    }

    /**
     * Clicks on the first displayed event with the given title (not available in the agenda view).
     *
     * @param title the event title
     * @return the dialog showing the event
     */
    public EventViewModal clickEvent(String title)
    {
        getEvent(title).click();
        return new EventViewModal(this);
    }

    /**
     * Clicks on one of the displayed events with the given title (not available in the agenda view).
     *
     * @param title the event title
     * @param position the position of the event among the displayed events with the given title, starting from 0, in
     *     the display order (by day, then by time)
     * @return the dialog showing the event
     */
    public EventViewModal clickEvent(String title, int position)
    {
        getEvents(title).get(position).click();
        return new EventViewModal(this);
    }

    /**
     * @param cssValue a CSS color, as returned by {@link WebElement#getCssValue(String)}
     * @return the color in the lower case {@code #rrggbb} format
     */
    private static String toHex(String cssValue)
    {
        return Color.fromString(cssValue).asHex();
    }

    private WebElement getContainer()
    {
        return getDriver().findElement(By.id(this.calendarId));
    }

    private static int getCenterX(WebElement element)
    {
        return element.getRect().getX() + element.getRect().getWidth() / 2;
    }

    private WebElement getDayCell(LocalDate day)
    {
        return getContainer().findElement(By.cssSelector(".fc-bg td.fc-day[data-date='" + day + "']"));
    }

    private static boolean contains(Rectangle rectangle, int x, int y)
    {
        return x >= rectangle.getX() && x < rectangle.getX() + rectangle.getWidth() && y >= rectangle.getY()
            && y < rectangle.getY() + rectangle.getHeight();
    }

    private void dropEventOnDay(String title, LocalDate day)
    {
        // The event is moved by the number of days between the day it is grabbed on and the day it is dropped on.
        Actions actions = getDriver().createActions().moveToElement(getEvent(title)).clickAndHold();
        // FullCalendar starts dragging only after the mouse moved.
        actions.moveByOffset(0, DRAG_START_DISTANCE).pause(DRAG_PAUSE).moveToElement(getDayCell(day))
            .pause(DRAG_PAUSE).release().perform();
    }

    private void drag(Actions grab, int verticalDistance)
    {
        int firstStep = verticalDistance / 2;
        grab.clickAndHold().moveByOffset(0, firstStep).pause(DRAG_PAUSE)
            .moveByOffset(0, verticalDistance - firstStep).pause(DRAG_PAUSE).release().perform();
    }

    private String[] getEventHours(String title)
    {
        // The week and day views display the start and end times, e.g. "10:00 - 11:00".
        return getEventTime(title).split(" - ");
    }

    private WebElement getSlot(String time)
    {
        return getContainer().findElement(By.cssSelector(".fc-slats tr[data-time='" + time + ":00']"));
    }

    private WebElement scrollToSlot(String time)
    {
        WebElement slot = getSlot(time);
        getDriver().executeScript("arguments[0].scrollIntoView({block: 'center'});", slot);
        return slot;
    }

    private int getSlotTop(String time)
    {
        return getSlot(time).getRect().getY();
    }

    private List<WebElement> getEvents(String title)
    {
        // The calendar is ready (see waitUntilReady), so its events are displayed: don't wait for an event when the
        // displayed period has none.
        return getDriver().findElementsWithoutWaiting(getContainer(), By.cssSelector("a.fc-event")).stream()
            .filter(event -> title.equals(event.findElement(EVENT_TITLE).getAttribute(TEXT_CONTENT).trim()))
            .collect(Collectors.toList());
    }

    private WebElement getAgendaEvent(String title)
    {
        return getDriver().findElementsWithoutWaiting(getContainer(), By.cssSelector(".fc-timeline-box .moccaevent"))
            .stream()
            .filter(event -> title.equals(event.findElement(By.tagName("h5")).getAttribute(TEXT_CONTENT).trim()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("No event with title [%s]", title)));
    }

    private WebElement getEvent(String title)
    {
        return getEvents(title).stream().findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("No event with title [%s]", title)));
    }
}
