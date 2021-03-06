package nz.gen.wellington.guardian.contentapiproxy.datasources.contentapi;

import nz.gen.wellington.guardian.contentapiproxy.datasources.AbstractGuardianDataSource;
import nz.gen.wellington.guardian.contentapiproxy.datasources.ContentApi;
import nz.gen.wellington.guardian.contentapiproxy.datasources.SectionCleaner;
import nz.gen.wellington.guardian.contentapiproxy.model.ArticleBundle;
import nz.gen.wellington.guardian.contentapiproxy.model.SearchQuery;
import nz.gen.wellington.guardian.model.Article;

import org.joda.time.DateTime;

import com.google.inject.Inject;
import com.google.inject.internal.Lists;

public class ContentApiDataSource extends AbstractGuardianDataSource {
	
	@Inject
	public ContentApiDataSource(ContentApi contentApi, SectionCleaner sectionCleaner) {
		this.contentApi = contentApi;
		this.sectionCleaner = sectionCleaner;
	}

	public ArticleBundle getArticles(SearchQuery query) {
		return new ArticleBundle(Lists.newArrayList(decommissioningNotice()));
	}

	private Article decommissioningNotice() {
		Article decommisioningNotice = new Article();
		decommisioningNotice.setHeadline("This application was  withdrawn from service on the 5th of May");
		decommisioningNotice.setPubDate(new DateTime().toDate());
		decommisioningNotice.setDescription("<p>Users requiring a complete set of content should move to one of the official Guardian applications.</p>");
		return decommisioningNotice;
	}
	
	public boolean isSupported(SearchQuery query) {
		return true;
	}
	
}
