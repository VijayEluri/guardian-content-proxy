package nz.gen.wellington.guardian.contentapiproxy.utils;

import org.apache.log4j.Logger;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.inject.Inject;


public class CachingHttpFetcher extends HttpFetcher {
	
	private final Logger log = Logger.getLogger(CachingHttpFetcher.class);
	
	private static final int DEFAULT_TTL = 300;
	MemcacheService cache;
	 
	 
	@Inject
	public CachingHttpFetcher() {	
		cache = MemcacheServiceFactory.getMemcacheService();
		cache.clearAll();
	}

	
	public String fetchContent(String url, String charEncoding) {
		log.info("Called for url '" + url);

		final String content = fetchFromCache(url);
		if (content != null) {
			log.info("Found content for url '" + url + "' in cache");
			return content;
		}
		
		log.info("Attempting to live fetch url: " + url);
		final String fetchedContent = super.fetchContent(url, charEncoding);

		if (fetchedContent != null) {
			Expiration expiration = Expiration.byDeltaSeconds(DEFAULT_TTL);
			cache.put(url, fetchedContent, expiration);
			log.info("Cached url: " + url);
		}
		return fetchedContent;		
	}
	
	
    public String fetchFromCache(String url) {
    	log.debug("Looking in cache for url '" + url);
    	return (String) cache.get(url);    	
    }
    
}
