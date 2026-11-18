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

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.model.reference.DocumentReference;

/**
 * The dialog used to create or edit a calendar event.
 *
 * @version $Id$
 * @since 2.22.1
 */
public class EventEditModal extends AbstractEventModal
{
    /**
     * The frequencies of a recurrent event.
     */
    public enum Frequency
    {
        /**
         * Every day.
         */
        DAILY("daily"),

        /**
         * Every day from Monday to Friday.
         */
        WORKDAYS("workdays"),

        /**
         * Every week, on the week day of the start of the event.
         */
        WEEKLY("weekly"),

        /**
         * Every two weeks.
         */
        BIWEEKLY("biweekly"),

        /**
         * Every month, on the day of the month of the start of the event.
         */
        MONTHLY("monthly"),

        /**
         * Every month, on the same week day of the month as the start of the event, e.g. the second Thursday.
         */
        MONTHLY_SPECIFIC("monthlySpecific"),

        /**
         * Every three months.
         */
        QUARTERLY("quarterly"),

        /**
         * Every year.
         */
        YEARLY("yearly"),

        /**
         * Every week, on chosen week days, see {@link EventEditModal#setRecurrenceDays(Set)}.
         */
        CUSTOM_WEEKLY("customWeekly");

        private final String value;

        Frequency(String value)
        {
            this.value = value;
        }

        /**
         * @return the value of the frequency option in the event form
         */
        String getValue()
        {
            return this.value;
        }
    }

    private static final String EVENT_PREFIX = "MoccaCalendar.MoccaCalendarEventClass_0_";

    private static final String RECURRENCY_PREFIX = "MoccaCalendar.Code.MoccaCalendarEventRecurrencyClass_0_";

    private static final By SAVE_BUTTON = By.id("save-moccacalendar-event");

    private static final By ONE_INSTANCE_FIELDS = By.id("oneEventFields");

    /**
     * Waits for the dialog to be loaded.
     *
     * @param calendar the calendar the dialog is opened from
     */
    EventEditModal(CalendarElement calendar)
    {
        super(calendar);
        waitUntilLoaded();
        getDriver().waitUntilElementIsVisible(getDialog(), SAVE_BUTTON);
    }

    /**
     * @param title the event title
     * @return this dialog
     */
    public EventEditModal setTitle(String title)
    {
        WebElement titleInput = getField("title");
        titleInput.clear();
        titleInput.sendKeys(title);
        return this;
    }

    /**
     * @return the event title
     */
    public String getTitle()
    {
        return getField("title").getAttribute("value");
    }

    /**
     * @param startDate the start date, in the {@code yyyy/MM/dd HH:mm} format (or {@code yyyy/MM/dd} for all day
     *     events)
     * @return this dialog
     */
    public EventEditModal setStartDate(String startDate)
    {
        setDate(getField("startDate"), startDate);
        return this;
    }

    /**
     * @return the start date
     */
    public String getStartDate()
    {
        return getField("startDate").getAttribute("value");
    }

    /**
     * @param endDate the end date, in the {@code yyyy/MM/dd HH:mm} format (or {@code yyyy/MM/dd} for all day events)
     * @return this dialog
     */
    public EventEditModal setEndDate(String endDate)
    {
        setDate(getField("endDate"), endDate);
        return this;
    }

    /**
     * @return the end date
     */
    public String getEndDate()
    {
        return getField("endDate").getAttribute("value");
    }

    /**
     * @param description the event description
     * @return this dialog
     */
    public EventEditModal setDescription(String description)
    {
        WebElement descriptionInput = getField("description");
        descriptionInput.clear();
        descriptionInput.sendKeys(description);
        return this;
    }

    /**
     * @return the event description
     */
    public String getDescription()
    {
        return getField("description").getAttribute("value");
    }

    /**
     * @param color the background color of the event in the calendar, e.g. {@code #ff0000}
     * @return this dialog
     */
    public EventEditModal setBackgroundColor(String color)
    {
        CalendarElement.setColor(getField("backgroundColor"), color);
        return this;
    }

    /**
     * @param color the text color of the event in the calendar, e.g. {@code #ffffff}
     * @return this dialog
     */
    public EventEditModal setTextColor(String color)
    {
        CalendarElement.setColor(getField("textColor"), color);
        return this;
    }

    /**
     * Marks the event as lasting the whole day. Since this changes the format of the dates, set it before the dates.
     *
     * @param allDay {@code true} for an all day event
     * @return this dialog
     */
    public EventEditModal setAllDay(boolean allDay)
    {
        setCheckbox(getDialog().findElement(By.id(EVENT_PREFIX + "allDay")), allDay);
        return this;
    }

