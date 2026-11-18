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
package com.xwiki.calendar.test.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI tests for the Calendar application.
 *
 * @version $Id$
 * @since 2.11
 */
@UITest(extraJARs = {
    "org.xwiki.platform:xwiki-platform-mailsender:15.10",
    "org.xwiki.platform:xwiki-platform-jodatime:15.10",
    "org.xwiki.platform:xwiki-platform-mail-send-default:15.10",
    "org.xwiki.platform:xwiki-platform-legacy-rendering-macro-include:15.10" }, resolveExtraJARs = true,
    properties = { "xwikiCfgPlugins=com.xpn.xwiki.plugin.jodatime.JodaTimePlugin,"
        + "com.xpn.xwiki.plugin.mailsender.MailSenderPlugin",
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:MoccaCalendar\\.MoccaCalendarEventSheet" })
class AllIT
{
    @Nested
    @Order(1)
    @DisplayName("Overall Calendar UI")
    class NestedCalendarIT extends CalendarIT
    {
    }

    @Nested
    @Order(2)
    @DisplayName("Calendar events: editing, colors, hours, drag & drop")
    class NestedCalendarEventIT extends CalendarEventIT
    {
    }

    @Nested
    @Order(3)
    @DisplayName("Recurrent events")
    class NestedRecurrentEventIT extends RecurrentEventIT
    {
    }

    @Nested
    @Order(4)
    @DisplayName("Calendar macros")
    class NestedCalendarMacroIT extends CalendarMacroIT
    {
    }

    @Nested
    @Order(5)
    @DisplayName("iCal export, subscription and import")
    class NestedICalIT extends ICalIT
    {
    }
}
