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
package org.xwiki.contrib.moccacalendar.importJob;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.job.DefaultJobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.stability.Unstable;

/**
 * The status of the import job.
 *
 * @version $Id$
 * @since 2.14
 */
@Unstable
public class ImportJobStatus extends DefaultJobStatus<ImportJobRequest>
{
    private List<String> uidList = new ArrayList<>();

    /**
     * Create a new import job status.
     *
     * @param jobType the job type.
     * @param request the request provided when the job was started.
     * @param observationManager the observation manager.
     * @param loggerManager the logger manager.
     */
    public ImportJobStatus(String jobType, ImportJobRequest request, ObservationManager observationManager,
        LoggerManager loggerManager)
    {
        super(jobType, request, null, observationManager, loggerManager);
        setCancelable(true);
    }

    /**
     * Update the events UID that have already been used.
     *
     * @param uid the id of the event.
     */
    public void storeUID(String uid)
    {
        uidList.add(uid);
    }

    /**
     * Check if an event with a given id has already been processed.
     *
     * @param uid the id of the event.
     * @return {@code true} if an event with the same id has already been processed, or {@code false} otherwise.
     */
    public boolean isDuplicate(String uid)
    {
        return uidList.contains(uid);
    }
}
