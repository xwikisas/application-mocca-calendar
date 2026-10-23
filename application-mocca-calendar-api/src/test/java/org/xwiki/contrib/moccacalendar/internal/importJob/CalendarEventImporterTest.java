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
package org.xwiki.contrib.moccacalendar.internal.importJob;

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CalendarEventImporter}.
 *
 * @version $Id$
 */
@ComponentTest
class CalendarEventImporterTest
{
    private static final String PARENT = "xwiki:Space.Calendar.WebHome";

    private static final String EVENT_NAME = "Meeting";

    private static final DocumentReference PARENT_REF =
        new DocumentReference("xwiki", List.of("Space", "Calendar"), "WebHome");

    private static final DocumentReference EVENT_REF =
        new DocumentReference("xwiki", List.of("Space", "Calendar", EVENT_NAME), "WebHome");

    @InjectMockComponents
    private CalendarEventImporter importer;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument eventDoc;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.wikiContextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.documentReferenceResolver.resolve(PARENT)).thenReturn(PARENT_REF);
        when(this.wiki.getDocument(EVENT_REF, this.context)).thenReturn(this.eventDoc);
        when(this.eventDoc.isNew()).thenReturn(true);
    }

    @Test
    void getUniqueEventName() throws Exception
    {
        assertSame(this.eventDoc, this.importer.getUniqueEventName(EVENT_NAME, PARENT, List.of()));

        verify(this.eventDoc).setTitle(EVENT_NAME);
        verify(this.eventDoc).setParentReference(PARENT_REF);
    }

    @Test
    void getUniqueEventNameWhenDocumentExists() throws Exception
    {
        when(this.eventDoc.isNew()).thenReturn(false);

        assertRenamed(this.importer.getUniqueEventName(EVENT_NAME, PARENT, List.of()));
    }

    @Test
    void getUniqueEventNameWhenSameNameInImport() throws Exception
    {
        assertRenamed(
            this.importer.getUniqueEventName(EVENT_NAME, PARENT, List.of(new XWikiDocument(EVENT_REF))));
    }

    private void assertRenamed(XWikiDocument eventDoc)
    {
        DocumentReference eventRef = eventDoc.getDocumentReference();
        assertNotEquals(EVENT_REF, eventRef);
        // The renamed event stays a new child space of the calendar space.
        assertEquals(PARENT_REF.getLastSpaceReference(), eventRef.getLastSpaceReference().getParent());
        assertEquals(EVENT_NAME, eventDoc.getTitle());
    }
}
