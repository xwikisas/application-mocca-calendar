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
package org.xwiki.contrib.moccacalendar.migrations;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MoccaCalendarMigrationScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class MoccaCalendarMigrationScriptServiceTest
{
    private static final WikiReference WIKI = new WikiReference("wiki");

    private static final String ERROR_MESSAGE = "errorMessage";

    @InjectMockComponents
    private MoccaCalendarMigrationScriptService scriptService;

    @MockComponent
    @Named("org.xwiki.contrib.moccacalendar.migrations.AddReccurrentProperty")
    private AddReccurrentProperty recurrentMigrator;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @BeforeEach
    void setUp()
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWikiReference()).thenReturn(WIKI);
    }

    @Test
    void countAndMigrateRequireWikiAdmin()
    {
        when(this.authorization.hasAccess(Right.ADMIN, WIKI)).thenReturn(false);

        assertEquals(-1, this.scriptService.countUnmigratedEventsForRecurrency());
        assertNotNull(this.scriptService.migrateEventsForRecurrenty(0, 10).get(ERROR_MESSAGE));
        verifyNoInteractions(this.recurrentMigrator);
    }

    @Test
    void migrateRejectsTooLargeLimit() throws Exception
    {
        when(this.authorization.hasAccess(Right.ADMIN, WIKI)).thenReturn(true);

        assertNotNull(this.scriptService.migrateEventsForRecurrenty(0, 100000).get(ERROR_MESSAGE));
        verify(this.recurrentMigrator, never()).addRecurrentPropertyToEvents(anyInt(), anyInt());
    }

    @Test
    void migrateAsWikiAdmin() throws Exception
    {
        when(this.authorization.hasAccess(Right.ADMIN, WIKI)).thenReturn(true);
        when(this.recurrentMigrator.addRecurrentPropertyToEvents(0, 1000)).thenReturn(3);

        Map<String, Object> result = this.scriptService.migrateEventsForRecurrenty(0, 1000);

        assertEquals(3, result.get("migrationCount"));
    }

    @Test
    void migrateRejectsNegativeOffset() throws Exception
    {
        when(this.authorization.hasAccess(Right.ADMIN, WIKI)).thenReturn(true);

        assertNotNull(this.scriptService.migrateEventsForRecurrenty(-1, 10).get(ERROR_MESSAGE));
        verify(this.recurrentMigrator, never()).addRecurrentPropertyToEvents(anyInt(), anyInt());
    }

    @Test
    void migrateRejectsZeroLimit() throws Exception
    {
        when(this.authorization.hasAccess(Right.ADMIN, WIKI)).thenReturn(true);

        assertNotNull(this.scriptService.migrateEventsForRecurrenty(0, 0).get(ERROR_MESSAGE));
        verify(this.recurrentMigrator, never()).addRecurrentPropertyToEvents(anyInt(), anyInt());
    }

    @Test
    void countAsWikiAdmin() throws Exception
    {
        when(this.authorization.hasAccess(Right.ADMIN, WIKI)).thenReturn(true);
        when(this.recurrentMigrator.countAllEvents()).thenReturn(5L);
        when(this.recurrentMigrator.countMigratedEvents()).thenReturn(2L);

        assertEquals(3, this.scriptService.countUnmigratedEventsForRecurrency());
    }
}
