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
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;

/**
 * Fetch documents according to some criteria from the database.
 * This helper also removes documents not visible from the current user from the result set.
 *
 * @version $Id: $
 * @since 2.11
 */
@Singleton
@Component(roles = { DefaultEventAssembly.class })
@Unstable
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

    /**
     * Run the given query and return a list of matching documents.
     * This also filters entries not visible to the current user.
     *
     * @param query must not be null
     * @return a list of document references, never null and not containing nulls
     * @throws QueryException if there are problems with the query
     * @see QueryManager#createQuery(String, String)
     * @see Query#execute()
     */
    public List<DocumentReference> executeQuery(EventQuery query) throws QueryException
    {
        StringBuilder hql = new StringBuilder();
        hql.append(query.selectClause).append(' ').append(query.whereClause).append(' ').append(query.orderClause);
        Query hqlQuery = queryManager.createQuery(hql.toString(), Query.HQL);
        hqlQuery.setWiki(query.getWikiId());

        for (Map.Entry<String, Object> param : query.queryParams.entrySet()) {
            hqlQuery.bindValue(param.getKey(), param.getValue());
        }
        hqlQuery.addFilter(hidden);

        logger.debug("sending query [{}] and params [{}]", hqlQuery.getStatement(), query.queryParams);
        List<String> results = hqlQuery.execute();

        return filterViewableEvents(results, query.getWikiId());
    }

    /**
     * Helper method which delegates the work to {@link #filterViewableEvents(List, String)}.
     * @param eventDocRefs a list of strings, not null.
     * @return the corresponding documents, filtered by view rights
     */
    public List<DocumentReference> filterViewableEvents(List<String> eventDocRefs) {
        return filterViewableEvents(eventDocRefs, null);
    }

    /**
     * Helper to resolve a list of strings into their document references.
     * This also filters away all documents not visible to the current user;
     * the returned result list will be shorter if not visible documents are found.
     *
     * Unstable: This method is only public as it is used to filter calendars, too.
     * It will be removed and replaced by a throughout use of the "viewable" query filter.
     *
     * @param eventDocRefs a list of strings, not null.
     * @param wiki wiki identifier
     * @return the corresponding documents, filtered by view rights.
     */
    public List<DocumentReference> filterViewableEvents(List<String> eventDocRefs, String wiki)
    {
        List<DocumentReference> visibleRefs = new ArrayList<>();
        // check view rights on results ... should use "viewable" filter when minimal platform version is >= 9.8
        final DocumentReference userReference = xcontextProvider.get().getUserReference();
        for (ListIterator<String> iter = eventDocRefs.listIterator(); iter.hasNext();) {
            DocumentReference eventDocRef = null;
            if (wiki != null) {
                eventDocRef = stringDocRefResolver.resolve(iter.next(), new WikiReference(wiki));
            } else {
                eventDocRef = stringDocRefResolver.resolve(iter.next());
            }
            if (authorizationManager.hasAccess(Right.VIEW, userReference, eventDocRef)) {
                visibleRefs.add(eventDocRef);
            }
        }

        return visibleRefs;
    }
}
