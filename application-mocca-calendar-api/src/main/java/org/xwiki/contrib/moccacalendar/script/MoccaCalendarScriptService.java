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
package org.xwiki.contrib.moccacalendar.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.EventInstance;
import org.xwiki.contrib.moccacalendar.EventSource;
import org.xwiki.contrib.moccacalendar.RecurrentEventGenerator;
import org.xwiki.contrib.moccacalendar.internal.AbstractSourceConfigurationClassInitializer;
import org.xwiki.contrib.moccacalendar.internal.DefaultSourceConfigurationClassInitializer;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.contrib.moccacalendar.internal.Utils;
import org.xwiki.contrib.moccacalendar.internal.utils.DefaultEventAssembly;
import org.xwiki.contrib.moccacalendar.internal.utils.EventQuery;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Script service to obtain information about calendars and event instances.
 *
 * @version $Id: $
 * @since 2.7
 */
@Named("moccacalendar")
@Singleton
@Component
public class MoccaCalendarScriptService implements ScriptService
{
    private static final String CALENDAR_BASE_QUERY = ", BaseObject as obj"
        + " where doc.fullName=obj.name and doc.name!='MoccaCalendarTemplate'" + " and obj.className='"
        + EventConstants.MOCCA_CALENDAR_CLASS_NAME + "' order by doc.title, doc.name";
    private static final String MOCCA_CALENDAR_EVENT_TEMPLATE = "MoccaCalendar.MoccaCalendarEventTemplate";

    private static final LocalDocumentReference GLOBAL_SETTINGS_PAGE = new LocalDocumentReference(
        Arrays.asList("MoccaCalendar", "Code"), "GlobalSettings");

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> stringDocRefResolver;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactWikiSerializer;
    // other code uses @Named("local") - which omits wiki name ?

    @Inject
    private QueryManager queryManager;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    @Named("hidden")
    private QueryFilter hidden;

    @Inject
    @Named("document")
    private QueryFilter documentFilter;

    @Inject
    @Named("viewable")
    private QueryFilter viewableFilter;

    @Inject
    private Map<String, RecurrentEventGenerator> eventGenerators;

    @Inject
    private Map<String, EventSource> eventSources;

    @Inject
    private DefaultEventAssembly eventAssembly;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;
    
    @Inject
    private Logger logger;

    /**
     * Get all calendars.
     *
     * @return a list of document references pointing to pages containing calendar objects.
     */
    public List<DocumentReference> getAllCalendars()
    {
        List<DocumentReference> calenderRefs = Collections.emptyList();

        try {
            Query query = queryManager.createQuery(CALENDAR_BASE_QUERY, Query.HQL).addFilter(hidden);
            query.addFilter(documentFilter);
            query.addFilter(viewableFilter);
            calenderRefs = query.execute();
        } catch (QueryException qe) {
            logger.error("error while fetching calendars", qe);
        }

        return calenderRefs;
    }

    /**
     * Queries events on the current wiki, see {@link #queryEvents(Date, Date, String, String, String, boolean)}.
     */
    public List<EventInstance> queryEvents(Date dateFrom, Date dateTo, String filter, String parentReference,
        boolean sortAscending) throws QueryException
    {
        final XWikiContext context = xcontextProvider.get();
        return queryEvents(dateFrom, dateTo, filter, context.getWikiId(), parentReference, sortAscending);
    }

