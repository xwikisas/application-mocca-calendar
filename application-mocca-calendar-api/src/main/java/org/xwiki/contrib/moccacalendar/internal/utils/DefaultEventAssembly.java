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
package org.xwiki.contrib.moccacalendar.internal.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;

/**
 * Fetch documents according to some criteria from the database.
 * This helper also removes documents not visible from the current user from the result set.
 *
 * @version $Id: $
 * @since 11.0
 */
@Singleton
@Component(roles = { DefaultEventAssembly.class })
public class DefaultEventAssembly
{

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> stringDocRefResolver;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    @Named("hidden")
    private QueryFilter hidden;

    @Inject
    private Logger logger;

    public List<DocumentReference> executeQuery(EventQuery query) throws QueryException
    {
        StringBuilder hql = new StringBuilder();
        hql.append(query.selectClause).append(' ').append(query.whereClause).append(' ').append(query.orderClause);
        Query hqlQuery = queryManager.createQuery(hql.toString(), Query.HQL);

        for (Map.Entry<String, Object> param : query.queryParams.entrySet()) {
            hqlQuery.bindValue(param.getKey(), param.getValue());
        }

        logger.debug("sending query [{}] and params [{}]", hqlQuery.getStatement(), query.queryParams);
        List<String> results = hqlQuery.execute();

        return filterViewableEvents(results);
    }

    // this is some rather unrelated helper
    public List<DocumentReference> filterViewableEvents(List<String> eventDocRefs)
    {
        List<DocumentReference> visibleRefs = new ArrayList<>();
        // check view rights on results ... should use "viewable" filter when minimal platform version is >= 9.8
        final DocumentReference userReference = xcontextProvider.get().getUserReference();
        for (ListIterator<String> iter = eventDocRefs.listIterator(); iter.hasNext();) {
            DocumentReference eventDocRef = stringDocRefResolver.resolve(iter.next());
            if (authorizationManager.hasAccess(Right.VIEW, userReference, eventDocRef)) {
                visibleRefs.add(eventDocRef);
            }
        }

        return visibleRefs;
    }
}
