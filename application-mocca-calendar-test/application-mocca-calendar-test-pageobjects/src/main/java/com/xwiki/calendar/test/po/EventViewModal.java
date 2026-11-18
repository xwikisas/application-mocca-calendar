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

/**
 * The dialog showing an event, opened when clicking on an event in the calendar.
 *
 * @version $Id$
 * @since 2.22.1
 */
public class EventViewModal extends AbstractEventModal
{
    private static final By EDIT_BUTTON = By.id("edit-moccacalendar-event");

    private static final By VIEW_BUTTON = By.id("view-moccacalendar-event");

    private static final By DELETE_BUTTON = By.id("delete-moccacalendar-event");

    /**
     * Waits for the dialog to be loaded.
     *
     * @param calendar the calendar the dialog is opened from
     */
    EventViewModal(CalendarElement calendar)
    {
        super(calendar);
        waitUntilLoaded();
    }

    /**
     * @param label the label of an event field, as displayed in the dialog (e.g. "Title", "Start date")
     * @return the value displayed for the given field
     */
    public String getFieldValue(String label)
    {
        return EventPage.getFieldValue(getDialog(), label);
    }

    /**
     * @param label the label of an event field, as displayed in the dialog (e.g. "Author")
     * @return {@code true} if the dialog displays the given field
     */
    public boolean hasField(String label)
    {
        return getDriver().hasElementWithoutWaiting(getDialog(), EventPage.getFieldValueLocator(label));
    }

    /**
     * @return {@code true} if the current user can edit the event
     */
    public boolean canEdit()
    {
        return getDriver().hasElementWithoutWaiting(getDialog(), EDIT_BUTTON);
    }

    /**
     * @return {@code true} if the current user can delete the event
     */
    public boolean canDelete()
    {
        return getDriver().hasElementWithoutWaiting(getDialog(), DELETE_BUTTON);
    }

    /**
     * @return {@code true} if the dialog offers to open the event page
     */
    public boolean canView()
    {
        return getDriver().hasElementWithoutWaiting(getDialog(), VIEW_BUTTON);
    }

    /**
     * Switches the dialog to edit mode.
     *
     * @return the dialog in edit mode
     */
    public EventEditModal clickEdit()
    {
        WebElement dialog = getDialog();
        dialog.findElement(EDIT_BUTTON).click();
        getDriver().waitUntilElementDisappears(dialog, EDIT_BUTTON);
        return new EventEditModal(this.calendar);
    }

    /**
     * Opens the page of the event.
     *
     * @return the event page
     */
    public EventPage clickView()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDialog().findElement(VIEW_BUTTON).click();
        getDriver().waitUntilPageIsReloaded();
        return new EventPage();
    }

    /**
     * Asks for the deletion of the event.
     *
     * @return the delete confirmation dialog
     */
    public DeleteEventModal clickDelete()
    {
        // The confirmation modal must be created before being opened, see BaseModal.
        DeleteEventModal deleteModal = new DeleteEventModal(this.calendar);
        getDialog().findElement(DELETE_BUTTON).click();
        deleteModal.waitUntilOpen();
        getDriver().waitUntilElementDisappears(DIALOG);
        return deleteModal;
    }
}
