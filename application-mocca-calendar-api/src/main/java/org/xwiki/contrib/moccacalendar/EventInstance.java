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
package org.xwiki.contrib.moccacalendar;

import org.joda.time.DateTime;

import org.xwiki.model.reference.DocumentReference;

/**
 * Data container decribing an actual event.
 * This might be either a single event or an instance of a recurrent event.
 *
 * For recurrent events the generators must set the startDate and endDate
 * for the event; the rest can be figured out by the calendar service.
 *
 * @version $Id: $
`* @since 2.7
 */
public class EventInstance
{
    private DocumentReference eventDocRef;

    private DateTime startDate;
    private DateTime endDate;
    private DateTime endDateExclusive;

    private boolean allDay;
    private boolean recurrent;

    private String title;
    private String description;
    private String descriptionHtml;

    private String textColor;
    private String backgroundColor;

    /**
     * a reference to the document describing the event.
     *
     * @return the document reference for the event
     */
    public DocumentReference getEventDocRef()
    {
        return eventDocRef;
    }

    /**
     * set the reference to the document describing the event.
     *
     * @param eventDocRef a document reference
     */
    public void setEventDocRef(DocumentReference eventDocRef)
    {
        this.eventDocRef = eventDocRef;
    }

    /**
     * the date when the event starts.
     *
     * @return the date when the event starts
     */
    public DateTime getStartDate()
    {
        return startDate;
    }

    /**
     * set the start date of the event.
     * this value must be set by event generators.
     *
     * @param startDate a (jodatime) date. The time part should be 00:00 for all day events
     */
    public void setStartDate(DateTime startDate)
    {
        this.startDate = startDate;
    }

    /**
     * the date when the event ends.
     *
     * @return the date when the event ends
     */
    public DateTime getEndDate()
    {
        return endDate;
    }

    /**
     * set the end date for the event.
     * this value must be set by event generators.
     *
     * @param endDate a (jodatime) date. The time part should be 00:00 for all-day events
     */
    public void setEndDate(DateTime endDate)
    {
        this.endDate = endDate;
    }

    /**
     * a date after the event has ended.
     *
     * @return a (jodatime) date
     */
    public DateTime getEndDateExclusive()
    {
        return endDateExclusive;
    }

    /**
     * set a date after the event has ended.
     * this should be the start of the next day after the event for all-day events.
     *
     * @param endDateExclusive a date after the event
     */
    public void setEndDateExclusive(DateTime endDateExclusive)
    {
        this.endDateExclusive = endDateExclusive;
    }

    /**
     * a flag if the event last a full day (or several full days). This information can be figured out by the {@link #getStartDate()} and
     * {@link #getEndDate()} and is only here for convenience.
     *
     * @return true if the event lasts all day
     */
    public boolean isAllDay()
    {
        return allDay;
    }

    /**
     * set the flag if the even lasts all day.
     *
     * @param allDay .
     */
    public void setAllDay(boolean allDay)
    {
        this.allDay = allDay;
    }

    /**
     * a flag if this event instance is part of a recurrent event or not.
     *
     * @return true if the event instance is member of recurrent series of events
     */
    public boolean isRecurrent()
    {
        return recurrent;
    }

    /**
     * set the recurrency flag.
     *
     * @param recurrent .
     */
    public void setRecurrent(boolean recurrent)
    {
        this.recurrent = recurrent;
    }

    /**
     * the title of the event.
     *
     * @return the title to be displayed in the overview
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * set the title of the event.
     *
     * @param title should not be null or empty
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * the longer description of the event, as plain text.
     *
     * @return the description of the event without HTML markup
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * set the longer description of the event.
     *
     * @param description .
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * the longer description of the event, as HTML markup text.
     *
     * @return the description as HTML
     */
    public String getDescriptionHtml()
    {
        return descriptionHtml;
    }

    /**
     * set the longer description as HTML.
     *
     * @param descriptionHtml .
     */
    public void setDescriptionHtml(String descriptionHtml)
    {
        this.descriptionHtml = descriptionHtml;
    }

    /**
     * the font color to be used to render this event in die calendar overview
     * (as RGB string).
     *
     * @return a color as RGB string
     */
    public String getTextColor()
    {
        return textColor;
    }

    /**
     * set the font color to be used when displaying the event.
     *
     * @param textColor .
     */
    public void setTextColor(String textColor)
    {
        this.textColor = textColor;
    }

    /**
     * color for the background when displaying this event (as RGB string).
     *
     * @return a color as RGB string
     */
    public String getBackgroundColor()
    {
        return backgroundColor;
    }

    /**
     * set the background color.
     * @param backgroundColor .
     */
    public void setBackgroundColor(String backgroundColor)
    {
        this.backgroundColor = backgroundColor;
    }

}
