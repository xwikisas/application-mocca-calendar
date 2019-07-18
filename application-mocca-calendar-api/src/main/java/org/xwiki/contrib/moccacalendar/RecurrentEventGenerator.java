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

import java.util.Date;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A generator that creates actual event instances from a given recurrent event.
 *  
 * @version $Id: $
 * @since 2.7
 */
@Role
@Unstable
public interface RecurrentEventGenerator
{

    /**
     * given the event document create a list of event instances happening between {@code dateFrom} and {@code dateTo}.
     * 
     * @param event
     *            the document describing the recurrent event. This event is guaranteed to contain a
     *            {@code MoccaCalendarEventRecurrencyClass} object with a frequency matching the name of the generator.
     * @param dateFrom
     *            the date after which generated event instances should end
     * @param dateTo
     *            the date before which generated event instances should start
     * @return a list of event instances; might be empty but should not be null
     */
    List<EventInstance> generate(XWikiDocument event, Date dateFrom, Date dateTo);
}
