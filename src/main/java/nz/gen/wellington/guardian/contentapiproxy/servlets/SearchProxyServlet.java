package nz.gen.wellington.guardian.contentapiproxy.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nz.gen.wellington.guardian.contentapiproxy.datasources.DateRefinementImprover;
import nz.gen.wellington.guardian.contentapiproxy.datasources.GuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi.ContentApiDataSource;
import nz.gen.wellington.guardian.contentapiproxy.model.ArticleBundle;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.contentapiproxy.output.ArticleToXmlRenderer;
import nz.gen.wellington.guardian.contentapiproxy.requests.RequestQueryParser;
import nz.gen.wellington.guardian.model.Refinement;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearchProxyServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(SearchProxyServlet.class);
		
	private GuardianDataSource dataSource;
	private RequestQueryParser requestQueryParser;
	private ArticleToXmlRenderer articleToXmlRenderer;
	private DateRefinementImprover dateRefinementImprover;
	
	@Inject
	public SearchProxyServlet(ContentApiDataSource dataSource, ArticleToXmlRenderer articleToXmlRenderer, RequestQueryParser requestQueryParser, DateRefinementImprover dateRefinementImprover) {
		this.dataSource = dataSource;
		this.articleToXmlRenderer = articleToXmlRenderer;
		this.requestQueryParser = requestQueryParser;
		this.dateRefinementImprover = dateRefinementImprover;
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.debug("Handling request for path: " + request.getRequestURI());
				
		SearchQuery query = requestQueryParser.getSearchQueryFromRequest(request);		
		if (dataSource.isSupported(query)) {
			final String output = getContent(query, dataSource);
			if (output != null) {
				outputResponse(response, output);
				return;
			}
		}
		
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);				
		return;
	}

	private void outputResponse(HttpServletResponse response, String output) throws IOException {
		log.debug("Outputting content: " + output.length() + " characters");
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/xml");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "max-age=300");
		response.addHeader("Etag", DigestUtils.md5Hex(output));
		
		PrintWriter writer = response.getWriter();
		writer.print(output);
		writer.flush();
		return;
	}
	
	private String getContent(SearchQuery query, GuardianDataSource datasource) {
		log.debug("Getting content for query: " + query.toString());
		ArticleBundle articleBundle = datasource.getArticles(query);
		if (articleBundle == null || articleBundle.getArticles() == null) {
			return null;
		}
		
		log.debug("Getting refinements");
		Map<String, List<Refinement>> refinements = datasource.getRefinements(query);		
		if (refinements != null) {
			log.debug("'Improving' the date refinements");
			refinements.remove("date");
			if (query.isSingleTagOrSectionQuery()) {
				List<Refinement> dateRefinements = dateRefinementImprover.generateDateRefinementsForTag(query);
				if (dateRefinements != null) {
					refinements.put("date", dateRefinements);
				}
			}
		}
		
		return articleToXmlRenderer.outputXml(articleBundle.getArticles(), articleBundle.getDescription(), refinements, query.isShowAllFields());
	}
	
}