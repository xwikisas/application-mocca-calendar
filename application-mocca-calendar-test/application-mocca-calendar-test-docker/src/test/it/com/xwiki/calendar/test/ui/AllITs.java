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
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI tests for the Calendar application.
 *
 * @version $Id$
 * @since 2.11
 */
@UITest(properties = {
    // XWiki needs the mailsender plugin JAR to be present before it starts since it's not an extension and it
    // cannot be provisioned after XWiki is started!
    "org.xwiki.platform:xwiki-platform-mailsender:14.10",
    // MailSender plugin uses MailSenderConfiguration from xwiki-platform-mail-api so we need to provide an
    // implementation for it.
    "org.xwiki.platform:xwiki-platform-mail-send-default:14.10" }, resolveExtraJARs = true)
class AllITs
{
    @Nested
    @DisplayName("Overall Calendar UI")
    class NestedCalendarIT extends CalendarIT
    {
    }
}
