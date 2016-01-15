/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Rob Winch
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContextFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String tenantId = getTenantId(request);
		if (tenantId == null) {
			filterChain.doFilter(request, response);
			return;
		}
		filterChain.doFilter(new TenantAwareHttpServletRequest(request, tenantId), response);
	}

	private String getTenantId(HttpServletRequest request) {
		String requestUrl = currentUrl(request);

		if ("".equals(requestUrl) || "/".equals(requestUrl)) {
			return null;
		}
		StringTokenizer tokens = new StringTokenizer(requestUrl, "/");
		if (tokens.hasMoreTokens()) {
			String result = tokens.nextToken();
			if (tokens.hasMoreTokens() || requestUrl.endsWith("/")) {
				return result;
			}
		}
		return null;
	}

	private String currentUrl(HttpServletRequest request) {
		StringBuilder url = new StringBuilder();
		url.append(request.getServletPath());

		String pathInfo = request.getPathInfo();
		if (pathInfo != null) {
			url.append(pathInfo);
		}
		return url.toString();
	}

	/**
	 * A wrapper for the HttpServletRequest that includes the tenant id in the
	 * context path. Note that we do not cache the servletPath or the
	 * contextPath as this can lead to problem with forwarding requests.
	 *
	 * @author Rob Winch
	 *
	 */
	private static class TenantAwareHttpServletRequest extends HttpServletRequestWrapper {
		private final String tenantId;

		public TenantAwareHttpServletRequest(HttpServletRequest request, String tenantId) {
			super(request);
			this.tenantId = tenantId;
		}

		@Override
		public String getServletPath() {
			String servletPath = super.getServletPath();
			int start = servletPath.indexOf(tenantId);
			if (start < 0) {
				return servletPath;
			}
			int end = start + tenantId.length();
			return servletPath.substring(end);
		}

		@Override
		public String getContextPath() {
			return super.getContextPath() + "/" + tenantId;
		}
	}
}
