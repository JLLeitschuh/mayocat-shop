package org.mayocat.shop.marketplace.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.mayocat.configuration.SiteSettings;
import org.mayocat.rest.Resource;
import org.mayocat.rest.representations.ResultSetRepresentation;
import org.mayocat.search.SearchEngine;
import org.mayocat.search.elasticsearch.ElasticSearchSearchEngine;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

/**
 * @version $Id$
 */
@Path("/marketplace/api/")
@Component("/marketplace/api/")
public class MarketplaceResource implements Resource
{
    @Inject
    private SiteSettings siteSettings;

    @Inject
    private ComponentManager componentManager;

    private ElasticSearchSearchEngine searchEngine;

    @GET
    @Path("products")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProducts(@QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("number") @DefaultValue("25") Integer number)
    {
        Client client = getSearchEngine().getClient();
        SearchResponse response =
                client.prepareSearch("entities").setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                        .setFrom(offset)
                        .setSize(number)
                        .setTypes("product")
                        .setExplain(false)
                        .execute()
                        .actionGet();

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (SearchHit hit : response.getHits()) {
            result.add(hit.getSource());
        }
        String thisAPIHref =
                "http://" + siteSettings.getDomainName() + "/marketplace/api/products?number=" + number + "&offset=" +
                        offset;
        ResultSetRepresentation<List<Map<String, Object>>> resultSet =
                new ResultSetRepresentation(thisAPIHref, number, offset, result,
                        Long.valueOf(response.getHits().getTotalHits()).intValue());
        return Response.ok(resultSet).build();
    }

    @GET
    @Path("shops/{shop}/products")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getShopProducts(@PathParam("shop") String shop,
            @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("number") @DefaultValue("25") Integer number)
    {
        Client client = getSearchEngine().getClient();
        SearchResponse response =
                client.prepareSearch("entities").setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                        .setFrom(offset)
                        .setSize(number)
                        .setTypes("product")
                        .setQuery(QueryBuilders.matchQuery("site.slug", shop))
                        //.setFilter(FilterBuilders.nestedFilter("site",
                        //        FilterBuilders.boolFilter().must(FilterBuilders.termFilter("slug", shop))))
                        //.setFilter(FilterBuilders.boolFilter().must(FilterBuilders.termFilter("site.slug", shop)))
                        .setExplain(false)
                        .execute()
                        .actionGet();

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (SearchHit hit : response.getHits()) {
            result.add(hit.getSource());
        }
        String thisAPIHref =
                "http://" + siteSettings.getDomainName() + "/marketplace/api/shops/" + shop + "/products?number=" +
                        number + "&offset=" + offset;
        ResultSetRepresentation<List<Map<String, Object>>> resultSet =
                new ResultSetRepresentation(thisAPIHref, number, offset, result,
                        Long.valueOf(response.getHits().getTotalHits()).intValue());
        return Response.ok(resultSet).build();
    }


    private ElasticSearchSearchEngine getSearchEngine()
    {
        if (searchEngine == null) {
            try {
                searchEngine =
                        (ElasticSearchSearchEngine) componentManager.getInstance(SearchEngine.class, "elasticsearch");
            } catch (ComponentLookupException e) {
                throw new RuntimeException("Cannot run marketplace API without elasticsearch search engine");
            }
        }
        return searchEngine;
    }
}