    /**
     * @return {@code true} if the event lasts the whole day
     */
    public boolean isAllDay()
    {
        return getDialog().findElement(By.id(EVENT_PREFIX + "allDay")).isSelected();
    }

    /**
     * @return {@code true} if the dialog lets the user make the event recurrent (only when creating an event)
     */
    public boolean canSetRecurrent()
    {
        return !getDriver().findElementsWithoutWaiting(getDialog(),
            By.cssSelector("input[type='checkbox'][name='" + EVENT_PREFIX + "recurrent']")).isEmpty();
    }

    /**
     * @param showAuthor whether the author of the event is displayed in the calendar
     * @return this dialog
     */
    public EventEditModal setShowAuthor(boolean showAuthor)
    {
        setCheckbox(getDialog().findElement(By.id(EVENT_PREFIX + "showAuthor")), showAuthor);
        return this;
    }

    /**
     * @return whether the author of the event is displayed in the calendar
     */
    public boolean isShowAuthor()
    {
        return getDialog().findElement(By.id(EVENT_PREFIX + "showAuthor")).isSelected();
    }

    /**
     * @param recurrent whether the event is recurrent
     * @return this dialog
     */
    public EventEditModal setRecurrent(boolean recurrent)
    {
        setCheckbox(getDialog().findElement(By.id(EVENT_PREFIX + "recurrent")), recurrent);
        if (recurrent) {
            getDriver().waitUntilElementIsVisible(getDialog(), By.id("recurrentFields"));
        } else {
            getDriver().waitUntilElementDisappears(getDialog(), By.id("recurrentFields"));
        }
        return this;
    }

    /**
     * @return {@code true} if the recurrence settings are displayed
     */
    public boolean isRecurrenceDisplayed()
    {
        return getDialog().findElement(By.id("recurrentFields")).isDisplayed();
    }

    /**
     * @param frequency the recurrence frequency
     * @return this dialog
     */
    public EventEditModal setFrequency(Frequency frequency)
    {
        getFrequencySelect().selectByValue(frequency.getValue());
        return this;
    }

    /**
     * @return the labels of the available recurrence frequencies
     */
    public List<String> getFrequencyOptions()
    {
        return getFrequencySelect().getOptions().stream().map(WebElement::getText).map(String::trim)
            .collect(Collectors.toList());
    }

    /**
     * @return {@code true} if the week days of a custom weekly recurrence are displayed
     */
    public boolean isRecurrenceDaysDisplayed()
    {
        return getDialog().findElement(By.id("recurrentDays")).isDisplayed();
    }

    /**
     * Chooses the days of a custom weekly recurrence (see {@link Frequency#CUSTOM_WEEKLY}).
     *
     * @param days the days of the week of the instances
     * @return this dialog
     */
    public EventEditModal setRecurrenceDays(Set<DayOfWeek> days)
    {
        for (DayOfWeek day : DayOfWeek.values()) {
            // The days are numbered from Sunday (1) to Saturday (7).
            int number = day.getValue() % 7 + 1;
            WebElement checkbox = getDialog().findElement(By.cssSelector(
                "#recurrentDays input[name='" + RECURRENCY_PREFIX + "days'][value='" + number + "']"));
            if (checkbox.isSelected() != days.contains(day)) {
                // The checkbox is covered by its label, which displays the name of the day: click the label.
                checkbox.findElement(By.xpath("./ancestor::label[1]")).click();
            }
        }
        return this;
    }

    /**
     * @param firstInstance the date before which a recurrent event has no instance, in the
     *     {@code yyyy/MM/dd HH:mm} format
     * @return this dialog
     */
    public EventEditModal setFirstInstance(String firstInstance)
    {
        setDate(getDialog().findElement(By.id(RECURRENCY_PREFIX + "firstInstance")), firstInstance);
        return this;
    }

    /**
     * @param lastInstance the start date of the last instance of a recurrent event, in the
     *     {@code yyyy/MM/dd HH:mm} format
     * @return this dialog
     */
    public EventEditModal setLastInstance(String lastInstance)
    {
        setDate(getDialog().findElement(By.id(RECURRENCY_PREFIX + "lastInstance")), lastInstance);
        return this;
    }

    /**
     * @return {@code true} if the user can choose the calendar of the event
     */
    public boolean canSelectCalendar()
    {
        return getDriver().hasElementWithoutWaiting(getDialog(),
            By.cssSelector("select#calendarParent:not([hidden])"));
    }

