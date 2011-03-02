package nz.gen.wellington.guardian.contentapiproxy.datasources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.contentapi.parsing.ContentApiStyleJSONParser;
import nz.gen.wellington.guardian.contentapi.urls.ContentApiStyleUrlBuilder;
import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.HttpForbiddenException;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.utils.CachingHttpFetcher;
import nz.gen.wellington.guardian.model.Article;
import nz.gen.wellington.guardian.model.Refinement;
import nz.gen.wellington.guardian.model.Section;
import nz.gen.wellington.guardian.model.Tag;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;

public class ContentApi {
	
	public static final String API_HOST = "http://content.guardianapis.com";
	public static final String API_KEY = "";
	private final String[] permittedRefinementTypes = {"keyword", "blog", "contributor", "section", "type", "date"};

	private static Logger log = Logger.getLogger(ContentApi.class);
	
	
	private CachingHttpFetcher httpFetcher;
	private ContentApiStyleJSONParser contentApiJsonParser;

	private Map<String, Section>  sectionsMap;
	
	@Inject
	public ContentApi(CachingHttpFetcher httpFetcher, ContentApiStyleJSONParser contentApiJsonParser) {
		this.httpFetcher = httpFetcher;
		this.contentApiJsonParser = contentApiJsonParser;
	}
	
	
	public List<Article> getArticles(SearchQuery query) {			
		final String content = getJSONContentForArticleQuery(query);			
		if (content != null) {				
			try {
				JSONObject json = new JSONObject(content);
				if (json != null && contentApiJsonParser.isResponseOk(json)) {
					return contentApiJsonParser.extractContentItems(json, getSections());
				}
					
			} catch (JSONException e) {
				log.info("JSON error while parsing response", e);
				log.info(e);
				return null;
			}
		}		
		return null;
	}
	
	
	public int getArticleCount(SearchQuery query) {
		final String content = getJSONContentForArticleQuery(query);
		if (content != null) {				
			try {
				JSONObject json = new JSONObject(content);
				if (json != null && contentApiJsonParser.isResponseOk(json)) {
					return contentApiJsonParser.extractContentItemsCount(json);
				}					
			} catch (JSONException e) {
				log.info("JSON error while parsing response", e);
				log.info(e);
				return 0;
			}
		}
		return 0;
	}
	
	
	private String getJSONContentForArticleQuery(SearchQuery query) {
		ContentApiStyleUrlBuilder urlBuilder = new ContentApiStyleUrlBuilder(API_HOST, API_KEY);		
		urlBuilder.setFormat("json");
		urlBuilder.setShowAll(query.isShowAllFields());
		
		if (query.getFromDate() != null) {
			urlBuilder.setFromDate(query.getFromDate().toString("yyyy-MM-dd"));
		}
		if (query.getToDate() != null) {
			urlBuilder.setToDate(query.getToDate().toString("yyyy-MM-dd"));
		}
		
		urlBuilder.setPageSize(query.getPageSize());
		
		if (query.getTags() != null && !query.getTags().isEmpty()) {
			for (Tag tag : query.getTags()) {
				urlBuilder.addTag(tag);
			}
		}
		
		final String callUrl = urlBuilder.toSearchQueryUrl();
		return getContentFromUrlSuppressingHttpExceptions(callUrl);
	}
	
	
	public Map<String, Section> getSections() {
		if (sectionsMap != null) {
			return sectionsMap;	// TODO put into the cache proper, so that it has a finite TTL
		}
		
		log.info("Fetching section list from content api");
		ContentApiStyleUrlBuilder urlBuilder = new ContentApiStyleUrlBuilder(API_HOST, API_KEY);
		urlBuilder.setFormat("json");
		final String callUrl = urlBuilder.toSectionsQueryUrl();
		final String content = getContentFromUrlSuppressingHttpExceptions(callUrl);
		if (content != null) {			
			List<Section> sections = contentApiJsonParser.parseSectionsRequestResponse(content);
			log.info("Found " + sections.size() + " good sections");
			
			Map<String, Section> sectionsMap = new HashMap<String, Section>();
			for (Section section : sections) {
				sectionsMap.put(section.getId(), section);
			}
			this.sectionsMap = sectionsMap;
			return sectionsMap;
		}
		return null;
	}
	
	
	public Article getArticle(String contentId) throws HttpForbiddenException {
		log.info("Fetching content item: " + contentId);
		ContentApiStyleUrlBuilder urlBuilder = new ContentApiStyleUrlBuilder(API_HOST, API_KEY);
		urlBuilder.setContentId(contentId);
		urlBuilder.setFormat("json");
		final String callUrl = urlBuilder.toContentItemUrl();
		final String content = httpFetcher.fetchContent(callUrl, "UTF-8");
		if (content != null) {				
			try {
				JSONObject json = new JSONObject(content);
				if (json != null && contentApiJsonParser.isResponseOk(json)) {			
					return contentApiJsonParser.extractContentItem(json, getSections());
				}
					
			} catch (JSONException e) {
				log.info("JSON error while processing call url: " + callUrl);
				log.info(e);
				return null;
			}				
		}		
		return null;		
	}
	
