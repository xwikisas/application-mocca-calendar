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
package org.xwiki.contrib.moccacalendar.internal.rest;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.importJob.ImportJobRequest;
import org.xwiki.contrib.moccacalendar.internal.importJob.ImportJob;
import org.xwiki.contrib.moccacalendar.rest.MoccaCalendarResource;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.rest.internal.resources.pages.ModifiablePageResource;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of {@link MoccaCalendarResource}.
 *
 * @version $Id$
 * @since 2.14
 */
@Unstable
@Component
@Named("org.xwiki.contrib.moccacalendar.internal.rest.DefaultMoccaCalendarResource")
@Singleton
public class DefaultMoccaCalendarResource extends ModifiablePageResource implements MoccaCalendarResource
{
    @Inject
    private Logger logger;

    @Inject
    private JobExecutor jobExecutor;

    @Override
    public Response importCalendarFile(String parentCalendar, byte[] file)
    {
        try {
            XWikiContext wikiContext = xcontextProvider.get();
            List<String> jobId = new ArrayList<>();
            jobId.add("moccacalendar");
            jobId.add("import");
            jobId.add(parentCalendar);
            Job job = this.jobExecutor.getJob(jobId);
            if (job == null) {
                ImportJobRequest importJobRequest =
                    new ImportJobRequest(jobId, file, parentCalendar, wikiContext.getUserReference());
                this.jobExecutor.execute(ImportJob.JOB_TYPE, importJobRequest);
                return Response.status(202).type(MediaType.TEXT_PLAIN_TYPE).build();
            } else {
                return Response.notModified().type(MediaType.TEXT_PLAIN_TYPE).build();
            }
        } catch (Exception e) {
            logger.warn("Failed to import .ics file data. Root cause: [{}]", ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