    /**
     * Get a list of events matching the date and filter criteria.
     *
     * @param dateFrom
     *            the start range
     * @param dateTo
     *            the end range; can be null. in that case dates form a single day are returned
     * @param filter
     *            how to filter the event. if null or "wiki" return all events
     * @param parentReference
     *            the page reference to use for the filter. can be null if filter is null or "wiki".
     * @param sortAscending
     *            if true, sort events ascending by start date, else descending
     * @return a list of event instances matching the criteria; might be empty but never null
     * @throws QueryException
     *             if an error occurs while fetching the events
     */
    public List<EventInstance> queryEvents(Date dateFrom, Date dateTo, String filter, String wiki,
        String parentReference, boolean sortAscending) throws QueryException
    {

        final XWikiContext context = xcontextProvider.get();

        if (dateTo == null) {
            dateTo = dateFrom;
        }

        EventQuery eventQuery =
            new EventQuery(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME, MOCCA_CALENDAR_EVENT_TEMPLATE, wiki);

        //
        // filter by date range
        //
        eventQuery.addDateLimits(dateFrom, dateTo);

        // and search only non-recurrent events
        // FIXME: we need another helper for this. Why not using XWQL instead?
        // some issue with the date filter? is this still open?
        eventQuery.addObjectProperty("IntegerProperty", "recurrent")
            .addCondition(" and recurrent.value = 0 ");

        //
        // now filter by event location
        //
        DocumentReference parentRef = (parentReference == null) ? null
            : stringDocRefResolver.resolve(parentReference);

        eventQuery.addLocationFilter(filter, parentRef);

        // finally the ordering
        eventQuery.setAscending(sortAscending);

        List<DocumentReference> visibleEvents = Collections.emptyList();

        try {
            visibleEvents = eventAssembly.executeQuery(eventQuery);
        } catch (QueryException qe) {
            logger.error("error while fetching regular events", qe);
        }

        List<EventInstance> events = new ArrayList<>();

        for (DocumentReference eventDocRef : visibleEvents) {
            try {
                XWikiDocument eventDoc = context.getWiki().getDocument(eventDocRef, context);
                BaseObject eventData = eventDoc
                    .getXObject(eventDoc.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME));
                if (eventData == null) {
                    logger.error("data inconsistency: query returned [{}] which contains no object for [{}]",
                        eventDocRef, EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME);
                    continue;
                }

                EventInstance event = new EventInstance();

                Date startDate = eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME);
                DateTime startDateTime = new DateTime(startDate.getTime());
                event.setStartDate(startDateTime);

                Date endDate = Utils.fetchOrGuessEndDate(eventData);
                DateTime endDateTime = new DateTime(endDate.getTime());
                event.setEndDate(endDateTime);

                completeEventData(event, eventDoc, eventData);

                events.add(event);
            } catch (XWikiException e) {
                logger.warn("cannot find event data [{}]", eventDocRef, e);
            }
        }

        //
        // so much for regular single events.
        // now about recurrent events
        //
        EventQuery recurrentEventQuery = new EventQuery(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME,
            MOCCA_CALENDAR_EVENT_TEMPLATE, wiki);

        //
        // filter by location
        //
        recurrentEventQuery.addLocationFilter(filter, parentRef);

        // and search only recurrent events
        recurrentEventQuery.addObjectProperty("IntegerProperty", "recurrent")
            .addCondition(" and recurrent.value = 1 ");

        try {
            List<DocumentReference> visibleRecurrentEventPages = eventAssembly
                .executeQuery(recurrentEventQuery);

            List<EventInstance> recurrentEventInstances = filterRecurrentEvents(visibleRecurrentEventPages,
                dateFrom, dateTo);

            events.addAll(recurrentEventInstances);

        } catch (QueryException | XWikiException e) {
            logger.error("error while fetching recurrent events", e);
        }

        for (Map.Entry<String, EventSource> meetings : eventSources.entrySet()) {
            if (!sourceIsActive(meetings, filter, parentRef)) {
                continue;
            }
            logger.debug("add events from [{}] source", meetings.getKey());
            // In case the filter is "wiki" and "parentRef" is null, we create one pointing at the wiki home
            // in order to be able to retrieve the target wiki reference when retrieving the events in MeetingEventSource
            if ("wiki".equals(filter) && parentRef == null) {
                try {
                    WikiDescriptor wikiDescriptor = wikiDescriptorManager.getById(wiki);
                    parentRef = wikiDescriptor.getMainPageReference();
                } catch (WikiManagerException e) {
                    logger.error("Could not retrieve wiki descriptor for [{}]", wiki, e);
                }
            }
            List<EventInstance> meetingEvents = meetings.getValue().getEvents(dateFrom, dateTo, filter,
                parentRef, sortAscending);
            if (meetingEvents != null) {
                for (EventInstance meeting : meetingEvents) {
                    fillInColorsFromNearestCalendar(meeting);
                    meeting.setSource(meetings.getKey());
                }
                events.addAll(meetingEvents);
            }
        }

        sortEvents(events, sortAscending);

        return events;
    }

    /**
     * Gets the union of events on a set of wikis.
     *
     * @param dateFrom the range start
     * @param dateTo the range end; can be null. in that case dates from a single day are returned
     * @param wikis list of wiki identifiers where events should be searched for
     * @param sortAscending if true, sort events ascending by start date, else descending
     * @return a list of event instances matching the criteria; might be empty but never null
     * @throws QueryException
     */
    public List<EventInstance> queryEvents(Date dateFrom, Date dateTo, List<String> wikis, boolean sortAscending)
        throws QueryException
    {
        List<EventInstance> events = new ArrayList<>();
        if (wikis != null) {
            for (String wiki : wikis) {
                events.addAll(queryEvents(dateFrom, dateTo, "wiki", wiki, null, sortAscending));
            }
            // Sort events globally
            sortEvents(events, sortAscending);
        }
        return events;
    }

    private boolean sourceIsActive(Entry<String, EventSource> meetings, String filter,
        DocumentReference parentRef)
    {
        String name = meetings.getKey();
        EventSource source = meetings.getValue();
        logger.debug("check if source [{}] is active", name);

        if (!source.isAvailable()) {
            logger.debug("source [{}] is unvailable", name);
            return false;
        }
        // name should never be null, except for "default sources", which are always enabled
        if (name == null) {
            return true;
        }

        if (!isGloballyEnabled(name)) {
            logger.debug("source [{}] is globally disabled", name);
            return false;
        }

        logger.debug("source [{}] is globally enabled", name);
        // no local checks for global filter
        if (filter == null || "wiki".equals(filter)) {
            return true;
        }

        boolean locallyEnabled;
        try {
            locallyEnabled = isLocallyEnabled(name, source, parentRef);
            logger.debug("is source [{}] locally enabled: [{}]", name, locallyEnabled);
        } catch (XWikiException e) {
            locallyEnabled = false;
            logger.warn("could not determine if source [{}] is active", name, e);
        }
        return locallyEnabled;
    }

    private boolean isGloballyEnabled(String sourceName)
    {
        XWikiContext context = xcontextProvider.get();
        LocalDocumentReference defaultConfigClass = DefaultSourceConfigurationClassInitializer
            .getConfigurationClass();
        boolean result = false;
        try {
            XWikiDocument globalPrefs = context.getWiki().getDocument(GLOBAL_SETTINGS_PAGE, context);
            BaseObject configObj = globalPrefs.getXObject(
                new DocumentReference(defaultConfigClass, new WikiReference(context.getWikiId())),
                DefaultSourceConfigurationClassInitializer.SOURCE_NAME_FIELD, sourceName);
            result = isActive(configObj);
        } catch (XWikiException e) {
            logger.warn("cannot load global calendar source settings", e);
        }
        return result;
    }

    private boolean isLocallyEnabled(String name, EventSource source, DocumentReference parentRef)
        throws XWikiException
    {
        XWikiContext context = xcontextProvider.get();
        LocalDocumentReference defaultConfigClass = DefaultSourceConfigurationClassInitializer
            .getConfigurationClass();
        LocalDocumentReference configClass = source.getConfigurationClass();
        // XWikiDocument configDoc = context.getDoc();
        XWikiDocument configDoc = context.getWiki().getDocument(parentRef, context);
        logger.trace("try config class [{}] in document [{}] for source [{}]", configClass, configDoc, name);
        BaseObject configObj;
        if (configClass != null) {
            configObj = configDoc.getXObject(configClass);
        } else {
            configObj = configDoc.getXObject(
                new DocumentReference(defaultConfigClass, new WikiReference(context.getWikiId())),
                DefaultSourceConfigurationClassInitializer.SOURCE_NAME_FIELD, name);
        }
        return isActive(configObj);
    }

    private boolean isActive(BaseObject configObject)
    {
        logger.trace("check config object [{}] from [{}] for active flag", configObject,
            (configObject != null) ? configObject.getReference() : "n/a");
        return (configObject != null
            && configObject.getIntValue(AbstractSourceConfigurationClassInitializer.ACTIVE_FIELD) == 1);
    }

    /**
     * Give the full name to a document to be used as a sheet to be used to display this event. If the event needs no
     * special sheet, return null.
     *
     * @param event
     *            the event instance to be displayed
     * @return the full name of a document sheet or null
     */
    public String getDisplaySheetForEvent(EventInstance event)
    {
        if (event == null || event.getSource() == null) {
            return null;
        }
        // now we should ask the source of the event for a display sheet
        // instead we directly fall back on the generic view sheet
        return "MoccaCalendar.Code.EventViews.Generic";
    }

    /**
     * Returns a list of all available event source names, except for the default ones.
     *
     * @return a list of strings, not null
     */
    public List<String> getAvailableSources()
    {
        return eventSources.keySet().stream().filter((String name) -> {
            logger.trace("check availability of source [{}]", name);
            return eventSources.get(name).isAvailable();
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Returns a map of all available event source names to their configuration classes.
     *
     * @return a map of strings to class references, not null
     */
    public Map<String, LocalDocumentReference> getAvailableSourceConfigurations()
    {
        Map<String, LocalDocumentReference> sources = new HashMap<>();

        for (Map.Entry<String, EventSource> source : eventSources.entrySet()) {
            if (!source.getValue().isAvailable()) {
                continue;
            }
            LocalDocumentReference configClass = source.getValue().getConfigurationClass();
            if (configClass == null) {
                configClass = DefaultSourceConfigurationClassInitializer.getConfigurationClass();
            }
            sources.put(source.getKey(), configClass);
        }
        return sources;
    }

    private List<EventInstance> filterRecurrentEvents(List<DocumentReference> eventReferences, Date dateFrom,
        Date dateTo) throws XWikiException
    {
        final XWikiContext context = xcontextProvider.get();
        final List<EventInstance> eventsInstances = new ArrayList<>();
        for (DocumentReference eventDocRef : eventReferences) {
            // XXX: similar, but different code from non-recurrent
            XWikiDocument eventDoc = context.getWiki().getDocument(eventDocRef, context);
            BaseObject eventData = eventDoc
                .getXObject(eventDoc.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME));
            BaseObject eventRecData = eventDoc.getXObject(
                eventDoc.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME));

            if (eventRecData == null) {
                // duh
                logger.info("found recurrent event [{}] without recurrency information; skipping",
                    eventDocRef);
                continue;
            }

            String eventType = eventRecData.getStringValue("frequency");
            RecurrentEventGenerator generator = this.eventGenerators.get(eventType);
            if (generator == null) {
                logger.error("no recurrent event generator found for frequency [{}] used by [{}]", eventType,
                    eventDocRef);
                continue;
            }

            Set<Long> deletions = deletedEventsOf(eventDoc);
            Map<Long, EventInstance> modifiedEvents = modifiedEventsOf(eventDoc, dateFrom, dateTo);

            for (EventInstance event : generator.generate(eventDoc, dateFrom, dateTo)) {
                if (deletions.contains(event.getStartDate().getMillis())) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("skip deleted event at {} for doc [{}])", event.getStartDate(),
                            eventDoc);
                    }
                    continue;
                }

                EventInstance modifiedEvent = modifiedEvents.remove(event.getStartDate().getMillis());
                if (modifiedEvent != null) {
                    event = modifiedEvent;
                }

                // add extra stuff here that the generator does not have to set
                event.setRecurrent(true);

                completeEventData(event, eventDoc, eventData);
                eventsInstances.add(event);
            }

            // TODO: what happens with modified events where the original event is not
            // in our time range, but the modified one is?
            if (!modifiedEvents.isEmpty()) {
                logger.info("we dropped some modifications: [{}]", modifiedEvents.size());
                if (logger.isDebugEnabled()) {
                    for (Map.Entry<Long, EventInstance> modification : modifiedEvents.entrySet()) {
                        logger.debug("event originally started at [{}]", new DateTime(modification.getKey()));
                        EventInstance modifiedEvent = modification.getValue();
                        logger.debug("  event start at [{}]", modifiedEvent.getStartDate());
                        logger.debug("  event end at [{}]", modifiedEvent.getEndDate());
                    }
                    logger.debug("======= end of list of dropped modifications");
                }
            }
        }
        return eventsInstances;
    }

    private void completeEventData(EventInstance event, XWikiDocument eventDoc, BaseObject eventData)
        throws XWikiException
    {
        final XWikiContext context = xcontextProvider.get();
        final DocumentReference eventDocRef = eventDoc.getDocumentReference();

        boolean isAllDay = eventData.getIntValue(EventConstants.PROPERTY_ALLDAY_NAME) == 1;
        event.setAllDay(isAllDay);
        String textColor = eventData.getStringValue(EventConstants.PROPERTY_TEXTCOLOR_NAME);
        String backgroundColor = eventData.getStringValue(EventConstants.PROPERTY_BACKGROUNDCOLOR_NAME);

        DateTime endDateExclusive = event.getEndDate();
        if (isAllDay) {
            // as end date is actually treated exclusive by the calendar
            // but inclusive by the input data:
            endDateExclusive = endDateExclusive.plusDays(1);
        }

        event.setEndDateExclusive(endDateExclusive);

        if (null == event.getTitle()) {
            event.setTitle(eventDoc.getRenderedTitle(Syntax.PLAIN_1_0, context));
        }

        if (null == event.getDescription()) {
            Utils.fillDescription(eventData, EventConstants.PROPERTY_DESCRIPTION_NAME, context, event);
        }

        event.setEventDocRef(eventDocRef);
        event.setModifiable(true);
        event.setMovable(!event.isRecurrent());
        event.setTextColor(textColor);
        event.setBackgroundColor(backgroundColor);

        fillInColorsFromNearestCalendar(event);
    }

    /**
     * Fill in the color and text color values from the "corresponding" calendar".
     *
     * The corresponding calendar page should be the default page of the parent space. this is the space of the page if the
     * event page is terminal, and the parent of the events page space, if the page is non-terminal
     */
    private void fillInColorsFromNearestCalendar(EventInstance event)
    {
        try {
            final DocumentReference eventDocRef = event.getEventDocRef();
            final XWikiContext context = xcontextProvider.get();
            final String defaultPageName = defaultEntityReferenceProvider
                .getDefaultReference(EntityType.DOCUMENT).getName();

            BaseObject calendarData = null;
            SpaceReference parentSpaceRef = null;
            if (defaultPageName.equals(eventDocRef.getName())) {
                EntityReference parentRef = eventDocRef.getLastSpaceReference().getParent();
                if ((parentRef != null) && (parentRef instanceof SpaceReference)) {
                    parentSpaceRef = (SpaceReference) parentRef;
                }
            } else {
                parentSpaceRef = eventDocRef.getLastSpaceReference();
            }

            if (parentSpaceRef != null) {
                DocumentReference parentDoc = new DocumentReference(defaultPageName, parentSpaceRef);
                XWikiDocument calendarDoc = context.getWiki().getDocument(parentDoc, context);
                calendarData = calendarDoc
                    .getXObject(calendarDoc.resolveClassReference(EventConstants.MOCCA_CALENDAR_CLASS_NAME));
            }

            if (calendarData == null) {
                // some arbitrary defaults
                if (event.getBackgroundColor().length() == 0) {
                    event.setBackgroundColor("#888");
                }
                // text color can be missing
                if (event.getTextColor().length() == 0) {
                    // not that great
                    event.setTextColor("");
                }
            } else {
                if (event.getBackgroundColor().length() == 0) {
                    event.setBackgroundColor(calendarData.getStringValue("color"));
                }
                if (event.getTextColor().length() == 0) {
                    event.setTextColor(calendarData.getStringValue("textColor"));
                }
            }
        } catch (XWikiException xe) {
            logger.warn("could not calculate colors for event", xe);
        }
    }

    /**
     * Find modification data for an event instance, if the event instance has been modified.
     *
     * @param eventDoc
     *            the document of the recurrent event
     * @param eventStartDate
     *            the original start date of the event instance
     * @return the index of a MoccaCalendarEventModificationClass object for the event instance, or -1 if no modification
     *         has been found for the event instance
     */
    public int getModifiedEventObjectIndex(Document eventDoc, Date eventStartDate)
    {
        final List<BaseObject> modificationNotices = eventDoc.getDocument().getXObjects(
            stringDocRefResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_MODIFICATION_CLASS_NAME));
        if (modificationNotices != null) {
            for (int i = 0, n = modificationNotices.size(); i < n; i++) {
                BaseObject modificationNotice = modificationNotices.get(i);
                Date modificationDate = (modificationNotice == null) ? null
                    : modificationNotice
                        .getDateValue(EventConstants.PROPERTY_ORIG_STARTDATE_OF_MODIFIED_NAME);
                if (eventStartDate.equals(modificationDate)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Create a dummy modification object to be used as placeholder in edit view.
     *
     * @param eventDoc
     *            the document containing the (recurrent) event to be modified.
     * @param eventStartDate
     *            the original start date of the unmodified event instance
     * @return a non-persistent event modification object containing default values
     */
    public com.xpn.xwiki.api.Object createModificationDummy(Document eventDoc, Date eventStartDate)
    {
        final XWikiDocument xwikiEventDoc = eventDoc.getDocument();
        final BaseObject eventData = xwikiEventDoc
            .getXObject(stringDocRefResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME));

        BaseObject modificationData = new BaseObject();
        modificationData.setXClassReference(
            stringDocRefResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_MODIFICATION_CLASS_NAME));
        modificationData.setOwnerDocument(xwikiEventDoc);
        modificationData.setNumber(-1);

        if (eventData != null) {
            Date defaultStartDate = (eventStartDate != null) ? eventStartDate
                : eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME);
            modificationData.setDateValue(EventConstants.PROPERTY_STARTDATE_NAME, defaultStartDate);

            final Date baseStartDate = eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME);
            final Date baseEndDate = Utils.fetchOrGuessEndDate(eventData);
            final long baseDuration = baseEndDate.getTime() - baseStartDate.getTime();

            Date defaultEndDate = new Date(defaultStartDate.getTime() + baseDuration);
            modificationData.setDateValue(EventConstants.PROPERTY_ENDDATE_NAME, defaultEndDate);

            modificationData.setLargeStringValue(EventConstants.PROPERTY_DESCRIPTION_NAME,
                eventData.getLargeStringValue(EventConstants.PROPERTY_DESCRIPTION_NAME));

            modificationData.setStringValue(EventConstants.PROPERTY_TITLE_NAME, xwikiEventDoc.getTitle());
        }

        return new com.xpn.xwiki.api.Object(modificationData, xcontextProvider.get());
    }

    /**
     * Create an event instance for the given date and document from the given source.
     *
     * If the document contains only information about one event, then the start date can be ignored. If there is no
     * matching information that the source can use to create an event from the document, then return null.
     *
     * @param eventDoc
     *            the document storing the event
     * @param eventStartDate
     *            the start date of the event
     * @param source
     *            the source which has generated the event, can be null.
     * @return the event instance matching the arguments, or null if no match as been found.
     */
    public EventInstance getEventInstance(final Document eventDoc, final Date eventStartDate,
        final String source)
    {
        EventSource eventSource = eventSources.get(source);
        if (eventSource != null) {
            return eventSource.getEventInstance(eventDoc, eventStartDate);
        }
        return getEventInstance(eventDoc, eventStartDate);
    }

    /**
     * Create an event instance for the given date and document. If the event instance has been modified, update the event
     * instance with the modifications. this methods does not take deletion marks into account, but always returns an event
     * instance.
     *
     * @param eventDoc
     *            the document of the recurrent event
     * @param eventStartDate
     *            the original start date of the event instance (might be null for the unaltered event)
     * @return the EventInstance with the (possibly modified) values of the event
     */
    public EventInstance getEventInstance(final Document eventDoc, final Date eventStartDate)
    {
        final XWikiDocument xwikiEventDoc = eventDoc.getDocument();
        final BaseObject eventData = xwikiEventDoc
            .getXObject(stringDocRefResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME));
        EventInstance event = null;
        if (eventData == null) {
            return event;
        }

        int objIndex;
        Date originalEventStartDate;
        if (eventStartDate == null) {
            originalEventStartDate = eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME);
            objIndex = -1;
        } else {
            originalEventStartDate = eventStartDate;
            objIndex = getModifiedEventObjectIndex(eventDoc, eventStartDate);
        }

        BaseObject modificationData = null;
        if (objIndex != -1) {
            modificationData = xwikiEventDoc.getXObject(
                stringDocRefResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_MODIFICATION_CLASS_NAME),
                objIndex);
        } else {
            // we create a dummy which always returns null for all properties
            modificationData = new BaseObject();
        }
        event = createModifiedEventData(xwikiEventDoc, eventData, modificationData, originalEventStartDate,
            null, null);

        try {
            completeEventData(event, xwikiEventDoc, eventData);
        } catch (XWikiException xe) {
            logger.info("could not create modified event data for document [{}] and date [{}]", eventDoc,
                eventStartDate, xe);
        }

        return event;
    }

    private void sortEvents(final List<EventInstance> events, final boolean ascending)
    {
        Collections.sort(events, new Comparator<EventInstance>()
        {
            @Override
            public int compare(EventInstance event1, EventInstance event2)
            {
                int result;
                final DateTime startDate1 = event1.getStartDate();
                final DateTime startDate2 = event2.getStartDate();
                if (startDate1 == null) {
                    result = (startDate2 == null) ? 0 : -1;
                } else if (startDate2 == null) {
                    result = 1;
                } else {
                    result = startDate1.compareTo(startDate2);
                }
                return (ascending) ? result : -result;
            }

        });
    }

    private Set<Long> deletedEventsOf(XWikiDocument eventDoc)
    {
        Set<Long> deletions = new HashSet<>();

        final List<BaseObject> deleteNotices = eventDoc.getXObjects(
            stringDocRefResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_DELETION_CLASS_NAME));
        if (deleteNotices != null) {
            for (BaseObject deleteNotice : deleteNotices) {
                Date deleted = (deleteNotice == null) ? null
                    : deleteNotice.getDateValue(EventConstants.PROPERTY_STARTDATE_OF_DELETED_NAME);
                if (deleted != null) {
                    deletions.add(deleted.getTime());
                }
            }
        }

        if (!deletions.isEmpty() && logger.isDebugEnabled()) {
            logger.debug("found {} deletions for event [{}])", deletions.size(), eventDoc);
        }

        return deletions;
    }

    /**
     * Find all modified events for an event document within a given time frame.
     *
     * @param eventDoc
     *            the document of the recurrent event
     * @param dateFrom
     *            the date from which the events are sought
     * @param dateTo
     *            the date up to which the events are sought
     * @return a map of original event dates to instances filled with the corresponding modifications
     */
    private Map<Long, EventInstance> modifiedEventsOf(XWikiDocument eventDoc, Date dateFrom, Date dateTo)
    {
        final Map<Long, EventInstance> results = new HashMap<>();
        final List<BaseObject> modificationNotices = eventDoc.getXObjects(
            stringDocRefResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_MODIFICATION_CLASS_NAME));
        final BaseObject eventData = eventDoc
            .getXObject(stringDocRefResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME));

        if (modificationNotices != null) {
            for (int i = 0, n = modificationNotices.size(); i < n; i++) {

                BaseObject modificationNotice = modificationNotices.get(i);
                if (modificationNotice == null) {
                    continue;
                }

                Date originalStartDate = modificationNotice
                    .getDateValue(EventConstants.PROPERTY_ORIG_STARTDATE_OF_MODIFIED_NAME);
                if (originalStartDate == null) {
                    continue;
                }

                EventInstance modifiedInstance = createModifiedEventData(eventDoc, eventData,
                    modificationNotice, originalStartDate, dateFrom, dateTo);
                if (modifiedInstance == null) {
                    continue;
                }

                results.put(originalStartDate.getTime(), modifiedInstance);
            }
        }
        return results;
    }

    /**
     * Helper to create an event instance from modification data. The very long parameter list is necessary as the code is
     * called from several places. If the modified event is not in the given date range, this helper returns a null. The
     * original start date is not optional, as we cannot guess it from the data if the modificationNotice is a dummy The
     * date ranges are optional, if they are null, no check for the date range is done.
     *
     * @param eventDoc
     *            the document containing the event
     * @param eventData
     *            the main even data
     * @param modificationNotice
     *            the object containing the modification
     * @param originalStartDate
     *            the original start date of the event, must not be null
     * @param dateFrom
     *            the start of the date range, can be null
     * @param dateTo
     *            the end of the date range can be null
     * @return the event instance with (only) the modified data filled in
     */
    private EventInstance createModifiedEventData(XWikiDocument eventDoc, BaseObject eventData,
        BaseObject modificationNotice, Date originalStartDate, Date dateFrom, Date dateTo)
    {
        final Date baseStartDate = eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME);
        final Date baseEndDate = Utils.fetchOrGuessEndDate(eventData);
        final long baseDuration = baseEndDate.getTime() - baseStartDate.getTime();

        // now get both the original start / end date
        // and the modified start / end date, and add a rudimentary event instance to result,
        // unless:
        // a) both the original and new end date are before the "dateFrom"
        // or
        // b) both the original start date or the modified start date are after the "dateTo"

        Date originalEndDate = new Date(originalStartDate.getTime() + baseDuration);
        Date actualStartDate = modificationNotice.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME);
        if (actualStartDate == null) {
            actualStartDate = originalStartDate;
        }
        // the following does not work if we have a modification without start date:
        // Date actualEndDate = Utils.fetchOrGuessEndDate(modificationNotice);
        // so instead:
        Date actualEndDate = modificationNotice.getDateValue(EventConstants.PROPERTY_ENDDATE_NAME);
        if (actualEndDate == null) {
            // we need to calculate the actual end date from the actual start date, but only if this has been defined
            // otherwise if we have no modified start date and no modified end date given,
            // then the end date is the same as the original end date
            // XXX: what if the "allDay" flag is changed on the event? currently this is not supported
            if (actualStartDate.equals(originalStartDate)) {
                actualEndDate = originalEndDate;
            } else {
                final boolean allDay = eventData.getIntValue(EventConstants.PROPERTY_ALLDAY_NAME) == 1;
                actualEndDate = Utils.guessEndDate(actualStartDate, allDay);
            }
        }

        // now we can figure out if the modified event is in the right time frame
        if (dateFrom != null && actualEndDate.before(dateFrom) && originalEndDate.before(dateFrom)) {
            return null;
        }
        if (dateTo != null && actualStartDate.after(dateTo) && originalStartDate.after(dateTo)) {
            return null;
        }

        EventInstance modifiedInstance = new EventInstance();
        modifiedInstance.setStartDate(new DateTime(actualStartDate.getTime()));
        modifiedInstance.setOriginalStartDate(new DateTime(originalStartDate.getTime()));
        modifiedInstance.setEndDate(new DateTime(actualEndDate.getTime()));

        XWikiContext context = xcontextProvider.get();
        String modifiedTitle = modificationNotice.getStringValue(EventConstants.PROPERTY_TITLE_NAME);
        if (modifiedTitle != null && !"".equals(modifiedTitle.trim())) {
            modifiedInstance.setTitle(eventDoc.getRenderedContent(modifiedTitle,
                eventDoc.getSyntax().toIdString(), Syntax.PLAIN_1_0.toIdString(), context));
        }
        String modifiedDescription = modificationNotice
            .getStringValue(EventConstants.PROPERTY_DESCRIPTION_NAME);
        if (modifiedDescription != null && !"".equals(modifiedDescription.trim())) {
            Utils.fillDescription(modificationNotice, EventConstants.PROPERTY_DESCRIPTION_NAME, context,
                modifiedInstance);
        }

        return modifiedInstance;
    }

}
