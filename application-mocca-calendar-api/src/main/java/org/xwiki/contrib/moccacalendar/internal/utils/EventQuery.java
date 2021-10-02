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

import java.lang.reflect.ParameterizedType;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.web.Utils;

/**
 * Fetch events of various types from the database.
 * @version $Id: $
 * @since 11.0
 */
public class EventQuery
{

    private static final String SELECT_CLAUSE_FORMAT = ", BaseObject as obj,"
        + " DateProperty as startdate, DateProperty as enddate";

    private static final String BASE_WHERE_CLAUSE_FORMAT = 
        "WHERE obj.id=startdate.id.id and startdate.id.name='%s'"
        + " and obj.id=enddate.id.id and enddate.id.name='%s'"
        + " and doc.fullName=obj.name and doc.fullName!='%s'"
        + " and obj.className='%s'";
    
    private static final String FILTER_WIKI = "wiki";
    private static final String FILTER_SPACE = "space";
    private static final String FILTER_PAGE = "page";
    
    private final String className;
    private final String templatePageName;
    private final String startDateName; 
    private final String endDateName;
    
    private Logger logger;
    
    protected StringBuilder selectClause = new StringBuilder();
    protected StringBuilder whereClause = new StringBuilder();
    protected StringBuilder orderClause = new StringBuilder();
    protected Map<String, Object> queryParams = new HashMap<>();
    
    public EventQuery(String className, String templatePageName, String startDateName, String endDateName)
    {
        this.className = className;
        this.templatePageName = templatePageName;
        this.startDateName = startDateName;
        this.endDateName = endDateName;
        this.logger = LoggerFactory.getLogger(this.getClass());
        initQuery();
    }
    
    public EventQuery(String className, String templatePageName)
    {
       this(className, templatePageName, EventConstants.PROPERTY_STARTDATE_NAME, EventConstants.PROPERTY_ENDDATE_NAME);
    }
    
    protected void initQuery() {
        selectClause.append(SELECT_CLAUSE_FORMAT);
        whereClause.append(String.format(BASE_WHERE_CLAUSE_FORMAT, startDateName, endDateName, templatePageName, className));
    }

    public EventQuery addSelect(String wherePart)
    {
        selectClause.append(wherePart);
        return this;
    }

    public EventQuery addObjectProperty(String propertyType, String propertyName)
    {
        addSelect(String.format(", %s as %s",
            propertyType, propertyName));
        addCondition(
            String.format(" and obj.id = %s.id.id and %s.id.name = '%s'", 
                propertyName, propertyName, propertyName));

        return this;
    }
    
    public EventQuery addCondition(String wherePart)
    {
        whereClause.append(wherePart);
        return this;
    }
    
    public EventQuery addParam(String name, Object value)
    {
        queryParams.put(name, value);
        return this;
    }

    //
    // special clause creations:
    //
    
    public EventQuery addDateLimits(Date dateFrom, Date dateTo)
    {
        // start date / lower limit check: find all events which are not finished before the start date
        // for this, confusingly, one need to compare the end date of the event with the start date for the range
        // as a complication: to find events without end date, use the start date for them
        whereClause.append(" and (enddate.value is not null and ");
        appendDateCriterion("enddate.value", "start", true);
        whereClause.append(" or ");
        appendDateCriterion("startdate.value", "start", true);
        whereClause.append(')');

        // compared to this the upper limit check is straightforward, as we always have a startDate in the event
        whereClause.append(" and ");
        appendDateCriterion("startdate.value", "end", false);
        
        appendDateParameters("start", dateFrom);
        appendDateParameters("end", dateTo);
        
        return this;
    }
    
    public EventQuery addLocationFilter(String filter, DocumentReference parentReference)
    {
        try {
        ParameterizedType stringSerializerType =
            new DefaultParameterizedType(null, EntityReferenceSerializer.class, String.class);
        @SuppressWarnings("deprecation") // we are not a component, sorry
        EntityReferenceSerializer<String> compactWikiSerializer = 
            Utils.getComponentManager().getInstance(stringSerializerType , "compact");
        
        switch (filter) {
            case FILTER_PAGE:
                selectClause.append(", XWikiSpace space");
                whereClause.append(" and doc.space = space.reference and space.parent = :space");
                queryParams.put("space", compactWikiSerializer.serialize(parentReference.getLastSpaceReference()));
                break;
            case FILTER_SPACE:
                // FIXME: we should use the "bindValue(...).literal(...) instead?
                whereClause.append(" and ( doc.space like :space escape '!')");
                String spaceRefStr = compactWikiSerializer.serialize(parentReference.getLastSpaceReference());
                String spaceLikeStr = spaceRefStr.replaceAll("([%_!])", "!$1").concat(".%");
                queryParams.put("space", spaceLikeStr);
                break;
            case FILTER_WIKI:
            default:
                // get events from the complete wiki: no filter to be added
                break;
        }
        } catch (ComponentLookupException cle) {
            logger.warn("could not find string serializer component; location filter ignored", cle);
        }
        return this;
    }

    
    public EventQuery setAscending(boolean direction) {
        orderClause.append("ORDER BY startdate.value ")
        .append(direction?"ASC":"DESC");
        return this;
    }
    
    
    // the date comparision in HQL is always a bit painful - hide it in a helper
    // for appendDateCriterion(query, "date", "field", true) this will create something like:
    //
    // ( year(date) > :fieldyear or ( year(date) = :fieldyear and ( month(date) > :fieldmonth or
    // ( month(date) = :fieldmonth and day(date) >= :fieldday ) ) ) )
    // ...
    protected void appendDateCriterion(String dateField, String prefix, boolean larger)
    {
        final char cmpSign = (larger) ? '>' : '<';

        whereClause.append("( year(" + dateField + ") ").append(cmpSign).append(" :" + prefix + "year ");
        whereClause.append(" or (year(" + dateField + ") = :" + prefix + "year and ");
        whereClause.append("(month(" + dateField + ") ").append(cmpSign).append(" :" + prefix + "month");
        whereClause.append(" or (month(" + dateField + ") = :" + prefix + "month ");
        whereClause.append(" and day(").append(dateField).append(") ").append(cmpSign).append("= :")
            .append(prefix).append("day");
        whereClause.append(')');
        whereClause.append(')');
        whereClause.append(')');
        whereClause.append(')');
    }

    protected void appendDateParameters(String prefix, Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        queryParams.put(prefix + "year", year);
        queryParams.put(prefix + "month", month);
        queryParams.put(prefix + "day", day);
    }

    //
    // setters in case we need to reuse the information
    //

    public String getClassName()
    {
        return className;
    }

    public String getTemplatePageName()
    {
        return templatePageName;
    }

    public String getStartDateName()
    {
        return startDateName;
    }

    public String getEndDateName()
    {
        return endDateName;
    }
    
}
