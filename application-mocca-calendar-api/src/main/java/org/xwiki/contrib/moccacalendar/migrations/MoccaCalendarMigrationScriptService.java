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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.QueryException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Script service to check for successful migration and add helpers in case something went wrong.
 *
 * @version $Id: $
 * @since 2.7
 */
@Named("moccacalendarmigration")
@Singleton
@Component
public class MoccaCalendarMigrationScriptService implements ScriptService
{
    /**
     * The maximal number of events that can be migrated in one call, matching the largest choice of the migration page.
     */
    private static final int MAX_LIMIT = 10000;

    private static final String ERROR_MESSAGE = "errorMessage";

    @Inject
    @Named("org.xwiki.contrib.moccacalendar.migrations.AddReccurrentProperty")
    private AddReccurrentProperty recurrentMigrator;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    /**
     * count the number of unmigrated events from the recurrent event feature.
     *
     * @return the number of event pages that have not been migrated, or -1 if they could not be counted or if the
     *         current user is not an administrator of the current wiki
     */
    public long countUnmigratedEventsForRecurrency()
    {
        if (!hasWikiAdminRight()) {
            return -1;
        }

        try {
            long allEvents = recurrentMigrator.countAllEvents();
            long migratedEvents = recurrentMigrator.countMigratedEvents();

            return allEvents - migratedEvents;
        } catch (QueryException e) {
            logger.warn("could not execute queries to count unmigrated events", e);
            return -1;
        }
    }

    /**
     * Migrate some events, as limited by the parameters.
     * @param offset where to begin to migrate
     * @param limit the maximal number of events to migrate at once
     * @return a map with two keys:
     *    &quot;migrationCount&quot; contains the number of (possibly) migrated events
     *    and &quot;errorMessage&quot; contains the message if an error happened, including when the current user is
     *    not an administrator of the current wiki or when the limit is out of bounds
     */
    public Map<String, Object> migrateEventsForRecurrenty(final int offset, final int limit)
    {
        Map<String, Object> results = new HashMap<String, Object>();

        if (!hasWikiAdminRight()) {
            results.put(ERROR_MESSAGE, "Only wiki administrators can migrate events.");
            return results;
        }
        if (offset < 0 || limit <= 0 || limit > MAX_LIMIT) {
            results.put(ERROR_MESSAGE, String.format("The offset must be non-negative and the limit between 1 and %d.",
                MAX_LIMIT));
            return results;
        }

        try {
            int count = recurrentMigrator.addRecurrentPropertyToEvents(offset, limit);
            results.put("migrationCount", count);
        } catch (QueryException | XWikiException e) {
            logger.error("failure to migrate events", e);
            results.put(ERROR_MESSAGE, e.getLocalizedMessage());
        }

        return results;
    }

    private boolean hasWikiAdminRight()
    {
        return this.authorization.hasAccess(Right.ADMIN, this.xcontextProvider.get().getWikiReference());
    }

}
