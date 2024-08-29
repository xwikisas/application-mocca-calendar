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
package org.xwiki.contrib.moccacalendar.rest;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

/**
 * Provides the APIs needed by the Mocca Calendar server in order to import an ical file.
 *
 * @version $Id$
 * @since 2.14
 */
@Unstable
@Path("/moccacalendar")
public interface MoccaCalendarResource
{
    /**
     * Import a given ical file and create a job that handles the event processing.
     * @param parentCalendar the calendar for which the events are created.
     * @param file the file to be processed.
     * @return HTML status code 202 to hint that the file had been accepted and the job started.
     * @throws XWikiRestException if an error occurred while creating the job.
     */
    @POST
    @Path("/import")
    Response importCalendarFile(@QueryParam("parentCalendar") String parentCalendar, byte[] file)
        throws XWikiRestException;
}
