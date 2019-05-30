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
 * Data container decribing an actual event. This might be either a single event or an instance of a recurrent event.
 *
 * @version $Id: $
`* @since 2.7
 */
// FIXME: should this be an interface with only getters instead?
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
     */
    public DocumentReference getEventDocRef()
    {
        return eventDocRef;
    }

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

    public void setStartDate(DateTime startDate)
    {
        this.startDate = startDate;
    }

    /**
     * the date when the event ends.
     */
    public DateTime getEndDate()
    {
        return endDate;
    }

    public void setEndDate(DateTime endDate)
    {
        this.endDate = endDate;
    }

    /**
     * a date after the event has ended.
     */
    public DateTime getEndDateExclusive()
    {
        return endDateExclusive;
    }

    public void setEndDateExclusive(DateTime endDateExclusive)
    {
        this.endDateExclusive = endDateExclusive;
    }

    /**
     * a flag if the event last a full day (or several full days). This information can be figured out by the {@link #getStartDate()} and
     * {@link #getEndDate()} and is only here for convenience.
     */
    public boolean isAllDay()
    {
        return allDay;
    }

    public void setAllDay(boolean allDay)
    {
        this.allDay = allDay;
    }

    /**
     * a flag if this event instance is part of a recurrent event or not.
     */
    public boolean isRecurrent()
    {
        return recurrent;
    }

    public void setRecurrent(boolean recurrent)
    {
        this.recurrent = recurrent;
    }

    /**
     * the title of the event.
     */
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * the longer decription of the event, as plain text.
     * 
     * @return
     */
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * the longer decription of the event, as HTML markup text.
     * 
     * @return
     */
    public String getDescriptionHtml()
    {
        return descriptionHtml;
    }

    public void setDescriptionHtml(String descriptionHtml)
    {
        this.descriptionHtml = descriptionHtml;
    }

    /**
     * color to be used to render this event (as RGB string).
     */
    public String getTextColor()
    {
        return textColor;
    }

    public void setTextColor(String textColor)
    {
        this.textColor = textColor;
    }

    /**
     * color for the background when displaying this event (as RGB string).
     */
    public String getBackgroundColor()
    {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor)
    {
        this.backgroundColor = backgroundColor;
    }

}
