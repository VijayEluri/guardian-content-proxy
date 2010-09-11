package nz.gen.wellington.guardian.contentapiproxy.servlets;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class GuiceServletConfig extends GuiceServletContextListener {
	
	@Override
	protected Injector getInjector() {
		return Guice.createInjector(new ServletModule() {

		     @Override
		     protected void configureServlets() {
		    	 serve("/about").with(AboutProxyServlet.class);
		    	 serve("/search").with(SearchProxyServlet.class);
		    	 serve("/sections").with(SectionProxyServlet.class);
		    	 serve("/favourites").with(FavouritesServlet.class);		    	 
		     }
		     
		});
	}

}
