/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tomakehurst.wiremock.mapping;

import static com.tomakehurst.wiremock.mapping.JsonMappingBinder.buildRequestPatternFrom;
import static com.tomakehurst.wiremock.mapping.JsonMappingBinder.write;
import static java.net.HttpURLConnection.HTTP_OK;

import com.tomakehurst.wiremock.global.GlobalSettings;
import com.tomakehurst.wiremock.global.GlobalSettingsHolder;
import com.tomakehurst.wiremock.http.RequestMethod;
import com.tomakehurst.wiremock.servlet.ResponseRenderer;
import com.tomakehurst.wiremock.verification.RequestJournal;
import com.tomakehurst.wiremock.verification.VerificationResult;

public class AdminRequestHandler extends AbstractRequestHandler {
	
	private final Mappings mappings;
	private final JsonMappingCreator jsonMappingCreator;
	private final RequestJournal requestJournal;
	private final GlobalSettingsHolder globalSettingsHolder;
	
	public AdminRequestHandler(Mappings mappings, RequestJournal requestJournal,
			GlobalSettingsHolder globalSettingsHolder, ResponseRenderer responseRenderer) {
		super(responseRenderer);
		this.mappings = mappings;
		this.requestJournal = requestJournal;
		this.globalSettingsHolder = globalSettingsHolder;
		jsonMappingCreator = new JsonMappingCreator(mappings);
	}

	@Override
	public ResponseDefinition handleRequest(Request request) {
		if (isNewMappingRequest(request)) {
			jsonMappingCreator.addMappingFrom(request.getBodyAsString());
			return ResponseDefinition.created();
		} else if (isResetRequest(request)) {
			mappings.reset();
			requestJournal.reset();
			return ResponseDefinition.ok();
		} else if (isRequestCountRequest(request)) {
			return getRequestCount(request);
		} else if (isGlobalSettingsUpdateRequest(request)) {
			GlobalSettings newSettings = JsonMappingBinder.read(request.getBodyAsString(), GlobalSettings.class);
			globalSettingsHolder.replaceWith(newSettings);
			return ResponseDefinition.ok();
		} else {
			return ResponseDefinition.notFound();
		}
	}

	private boolean isGlobalSettingsUpdateRequest(Request request) {
		return request.getMethod() == RequestMethod.POST && request.getUrl().equals("/settings");
	}

	private ResponseDefinition getRequestCount(Request request) {
		RequestPattern requestPattern = buildRequestPatternFrom(request.getBodyAsString());
		int matchingRequestCount = requestJournal.countRequestsMatching(requestPattern);
		ResponseDefinition response = new ResponseDefinition(HTTP_OK, write(new VerificationResult(matchingRequestCount)));
		response.addHeader("Content-Type", "application/json");
		return response;
	}

	private boolean isResetRequest(Request request) {
		return request.getMethod() == RequestMethod.POST && request.getUrl().equals("/reset");
	}

	private boolean isNewMappingRequest(Request request) {
		return request.getMethod() == RequestMethod.POST && request.getUrl().equals("/mappings/new");
	}
	
	private boolean isRequestCountRequest(Request request) {
		return request.getMethod() == RequestMethod.POST && request.getUrl().equals("/requests/count");
	}
	
}