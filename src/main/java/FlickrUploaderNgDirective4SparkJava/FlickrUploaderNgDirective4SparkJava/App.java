package FlickrUploaderNgDirective4SparkJava.FlickrUploaderNgDirective4SparkJava;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import org.scribe.model.Token;

import spark.Request;
import spark.Response;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.photos.Size;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;
import com.google.gson.Gson;


public class App {

	public static final String SETUP_PROPERTIES = "setup.properties",
			APPLICATION_TYPE_JSON_UTF8 = "application/json; charset=UTF-8",
			JETTY_MULTIPART_CONFIG = "org.eclipse.jetty.multipartConfig";

	private String apiKey, sharedSecret, authToken, authTokenSecret;
	private Flickr flickr;
	private Auth auth;

	public void initRoutes() {
		staticFileLocation("WEB-INF");
		post("/uploadimage", (req, res) -> uploadImage(req, res),
				new Gson()::toJson);
	}

	public App() throws Exception {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Properties setupProperties = new Properties();
		try (InputStream setupStream = loader
				.getResourceAsStream(SETUP_PROPERTIES)) {
			setupProperties.load(setupStream);
			apiKey = setupProperties.getProperty("apiKey");
			sharedSecret = setupProperties.getProperty("sharedSecret");
			authToken = setupProperties.getProperty("authToken");
			authTokenSecret = setupProperties.getProperty("authTokenSecret");
		} catch (IOException e) {
			throw new Exception("Error while reading setup properties: " + e);
		}
		flickr = new Flickr(apiKey, sharedSecret, new REST());			
		AuthInterface authInterface = flickr.getAuthInterface();
		Token requestToken = new Token(authToken, authTokenSecret);
		auth = authInterface.checkToken(requestToken);
		flickr.setAuth(auth);
	}

	private Collection<Size> uploadImage(Request req, Response res)
			throws Exception {
		res.type(APPLICATION_TYPE_JSON_UTF8);
		if (req.raw().getAttribute(JETTY_MULTIPART_CONFIG) == null) {
			String tmpDir = System.getProperty("java.io.tmpdir");
			MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
					tmpDir);
			req.raw().setAttribute(JETTY_MULTIPART_CONFIG,
					multipartConfigElement);
			Part file = req.raw().getPart("file");
			InputStream is = file.getInputStream();

			UploadMetaData metaData = new UploadMetaData();
			metaData.setPublicFlag(true);
			metaData.setAsync(false);
			RequestContext.getRequestContext().setAuth(auth);
			Uploader uploader = flickr.getUploader();
			String photoId = uploader.upload(is, metaData);
			Collection<Size> sizes = flickr.getPhotosInterface().getSizes(
					photoId);
			return sizes;
		}
		throw new Exception(JETTY_MULTIPART_CONFIG  + " is not null!");
	}

	public static void main(String[] args) throws Exception {
		App app = new App();
		app.initRoutes();
	}

}