    /**
     * @return the titles of the calendars the event can be created in
     */
    public List<String> getCalendarOptions()
    {
        return getCalendarSelect().getOptions().stream().map(WebElement::getText).map(String::trim)
            .collect(Collectors.toList());
    }

    /**
     * @return the title of the calendar selected for the event, when the calendar can be chosen
     */
    public String getSelectedCalendar()
    {
        return getCalendarSelect().getFirstSelectedOption().getText().trim();
    }

    /**
     * Chooses the calendar of the event by its page, since several calendars can have the same title.
     *
     * @param calendarReference the reference of the page of the calendar of the event
     * @return this dialog
     */
    public EventEditModal selectCalendar(DocumentReference calendarReference)
    {
        // The options are identified by the full name of the calendar page.
        getCalendarSelect().selectByValue(
            getUtil().serializeReference(calendarReference.removeParent(calendarReference.getWikiReference())));
        return this;
    }

    /**
     * @return {@code true} if the dialog asks whether to edit only the clicked instance of a recurrent event or all
     *     of them
     */
    public boolean hasInstanceEditSwitch()
    {
        return getDriver().hasElementWithoutWaiting(getDialog(), By.id("instanceEditSwitch"));
    }

    /**
     * Edits only the clicked instance of a recurrent event.
     *
     * @return this dialog
     */
    public EventEditModal editThisInstanceOnly()
    {
        getDialog().findElement(By.id("instanceEditYes")).click();
        getDriver().waitUntilElementIsVisible(getDialog(), ONE_INSTANCE_FIELDS);
        return this;
    }

    /**
     * Edits all the instances of a recurrent event.
     *
     * @return this dialog
     */
    public EventEditModal editAllInstances()
    {
        getDialog().findElement(By.id("instanceEditNo")).click();
        getDriver().waitUntilElementIsVisible(getDialog(), By.id("allEventsFields"));
        return this;
    }

    /**
     * Saves the event, and waits for the calendar to be refreshed.
     */
    public void save()
    {
        getDialog().findElement(SAVE_BUTTON).click();
        waitUntilClosed();
        this.calendar.waitUntilReady();
    }

    /**
     * Saves the event when the values are expected to be rejected.
     *
     * @return the validation error messages displayed in the dialog
     */
    public List<String> saveWithErrors()
    {
        getDialog().findElement(SAVE_BUTTON).click();
        waitUntilNoPendingRequest();
        getDriver().waitUntilCondition(driver -> !getValidationErrors().isEmpty());
        return getValidationErrors();
    }

    /**
     * @return the validation error messages displayed in the dialog
     */
    public List<String> getValidationErrors()
    {
        return getDriver().findElementsWithoutWaiting(getDialog(), By.className("xErrorMsg")).stream()
            .filter(WebElement::isDisplayed).map(WebElement::getText).map(String::trim).collect(Collectors.toList());
    }

    private boolean isEditingOneInstance()
    {
        List<WebElement> oneInstanceFields = getDriver().findElementsWithoutWaiting(getDialog(), ONE_INSTANCE_FIELDS);
        return !oneInstanceFields.isEmpty() && oneInstanceFields.get(0).isDisplayed();
    }

    private WebElement getField(String eventProperty)
    {
        if (isEditingOneInstance()) {
            return getDialog().findElement(By.cssSelector("#oneEventFields [id$='_" + eventProperty + "']"));
        }
        return getDialog().findElement(By.id(EVENT_PREFIX + eventProperty));
    }

    private Select getFrequencySelect()
    {
        return new Select(getDialog().findElement(By.id(RECURRENCY_PREFIX + "frequency")));
    }

    private Select getCalendarSelect()
    {
        return new Select(getDialog().findElement(By.cssSelector("select#calendarParent:not([hidden])")));
    }

    private void setCheckbox(WebElement checkbox, boolean checked)
    {
        if (checkbox.isSelected() != checked) {
            checkbox.click();
        }
        // Changing the "all day" flag might check the date format on the server.
        waitUntilNoPendingRequest();
    }

    private void setDate(WebElement dateInput, String value)
    {
        // The date picker fills an empty field with the current date as soon as it gets the focus, and rejects the
        // typed value if it is appended to that date. So focus the field first, then select what the picker put there
        // and type over it.
        dateInput.click();
        dateInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        // Leave the field with TAB (Escape would close the dialog) in order to close the date picker and to trigger
        // the date check done on change.
        dateInput.sendKeys(value, Keys.TAB);
        waitUntilNoPendingRequest();
    }
}
