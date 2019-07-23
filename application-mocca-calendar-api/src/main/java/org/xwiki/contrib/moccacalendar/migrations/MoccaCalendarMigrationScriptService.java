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
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.QueryException;
import org.xwiki.script.service.ScriptService;

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

    @Inject
    @Named("org.xwiki.contrib.moccacalendar.migrations.AddReccurrentProperty")
    private AddReccurrentProperty recurrentMigrator;

    @Inject
    private Logger logger;

    /**
     * count the number of unmigrated events from the recurrent event feature.
     *
     * @return the number of event pages that have not been migrated.
     */
    public long countUnmigratedEventsForRecurrency()
    {

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
     *    and &quot;errorMessage&quot; contains the message if an error happened
     */
    public Map<String, Object> migrateEventsForRecurrenty(final int offset, final int limit)
    {
        Map<String, Object> results = new HashMap<String, Object>();

        try {
            int count = recurrentMigrator.addRecurrentPropertyToEvents(offset, limit);
            results.put("migrationCount", count);
        } catch (QueryException | XWikiException e) {
            logger.error("failure to migrate events", e);
            results.put("errorMessage", e.getLocalizedMessage());
        }

        return results;
    }

}
