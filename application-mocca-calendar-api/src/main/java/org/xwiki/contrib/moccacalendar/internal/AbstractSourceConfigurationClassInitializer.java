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
package org.xwiki.contrib.moccacalendar.internal;

import java.util.Arrays;
import java.util.List;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Abstract initializer class to add configuration classes for event sources.
 *
 * @version $Id: $
 * @since 2.11
 */
public abstract class AbstractSourceConfigurationClassInitializer extends AbstractMandatoryClassInitializer
{
    public static final String EVENT_SOURCE_CLASSES_SPACE_PREFIX = "MoccaCalendar.Code.";
    public static final String ACTIVE_FIELD = "active";

    // you know, because checkstyle
    private static final List<String> CODE_SPACE = Arrays.asList("MoccaCalendar", "Code");

    /**
     * Initialize a configuration class by its class name only.
     * The space for the class will be the default code space for the calendar.
     *
     * @param className the page name of the class
     */
    public AbstractSourceConfigurationClassInitializer(String className)
    {
        super(new LocalDocumentReference(CODE_SPACE, className));
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addBooleanField(ACTIVE_FIELD, "", "checkbox", Boolean.FALSE);
    }

    /**
     * We do not need a document sheet for these classes.
     */
    protected boolean updateDocumentSheet(XWikiDocument document)
    {
        return false;
    }

    public static EntityReference getDefaultConfigClassSpace()
    {
        return new LocalDocumentReference(CODE_SPACE, "WebHome").getParent();
    }
}
