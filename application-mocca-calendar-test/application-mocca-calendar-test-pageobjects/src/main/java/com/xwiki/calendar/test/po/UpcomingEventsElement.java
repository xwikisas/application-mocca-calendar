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

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The list of upcoming events displayed by the {@code moccacalendar.upcomingEvents} macro, either in the page content
 * or in the "Upcoming Events" panel.
 *
 * @version $Id$
 * @since 2.22.1
 */
public class UpcomingEventsElement extends BaseElement
{
    private static final By EVENT_TITLE = By.className("moccacal-event-title");

    private final WebElement container;

    /**
     * @param container the element containing the list of events
     */
    public UpcomingEventsElement(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the list of upcoming events displayed in the page content
     */
    public static UpcomingEventsElement inContent()
    {
        return new UpcomingEventsElement(
            getUtil().getDriver().findElement(By.cssSelector("#xwikicontent .moccacal-event-list")));
    }

    /**
     * @return the list of upcoming events displayed in the "Upcoming Events" panel
     */
    public static UpcomingEventsElement inPanel()
    {
        return new UpcomingEventsElement(
            getUtil().getDriver().findElement(By.cssSelector(".panel .moccacal-event-list")));
    }

    /**
     * @return the header of the panel containing the list
     */
    public String getPanelHeader()
    {
        // Match the "panel" class exactly, not "panel-body".
        WebElement panel = this.container.findElement(
            By.xpath("ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' panel ')][1]"));
        return panel.findElement(By.className("xwikipaneltitle")).getText().trim();
    }

    /**
     * @return the titles of the listed events, in the displayed order
     */
    public List<String> getEventTitles()
    {
        return this.container.findElements(EVENT_TITLE).stream().map(WebElement::getText).map(String::trim)
            .collect(Collectors.toList());
    }

    /**
     * @param title the title of a listed event
     * @return the schedule (time or dates) displayed for the event
     */
    public String getSchedule(String title)
    {
        return getEvent(title).findElement(By.className("moccacal-event-schedule")).getText().trim();
    }

    /**
     * Opens the page of the given event.
     *
     * @param title the title of a listed event
     * @return the event page
     */
    public EventPage clickEvent(String title)
    {
        getDriver().addPageNotYetReloadedMarker();
        getEvent(title).findElement(By.cssSelector(".moccacal-event-title a")).click();
        getDriver().waitUntilPageIsReloaded();
        return new EventPage();
    }

    private WebElement getEvent(String title)
    {
        return this.container.findElements(By.className("moccacal-event")).stream()
            .filter(event -> title.equals(event.findElement(EVENT_TITLE).getText().trim()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("No event with title [%s]", title)));
    }
}
