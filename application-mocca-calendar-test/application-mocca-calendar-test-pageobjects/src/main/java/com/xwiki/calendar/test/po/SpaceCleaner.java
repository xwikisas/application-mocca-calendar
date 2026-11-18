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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.ui.TestUtils;

/**
 * Removes the pages left by a previous run of the tests on the same XWiki instance.
 *
 * @version $Id$
 */
public final class SpaceCleaner
{
    private SpaceCleaner()
    {
    }

    /**
     * Deletes a space with all its pages and nested spaces, and waits for the deletion to finish.
     * <p>
     * {@link TestUtils#deleteSpace(EntityReference)} can't be used: up to XWiki 15.10 at least, it passes the page name
     * and the action to the wrong {@code getURL} method, so it opens a URL that deletes nothing.
     *
     * @param setup the test utilities
     * @param space the space to delete; nothing happens if it doesn't exist
     */
    public static void deleteSpace(TestUtils setup, EntityReference space)
    {
        // The deletion is refused, silently, if the CSRF token cached by the test utilities is not the one of the
        // current user.
        setup.recacheSecretToken();
        DocumentReference spaceHome = new DocumentReference("WebHome", new SpaceReference(space));
        setup.gotoPage(setup.getURL(spaceHome, "deletespace", "confirm=1&async=false"));
    }
}
