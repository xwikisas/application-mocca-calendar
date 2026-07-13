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

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.property.RRule;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Generates a {@link RRule} based on the given XWikiDocument.
 *
 * @version $Id:$
 * @since 2.20
 */
@Component(roles = ICalRecurrenceGenerator.class)
@Singleton
public class ICalRecurrenceGenerator
{
    private static final String FREQ_WEEKLY = "FREQ=WEEKLY";

    private static final String FREQ_KEY_CUSTOM = "customWeekly";

    private static final String FREQ_MONTHLY = "FREQ=MONTHLY";

    private static final Map<String, List<String>> FREQUENCY_MAP = new HashMap<>();

    private static final Map<String, String> DAY_MAP = new HashMap<>();

    private static final DateTimeFormatter DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    static {
        FREQUENCY_MAP.put("daily", List.of("FREQ=DAILY"));
        FREQUENCY_MAP.put("workdays", List.of(FREQ_WEEKLY, "BYDAY=MO,TU,WE,TH,FR"));
        FREQUENCY_MAP.put("weekly", List.of(FREQ_WEEKLY));
        FREQUENCY_MAP.put("biweekly", List.of(FREQ_WEEKLY, "INTERVAL=2"));
        FREQUENCY_MAP.put("monthly", List.of(FREQ_MONTHLY));
        FREQUENCY_MAP.put("quarterly", List.of(FREQ_MONTHLY, "INTERVAL=3"));
        FREQUENCY_MAP.put("yearly", List.of("FREQ=YEARLY"));
        FREQUENCY_MAP.put(FREQ_KEY_CUSTOM, List.of(FREQ_WEEKLY));
    }

    static {
        DAY_MAP.put("1", "SU");
        DAY_MAP.put("2", "MO");
        DAY_MAP.put("3", "TU");
        DAY_MAP.put("4", "WE");
        DAY_MAP.put("5", "TH");
        DAY_MAP.put("6", "FR");
        DAY_MAP.put("7", "SA");
    }

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private Logger logger;

    /**
     * Adds recurrence rule to the event if it is a recurring event.
     *
     * @param eventDocument the event document
     * @return an Optional containing the {@link RRule} if the event is recurring, or an empty Optional otherwise
     */
    public Optional<RRule> getRecurrenceRule(XWikiDocument eventDocument)
    {
        DocumentReference eventClassRef =
            this.referenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME);
        BaseObject eventData = eventDocument.getXObject(eventClassRef);
        int recurrentFlag = eventData.getIntValue(EventConstants.PROPERTY_RECURRENT_NAME);
        if (recurrentFlag != 1) {
            this.logger.info("Event [{}] is not marked at recurrent. Skipping rrule generation.",
                eventDocument.getDocumentReference());
            return Optional.empty();
        }
        DocumentReference eventRecClassRef =
            this.referenceResolver.resolve(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME);
        BaseObject recurrencyObj = eventDocument.getXObject(eventRecClassRef);
        if (recurrencyObj == null) {
            this.logger.warn("Event [{}] is marked as recurrent but has no recurrency object.",
                eventDocument.getDocumentReference());
            return Optional.empty();
        }

        int allDay = eventData.getIntValue(EventConstants.PROPERTY_ALLDAY_NAME);
        String rruleValue = buildRecurrenceRule(allDay, recurrencyObj);
        return Optional.ofNullable(rruleValue).filter(StringUtils::isNotBlank)
            .map(value -> new RRule(new Recur(value)));
    }

    private String buildRecurrenceRule(int allDay, BaseObject recurrencyObj)
    {
        String frequency = recurrencyObj.getStringValue(EventConstants.PROPERTY_FREQUENCY_NAME);
        if (StringUtils.isBlank(frequency)) {
            this.logger.warn("Missing recurrence frequency for event [{}].", recurrencyObj.getDocumentReference());
            return "";
        }
        List<String> rruleParts = new ArrayList<>(FREQUENCY_MAP.getOrDefault(frequency, new ArrayList<>()));
        if (rruleParts.isEmpty()) {
            this.logger.warn("Unknown recurrence frequency [{}].", frequency);
            return "";
        }
        if (FREQ_KEY_CUSTOM.equals(frequency)) {
            addCustomWeeklyDays(rruleParts, recurrencyObj);
        }

        // Add UNTIL parameter if lastInstance is set
        Date lastInstance = recurrencyObj.getDateValue(EventConstants.PROPERTY_LASTINSTANCE_NAME);
        if (lastInstance != null) {
            String untilValue = formatUntilDate(lastInstance, allDay);
            if (StringUtils.isNotBlank(untilValue)) {
                rruleParts.add("UNTIL=" + untilValue);
            }
        }
        return String.join(";", rruleParts);
    }

    private void addCustomWeeklyDays(List<String> rruleParts, BaseObject recurrencyObj)
    {
        List<String> daysList =
            Objects.requireNonNullElseGet((List<?>) recurrencyObj.getListValue("days"), Collections::emptyList).stream()
                .map(String::valueOf).collect(Collectors.toList());
        if (daysList.isEmpty()) {
            return;
        }
        List<String> byDay = new ArrayList<>();
        for (String dayCode : daysList) {
            String day = DAY_MAP.get(dayCode.trim());
            if (day != null) {
                byDay.add(day);
            }
        }
        if (!byDay.isEmpty()) {
            rruleParts.add("BYDAY=" + String.join(",", byDay));
        }
    }

    /**
     * Formats the UNTIL date for the recurrence rule.
     *
     * @param untilDate the date to format
     * @param allDay    flag indicating if this is an all-day event
     * @return the formatted date string, or empty String if formatting fails
     */
    private String formatUntilDate(Date untilDate, int allDay)
    {
        try {
            if (allDay == 1) {
                // Format as YYYYMMDD for all-day events
                java.util.Calendar calUntil = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
                calUntil.setTime(untilDate);
                int year = calUntil.get(java.util.Calendar.YEAR);
                int month = calUntil.get(java.util.Calendar.MONTH) + 1;
                int day = calUntil.get(java.util.Calendar.DAY_OF_MONTH);
                return String.format("%04d%02d%02d", year, month, day);
            } else {
                // Format as YYYYMMDDTHHMMSSZ for timed events
                Instant instant = untilDate.toInstant();
                return DATETIME_FORMATTER.format(instant);
            }
        } catch (Exception e) {
            this.logger.error("Failed to format UNTIL date", e);
            return "";
        }
    }
}
