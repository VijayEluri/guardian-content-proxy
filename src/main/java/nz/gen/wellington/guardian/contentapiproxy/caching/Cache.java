package nz.gen.wellington.guardian.contentapiproxy.caching;

import java.io.IOException;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class Cache {

	private final Logger log = Logger.getLogger(Cache.class);
	
	private static final String KEY_PREFIX = "GUARDIANLITE5:";	// TODO hardcoded version number
	
	private MemcachedClient memcachedClient;
	private String memcacheUrl;
	
	@Inject
	public Cache(@Named("memcacheUrl") String memcacheUrl) {
		this.memcacheUrl = memcacheUrl;
	}
	
	public void put(String url, String content, int ttl) {
		try {
			getClient().set(makeCacheKey(url), ttl, content);
		} catch (IOException e) {
			log.error(e);
		}
	}

	public String get(String url) {
		try {
			return (String) getClient().get(makeCacheKey(url));
		} catch (IOException e) {
			log.error(e);
		} 
		return null;
	}
	
	private String makeCacheKey(String url) {
		return KEY_PREFIX + DigestUtils.shaHex(url);
	}
	
	private MemcachedClient getClient() throws IOException {
		if (memcachedClient == null) {
			memcachedClient= new MemcachedClient(AddrUtil.getAddresses(memcacheUrl));
		}
		return memcachedClient;
	}

}
