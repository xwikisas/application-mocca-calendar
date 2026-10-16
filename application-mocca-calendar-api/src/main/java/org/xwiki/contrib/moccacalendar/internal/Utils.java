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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xwiki.contrib.moccacalendar.EventInstance;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Some internal helper methods that did not fit in elsewhere. Do not expect
 * these helpers or the complete class to form a stable API; they might go away sooner or later.
 *
 * @version $Id: $
 * @since 2.7
 */
@Unstable
public final class Utils
{

    /*
     * the hard wired guess of the duration of an event where the end date is missing.
     */
    private static final int EVENT_DURATION_MIN = 30;

    private static final String PARAGRAPH = "p";

    private static final List<String> PREVIEW_TEXT_BLOCKS = List.of(PARAGRAPH, "ul", "ol");

    private static final List<String> PREVIEW_IMAGE_BLOCKS = List.of(PARAGRAPH, "figure");

    private static final String IMAGE = "img";

    private static final int PREVIEW_MAX_IMAGES = 2;

    private Utils()
    {
        // no instances, please
    }

    /**
     * Return the end date for the given object.
     * This uses default values to determine the names of the start and end date properties.
     *
     * @param eventData the object describing the data.
     *            should have a 'startDate' and optional 'allDay' and  'endDate' property.
     * @return the end date of the event
     * @see EventConstants
     * @see Utils#fetchOrGuessEndDate(BaseObject, String, String, String)
     */
    public static Date fetchOrGuessEndDate(BaseObject eventData)
    {
        return fetchOrGuessEndDate(eventData, EventConstants.PROPERTY_STARTDATE_NAME,
            EventConstants.PROPERTY_ENDDATE_NAME, EventConstants.PROPERTY_ALLDAY_NAME);
    }

    /**
     * Return the end date of the event (or event modification).
     * This either gets the date from the &quot;endDate&quot; property
     * or calculates it from the &quot;startDate&quot; property
     *
     * @param eventData the object describing the event
     * @param startDateName the name of the start date property
     * @param endDateName the name of the start date property
     * @param allDayName the name of the all day flag, can be null
     * @return the stored or guessed end date
     * @see Utils#guessEndDate(Date, boolean)
     */
    public static Date fetchOrGuessEndDate(BaseObject eventData, String startDateName, String endDateName,
        String allDayName)
    {
        Date endDate = eventData.getDateValue(endDateName);
        if (endDate == null) {
            final boolean allDay = (allDayName != null) && (eventData.getIntValue(allDayName) == 1);
            final Date startDate = eventData.getDateValue(startDateName);
            endDate = guessEndDate(startDate, allDay);
        }

        return endDate;
    }

    /**
     * Guess the end date of an event, given a start date.
     *
     * @param startDate the start date of the event.
     * @param allDay flag, if the event lasts all day.
     * @return the end date of the event
     */
    public static Date guessEndDate(Date startDate, boolean allDay)
    {
        Date endDate;
        if (allDay) {
            // if we have an all day event, then the hours and minutes, etc. are irrelevant.
            // assume the event lasts one day by setting the (inclusive) end date to the
            // start date
            endDate = new Date(startDate.getTime());
        } else {
            // FIXME: hardwired default event duration of 30 minutes.
            endDate = new Date(startDate.getTime() + EVENT_DURATION_MIN * 60 * 1000L);
        }
        return endDate;
    }

    /**
     * fill the description of the event instance from the value in the given base object.
     * The base object should either be a MoccaCalendarEvent or a MoccaCalendarEventModification.
     *
     * @param eventData the xwiki object to get the information from
     * @param descriptionPropertyName then name of the property storing the description
     * @param context the current context
     * @param event the event instance whose description will be set
     */
    public static void fillDescription(BaseObject eventData, String descriptionPropertyName, XWikiContext context,
        EventInstance event)
    {
        XWikiDocument eventDoc = eventData.getOwnerDocument();
        String idString = eventDoc.getSyntax().toIdString();
        String description = eventData.getStringValue(descriptionPropertyName);
        event
            .setDescription(eventDoc.getRenderedContent(description, idString, Syntax.PLAIN_1_0.toIdString(), context));
        event.setDescriptionHtml(
            eventDoc.getRenderedContent(description, idString, Syntax.HTML_5_0.toIdString(), context));
    }

    /**
     * Shortens a rendered event description for a preview: keeps its first paragraph or list, whichever comes first,
     * and at most {@value #PREVIEW_MAX_IMAGES} images (the ones of the first paragraph or list, then the ones of the
     * other paragraphs and figures). Everything else is left out.
     *
     * @param descriptionHtml the description rendered as HTML
     * @return the preview HTML under the {@code html} key, and under the {@code truncated} key whether some content was
     *     left out
     * @since 2.20.2
     */
    public static Map<String, Object> getDescriptionPreview(String descriptionHtml)
    {
        Document html = Jsoup.parseBodyFragment(StringUtils.defaultString(descriptionHtml));
        html.outputSettings().prettyPrint(false);
        Elements blocks = html.body().children();
        Element firstTextBlock =
            blocks.stream().filter(block -> PREVIEW_TEXT_BLOCKS.contains(block.tagName()) && block.hasText())
                .findFirst().orElse(null);

        // The images of the first paragraph or list come first, then the ones of the other paragraphs and figures.
        Elements images = firstTextBlock != null ? firstTextBlock.select(IMAGE) : new Elements();
        int firstTextBlockImages = images.size();
        boolean truncated = false;
        for (Element block : blocks) {
            if (block != firstTextBlock) {
                // Everything else is left out, except the images of paragraphs and figures.
                boolean isImageBlock = PREVIEW_IMAGE_BLOCKS.contains(block.tagName());
                truncated |= block.hasText() || !isImageBlock;
                if (isImageBlock) {
                    images.addAll(block.select(IMAGE));
                }
            }
        }
        if (images.size() > PREVIEW_MAX_IMAGES) {
            images.subList(PREVIEW_MAX_IMAGES, images.size()).forEach(Element::remove);
            truncated = true;
        }

        StringBuilder preview = new StringBuilder();
        if (firstTextBlock != null) {
            preview.append(firstTextBlock.outerHtml());
        }
        List<Element> otherImages = images.subList(Math.min(firstTextBlockImages, PREVIEW_MAX_IMAGES),
            Math.min(images.size(), PREVIEW_MAX_IMAGES));
        if (!otherImages.isEmpty()) {
            preview.append("<p>").append(otherImages.stream().map(Element::outerHtml).collect(Collectors.joining()))
                .append("</p>");
        }

        return Map.of("html", preview.toString(), "truncated", truncated);
    }
}
