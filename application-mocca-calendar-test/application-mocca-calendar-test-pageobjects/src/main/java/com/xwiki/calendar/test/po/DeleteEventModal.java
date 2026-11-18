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
import org.xwiki.test.ui.po.BaseModal;

/**
 * The dialog asking for the confirmation of an event deletion.
 *
 * @version $Id$
 * @since 2.22.1
 */
public class DeleteEventModal extends BaseModal
{
    private static final By DELETE_ALL_BUTTON = By.cssSelector("input.do-delete-all");

    private final CalendarElement calendar;

    /**
     * The modal is not expected to be open yet.
     *
     * @param calendar the calendar the deleted event is displayed in, refreshed after the deletion
     */
    DeleteEventModal(CalendarElement calendar)
    {
        super(By.id("deleteEvents"));
        this.calendar = calendar;
    }

    /**
     * Waits until the dialog is open.
     *
     * @return this dialog
     */
    public DeleteEventModal waitUntilOpen()
    {
        getDriver().waitUntilCondition(driver -> isDisplayed());
        return this;
    }

    /**
     * @return the confirmation message
     */
    public String getMessage()
    {
        return this.container.findElement(By.className("modal-body")).getText();
    }

    /**
     * @return {@code true} if the dialog offers to delete all the instances of a recurrent event
     */
    public boolean canDeleteAllInstances()
    {
        return this.container.findElement(DELETE_ALL_BUTTON).isDisplayed();
    }

    /**
     * Confirms the deletion: deletes the event, or only the selected instance in case of a recurrent event. Waits for
     * the calendar to be refreshed.
     */
    public void clickYes()
    {
        clickAndWaitForDeletion(By.cssSelector("input.do-delete"));
    }

    /**
     * Deletes all the instances of a recurrent event. Waits for the calendar to be refreshed.
     */
    public void clickDeleteAllInstances()
    {
        clickAndWaitForDeletion(DELETE_ALL_BUTTON);
    }

    /**
     * Cancels the deletion, which goes back to the event dialog.
     *
     * @return the event dialog
     */
    public EventViewModal clickNo()
    {
        this.container.findElement(By.cssSelector("input.btn-default")).click();
        waitForClosed();
        return new EventViewModal(this.calendar);
    }

    private void clickAndWaitForDeletion(By button)
    {
        this.container.findElement(button).click();
        waitForClosed();
        this.calendar.waitUntilReady();
    }
}
