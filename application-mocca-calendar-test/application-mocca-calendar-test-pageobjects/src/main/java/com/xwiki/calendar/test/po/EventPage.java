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

import java.util.Locale;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The page of a calendar event.
 *
 * @version $Id$
 * @since 2.22.1
 */
public class EventPage extends ViewPage
{
    private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";

    /**
     * @param label the label of an event field, as displayed in the page (e.g. "Title", "Start date")
     * @return the value displayed for the given field
     */
    public String getFieldValue(String label)
    {
        return getFieldValue(getDriver().findElement(By.id("xwikicontent")), label);
    }

    static String getFieldValue(SearchContext container, String label)
    {
        return container.findElement(getFieldValueLocator(label)).getText().trim();
    }

    static By getFieldValueLocator(String label)
    {
        // The skin displays the labels in upper case, so their case in the HTML is not what the user sees, and it
        // differs between the labels (e.g. the "description" label of the events of a subscribed calendar).
        return By.xpath(String.format(
            ".//dt[translate(normalize-space(.), '%s', '%s') = '%s']/following-sibling::dd[1]", UPPER_CASE, LOWER_CASE,
            label.toLowerCase(Locale.ROOT)));
    }

    /**
     * @return the text of the link going back to the calendar of the event
     */
    public String getBackToCalendarText()
    {
        return getBackToCalendarLink().getText();
    }

    /**
     * Goes back to the calendar of the event.
     *
     * @return the calendar page
     */
    public CalendarPage clickBackToCalendar()
    {
        getDriver().addPageNotYetReloadedMarker();
        getBackToCalendarLink().click();
        getDriver().waitUntilPageIsReloaded();
        return new CalendarPage();
    }

    private WebElement getBackToCalendarLink()
    {
        return getDriver().findElement(By.xpath("//div[@id = 'xwikicontent']//hr/following::a[1]"));
    }
}
