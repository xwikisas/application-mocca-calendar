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
 * Data container describing an actual event.
 * This might be either a single event or an instance of a recurrent event.
 *
 * For recurrent events the generators must set the startDate and endDate
 * for the event; the rest can be figured out by the calendar service.
 *
 * @version $Id: $
 * @since 2.7
 */
public class EventInstance
{
    private DocumentReference eventDocRef;

    private DateTime startDate;
    private DateTime originalStartDate;
    private DateTime endDate;
    private DateTime endDateExclusive;

    private boolean allDay;
    private boolean recurrent;

    private boolean modifiable;
    private boolean movable;

    private String title;
    private String description;
    private String descriptionHtml;

    private String textColor;
    private String backgroundColor;

    private String source;

    /**
     * A reference to the document describing the event.
     *
     * @return the document reference for the event
     */
    public DocumentReference getEventDocRef()
    {
        return eventDocRef;
    }

    /**
     * Set the reference to the document describing the event.
     *
     * @param eventDocRef a document reference
     */
    public void setEventDocRef(DocumentReference eventDocRef)
    {
        this.eventDocRef = eventDocRef;
    }

    /**
     * The date when the event starts.
     *
     * @return the date when the event starts
     */
    public DateTime getStartDate()
    {
        return startDate;
    }

    /**
     * Set the start date of the event.
     * This value must be set by event generators.
     *
     * @param startDate a (jodatime) date. The time part should be 00:00 for all day events
     */
    public void setStartDate(DateTime startDate)
    {
        this.startDate = startDate;
    }

    /**
     * The date when the event would have started if not modified.
     * This can be null if the event is not modified.
     *
     * @return the date when the event would have started if not modified.
     */
    public DateTime getOriginalStartDate()
    {
        return originalStartDate;
    }

    /**
     * Set the date when the event would have started if not modified.
     *
     * @param originalStartDate a (jodatime) date. The time part should be 00:00 for all day events
     */
    public void setOriginalStartDate(DateTime originalStartDate)
    {
        this.originalStartDate = originalStartDate;
    }

    /**
     * The date when the event ends.
     *
     * @return the date when the event ends
     */
    public DateTime getEndDate()
    {
        return endDate;
    }

    /**
     * Set the end date for the event.
     * This value must be set by event generators.
     *
     * @param endDate a (jodatime) date. The time part should be 00:00 for all-day events
     */
    public void setEndDate(DateTime endDate)
    {
        this.endDate = endDate;
    }

    /**
     * A date after the event has ended.
     *
     * @return a (jodatime) date
     */
    public DateTime getEndDateExclusive()
    {
        return endDateExclusive;
    }

    /**
     * Set a date after the event has ended.
     * This should be the start of the next day after the event for all-day events.
     *
     * @param endDateExclusive a date after the event
     */
    public void setEndDateExclusive(DateTime endDateExclusive)
    {
        this.endDateExclusive = endDateExclusive;
    }

    /**
     * A flag if the event last a full day (or several full days). This information can be figured out by the
     * {@link #getStartDate()} and {@link #getEndDate()} and is only provided here for convenience.
     *
     * @return true if the event lasts all day
     */
    public boolean isAllDay()
    {
        return allDay;
    }

    /**
     * Set the flag if the event lasts all day.
     *
     * @param allDay .
     */
    public void setAllDay(boolean allDay)
    {
        this.allDay = allDay;
    }

    /**
     * A flag if this event instance is part of a recurrent event or not.
     *
     * @return true if the event instance is member of recurrent series of events
     */
    public boolean isRecurrent()
    {
        return recurrent;
    }

    /**
     * Set the recurrency flag.
     *
     * @param recurrent .
     */
    public void setRecurrent(boolean recurrent)
    {
        this.recurrent = recurrent;
    }

    /**
     * The title of the event.
     *
     * @return the title to be displayed in the overview
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Set the title of the event.
     *
     * @param title should not be null or empty
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * The longer description of the event, as plain text.
     *
     * @return the description of the event without HTML markup
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Set the longer description of the event.
     *
     * @param description the description of the event without HTML markup.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * The longer description of the event, as HTML markup text.
     *
     * @return the description as HTML
     */
    public String getDescriptionHtml()
    {
        return descriptionHtml;
    }

    /**
     * Set the longer description as HTML.
     *
     * @param descriptionHtml the description as HTML.
     */
    public void setDescriptionHtml(String descriptionHtml)
    {
        this.descriptionHtml = descriptionHtml;
    }

    /**
     * The font color to be used to render this event in the calendar overview
     * (as RGB string).
     *
     * @return a color as RGB string
     */
    public String getTextColor()
    {
        return textColor;
    }

    /**
     * Set the font color to be used when displaying the event.
     *
     * @param textColor a color as RGB string.
     */
    public void setTextColor(String textColor)
    {
        this.textColor = textColor;
    }

    /**
     * Color for the background when displaying this event (as RGB string).
     *
     * @return a color as RGB string
     */
    public String getBackgroundColor()
    {
        return backgroundColor;
    }

    /**
     * Set the background color.
     *
     * @param backgroundColor a color as RGB string.
     */
    public void setBackgroundColor(String backgroundColor)
    {
        this.backgroundColor = backgroundColor;
    }

    /**
     * The informal name of the source this event is coming from.
     * The default value is null, which means it is created by the event calendar itself.
     *
     * @return the source name, possibly null.
     */
    public String getSource()
    {
        return source;
    }

    /**
     * Set the name of the source which has created this event.
     *
     * @param source the source name
     */
    public void setSource(String source)
    {
        this.source = source;
    }

    /**
     * Indicate if this event is modifiable.
     * This does not include any rights check, just signaling if the event source
     * supports storing modifications to this event
     * @return true if modifiable
     */
    public boolean isModifiable()
    {
        return modifiable;
    }

    /**
     * Set the indicator if this event is modifiable.
     * This should be called only by the event source
     * @param modifiable true if modifiable
     */
    public void setModifiable(boolean modifiable)
    {
        this.modifiable = modifiable;
    }

    /**
     * Indicate if this event is movable, i.e. start and end date can be updated.
     * This does not include any rights check, just signaling if the event source
     * supports storing date changes to this event
     * @return true if movable
     */
    public boolean isMovable()
    {
        return movable;
    }

    /**
     * Set the indicator if this event is movable.
     * This should be called only by the event source
     * @param movable true if movable
     */
    public void setMovable(boolean movable)
    {
        this.movable = movable;
    }

}
