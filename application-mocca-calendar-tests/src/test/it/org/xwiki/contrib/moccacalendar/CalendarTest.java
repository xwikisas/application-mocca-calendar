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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
// import org.junit.Rule;

import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.ui.AbstractTest;
// import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * UI Tests for the MoccaCalendar application - Stub.
 */
public class CalendarTest extends AbstractTest
{

    // use this when dependency on xwiki > 5.1
    // @Rule
    // public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil(), getDriver());

    @Before
    public void setUp()
    {
        // Login as superadmin to have delete rights.
        getDriver().get(getUtil().getURLToLoginAs("superadmin", "pass"));
        getUtil().recacheSecretToken();
    }

    @Test
    public void testViewCalendar()
    {
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("Calendar");
        Assert.assertEquals("MoccaCalendar", vp.getMetaDataValue("space"));
        Assert.assertEquals("WebHome", vp.getMetaDataValue("page"));
        // TODO: really test something ...
    }
}
