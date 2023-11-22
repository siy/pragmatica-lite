package org.pragmatica.http.server.routing;

import org.pragmatica.http.protocol.HttpMethod;
import org.pragmatica.http.server.Handler;
import org.pragmatica.http.server.config.serialization.ContentType;
import org.pragmatica.http.util.Utils;
import org.pragmatica.lang.Result;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.pragmatica.lang.Promise.resolved;

//TODO: rework API
//TODO: better support for path parameter extraction
public record Route<T>(HttpMethod method, String path, Handler<T> handler, ContentType contentType) implements RouteSource {
	public Route {
		path = Utils.normalize(path);
	}

	@Override
	public String toString() {
		return "Route: " + method + ": " + path +  ", contentType=" + contentType;
	}

	@Override
	public Stream<Route<?>> routes() {
		return Stream.of(this);
	}

	@Override
	public RouteSource withPrefix(String prefix) {
		return new Route<>(method, Utils.normalize(prefix + path), handler, contentType);
	}

	public static RouteSource from(String basePath, RouteSource... routes) {
		return () -> Stream.of(routes)
			.map(route -> route.withPrefix(basePath))
			.flatMap(RouteSource::routes);
	}

	public static RouteBuilder0 from(String path) {
		return new RouteBuilder0(path);
	}

	public static RouteBuilder1 options(String path) {
		return new RouteBuilder1(path, HttpMethod.OPTIONS);
	}

	public static RouteBuilder1 get(String path) {
		return new RouteBuilder1(path, HttpMethod.GET);
	}

	public static RouteBuilder1 head(String path) {
		return new RouteBuilder1(path, HttpMethod.HEAD);
	}

	public static RouteBuilder1 post(String path) {
		return new RouteBuilder1(path, HttpMethod.POST);
	}

	public static RouteBuilder1 put(String path) {
		return new RouteBuilder1(path, HttpMethod.PUT);
	}

	public static RouteBuilder1 patch(String path) {
		return new RouteBuilder1(path, HttpMethod.PATCH);
	}

	public static RouteBuilder1 delete(String path) {
		return new RouteBuilder1(path, HttpMethod.DELETE);
	}

	public static RouteBuilder1 trace(String path) {
		return new RouteBuilder1(path, HttpMethod.TRACE);
	}

	public static RouteBuilder1 connect(String path) {
		return new RouteBuilder1(path, HttpMethod.CONNECT);
	}

	public record RouteBuilder0(String path) {
		private RouteBuilder1 with(HttpMethod method) {
			return new RouteBuilder1(path, method);
		}
		public RouteBuilder1 options() {
			return with(HttpMethod.OPTIONS);
		}

		public RouteBuilder1 get() {
			return with(HttpMethod.GET);
		}

		public RouteBuilder1 head() {
			return with(HttpMethod.HEAD);
		}

		public RouteBuilder1 post() {
			return with(HttpMethod.POST);
		}

		public RouteBuilder1 put() {
			return with(HttpMethod.PUT);
		}

		public RouteBuilder1 patch() {
			return with(HttpMethod.PATCH);
		}

		public RouteBuilder1 delete() {
			return with(HttpMethod.DELETE);
		}

		public RouteBuilder1 trace() {
			return with(HttpMethod.TRACE);
		}

		public RouteBuilder1 connect() {
			return with(HttpMethod.CONNECT);
		}
	}

	public record RouteBuilder1(String path, HttpMethod method) {
		public RouteBuilder2 text() {
			return new RouteBuilder2(path, method, ContentType.TEXT_PLAIN);
		}

		public RouteBuilder2 json() {
			return new RouteBuilder2(path, method, ContentType.APPLICATION_JSON);
		}

		public <T> Route<T> then(Handler<T> handler) {
			return text().then(handler);
		}

		public <T> Route<T> then(Supplier<Result<T>> supplier) {
			return text().then(supplier);
		}
	}

	public record RouteBuilder2(String path, HttpMethod method, ContentType contentType) {
		public <T> Route<T> then(Handler<T> handler) {
			return new Route<>(method, path, handler, contentType);
		}

		public <T> Route<T> then(Supplier<Result<T>> supplier) {
			return then(_ -> resolved(supplier.get()));
		}
	}
}
