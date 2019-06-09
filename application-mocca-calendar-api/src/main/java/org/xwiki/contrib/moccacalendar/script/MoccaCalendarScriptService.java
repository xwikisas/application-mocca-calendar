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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.EventInstance;
import org.xwiki.contrib.moccacalendar.RecurrentEventGenerator;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
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
    private static final String BASE_QUERY_PREFIX = ", BaseObject as obj, IntegerProperty as recurrent, DateProperty as startdate, DateProperty as enddate"
        + " where obj.id=startdate.id.id and startdate.id.name='startDate'"
        + " and obj.id=enddate.id.id and enddate.id.name='endDate'"
        + " and obj.id=recurrent.id.id and recurrent.id.name='recurrent'"
        + " and doc.fullName=obj.name and doc.fullName!='MoccaCalendar.MoccaCalendarEventTemplate'"
        + " and obj.className='" + EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME + "'";
    private static final String CALENDAR_BASE_QUERY = ", BaseObject as obj"
        + " where doc.fullName=obj.name and doc.name!='MoccaCalendarTemplate'" + " and obj.className='"
        + EventConstants.MOCCA_CALENDAR_CLASS_NAME + "' order by doc.title, doc.name";

    private static final String FILTER_WIKI = "wiki";
    private static final String FILTER_SPACE = "space";
    private static final String FILTER_PAGE = "page";
    private static final int EVENT_DURATION_MIN = 30;

    /**
     * a small helper class to keep the data for a HQL query.
     */
    private static class QueryData
    {
        private final StringBuilder hql = new StringBuilder();
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * build the hql query.
         * @return the string builded to construct the query
         */
        public StringBuilder getHql()
        {
            return hql;
        }

        /**
         * the paramters for the query.
         * @return a map of parameters
         */
        Map<String, Object> getQueryParams()
        {
            return queryParams;
        }
    }

    @Inject
    private AuthorizationManager authorizationManager;

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
    private Map<String, RecurrentEventGenerator> eventGenerators;

    @Inject
    private Logger logger;

    /**
     * get all calendars.
     * @return a list of document references point to pages containing calendar objects.
     */
    public List<DocumentReference> getAllCalendars()
    {
        List<DocumentReference> calenderRefs = Collections.emptyList();

        try {
            Query query = queryManager.createQuery(CALENDAR_BASE_QUERY, Query.HQL);
            List<String> results = query.execute();
            calenderRefs = filterViewableEvents(results);
        } catch (QueryException qe) {
            logger.error("error while fetching calendars", qe);
        }

        return calenderRefs;
    }

    /**
     * get a list of events matching the date and filter criteria.
     * 
     * @param dateFrom
     *            the start range
     * @param dateTo
     *            the end range; can be null. in that case dates form a single day are returned
     * @param filter
     *            ho to filter the event. if null or "wiki" return all events
     * @param parentReference
     *            the page reference to use for the filter. can be null if filter is null or "wiki".
     * @param sortAscending
     *            if true, sort events ascending by start date, else descending
     * @return a list of event instances matching the criteria; might be empty but never null
     * @throws QueryException
     */
    public List<EventInstance> queryEvents(Date dateFrom, Date dateTo, String filter, String parentReference,
        boolean sortAscending) throws QueryException
    {

        final XWikiContext context = xcontextProvider.get();

        if (dateTo == null) {
            dateTo = dateFrom;
        }

        QueryData simpleEvents = new QueryData();

        simpleEvents.getHql().append(BASE_QUERY_PREFIX);

        //
        // filter by date range
        //

        addDateRangeFilter(simpleEvents, dateFrom, dateTo);

        // and search only non-recurrent events
        simpleEvents.getHql().append(" and recurrent.value = 0 ");

        //
        // now filter by event location
        //

        addLocationFilter(simpleEvents, filter, parentReference);

        // finally the ordering
        addOrderBy(sortAscending, simpleEvents);

        // needed for the location filter, but must happen at the end
        simpleEvents.getHql().append(" escape ! ");

        List<String> results = Collections.emptyList();

        try {

            logger.debug("sending query [{}] and params [{}]", simpleEvents.getHql(), simpleEvents.getQueryParams());

            Query query = queryManager.createQuery(simpleEvents.getHql().toString(), Query.HQL);

            for (Map.Entry<String, Object> param : simpleEvents.getQueryParams().entrySet()) {
                query.bindValue(param.getKey(), param.getValue());
            }

            results = query.execute();
        } catch (QueryException qe) {
            logger.error("error while fetching regular events", qe);
        }

        List<DocumentReference> visibleEvents = filterViewableEvents(results);

        List<EventInstance> events = new ArrayList<>();

        for (DocumentReference eventDocRef : visibleEvents) {
            try {
                // DocumentReference eventDocRef = stringDocRefResolver.resolve(docRef);
                XWikiDocument eventDoc = context.getWiki().getDocument(eventDocRef, context);
                BaseObject eventData = eventDoc
                    .getXObject(stringDocRefResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME));
                if (eventData == null) {
                    logger.error("data inconsistency: query returned [{}] which contains no object for [{}]",
                        eventDocRef, EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME);
                    continue;
                }

                EventInstance event = new EventInstance();
                event.setEventDocRef(eventDocRef);

                DateTime startDateTime = new DateTime(eventData.getDateValue("startDate").getTime());
                event.setStartDate(startDateTime);

                Date endDate = eventData.getDateValue("endDate");
                DateTime endDateTime;
                if (endDate == null) {
                    endDateTime = startDateTime.plusMinutes(EVENT_DURATION_MIN);
                } else {
                    endDateTime = new DateTime(endDate.getTime());
                }
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

        QueryData recurrentEventQuery = new QueryData();

        recurrentEventQuery.getHql().append(BASE_QUERY_PREFIX);

        //
        // filter by location
        //
        addLocationFilter(recurrentEventQuery, filter, parentReference);
        // and search only recurrent events
        recurrentEventQuery.getHql().append(" and recurrent.value = 1 ");

        // need to be added last - but does not work unless we have an order by ?
        // recurrentEventQuery.hql.append(" escape ! ");

        try {
            List<String> allRecurrentEvents = Collections.emptyList();

            logger.debug("sending query [{}] and params [{}]", recurrentEventQuery.getHql(),
                recurrentEventQuery.getQueryParams());

            Query query = queryManager.createQuery(recurrentEventQuery.getHql().toString(), Query.HQL);

            for (Map.Entry<String, Object> param : recurrentEventQuery.getQueryParams().entrySet()) {
                query.bindValue(param.getKey(), param.getValue());
            }

            allRecurrentEvents = query.execute();

            List<DocumentReference> visibleRecurrentEvents = filterViewableEvents(allRecurrentEvents);

            List<EventInstance> recurrentEventInstances = filterRecurrentEvents(visibleRecurrentEvents,
                dateFrom, dateTo);

            events.addAll(recurrentEventInstances);

        } catch (QueryException | XWikiException e) {
            logger.error("error while fetching recurrent events", e);
        }

        // FIXME: some code duplication and method is too long
        // TODO: now we gotta sort them, right? (at least for the "agenda view")

        return events;
    }

    private List<DocumentReference> filterViewableEvents(List<String> eventDocRefs)
    {
        List<DocumentReference> visibleRefs = new ArrayList<>();
        // check view rights on results ... should use "viewable" filter when minimal platform version is >= 9.8
        final DocumentReference userReference = xcontextProvider.get().getUserReference();
        for (ListIterator<String> iter = eventDocRefs.listIterator(); iter.hasNext();) {
            DocumentReference eventDocRef = stringDocRefResolver.resolve(iter.next());
            if (authorizationManager.hasAccess(Right.VIEW, userReference, eventDocRef)) {
                visibleRefs.add(eventDocRef);
            }
        }

        return visibleRefs;
    }

    private List<EventInstance> filterRecurrentEvents(List<DocumentReference> eventReferences, Date dateFrom,
        Date dateTo) throws XWikiException
    {
        final XWikiContext context = xcontextProvider.get();
        final List<EventInstance> eventsInstances = new ArrayList<>();
        for (DocumentReference eventDocRef : eventReferences) {
            // DocumentReference eventDocRef = stringDocRefResolver.resolve(docRef);
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
                logger.error("no recurrent event generator found for frequency [{}]", eventType);
                continue;
            }

            Set<Long> deletions = deletedEventsOf(eventDoc);
            for (EventInstance event : generator.generate(eventDoc, dateFrom, dateTo)) {
                if (deletions.contains(event.getStartDate().getMillis())) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("skip deleted event at {} for doc [{}])",
                            event.getStartDate(), eventDoc);
                    }
                    continue;
                }

                // add extra stuff here that the generator does not have to set
                event.setRecurrent(true);
                event.setEventDocRef(eventDocRef);

                completeEventData(event, eventDoc, eventData);
                eventsInstances.add(event);
            }
        }
        return eventsInstances;
    }

    private void completeEventData(EventInstance event, XWikiDocument eventDoc, BaseObject eventData)
        throws XWikiException
    {
        final XWikiContext context = xcontextProvider.get();
        final String defaultPageName = defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT)
            .getName();
        final DocumentReference eventDocRef = eventDoc.getDocumentReference();

        boolean isAllDay = eventData.getIntValue("allDay") == 1;

        DateTime endDateExclusive = event.getEndDate();
        if (isAllDay) {
            // as end date is actually treated exclusive by the calendar
            // but inclusive by the input data:
            endDateExclusive = endDateExclusive.plusDays(1);
        }

        event.setEndDateExclusive(endDateExclusive);

        event.setAllDay(isAllDay);

        event.setTitle(eventDoc.getRenderedTitle(Syntax.PLAIN_1_0, context));

        String description = eventData.getStringValue("description");
        // TODO: fetch the proper renderer here instead?
        // TODO: are we sure the description is always stored as xwiki/2.1?
        event.setDescription(eventDoc.getRenderedContent(description, eventDoc.getSyntax().toIdString(),
            Syntax.PLAIN_1_0.toIdString(), context));
        event.setDescriptionHtml(eventDoc.getRenderedContent(description, eventDoc.getSyntax().toIdString(),
            Syntax.HTML_5_0.toIdString(), context));

        /*
         * the corresponding calendar page should be the default page of the parent space. this is rthe spce of the page if the
         * event page is terminal, and the parent of the events page space, if the page is non-terminal
         */
        // TODO: this is complicated and creates wrong results for event in sub-sub pages
        // (and also those who use the old parent/child relationsship)
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
                .getXObject(stringDocRefResolver.resolve(EventConstants.MOCCA_CALENDAR_CLASS_NAME));
        }

        if (calendarData == null) {
            // some arbitrary defaults
            event.setBackgroundColor("#888");
            // text can be missing
            event.setTextColor("");
        } else {
            event.setTextColor(calendarData.getStringValue("textColor"));
            event.setBackgroundColor(calendarData.getStringValue("color"));
        }

    }

    //
    // these helpers probably should be "QueryData" methods
    //

    private void addDateRangeFilter(QueryData data, Date dateFrom, Date dateTo)
    {
        // start date / lower limit check: find all events which are not finished before the start date
        // for this, confusingly, one need to compare the end date of the event with the start date for the range
        // as a complication: to find events without end date, use the start date for them
        data.getHql().append("and (enddate.value is not null and ");
        appendDateCriterion(data, "enddate.value", "start", true);
        data.getHql().append(" or ");
        appendDateCriterion(data, "startdate.value", "start", true);
        data.getHql().append(')');

        // compared to this the upper limit check is straightforward, as we always have a startDate in the event
        data.getHql().append(" and ");
        appendDateCriterion(data, "startdate.value", "end", false);

        appendDateParameters(data, "start", dateFrom);
        appendDateParameters(data, "end", dateTo);
    }

    private void addLocationFilter(QueryData data, String filter, String parentReference)
    {
        switch (filter) {
            case FILTER_PAGE:
                DocumentReference parentRef = stringDocRefResolver.resolve(parentReference);
                data.getHql().insert(0, ", XWikiSpace space");
                data.getHql().append(" and doc.space = space.reference and space.parent = :space");
                data.getQueryParams().put("space", compactWikiSerializer.serialize(parentRef.getLastSpaceReference()));
                break;
            case FILTER_SPACE:
                parentRef = stringDocRefResolver.resolve(parentReference);
                // XXX maybe use the "bindValue(...).literal(...) instead?
                data.getHql().append(" and ( doc.space like :space )");
                String spaceRefStr = compactWikiSerializer.serialize(parentRef.getLastSpaceReference());
                String spaceLikeStr = spaceRefStr.replaceAll("([%_!])", "!$1").concat(".%");
                data.getQueryParams().put("space", spaceLikeStr);
                break;
            case FILTER_WIKI:
            default:
                // get events from the complete wiki: no filter to be added
                break;
        }
    }

    private void addOrderBy(boolean sortAscending, QueryData simpleEvents)
    {
        simpleEvents.getHql().append(" order by startdate.value ");
        if (sortAscending) {
            simpleEvents.getHql().append("asc");
        } else {
            simpleEvents.getHql().append("desc");
        }
    }

    // the date comparision in HQL is always a bit painful - hide it in a helper
    // for appendDateCriterion(query, "date", "field", true) this will create something like:
    //
    // ( year(date) > :fieldyear or ( year(date) = :fieldyear and ( month(date) > :fieldmonth or
    // ( month(date) = :fieldmonth and day(date) >= :fieldday ) ) ) )
    // ...
    private static void appendDateCriterion(QueryData data, String dateField, String prefix, boolean larger)
    {
        final char cmpSign = (larger) ? '>' : '<';

        data.getHql().append("( year(" + dateField + ") ").append(cmpSign).append(" :" + prefix + "year ");
        data.getHql().append(" or (year(" + dateField + ") = :" + prefix + "year and ");
        data.getHql().append("(month(" + dateField + ") ").append(cmpSign).append(" :" + prefix + "month");
        data.getHql().append(" or (month(" + dateField + ") = :" + prefix + "month ");
        data.getHql().append(" and day(").append(dateField).append(") ").append(cmpSign).append("= :")
            .append(prefix).append("day");
        data.getHql().append(')');
        data.getHql().append(')');
        data.getHql().append(')');
        data.getHql().append(')');
    }

    private void appendDateParameters(QueryData data, String prefix, Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        data.getQueryParams().put(prefix + "year", year);
        data.getQueryParams().put(prefix + "month", month);
        data.getQueryParams().put(prefix + "day", day);
    }

    private Set<Long> deletedEventsOf(XWikiDocument eventDoc)
    {
        Set<Long> deletions = new HashSet<>();

        final List<BaseObject> deleteNotes = eventDoc.
            getXObjects(stringDocRefResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_DELETION_CLASS_NAME));
        if (deleteNotes != null) {
            for (BaseObject deleteNotice : deleteNotes) {
                Date deleted = deleteNotice.getDateValue(EventConstants.PROPERTY_STARTDATE_OF_DELETED_NAME);
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
}
