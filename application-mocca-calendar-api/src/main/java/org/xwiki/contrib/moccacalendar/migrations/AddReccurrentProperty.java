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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.UniqueDocumentFilter;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.IntegerProperty;

/**
 * Migration helper which adds a value of zero to the recurrent property of events.
 *
 * @version $Id: $
 * @since 2.7
 */
@Named("org.xwiki.contrib.moccacalendar.migrations.AddReccurrentProperty")
@Singleton
@Component(roles = {AddReccurrentProperty.class, EventListener.class})
public class AddReccurrentProperty implements EventListener
{

    /**
     * The events observed by this event listener.
     */
    private static final List<Event> EVENTS = Arrays.asList(new ExtensionUpgradedEvent());

    /**
     * The events observed by this event listener in "recovery" mode.
     */
    private static final List<Event> EVENTS_STARTUP = Arrays.asList(new ApplicationReadyEvent(),
        new WikiReadyEvent(), new ExtensionUpgradedEvent());

    @Inject
    private WikiDescriptorManager wikiManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named(UniqueDocumentFilter.HINT)
    private QueryFilter uniqueResults;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        if (System.getProperty("moccacalendar.migrate.events") != null) {
            return EVENTS_STARTUP;
        }
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return getClass().getName();
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ApplicationReadyEvent) {
            addRecurrentPropertyToEvents(wikiManager.getCurrentWikiId());
        } else if (event instanceof WikiReadyEvent) {
            addRecurrentPropertyToEvents(((WikiReadyEvent) event).getWikiId());
        } else if (event instanceof ExtensionUpgradedEvent) {
            ExtensionUpgradedEvent xie = (ExtensionUpgradedEvent) event;

            ExtensionId extensionId = xie.getExtensionId();
            final String namespace = xie.getNamespace();
            if ("org.xwiki.contrib:application-mocca-calendar-ui".equals(extensionId.getId())) {
                if (namespace == null || "".equals(namespace)) {
                    // upgrade on all wikis
                    try {
                        for (String wikiId : wikiManager.getAllIds()) {
                            addRecurrentPropertyToEvents(wikiId);
                        }
                    } catch (WikiManagerException e) {
                        logger.error("failed to migrate events", e);
                    }
                } else if (namespace.startsWith("wiki:")) {
                    // single wiki
                    String wikiId = namespace.substring(5);
                    addRecurrentPropertyToEvents(wikiId);
                } else {
                    logger.error("unknown installation namespace [{}]); skip migration step", namespace);
                }
            }
        }
    }


    /**
     * helper to find out if events are unmigrated.
     * @return the number of all events
     * @throws QueryException if the query fails
     */
    public long countAllEvents() throws QueryException
    {
        Query query = allEventsQuery();
        query.addFilter(uniqueResults).addFilter(countFilter);
        long result = (Long) query.execute().get(0);
        return result;
    }

    /**
     * helper to find out if events are unmigrated.
     * @return the number of all migrated events
     * @throws QueryException if the query fails
     */
    public long countMigratedEvents() throws QueryException
    {
        Query query = allMigratedEventsQuery();
        query.addFilter(uniqueResults).addFilter(countFilter);
        long result = (Long) query.execute().get(0);
        return result;
    }

    /**
     * Migrate a number of events as given by the parameters.
     * Actually first loads each event and checks if it already has
     * a &quot;recurrent&quot; property; if not, then adds it with a value of 0.
     * @param offset where it start the migration
     * @param limit the maximal number of events
     * @return the number of events checked
     * @throws QueryException if fetching the events failed
     * @throws XWikiException if migration on of the events failed
     */
    public int addRecurrentPropertyToEvents(int offset, int limit) throws QueryException, XWikiException
    {
        final XWikiContext xcontext = this.xcontextProvider.get();
        Query query = allEventsQuery();
        query.setOffset(offset).setLimit(limit);

        List<String> results = query.execute();
        for (String docName : results) {
            if (logger.isTraceEnabled()) {
                logger.trace("try to migrate event document [{}]", docName);
            }
            addRecurrentPropertyToDocument(xcontext, docName);
        }
        return results.size();
    }

    private void addRecurrentPropertyToEvents(String wikiId)
    {
        final XWikiContext xcontext = this.xcontextProvider.get();
        final String currentWikiId = xcontext.getWikiId();

        try {
            logger.info("migrate existing calendar events for wiki [{}]", wikiId);

            try {
                xcontext.setWikiId(wikiId);

                // we cannot query for events with a missing property;
                // so instead we have to query all events and check each of them
                Query query = allEventsQuery();
                List<String> results = query.execute();
                int step = Math.max(results.size() / 100 * 10, 10);
                int counter = 0;
                for (String docName : results) {
                    addRecurrentPropertyToDocument(xcontext, docName);
                    counter++;
                    if (counter % step == 0) {
                        logger.info("migrated {} events on wiki [{}]", counter, wikiId);
                    }
                }
                logger.info("migrated all events on wiki [{}]", wikiId);

            } finally {
                xcontext.setWikiId(currentWikiId);
            }
        } catch (Exception e) {
            this.logger.error("Error while migrating calendar events in wiki [{}].", wikiId, e);
        }

    }

    private Query allEventsQuery() throws QueryException
    {
        Query query = queryManager.createQuery(
            String.format("from doc.object(%s) as event order by doc.creationDate",
                EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME),
            Query.XWQL);
        return query;
    }

    private Query allMigratedEventsQuery() throws QueryException
    {
        Query query = queryManager.createQuery(
            String.format("from doc.object(%s) as event where not event.recurrent = -1 order by doc.creationDate",
                EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME),
            Query.XWQL);
        return query;
    }

    @SuppressWarnings("deprecation")
    private void addRecurrentPropertyToDocument(final XWikiContext xcontext, String docName) throws XWikiException
    {
        XWikiDocument doc = xcontext.getWiki().getDocument(docName, xcontext);

        List<BaseObject> events = doc.getObjects(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME);

        boolean modified = false;
        for (BaseObject event : events) {
            boolean hasRecurrentProperty = event.getPropertyList().contains(EventConstants.PROPERTY_RECURRENT_NAME)
                && event.getField(EventConstants.PROPERTY_RECURRENT_NAME) != null;
            if (hasRecurrentProperty) {
                // since XWiki 11.2(?) properties are auto-added even when not in the database
                // we need to check for an empty value instead
                String recurStringVal = event.getStringValue(EventConstants.PROPERTY_RECURRENT_NAME);
                hasRecurrentProperty = recurStringVal != null && !"".equals(recurStringVal);
            }
            if (!hasRecurrentProperty) {
                IntegerProperty recurrent = new IntegerProperty();
                recurrent.setValue(0);
                event.safeput(EventConstants.PROPERTY_RECURRENT_NAME, recurrent);
                modified = true;
                if (logger.isTraceEnabled()) {
                    logger.trace("added the property 'recurrent' to doc {}", docName);
                }
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("doc {} already has a property 'recurrent' with prop {} and value [{}]",
                        docName, event.getField(EventConstants.PROPERTY_RECURRENT_NAME),
                        event.getStringValue(EventConstants.PROPERTY_RECURRENT_NAME));
                }
            }
        }

        if (modified) {
            xcontext.getWiki().saveDocument(doc, "add recurrent=false to event", true, xcontext);
            if (logger.isTraceEnabled()) {
                logger.trace("migrated event [{}]", doc.getPrefixedFullName());
            }
        }
    }

}
