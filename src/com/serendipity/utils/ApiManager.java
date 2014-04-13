package com.serendipity.utils;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import android.graphics.Typeface;

public class ApiManager {
	private static final String API_URL = "http://api.yelp.com/v2/search";

	private OAuthService service;
	private Token accessToken;

	public ApiManager(String consumerKey, String consumerSecret, String token, String tokenSecret) {
		this.service = new ServiceBuilder().provider(YelpApi.class).apiKey(consumerKey).apiSecret(consumerSecret).build();
		this.accessToken = new Token(token, tokenSecret);
	}
	
	public String search(String term, double latitude, double longitude) {
		return search(term, latitude, longitude, -1);
	}
	
	/**
	 * Search with term and location.
	 *
	 * @param term Search term
	 * @param latitude Latitude
	 * @param longitude Longitude
	 * @return JSON string response
	 */
	public String search(String term, double latitude, double longitude, int limit) {
		OAuthRequest request = new OAuthRequest(Verb.GET, API_URL);
		request.addQuerystringParameter("term", term);
		request.addQuerystringParameter("ll", latitude + "," + longitude);
		
		if (limit > 0) {
			request.addQuerystringParameter("limit", String.valueOf(limit));
		}
		
		this.service.signRequest(this.accessToken, request);
		Response response = request.send();
		return response.getBody();
	}
}
