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
package org.xwiki.contrib.moccacalendar.internal.ical;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Generates iCalendar (ICS) output for Mocca Calendar events. This class converts Mocca Calendar events to iCalendar
 * format for export.
 *
 * @version $Id:$
 * @since 2.20
 */
@Singleton
@Component(roles = ICalGenerator.class)
public class ICalGenerator
{
    private static final String PROD_ID = "-//XWiki//iCal4j 1.0//EN";

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("document")
    private QueryFilter documentFilter;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private ICalEventGenerator eventGenerator;

    /**
     * Generates an iCalendar file for the specified calendar document.
     *
     * @param calendarReference the full name or reference of the calendar document
     * @param outputStream      the output stream to write the iCalendar data to
     * @throws IOException           if an error occurs while writing to the output stream
     * @throws QueryException        if an error occurs while querying for events
     * @throws XWikiException        if an error occurs while accessing the XWiki document
     * @throws AccessDeniedException if the user does not have permission to view the calendar
     */
    public void generateCalendar(String calendarReference, OutputStream outputStream)
        throws IOException, QueryException, XWikiException, AccessDeniedException
    {
        if (StringUtils.isBlank(calendarReference)) {
            throw new FileNotFoundException("No calendar reference provided for iCal generation.");
        }
        XWikiContext context = this.xcontextProvider.get();
        DocumentReference docRef = this.referenceResolver.resolve(calendarReference);
        this.contextualAuthorizationManager.checkAccess(Right.VIEW, docRef);
        XWikiDocument calendarDocument = context.getWiki().getDocument(docRef, context);
        if (calendarDocument == null || calendarDocument.isNew()) {
            throw new FileNotFoundException(String.format("Cannot access calendar [%s].", calendarReference));
        }
        Calendar calendar = createCalendar(calendarDocument);
        addEvents(calendar, calendarReference, context);
        CalendarOutputter outputter = new CalendarOutputter();
        outputter.output(calendar, outputStream);
    }

    /**
     * Creates a basic iCal object with metadata.
     *
     * @param calendarDocument the calendar document containing metadata
     * @return the created Calendar object
     */
    private Calendar createCalendar(XWikiDocument calendarDocument)
    {
        Calendar calendar = new Calendar();
        calendar.add(new ProdId(PROD_ID));
        calendar.add(ImmutableVersion.VERSION_2_0);
        calendar.add(ImmutableCalScale.GREGORIAN);
        calendar.add(new XProperty("X-WR-CALNAME", calendarDocument.getTitle()));
        return calendar;
    }

    /**
     * Queries and adds all events from the specified calendar to the iCalendar object.
     *
     * @param calendar          the iCalendar object to add events to
     * @param calendarReference the reference of the calendar document
     * @param context           the current XWiki context
     */
    private void addEvents(Calendar calendar, String calendarReference, XWikiContext context)
        throws QueryException, XWikiException
    {
        Query query = this.queryManager.createQuery(
            "from doc.object(MoccaCalendar.MoccaCalendarEventClass) as event where doc.parent = :parent", Query.XWQL);
        query.bindValue("parent", calendarReference);
        query.addFilter(this.documentFilter);
        List<DocumentReference> eventDocRefs = query.execute();
        for (DocumentReference eventDocRef : eventDocRefs) {
            XWikiDocument eventDocument = context.getWiki().getDocument(eventDocRef, context);
            if (eventDocument != null && !eventDocument.isNew()) {
                VEvent event = this.eventGenerator.createEvent(eventDocument);
                if (event != null) {
                    calendar.add(event);
                }
            }
        }
    }

}
