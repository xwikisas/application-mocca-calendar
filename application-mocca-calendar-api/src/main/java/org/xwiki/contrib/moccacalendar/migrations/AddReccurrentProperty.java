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
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.IntegerProperty;

/**
 * Migration helper which adds a value of zero to the recurrent property of events.
 */
@Component
@Named("org.xwiki.contrib.moccacalendar.migrations.AddReccurrentProperty")
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
        // TODO stub
        if (event instanceof ApplicationReadyEvent) {
            String wikiId = wikiManager.getCurrentWikiId();
            try {
                logger.info("migrate existing calendar events for wiki [{}]", wikiId);
                addRecurrentPropertyToEvents(wikiId);
            } catch (Exception e) {
                this.logger.error("Error while migrating calendar events in wiki [{}].", wikiId, e);
            }
        } else if (event instanceof WikiReadyEvent) {
            final String wikiId = ((WikiReadyEvent) event).getWikiId();
            try {
                logger.info("migrate existing calendar events for wiki is [{}]", wikiId);
                addRecurrentPropertyToEvents(wikiId);
            } catch (Exception e) {
                this.logger.error("Error while migrating calendar events in wiki [{}].", wikiId, e);
            }
        } else if (event instanceof ExtensionUpgradedEvent) {
            ExtensionUpgradedEvent xie = (ExtensionUpgradedEvent) event;

            ExtensionId extensionId = xie.getExtensionId();
            final String namespace = xie.getNamespace();

            if ("org.xwiki.contrib:application-mocca-calendar-api".equals(extensionId.getId())) {
                logger.error(
                    "################### API UPGRADED ! in version [{}] on namespace [{}] - event source [{}] and data [{}]",
                    extensionId.getVersion(), namespace, source, data);
            }
            if ("org.xwiki.contrib:application-mocca-calendar-ui".equals(extensionId.getId())) {
                logger.error(
                    "################### UHI UPGRADED ! in version [{}] on namespace [{}] - event source [{}] and data [{}]",
                    extensionId.getVersion(), namespace, source, data);
                // XXX parse namespace? Huh?
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void addRecurrentPropertyToEvents(String wikiId) throws QueryException, XWikiException
    {
        final XWikiContext xcontext = this.xcontextProvider.get();
        final String currentWikiId = xcontext.getWikiId();

        try {
            xcontext.setWikiId(wikiId);

            // we cannot query for events with a missing property;
            // so instead we have to query all events and check each of them

            Query query = queryManager.createQuery(
                String.format("from doc.object(%s) as event", EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME),
                Query.XWQL);
            List<String> results = query.execute();
            for (String docName : results) {
                XWikiDocument doc = xcontext.getWiki().getDocument(docName, xcontext);

                List<BaseObject> events = doc.getObjects(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME);

                boolean modified = false;
                for (BaseObject event : events) {
                    if (!event.getPropertyList().contains(EventConstants.PROPERTY_RECURRENT_NAME)) {
                        IntegerProperty recurrent = new IntegerProperty();
                        recurrent.setValue(0);
                        event.safeput(EventConstants.PROPERTY_RECURRENT_NAME, recurrent);
                        modified = true;
                    }
                }

                if (modified) {
                    xcontext.getWiki().saveDocument(doc, "add recurrent=false to event", true, xcontext);
                    logger.debug("migrated event [{}]", doc.getPrefixedFullName());
                }
            }

        } finally {
            xcontext.setWikiId(currentWikiId);
        }
    }

}
