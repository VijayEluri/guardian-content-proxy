package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.AboutDataSource;
import nz.gen.wellington.guardian.contentapiproxy.model.Article;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@SuppressWarnings("serial")
@Singleton
public class AboutProxyServlet extends CacheAwareProxyServlet {
		
	private AboutDataSource datasource;
	private ArticleToXmlRenderer articleToXmlRenderer;

	
	@Inject
	public AboutProxyServlet(AboutDataSource datasource, ArticleToXmlRenderer articleToXmlRenderer) {
		super();
		this.datasource = datasource;
		this.articleToXmlRenderer = articleToXmlRenderer;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Handling request for path: " + request.getRequestURI());
				
		if (request.getRequestURI().equals("/about")) {
			
            final String queryCacheKey = "about";
            String output = cacheGet(queryCacheKey);
            if (output != null) {
            	log.info("Returning cached results for call url: " + queryCacheKey);				
            }
            
			if (output == null) {			
				log.info("Building result for call: " + queryCacheKey);	
				output = getContent();
				if (output != null) {
					cacheContent(queryCacheKey, output);
				}				
			}
			
			if (output != null) {
				log.info("Outputing content: " + output.length() + " characters");
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("text/xml");
				response.setCharacterEncoding("UTF-8");
				PrintWriter writer = response.getWriter();
				writer.print(output);
				writer.flush();
				
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);				
			}
		}
		
		return;
	}

	
	private String getContent() {
		List<Article> articles = datasource.getArticles();
		if (articles == null) {
			return null;
		}		
		return articleToXmlRenderer.outputXml(articles, null, null, true);
	}
	
}