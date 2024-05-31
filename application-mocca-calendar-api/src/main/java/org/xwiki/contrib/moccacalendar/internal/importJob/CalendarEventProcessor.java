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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.importJob.result.MoccaCalendarEventResult;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.CalendarComponent;

/**
 * Ical file import helper class for processing an event.
 *
 * @version $Id$
 * @since 2.14
 */
@Component(roles = CalendarEventProcessor.class)
@Singleton
public class CalendarEventProcessor
{
    @Inject
    private CalendarEventDurationProcessor durationProcessor;

    /**
     * Process the ical event and adapt them to the Mocca calendar event and recurrent event class.
     *
     * @param component the ical event.
     * @return a {@link MoccaCalendarEventResult} containing the required fields for a Mocca calendar event and
     *     recurrent event class .
     */
    public MoccaCalendarEventResult processEvent(CalendarComponent component)
    {
        MoccaCalendarEventResult eventResult = new MoccaCalendarEventResult();
        eventResult.setTitle(processTitle(component));
        eventResult.setDescription(processDescription(component));
        eventResult.setAllDay(durationProcessor.isAllDay(component));
        eventResult.setStartDate(
            durationProcessor.processEventDates(component, CalendarKeys.ICS_CALENDAR_PROPERTY_START_DATE));
        eventResult.setEndDate(
            durationProcessor.processEventDates(component, CalendarKeys.ICS_CALENDAR_PROPERTY_END_DATE));
        setRecurrence(eventResult, component);
        return eventResult;
    }

    private void setRecurrence(MoccaCalendarEventResult eventResult, CalendarComponent component)
    {
        String recFreq = durationProcessor.getRecurrenceFrequency(component);
        eventResult.setIsRecurrent(0);
        if (durationProcessor.isRecurrentEvent(component) == 1 && !recFreq.isEmpty()) {
            eventResult.setIsRecurrent(1);
            eventResult.setRecEndDate(durationProcessor.getRecurrenceEndDate(component));
            eventResult.setRecurrenceFreq(recFreq);
        }
    }

    private String processTitle(CalendarComponent component)
    {
        return component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_SUMMARY).getValue();
    }

    private String processDescription(CalendarComponent component)
    {
        Property descrProperty = component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_DESCRIPTION);
        Property organizerProperty = component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_ORGANIZER);
        Property locationProperty = component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_LOCATION);
        PropertyList<Property> attendeesProperty = component.getProperties(CalendarKeys.ICS_CALENDAR_PROPERTY_ATTENDEE);
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
}
