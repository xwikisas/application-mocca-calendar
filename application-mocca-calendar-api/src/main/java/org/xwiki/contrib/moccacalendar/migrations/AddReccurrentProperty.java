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
import org.xwiki.bridge.event.AbstractWikiEvent;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.EndFoldEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.RegexEventFilter;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.UniqueDocumentFilter;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroFactory;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;
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

    private static final EventFilter OLD_MACRO_LOCATION =
        new RegexEventFilter("[^:]*:MoccaCalendar.Macro");

    /**
     * The events observed by this event listener.
     */
    private static final List<Event> EVENTS = Arrays.asList(new ExtensionUpgradedEvent(),
        new DocumentDeletedEvent(OLD_MACRO_LOCATION));

    /**
     * The events observed by this event listener in "recovery" mode.
     */
    private static final List<Event> EVENTS_STARTUP = Arrays.asList(new ApplicationReadyEvent(),
        new WikiReadyEvent(), new ExtensionUpgradedEvent(), new DocumentDeletedEvent(OLD_MACRO_LOCATION));

    @Inject
    private WikiDescriptorManager wikiManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private WikiMacroFactory macroFactory;

    @Inject
    private WikiMacroManager macroManager;

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

    private static class BeginMigrationEvent extends AbstractWikiEvent implements BeginFoldEvent
    {
        private static final long serialVersionUID = 23L;
    }

    private static class FinishMigrationEvent extends AbstractWikiEvent implements EndFoldEvent
    {
        private static final long serialVersionUID = 42L;
    }

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
        logger.info("handling event [{}]", event);
        if (event instanceof ApplicationReadyEvent) {
            addRecurrentPropertyToEvents(wikiManager.getCurrentWikiId());
        } else if (event instanceof WikiReadyEvent) {
            addRecurrentPropertyToEvents(((WikiReadyEvent) event).getWikiId());
        } else if (event instanceof DocumentDeletedEvent) {
            registerCalendarMacroAtNewLocation((XWikiDocument) source);
        } else if (event instanceof ExtensionUpgradedEvent) {
            addRecurrentPropertyToEvents((ExtensionUpgradedEvent) event);
        } else {
            logger.warn("ignored event [{}] which we listened for", event);
        }
    }


    private void registerCalendarMacroAtNewLocation(XWikiDocument oldMacroDoc)
    {
        try {
            final XWikiContext context = xcontextProvider.get();

            // first the listener unregistering the macro in the old place might not have been called
            // so we do this first
            DocumentReference oldMacroDocRef = oldMacroDoc.getDocumentReference();
            if (macroManager.hasWikiMacro(oldMacroDocRef)) {
                logger.debug("unregister moccacalendar macro in its old place");
                macroManager.unregisterWikiMacro(oldMacroDocRef);
            }

            DocumentReference newMacroDocRef = new DocumentReference("Macro",
                new SpaceReference(context.getWikiId(), "MoccaCalendar", "Code"));
            XWikiDocument newMacroDoc = context.getWiki().getDocument(newMacroDocRef, context);
            if (newMacroDoc.isNew()) {
                logger.warn("could not find moccacalendar macro in its new place");
            } else {
                WikiMacro macro = macroFactory.createWikiMacro(newMacroDocRef);
                logger.debug("found [{}] in [{}]", macro.getDescriptor().getId(), newMacroDocRef);
                macroManager.registerWikiMacro(newMacroDocRef, macro);
                logger.info("registered moccacalendar macro in its new place");
            }
        } catch (Exception xe) {
            logger.warn("could not register new moccacalendar macro automatically", xe);
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
        Query query = allEventsQuery();
        query.setOffset(offset).setLimit(limit);

        return addRecurrentPropertyToEvents(query, null, false);
    }

    private void addRecurrentPropertyToEvents(ExtensionUpgradedEvent xie)
    {
        ExtensionId extensionId = xie.getExtensionId();
        final String namespace = xie.getNamespace();
        if ("com.xwiki.mocca-calendar:application-mocca-calendar-ui".equals(extensionId.getId())) {
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
                addRecurrentPropertyToEvents(query, wikiId, true);
            } finally {
                xcontext.setWikiId(currentWikiId);
            }
        } catch (Exception e) {
            this.logger.error("Error while migrating calendar events in wiki [{}].", wikiId, e);
        }
    }

    private int addRecurrentPropertyToEvents(final Query eventQuery, final String wikiIdOrNull,
        final boolean logProgress) throws QueryException, XWikiException
    {
        final XWikiContext xcontext = this.xcontextProvider.get();
        final String wikiId = (wikiIdOrNull == null) ? xcontext.getWikiId() : wikiIdOrNull;
        final List<String> results = eventQuery.execute();
        final int step = Math.max(results.size() / 100 * 10, 10);
        int counter = 0;

        try {
            observationManager.notify(new BeginMigrationEvent(), null);
            for (String docName : results) {
                addRecurrentPropertyToDocument(xcontext, docName);
                counter++;
                if (logProgress && counter % step == 0) {
                    logger.info("migrated {} events on wiki [{}]", counter, wikiId);
                }
            }
            if (logProgress) {
                logger.info("migrated all events on wiki [{}]", wikiId);
            }
        } finally {
            observationManager.notify(new FinishMigrationEvent(), null);
        }
        return counter;
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
