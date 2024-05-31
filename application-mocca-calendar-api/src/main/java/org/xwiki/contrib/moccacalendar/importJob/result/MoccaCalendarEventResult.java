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
package org.xwiki.contrib.moccacalendar.importJob.result;

import java.util.Date;

import org.xwiki.stability.Unstable;

/**
 * Represents the data of a mocca calendar event.
 *
 * @version $Id$
 * @since 2.14
 */
@Unstable
public class MoccaCalendarEventResult
{
    private Date startDate;

    private Date endDate;

    private Date recEndDate;

    private int isRecurrent;

    private String recurrenceFreq;

    private int allDay;

    private String description;

    private String title;

    /**
     * Default constructor.
     */
    public MoccaCalendarEventResult()
    {
        // TODO document why this constructor is empty
    }

    /**
     * Get the end date of the recurrence.
     *
     * @return the {@link Date} until the recurrence takes place.
     */
    public Date getRecEndDate()
    {
        return recEndDate;
    }

    /**
     * Set the end date of the recurrence.
     *
     * @param recEndDate the {@link Date} until the recurrence takes place.
     */
    public void setRecEndDate(Date recEndDate)
    {
        this.recEndDate = recEndDate;
    }

    /**
     * Get the end date of the processed event.
     *
     * @return the end {@link Date} of the event.
     */
    public Date getEndDate()
    {
        return endDate;
    }

    /**
     * Set the end date of the event.
     *
     * @param endDate the {@link Date} when the event ends.
     */
    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    /**
     * Get the start date of the processed event.
     *
     * @return the start {@link Date} of the event.
     */
    public Date getStartDate()
    {
        return startDate;
    }

    /**
     * Set the start date of the event.
     *
     * @param startDate the {@link Date} when the event starts.
     */
    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    /**
     * Get the all day flag.
     *
     * @return {@code 1} if the event takes all day, or {@code 0} otherwise.
     */
    public int getAllDay()
    {
        return allDay;
    }

    /**
     * Set the all day flag.
     *
     * @param allDay the all day flag. {@code 1} if the event takes all day, or {@code 0} otherwise.
     */
    public void setAllDay(int allDay)
    {
        this.allDay = allDay;
    }

    /**
     * Get the recurrence flag.
     *
     * @return {@code 1} if the event is recurrent, or {@code 0} otherwise.
     */
    public int getIsRecurrent()
    {
        return isRecurrent;
    }

    /**
     * Set the recurrence flag.
     *
     * @param isRecurrent the recurrence flag. {@code 1} if the event is recurrent, or {@code 0} otherwise.
     */
    public void setIsRecurrent(int isRecurrent)
    {
        this.isRecurrent = isRecurrent;
    }

    /**
     * Get the event description.
     *
     * @return the event description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Set the event description.
     *
     * @param description the event description.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Get the recurrence frequency of the event.
     *
     * @return the recurrence frequency of the event, if there is any.
     */
    public String getRecurrenceFreq()
    {
        return recurrenceFreq;
    }

    /**
     * Set the recurrence frequency.
     *
     * @param recurrenceFreq the event recurrence frequency.
     */
    public void setRecurrenceFreq(String recurrenceFreq)
    {
        this.recurrenceFreq = recurrenceFreq;
    }

    /**
     * Get the title of the event.
     *
     * @return the title of the event.
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Set the event title.
     *
     * @param title the event title.
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
}
