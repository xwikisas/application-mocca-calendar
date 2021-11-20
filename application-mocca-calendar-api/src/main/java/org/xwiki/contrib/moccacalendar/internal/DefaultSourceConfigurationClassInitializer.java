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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * A configuration class for all classes that do not want to define their own configuration.
 * This class also has a "source" field that contains the name of the event source to be configured.
 *
 * @version $Id: $
 * @since 2.11
 */
@Component
@Named(AbstractSourceConfigurationClassInitializer.EVENT_SOURCE_CLASSES_SPACE_PREFIX
    + DefaultSourceConfigurationClassInitializer.DEFAULT_SOURCE_CONFIG_CLASS_NAME)
@Singleton
public class DefaultSourceConfigurationClassInitializer extends AbstractSourceConfigurationClassInitializer
{
    /** The page name of the default event source configuration class. */
    public static final String DEFAULT_SOURCE_CONFIG_CLASS_NAME = "DefaultEventSourceConfigClass";
    /** The name of the field to store the name of the source. */
    public static final String SOURCE_NAME_FIELD = "source";

    /**
     * Default constructor.
     */
    public DefaultSourceConfigurationClassInitializer()
    {
        super(DEFAULT_SOURCE_CONFIG_CLASS_NAME);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        super.createClass(xclass);
        xclass.addTextField(SOURCE_NAME_FIELD, "Source Name", 30);
    }

    /**
     * Get the reference to the default source configuration class.
     * @return the local reference to the default configuration class.
     */
    public static LocalDocumentReference getConfigurationClass()
    {
        return new LocalDocumentReference(DEFAULT_SOURCE_CONFIG_CLASS_NAME,
            AbstractSourceConfigurationClassInitializer.getDefaultConfigClassSpace());
    }
}
