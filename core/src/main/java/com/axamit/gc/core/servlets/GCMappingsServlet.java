/*
 * Axamit, gc.support@axamit.com
 */

package com.axamit.gc.core.servlets;

import com.axamit.gc.core.util.Constants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.query.Query;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Servlet return list of mappings in JSON format for project ID passed with selector
 * projectId-[number] e.g. '/etc/cloudservices/gathercontent/gathercontent-importer.gcmappings.projectId-12345.json'.
 *
 * @author Axamit, gc.support@axamit.com
 */
@SlingServlet(
        resourceTypes = {"sling/servlet/default"},
        selectors = {"gcmappings"},
        methods = {HttpConstants.METHOD_GET}
)
public final class GCMappingsServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(GCMappingsServlet.class);

    private static final String MAPPING_BY_PROJECT_ID_QUERY =
            "SELECT * FROM [nt:unstructured] AS mapping WHERE ISDESCENDANTNODE(mapping, '%s')"
                    + " AND [sling:resourceType]='gathercontent/components/content/mapping'"
                    + " AND [projectId] = '%s'";

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {
        String[] selectors = request.getRequestPathInfo().getSelectors();
        String projectId = null;
        for (String selector : selectors) {
            if (selector.startsWith(Constants.PROJECT_ID_SELECTOR)) {
                projectId = selector.substring(Constants.PROJECT_ID_SELECTOR.length());
                break;
            }
        }

        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        if (projectId != null) {
            ResourceResolver resourceResolver = request.getResourceResolver();
            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            Page containingPage = pageManager.getContainingPage(request.getResource());
            Iterator<Resource> mappingResources = resourceResolver.findResources(
                    String.format(MAPPING_BY_PROJECT_ID_QUERY, containingPage.getPath(), projectId), Query.JCR_SQL2);
            Map<String, String> mappingsMap = new HashMap<>();
            try {
                while (mappingResources.hasNext()) {
                    Resource mappingResource = mappingResources.next();
                    String mappingPath = mappingResource.getPath();
                    String mappingName = mappingResource.getValueMap().get(Constants.MAPPING_NAME_PN, String.class);
                    mappingsMap.put(mappingPath, mappingName);
                }
                for (Map.Entry mappingEntry : mappingsMap.entrySet()) {
                    JSONObject jsonObjectMapping = new JSONObject();

                    jsonObjectMapping.put("text", mappingEntry.getKey());
                    jsonObjectMapping.put("value", mappingEntry.getValue());
                    jsonObjectMapping.put("qtip", mappingEntry.getKey());

                    jsonArray.put(jsonObjectMapping);
                }
                jsonObject.put("gcmappings", jsonArray);
            } catch (JSONException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        response.getWriter().write(jsonObject.toString());
    }
}
