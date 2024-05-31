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

import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Represents a request to start a import job.
 *
 * @version $Id$
 * @since 2.14
 */
@Unstable
public class ImportJobRequest extends AbstractRequest
{
    private byte[] file;

    private String parentRef;

    private DocumentReference userReference;

    /**
     * Default constructor.
     */
    public ImportJobRequest()
    {
        setDefaultId();
    }

    /**
     * Creates a specific request for the ical file import job.
     *
     * @param requestId the ID of the request.
     * @param file the file to be processed.
     * @param parentRef the reference to the parent calendar.
     * @param userReference the user who requests the job.
     */
    public ImportJobRequest(List<String> requestId, byte[] file, String parentRef, DocumentReference userReference)
    {

        setId(requestId);
        this.file = file;
        this.parentRef = parentRef;
        this.userReference = userReference;
    }

    /**
     * Get the content of the given file.
     *
     * @return the content of the file as a {@link Byte} array.
     */
    public byte[] getFile()
    {
        return file;
    }

    /**
     * Get the reference to the parent calendar.
     *
     * @return the reference to the parent calendar.
     */
    public String getParentRef()
    {
        return parentRef;
    }

    /**
     * Get the reference to the user who requested the import.
     *
     * @return the reference to the user.
     */
    public DocumentReference getUserReference()
    {
        return userReference;
    }

    private void setDefaultId()
    {
        List<String> id = new ArrayList<>();
        id.add("moccaCalendar");
        id.add("import");
        setId(id);
    }
}
