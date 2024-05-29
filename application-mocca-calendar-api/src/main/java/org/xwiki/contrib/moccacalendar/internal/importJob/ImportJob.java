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
package org.xwiki.contrib.moccacalendar.internal.importJob;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobRequest;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobStatus;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.CalendarComponent;

@Component
@Named(ImportJob.JOB_TYPE)
public class ImportJob extends AbstractJob<ImportJobRequest, ImportJobStatus> implements GroupedJob
{
    /**
     * Admin Tools health check job type.
     */
    public static final String JOB_TYPE = "moccacalendar.import";

    private String importedFileContent;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    public JobGroupPath getGroupPath()
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        String wikiId = wikiContext.getWikiId();
        List<String> path = new ArrayList<>();
        path.add("moccacalendar");
        path.add("import");
        path.add(wikiId);
        return new JobGroupPath(path);
    }

    @Override
    protected ImportJobStatus createNewStatus(ImportJobRequest request)
    {
        return new ImportJobStatus(JOB_TYPE, request, observationManager, loggerManager);
    }

    @Override
    protected void runInternal()
    {
        try {
            this.importedFileContent = new String(request.getFile());
            List<CalendarComponent> sortedComponents = getCalendarComponents().stream().sorted((c1, c2) -> {
                String dtStartStr1 = c1.getProperty("DTSTART").getValue();
                String dtStartStr2 = c2.getProperty("DTSTART").getValue();
                return dtStartStr1.compareTo(dtStartStr2);
            }).collect(Collectors.toList());
            this.progressManager.pushLevelProgress(sortedComponents.size(), this);
            DocumentReference eventClassRef =
                documentReferenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME);
            DocumentReference eventRecClassRef =
                documentReferenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME);
            CalendarEventDateProcessor dateProcessor = new CalendarEventDateProcessor();
            for (CalendarComponent component : sortedComponents) {
                String eventUID = component.getProperty("UID").getValue();
                if (eventUID.equals("58o56mrujl4ishdqvvmvq9ve72@google.com")) {
                    String a = "";
                    String c = a + "21";
                }
                progressManager.startStep(this);
                if (status.isCanceled()) {
                    break;
                } else if (!this.status.isDuplicate(eventUID)) {
                    XWikiContext wikiContext = wikiContextProvider.get();
                    XWikiDocument eventDoc =
                        getUniqueEventName(component.getProperty("SUMMARY").getValue().trim(), request.getParentRef());
                    BaseObject eventObj = eventDoc.newXObject(eventClassRef, wikiContext);
                    BaseObject eventRecObj = eventDoc.newXObject(eventRecClassRef, wikiContext);

                    for (String moccaCalendarField : EventConstants.getClassFields()) {
                        if (moccaCalendarField.equals(EventConstants.PROPERTY_STARTDATE_NAME)) {
                            dateProcessor.processEventDate(component, ZoneId.of("UTC").getId());
                            setDates(eventObj, eventRecObj, dateProcessor);
                        } else {
                            eventObj.set(moccaCalendarField, generateEventData(component, moccaCalendarField),
                                wikiContext);
                        }
                    }

                    eventDoc.setAuthorReference(request.getUserReference());
                    eventDoc.setCreatorReference(request.getUserReference());
                    wikiContext.getWiki().saveDocument(eventDoc, "Created new event.", wikiContext);
                    this.status.storeUID(eventUID);
                } else {
                    String b = "";
                    String d = b + "21";
                }
                progressManager.endStep(this);
                Thread.yield();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void setDates(BaseObject eventObj, BaseObject recEventObj, CalendarEventDateProcessor dateProcessor)
    {
        Date startDate = dateProcessor.getStartDate();
        eventObj.set(EventConstants.PROPERTY_STARTDATE_NAME, startDate, this.wikiContextProvider.get());
        eventObj.set(EventConstants.PROPERTY_ENDDATE_NAME, dateProcessor.getEndDate(), this.wikiContextProvider.get());
        eventObj.set(EventConstants.PROPERTY_ALLDAY_NAME, dateProcessor.getAllDay(), this.wikiContextProvider.get());
        int recurrenceValue = dateProcessor.getIsRecurrent();
        eventObj.set(EventConstants.PROPERTY_RECURRENT_NAME, recurrenceValue, this.wikiContextProvider.get());
        if (recurrenceValue == 1) {
            LocalDate localDateStartDate = startDate.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            recEventObj.set(EventConstants.PROPERTY_FIRSTINSTANCE_NAME, startDate, this.wikiContextProvider.get());

            if (dateProcessor.getRecEndDate() != null) {
                recEventObj.set(EventConstants.PROPERTY_LASTINSTANCE_NAME, dateProcessor.getRecEndDate(),
                    this.wikiContextProvider.get());
            } else {
                // Add 5 years to the start date
                LocalDate futureLocalDate = localDateStartDate.plusYears(5);
                Date futureDate = Date.from(futureLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant());
                recEventObj.set(EventConstants.PROPERTY_LASTINSTANCE_NAME, futureDate, this.wikiContextProvider.get());
            }

            recEventObj.set("frequency", dateProcessor.getRecurrenceFreq(), this.wikiContextProvider.get());
        }
    }

    private String generateEventData(CalendarComponent component, String field)
    {
        String value;
        switch (field) {
            case EventConstants.PROPERTY_TITLE_NAME:
                value = component.getProperty("SUMMARY").getValue();
                return value;
            case EventConstants.PROPERTY_DESCRIPTION_NAME:
                return formDescription(component);
            default:
                return "";
        }
    }

    private String formDescription(CalendarComponent component)
    {
        Property descrProperty = component.getProperty("DESCRIPTION");
        Property organizerProperty = component.getProperty("ORGANIZER");
        Property locationProperty = component.getProperty("LOCATION");
        PropertyList<Property> attendeesProperty = component.getProperties("ATTENDEE");
        StringBuilder descriptionBuilder = new StringBuilder();

        appendOrganizerPropriety(descriptionBuilder, organizerProperty);
        for (Property attendeeProperty : attendeesProperty) {
            appendPropriety(descriptionBuilder, "Attendee:", attendeeProperty);
        }

        appendPropriety(descriptionBuilder, "Location:", locationProperty);
        descriptionBuilder.append("\n");
        appendPropriety(descriptionBuilder, "", descrProperty);

        return descriptionBuilder.toString();
    }

    private void appendOrganizerPropriety(StringBuilder descriptionBuilder, Property property)
    {
        if (property != null && (!property.getValue().contains("unknownorganizer"))) {
            descriptionBuilder.append(String.format("Organizer: %s\n", property.getValue()));
        }
    }

    private void appendPropriety(StringBuilder descriptionBuilder, String label, Property property)
    {
        if (property != null) {
            descriptionBuilder.append(String.format("%s %s\n", label, property.getValue()));
        }
    }

    private List<CalendarComponent> getCalendarComponents() throws IOException, ParserException
    {
        int beginIndex = importedFileContent.indexOf("BEGIN:VCALENDAR");
        int endIndex = importedFileContent.indexOf("END:VCALENDAR") + "END:VCALENDAR".length();
        String filteredCalendarContent = importedFileContent.substring(beginIndex, endIndex);

        StringReader stringReader = new StringReader(filteredCalendarContent);
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = builder.build(stringReader);
        return calendar.getComponents("VEVENT");
    }

    private XWikiDocument getUniqueEventName(String eventName, String parentRef)
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();

        DocumentReference parentDocRef = documentReferenceResolver.resolve(parentRef);
        SpaceReference documentSpaceRef = new SpaceReference(eventName, parentDocRef.getLastSpaceReference());
        DocumentReference eventRef = new DocumentReference("WebHome", documentSpaceRef);
        XWikiDocument eventDoc;

        if (wiki.exists(eventRef, wikiContext)) {
            String newEventName = eventName + "_" + System.currentTimeMillis();
            SpaceReference newDocumentSpaceRef = new SpaceReference(newEventName, parentDocRef.getLastSpaceReference());
            DocumentReference newEventRef = new DocumentReference("WebHome", newDocumentSpaceRef);
            eventDoc = new XWikiDocument(newEventRef);
        } else {
            eventDoc = new XWikiDocument(eventRef);
        }
        eventDoc.setTitle(eventName);
        eventDoc.setParentReference(parentDocRef);
        return eventDoc;
    }
}
