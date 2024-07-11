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

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.importJob.result.MoccaCalendarEventResult;
import org.xwiki.localization.ContextualLocalizationManager;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.CalendarComponent;

/**
 * Ical file import helper class used for processing the data related to an event from a {@link CalendarComponent}
 * extracted from a file, and mapping it to a {@link MoccaCalendarEventResult}.
 *
 * @version $Id$
 * @since 2.14
 */
@Component(roles = CalendarEventProcessor.class)
@Singleton
public class CalendarEventProcessor
{
    private static final String LINE_BREAK = "\n";

    private static final String LABEL_DESCRIPTION = "description";

    private static final String MAILTO = "mailto:";

    private static final String ESCAPED_MAILTO = "mailto~:";

    private static final String DOUBLE_SLASHES = "//";

    private static final String ESCAPED_DOUBLE_SLASHES = "\\/\\/";

    @Inject
    private CalendarEventDurationProcessor durationProcessor;

    @Inject
    private ContextualLocalizationManager contextLocalization;

    /**
     * Process the ical event and adapt them to the Mocca calendar event and recurrent event class.
     *
     * @param component the ical event.
     * @return a {@link MoccaCalendarEventResult} containing the required fields for a Mocca calendar event and
     *     recurrent event class .
     */
    public MoccaCalendarEventResult getMoccaEvent(CalendarComponent component)
    {
        MoccaCalendarEventResult eventResult = new MoccaCalendarEventResult();
        eventResult.setTitle(getTitle(component));
        eventResult.setDescription(getDescription(component));
        eventResult.setAllDay(durationProcessor.isAllDay(
            component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_START_DATE).getValue()));
        eventResult.setStartDate(
            durationProcessor.getEventDate(component, CalendarKeys.ICS_CALENDAR_PROPERTY_START_DATE));
        eventResult.setEndDate(durationProcessor.getEventDate(component, CalendarKeys.ICS_CALENDAR_PROPERTY_END_DATE));
        setRecurrence(eventResult, component);
        return eventResult;
    }

    private void setRecurrence(MoccaCalendarEventResult eventResult, CalendarComponent component)
    {
        String recFreq = durationProcessor.getRecurrenceFrequency(component).orElse("");
        eventResult.setIsRecurrent(0);
        if (durationProcessor.isRecurrentEvent(component) == 1 && !recFreq.isEmpty()) {
            eventResult.setIsRecurrent(1);
            eventResult.setRecEndDate(durationProcessor.getRecurrenceEndDate(component));
            eventResult.setRecurrenceFreq(recFreq);
        }
    }

    private String getTitle(CalendarComponent component)
    {
        return component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_SUMMARY).getValue();
    }

    private String getDescription(CalendarComponent component)
    {
        Property descrProperty = component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_DESCRIPTION);
        Property organizerProperty = component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_ORGANIZER);
        Property locationProperty = component.getProperty(CalendarKeys.ICS_CALENDAR_PROPERTY_LOCATION);
        PropertyList<Property> attendeesProperty = component.getProperties(CalendarKeys.ICS_CALENDAR_PROPERTY_ATTENDEE);
        StringBuilder descriptionBuilder = new StringBuilder();

        appendOrganizerProperty(descriptionBuilder, organizerProperty);
        for (Property attendeeProperty : attendeesProperty) {
            appendProperty(descriptionBuilder, "attendee", attendeeProperty);
        }

        appendProperty(descriptionBuilder, "location", locationProperty);
        appendProperty(descriptionBuilder, LABEL_DESCRIPTION, descrProperty);

        return descriptionBuilder.toString();
    }

    private void appendOrganizerProperty(StringBuilder descriptionBuilder, Property property)
    {
        if (property != null && (!property.getValue().contains("unknownorganizer"))) {
            String translationPlain =
                contextLocalization.getTranslationPlain("MoccaCalendar.import.generated.description.organizer",
                    escapeSyntax(property.getValue()));
            descriptionBuilder.append(translationPlain);
            descriptionBuilder.append(LINE_BREAK);
        }
    }

    private void appendProperty(StringBuilder descriptionBuilder, String label, Property property)
    {
        if (property != null) {
            String cleanedValue = escapeSyntax(property.getValue());
            if (!label.equals(LABEL_DESCRIPTION)) {
                String translationKey = String.format("%s.%s", "MoccaCalendar.import.generated.description", label);
                String translationPlain = contextLocalization.getTranslationPlain(translationKey, cleanedValue);
                descriptionBuilder.append(translationPlain);
                descriptionBuilder.append(LINE_BREAK);
            } else {
                descriptionBuilder.append(LINE_BREAK);
                descriptionBuilder.append("{{html clean=\"false\"}}");
                descriptionBuilder.append(cleanedValue);
                descriptionBuilder.append("{{/html}}");
            }
        }
    }

    private String escapeSyntax(String inputValue)
    {
        String escapedValue = Jsoup.clean(inputValue, Safelist.basic());
        String escapedMailSyntax = maybeEscapeMailWikiSyntax(escapedValue);
        return maybeEscapeURLSyntax(escapedMailSyntax);
    }

    private String maybeEscapeMailWikiSyntax(String inputValue)
    {
        return inputValue.contains(MAILTO) ? inputValue.replace(MAILTO, ESCAPED_MAILTO) : inputValue;
    }

    private String maybeEscapeURLSyntax(String inputValue)
    {
        return inputValue.contains(DOUBLE_SLASHES) ? inputValue.replace(DOUBLE_SLASHES, ESCAPED_DOUBLE_SLASHES)
            : inputValue;
    }
}
