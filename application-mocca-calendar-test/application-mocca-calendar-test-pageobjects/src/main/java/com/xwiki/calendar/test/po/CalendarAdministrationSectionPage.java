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
import org.xwiki.administration.test.po.AdministrationSectionPage;

/**
 * The "Calendar" section of the wiki administration, used to configure the calendar event sources.
 *
 * @version $Id$
 * @since 2.22.1
 */
public class CalendarAdministrationSectionPage extends AdministrationSectionPage
{
    private static final String SECTION = "moccacalendar.sources.section";

    /**
     * Default constructor.
     */
    public CalendarAdministrationSectionPage()
    {
        super(SECTION);
    }

    /**
     * Opens the "Calendar" section of the wiki administration.
     *
     * @return the administration section
     */
    public static CalendarAdministrationSectionPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(SECTION);
        return new CalendarAdministrationSectionPage();
    }

    /**
     * @return the names of the event sources that can be enabled
     */
    public List<String> getAvailableSources()
    {
        return getSourceNames(".form-control-static label");
    }

    /**
     * @return the names of the event sources whose extension has to be installed first
     */
    public List<String> getInstallableSources()
    {
        return getSourceNames("label.text-muted");
    }

    /**
     * @param sourceName the name of an event source whose extension is not installed
     * @return {@code true} if the section offers a link to install the extension of the given source
     */
    public boolean hasInstallLink(String sourceName)
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(".moccacalendar-sources .list-group-item"))
            .stream()
            .filter(item -> item.getText().contains(sourceName))
            .anyMatch(item -> getDriver().hasElementWithoutWaiting(item, By.cssSelector("a.pull-right")));
    }

    private List<String> getSourceNames(String labelSelector)
    {
        return getDriver()
            .findElementsWithoutWaiting(By.cssSelector(".moccacalendar-sources .list-group-item " + labelSelector))
            .stream().map(WebElement::getText).map(String::trim).collect(Collectors.toList());
    }
}
