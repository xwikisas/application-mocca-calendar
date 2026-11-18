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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The edit form of a calendar page: either a calendar holding its own events, or a subscribed calendar showing the
 * events of an external calendar. Some fields exist only for one kind of calendar.
 *
 * @version $Id$
 * @since 2.22.1
 */
public class CalendarEditPage extends InlinePage
{
    private static final String CALENDAR_FIELD_PREFIX = "MoccaCalendar.MoccaCalendarClass_0_";

    private static final String SUBSCRIBED_CALENDAR_FIELD_PREFIX = "MoccaCalendar.MoccaCalendarSubscribeClass_0_";

    private static final String ICAL = "iCal";

    /**
     * Opens the edit form of an existing calendar.
     *
     * @param reference the reference of the calendar page
     * @return the edit form
     */
    public static CalendarEditPage gotoPage(EntityReference reference)
    {
        getUtil().gotoPage(reference, "edit", "editor=inline");
        return new CalendarEditPage();
    }

    /**
     * @param color the default background color of the events of the calendar, e.g. {@code #ff0000}
     * @return this page
     */
    public CalendarEditPage setBackgroundColor(String color)
    {
        CalendarElement.setColor(getField("color"), color);
        return this;
    }

    /**
     * @param color the default text color of the events of the calendar, e.g. {@code #ffffff}
     * @return this page
     */
    public CalendarEditPage setTextColor(String color)
    {
        CalendarElement.setColor(getField("textColor"), color);
        return this;
    }

    /**
     * Only for a calendar holding its own events.
     *
     * @param showAuthor whether the author of the events is displayed by default
     * @return this page
     */
    public CalendarEditPage setShowAuthor(boolean showAuthor)
    {
        return setChecked("showAuthor", showAuthor);
    }

    /**
     * Only for a subscribed calendar.
     *
     * @param url the URL of the iCal feed to show the events of
     * @return this page
     */
    public CalendarEditPage setICalURL(String url)
    {
        return setText(ICAL, url);
    }

    /**
     * Only for a subscribed calendar.
     *
     * @param description the description of the subscribed calendar
     * @return this page
     */
    public CalendarEditPage setDescription(String description)
    {
        return setText("description", description);
    }

    /**
     * Only for a subscribed calendar.
     *
     * @param globalDisplay whether the subscribed events are also displayed in the calendar of the home page
     * @return this page
     */
    public CalendarEditPage setDisplayInGlobalCalendar(boolean globalDisplay)
    {
        return setChecked("globalDisplay", globalDisplay);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends ViewPage> T createViewPage()
    {
        return (T) new CalendarPage();
    }

    /**
     * @param property the name of a property of the calendar, the same for both kinds of calendar
     * @return the field of the property
     */
    private WebElement getField(String property)
    {
        // The fields are named after the object holding the calendar properties, which depends on the calendar kind.
        boolean subscribed = getDriver().hasElementWithoutWaiting(By.id(SUBSCRIBED_CALENDAR_FIELD_PREFIX + ICAL));
        String prefix = subscribed ? SUBSCRIBED_CALENDAR_FIELD_PREFIX : CALENDAR_FIELD_PREFIX;
        return getDriver().findElement(By.id(prefix + property));
    }

    private CalendarEditPage setText(String property, String value)
    {
        WebElement field = getField(property);
        field.clear();
        field.sendKeys(value);
        return this;
    }

    private CalendarEditPage setChecked(String property, boolean checked)
    {
        WebElement checkbox = getField(property);
        if (checkbox.isSelected() != checked) {
            checkbox.click();
        }
        return this;
    }
}
