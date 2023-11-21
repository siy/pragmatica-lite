package org.pragmatica.http.server.routing;

import io.netty.handler.codec.http.HttpMethod;
import org.pragmatica.http.server.Handler;
import org.pragmatica.http.server.config.serialization.ContentType;
import org.pragmatica.http.server.util.Utils;
import org.pragmatica.lang.Result;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.pragmatica.lang.Promise.resolved;

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
		public RouteBuilder1 options() {
			return new RouteBuilder1(path, HttpMethod.OPTIONS);
		}

		public RouteBuilder1 get() {
			return new RouteBuilder1(path, HttpMethod.GET);
		}

		public RouteBuilder1 head() {
			return new RouteBuilder1(path, HttpMethod.HEAD);
		}

		public RouteBuilder1 post() {
			return new RouteBuilder1(path, HttpMethod.POST);
		}

		public RouteBuilder1 put() {
			return new RouteBuilder1(path, HttpMethod.PUT);
		}

		public RouteBuilder1 patch() {
			return new RouteBuilder1(path, HttpMethod.PATCH);
		}

		public RouteBuilder1 delete() {
			return new RouteBuilder1(path, HttpMethod.DELETE);
		}

		public RouteBuilder1 trace() {
			return new RouteBuilder1(path, HttpMethod.TRACE);
		}

		public RouteBuilder1 connect() {
			return new RouteBuilder1(path, HttpMethod.CONNECT);
		}
	}

	public record RouteBuilder1(String path, HttpMethod method) {
		public RouteBuilder2 text() {
			return new RouteBuilder2(path, method, ContentType.TEXT_PLAIN);
		}

		public RouteBuilder2 json() {
			return new RouteBuilder2(path, method, ContentType.APPLICATION_JSON);
		}

		public <T> Route<T> with(Handler<T> handler) {
			return new Route<>(method, path, handler, ContentType.TEXT_PLAIN);
		}

		public <T> Route<T> with(Supplier<Result<T>> supplier) {
			return new Route<>(method, path, _ -> resolved(supplier.get()), ContentType.TEXT_PLAIN);
		}
	}

	public record RouteBuilder2(String path, HttpMethod method, ContentType contentType) {
		public <T> Route<T> with(Handler<T> handler) {
			return new Route<>(method, path, handler, contentType);
		}

		public <T> Route<T> with(Supplier<Result<T>> supplier) {
			return new Route<>(method, path, _ -> resolved(supplier.get()), contentType);
		}
	}
}
