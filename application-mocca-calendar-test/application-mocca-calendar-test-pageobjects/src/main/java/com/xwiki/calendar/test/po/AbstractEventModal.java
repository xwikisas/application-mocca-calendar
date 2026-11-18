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
import org.xwiki.test.ui.po.BaseElement;

/**
 * Base class for the dialog used to view, create and edit calendar events.
 *
 * @version $Id$
 * @since 2.22.1
 */
public abstract class AbstractEventModal extends BaseElement
{
    protected static final By DIALOG = By.className("xdialog-box-moccacal-modal-popup");

    protected static final By CANCEL_BUTTON = By.id("cancel-moccacalendar-event");

    /**
     * The calendar the dialog was opened from, refreshed when the dialog changes an event.
     */
    protected final CalendarElement calendar;

    /**
     * @param calendar the calendar the dialog is opened from
     */
    protected AbstractEventModal(CalendarElement calendar)
    {
        this.calendar = calendar;
    }

    /**
     * Waits until the dialog content is loaded.
     */
    protected void waitUntilLoaded()
    {
        getDriver().waitUntilElementIsVisible(getDialog(), CANCEL_BUTTON);
        waitUntilNoPendingRequest();
    }

    protected WebElement getDialog()
    {
        return getDriver().findElement(DIALOG);
    }

    /**
     * @return the title of the dialog
     */
    public String getDialogTitle()
    {
        WebElement title = getDialog().findElement(By.className("xdialog-title"));
        String text = title.getText();
        // The title also holds the close button.
        for (WebElement closeButton : title.findElements(By.className("xdialog-close"))) {
            text = text.replace(closeButton.getText(), "");
        }
        return text.trim();
    }

    /**
     * Closes the dialog without saving anything.
     */
    public void cancel()
    {
        getDialog().findElement(CANCEL_BUTTON).click();
        waitUntilClosed();
    }

    protected void waitUntilClosed()
    {
        getDriver().waitUntilElementDisappears(DIALOG);
        waitUntilNoPendingRequest();
    }

    protected void waitUntilNoPendingRequest()
    {
        getDriver().waitUntilJavascriptCondition("return (!window.jQuery || jQuery.active === 0) "
            + "&& (!window.Ajax || Ajax.activeRequestCount === 0)");
    }
}