	@Deprecated // TODO query for whole tags rather than interating over single records.
	public String getShortUrlFor(String contentId) throws HttpForbiddenException {
		log.info("Fetching short url for: " + contentId);
		Article article = this.getArticle(contentId);	
		if (article != null) {
			return article.getShortUrl();
		}
		return null;
	}
	
	public Map<String, List<Refinement>> getTagRefinements(Tag tag, DateTime fromDate, DateTime toDate) {
		ContentApiStyleUrlBuilder urlBuilder = new ContentApiStyleUrlBuilder(API_HOST, API_KEY);
		urlBuilder.addTag(tag);
		urlBuilder.setShowAll(false);
		urlBuilder.setShowRefinements(true);
		if (fromDate != null) {
			urlBuilder.setFromDate(fromDate.toString("YYYY-MM-dd"));
		}
		if (toDate != null) {
			urlBuilder.setFromDate(toDate.toString("YYYY-MM-dd"));
		}
		urlBuilder.setFormat("json");	
		final String callUrl = urlBuilder.toSearchQueryUrl();		
		log.info("Fetching from: " + callUrl);
		return processRefinements(callUrl);
	}

	
	// TODO Push refinements parsing to the shared class.
	private Map<String, List<Refinement>> processRefinements(String callUrl) {
		final String content = getContentFromUrlSuppressingHttpExceptions(callUrl);
		if (content == null) {
			log.warn("Failed to fetch url: " + callUrl);
			return null;
		}
		
		JSONObject json;
		try {
			json = new JSONObject(content);
			JSONObject response = json.getJSONObject("response");
				
			if (json != null && contentApiJsonParser.isResponseOk(json)) {
				Map<String, List<Refinement>> refinements = new HashMap<String, List<Refinement>>();
				
				if (response.has("refinementGroups")) {
					JSONArray refinementGroups = response.getJSONArray("refinementGroups");
					for (int i = 0; i < refinementGroups.length(); i++) {
						JSONObject refinementGroup = refinementGroups.getJSONObject(i);
						String type = refinementGroup.getString("type");
						
						boolean isPermittedRefinementType = Arrays.asList(permittedRefinementTypes).contains(type);
						if (isPermittedRefinementType) {
							
							List<Refinement> tagRefinements = refinements.get(type);
							if (tagRefinements == null) {
								tagRefinements = new ArrayList<Refinement>();
								refinements.put(type, tagRefinements);
							}
							
							JSONArray refinementsJSON = refinementGroup.getJSONArray("refinements");
							for (int j = 0; j < refinementsJSON.length(); j++) {
								JSONObject refinement = refinementsJSON.getJSONObject(j);
								tagRefinements.add(
										new Refinement(type, 
												refinement.getString("id"), 
												refinement.getString("displayName"), 
												refinement.getString("refinedUrl"),
												refinement.getInt("count"))
								);
							}
						}
						
					}
				}
				return refinements;
			}
		} catch (JSONException e) {
			log.error(e.getMessage());
		}
		
		return null;
	}
	
	private String getContentFromUrlSuppressingHttpExceptions(final String callUrl) {
		String content;
		try {
			content = httpFetcher.fetchContent(callUrl, "UTF-8");
		} catch (HttpForbiddenException e1) {
			return null;			
		}
		return content;
	}
	
}
