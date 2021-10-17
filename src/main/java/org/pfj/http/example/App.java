package org.pfj.http.example;

import org.pfj.http.server.WebError;
import org.pfj.http.server.WebServer;
import org.pfj.lang.Promise;

import java.util.concurrent.atomic.AtomicInteger;

import static org.pfj.http.server.Route.*;
import static org.pfj.http.server.Route.from;
import static org.pfj.lang.Promise.failure;
import static org.pfj.lang.Promise.success;

public class App {
	public static void main(final String[] args) throws Exception {
		WebServer.create(
				//Full description
				from("/hello").get().text().with(request -> success("Hello world")),

				//Default content type (text)
				from("/hello").get().with(request -> success("Hello world: " + request.bodyAsString())),

				//Shortcut for method, explicit content type
				get("/getbody").text().with(request -> success("What is this? " + request.bodyAsString())),

				//Shortcut for method, default content type
				get("/getbody").with(request -> success("What is this? " + request.bodyAsString())),

				//Runtime exception handling example
				get("/boom").with(request -> {
					throw new RuntimeException("Some exception message");
				}),

				//Error handling
				get("/boom2").with(request -> failure(WebError.UNPROCESSABLE_ENTITY)),

				//Long-running process
				get("/delay").with(request -> delayedResponse()),

				//Nested routes
				from(
					"/v1",
					from(
						"/user",
						//Full description
						from("/list").get().json().with(request -> success("User list")),

						//Shortcut with non-default content type
						get("/profile").json().with(request -> success(new UserProfile("John", "Doe", "john.doe@gmail.com")))
					)
				)
			)
			.start()
			.join();
	}

	private static final AtomicInteger counter = new AtomicInteger();

	private static Promise<Integer> delayedResponse() {
		return Promise.<Integer>promise()
			.async(promise -> {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					//ignore
				}
				promise.succeed(counter.incrementAndGet());
			});
	}
}
