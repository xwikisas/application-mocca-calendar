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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.moccacalendar.migrations.AddReccurrentProperty;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Move an event if its calendar is changed.
 *
 * @version $Id: $
 * @since 2.7.1
 */
@Named("org.xwiki.contrib.moccacalendar.internal.CalendarEventParentChangeListener")
@Singleton
@Component(roles = { AddReccurrentProperty.class, EventListener.class })

public class CalendarEventParentChangeListener implements EventListener
{
    @Inject
    private Logger logger;

    @Inject
    private EntityReferenceProvider refProvider;

    @Override
    public String getName()
    {
        return getClass().getName();
    }

    @Override
    public List<Event> getEvents()
    {
        // listen after the document has been saved
        // otherwise if we need to rename the document, it is later saved at the old location
        // which results in a copy
        return Arrays.asList(new DocumentUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // DocumentUpdatingEvent updatingEvent = (DocumentUpdatingEvent) event;
        XWikiDocument doc = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        BaseObject eventData = doc
            .getXObject(doc.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME));

        if (eventData == null) {
            return;
        }

        XWikiDocument originalDocument = doc.getOriginalDocument();

        // better instead:
        if (parentMatchesLocationParent(doc)) {
            logger.debug("document parent for event [{}] is ok - no move needed", doc.getDocumentReference());
            return;
        }

        DocumentReference targetReference = newDocumentLocation(doc, context);
        try {
            doc.rename(targetReference, context);
            logger.debug("renamed document [{}] to [{}]", originalDocument.getDocumentReference(),
                doc.getDocumentReference());
        } catch (XWikiException e) {
            logger.warn("could not move document [{}] to its new parent [{}]", doc.getDocumentReference(),
                targetReference, e);
        }
    }

    private boolean parentMatchesLocationParent(XWikiDocument doc)
    {
        DocumentReference docRef = doc.getDocumentReference();
        DocumentReference parentRef = doc.getParentReference();

        logger.trace("check document [{}] with parent ref [{}]", docRef, parentRef);

        SpaceReference parentLocation = parentRef.getLastSpaceReference();
        SpaceReference locationParent = docRef.getLastSpaceReference();

        String defaultPageName = refProvider.getDefaultReference(EntityType.DOCUMENT).getName();
        if (defaultPageName.equals(docRef.getName())) {
            EntityReference locationParentParent = locationParent.getParent();
            if (locationParentParent instanceof SpaceReference) {
                locationParent = (SpaceReference) locationParentParent;
            } else {
                locationParent = null;
            }
        }

        logger.trace("parent ref is [{}] while location parent is [{}]", parentLocation, locationParent);

        return parentLocation.equals(locationParent);
    }

    private DocumentReference newDocumentLocation(XWikiDocument eventDocument, XWikiContext context)
    {
        DocumentReference docRef = eventDocument.getDocumentReference();
        DocumentReference parentRef = eventDocument.getParentReference();

        SpaceReference targetSpaceRef = parentRef.getLastSpaceReference();
        if (targetSpaceRef == null) {
            // XXX how can this happen?
            logger.warn("could not move event [{}] to calendar [{}], as this has no space", docRef,
                parentRef);
            return docRef;
        }

        DocumentReference targetReference = null;
        EntityReference defaultRef = refProvider.getDefaultReference(EntityType.DOCUMENT);
        if (docRef.getName().equals(defaultRef.getName())) {
            // we are a non-terminal page
            targetReference = getDocumentWithUnusedSpace(docRef, targetSpaceRef, context);
        } else {
            targetReference = getUnusedDocument(docRef, targetSpaceRef, context);
        }

        logger.trace("new location for document [{}] is computed to [{}]", docRef, targetReference);

        return targetReference;
    }

    private DocumentReference getUnusedDocument(DocumentReference docRef, SpaceReference targetSpaceRef,
        XWikiContext context)
    {
        String origDocName = docRef.getName();
        DocumentReference targetDocReference = new DocumentReference(origDocName, targetSpaceRef);

        // if this does not work the first time around it is likely to burn lots of CPU cycles
        // but that is very unlikely to happen ...
        while (context.getWiki().exists(targetDocReference, context)) {
            logger.trace("doc already exists [{}]; retry", targetDocReference);
            String docName = origDocName + '_' + String.valueOf(System.currentTimeMillis());
            targetDocReference = new DocumentReference(docName, targetSpaceRef);
        }

        return targetDocReference;
    }

    private DocumentReference getDocumentWithUnusedSpace(DocumentReference docRef,
        SpaceReference targetParentSpace, XWikiContext context)
    {
        String origDocSpaceName = docRef.getLastSpaceReference().getName();
        SpaceReference targetDocSpace = new SpaceReference(origDocSpaceName, targetParentSpace);
        DocumentReference targetReference = new DocumentReference(docRef.getName(), targetDocSpace);

        // see above: should not loop more than once
        while (context.getWiki().exists(targetReference, context)) {
            logger.trace("space already exists [{}]; retry", targetReference);

            String docSpaceName = origDocSpaceName + '_' + String.valueOf(System.currentTimeMillis());
            targetDocSpace = new SpaceReference(docSpaceName, targetParentSpace);
            targetReference = new DocumentReference(docRef.getName(), targetDocSpace);
        }

        return targetReference;
    }
}
