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
package org.xwiki.contrib.moccacalendar.internal.meetings;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.internal.AbstractSourceConfigurationClassInitializer;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Initialize the source for the meetings to be displayed in the calendar.
 *
 * @version $Id: $
 * @since 2.11
 */
@Component
@Named(AbstractSourceConfigurationClassInitializer.EVENT_SOURCE_CLASSES_SPACE_PREFIX
    + MeetingsSourceConfigurationClassInitializer.MEETINGS_CONFIG_CLASS_NAME)
@Singleton
public class MeetingsSourceConfigurationClassInitializer extends AbstractSourceConfigurationClassInitializer
{
    static final String MEETINGS_CONFIG_CLASS_NAME = "MeetingsSourceConfigClass";
    static final String MEETINGS_PAGE_FIELD_NAME = "meetings";

    /**
     * Default constructor.
     */
    public MeetingsSourceConfigurationClassInitializer()
    {
        super(MEETINGS_CONFIG_CLASS_NAME);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        super.createClass(xclass);
        xclass.addPageField(MEETINGS_PAGE_FIELD_NAME, "Meetings", 0, false, false,
            "select doc.fullName from XWikiDocument as doc, BaseObject as sheet, StringProperty as name where"
                + " doc.fullName = sheet.name and sheet.className='XWiki.DocumentSheetBinding'"
                + " and sheet.id = name.id.id and name.id.name='sheet' and name.value='Meeting.Code.WebHomeSheet'"
                + " and doc.name not like '%Template'");
    }

    static LocalDocumentReference classRef() {
        return new LocalDocumentReference(MEETINGS_CONFIG_CLASS_NAME,
            AbstractSourceConfigurationClassInitializer.getDefaultConfigClassSpace());
    }
}
