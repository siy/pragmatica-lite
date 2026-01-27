package org.pragmatica.http.routing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.http.routing.ContentCategory.BINARY;
import static org.pragmatica.http.routing.ContentCategory.HTML;
import static org.pragmatica.http.routing.ContentCategory.JSON;
import static org.pragmatica.http.routing.ContentCategory.PLAIN_TEXT;
import static org.pragmatica.http.routing.HttpMethod.GET;
import static org.pragmatica.http.routing.StaticFileRouteSource.detectContentType;
import static org.pragmatica.http.routing.StaticFileRouteSource.staticFiles;

class StaticFileRouteSourceTest {

    @Nested
    class ContentTypeDetection {
        @Test
        void detectsHtmlContentType() {
            var contentType = detectContentType("/index.html");
            assertThat(contentType.headerText()).isEqualTo("text/html; charset=UTF-8");
            assertThat(contentType.category()).isEqualTo(HTML);
        }

        @Test
        void detectsHtmContentType() {
            var contentType = detectContentType("/page.htm");
            assertThat(contentType.headerText()).isEqualTo("text/html; charset=UTF-8");
            assertThat(contentType.category()).isEqualTo(HTML);
        }

        @Test
        void detectsCssContentType() {
            var contentType = detectContentType("/styles/main.css");
            assertThat(contentType.headerText()).isEqualTo("text/css; charset=UTF-8");
            assertThat(contentType.category()).isEqualTo(PLAIN_TEXT);
        }

        @Test
        void detectsJsContentType() {
            var contentType = detectContentType("/scripts/app.js");
            assertThat(contentType.headerText()).isEqualTo("text/javascript; charset=UTF-8");
            assertThat(contentType.category()).isEqualTo(PLAIN_TEXT);
        }

        @Test
        void detectsJsonContentType() {
            var contentType = detectContentType("/data/config.json");
            assertThat(contentType.headerText()).isEqualTo("application/json; charset=UTF-8");
            assertThat(contentType.category()).isEqualTo(JSON);
        }

        @Test
        void detectsPngContentType() {
            var contentType = detectContentType("/images/logo.png");
            assertThat(contentType.headerText()).isEqualTo("image/png");
            assertThat(contentType.category()).isEqualTo(BINARY);
        }

        @Test
        void detectsJpgContentType() {
            var contentType = detectContentType("/images/photo.jpg");
            assertThat(contentType.headerText()).isEqualTo("image/jpeg");
            assertThat(contentType.category()).isEqualTo(BINARY);
        }

        @Test
        void detectsJpegContentType() {
            var contentType = detectContentType("/images/photo.jpeg");
            assertThat(contentType.headerText()).isEqualTo("image/jpeg");
            assertThat(contentType.category()).isEqualTo(BINARY);
        }

        @Test
        void detectsGifContentType() {
            var contentType = detectContentType("/images/animation.gif");
            assertThat(contentType.headerText()).isEqualTo("image/gif");
            assertThat(contentType.category()).isEqualTo(BINARY);
        }

        @Test
        void detectsSvgContentType() {
            var contentType = detectContentType("/icons/icon.svg");
            assertThat(contentType.headerText()).isEqualTo("image/svg+xml");
            assertThat(contentType.category()).isEqualTo(BINARY);
        }

        @Test
        void detectsIcoContentType() {
            var contentType = detectContentType("/favicon.ico");
            assertThat(contentType.headerText()).isEqualTo("image/x-icon");
            assertThat(contentType.category()).isEqualTo(BINARY);
        }

        @Test
        void detectsWoffContentType() {
            var contentType = detectContentType("/fonts/font.woff");
            assertThat(contentType.headerText()).isEqualTo("font/woff");
            assertThat(contentType.category()).isEqualTo(BINARY);
        }

        @Test
        void detectsWoff2ContentType() {
            var contentType = detectContentType("/fonts/font.woff2");
            assertThat(contentType.headerText()).isEqualTo("font/woff2");
            assertThat(contentType.category()).isEqualTo(BINARY);
        }

        @Test
        void detectsTtfContentType() {
            var contentType = detectContentType("/fonts/font.ttf");
            assertThat(contentType.headerText()).isEqualTo("font/ttf");
            assertThat(contentType.category()).isEqualTo(BINARY);
        }

        @Test
        void detectsEotContentType() {
            var contentType = detectContentType("/fonts/font.eot");
            assertThat(contentType.headerText()).isEqualTo("application/vnd.ms-fontobject");
            assertThat(contentType.category()).isEqualTo(BINARY);
        }

        @Test
        void detectsXmlContentType() {
            var contentType = detectContentType("/config.xml");
            assertThat(contentType.headerText()).isEqualTo("application/xml; charset=UTF-8");
            assertThat(contentType.category()).isEqualTo(PLAIN_TEXT);
        }

        @Test
        void detectsTxtContentType() {
            var contentType = detectContentType("/readme.txt");
            assertThat(contentType.headerText()).isEqualTo("text/plain; charset=UTF-8");
            assertThat(contentType.category()).isEqualTo(PLAIN_TEXT);
        }

        @Test
        void detectsMapContentType() {
            var contentType = detectContentType("/app.js.map");
            assertThat(contentType.headerText()).isEqualTo("application/json; charset=UTF-8");
            assertThat(contentType.category()).isEqualTo(JSON);
        }

        @Test
        void returnsDefaultForUnknownExtension() {
            var contentType = detectContentType("/file.xyz");
            assertThat(contentType.headerText()).isEqualTo("application/octet-stream");
            assertThat(contentType.category()).isEqualTo(BINARY);
        }

        @Test
        void returnsDefaultForNoExtension() {
            var contentType = detectContentType("/LICENSE");
            assertThat(contentType.headerText()).isEqualTo("application/octet-stream");
            assertThat(contentType.category()).isEqualTo(BINARY);
        }

        @Test
        void handlesUpperCaseExtension() {
            var contentType = detectContentType("/image.PNG");
            assertThat(contentType.headerText()).isEqualTo("image/png");
        }

        @Test
        void handlesMixedCaseExtension() {
            var contentType = detectContentType("/script.Js");
            assertThat(contentType.headerText()).isEqualTo("text/javascript; charset=UTF-8");
        }
    }

    @Nested
    class RouteCreation {
        @Test
        void createsRouteWithCorrectMethod() {
            var routeSource = staticFiles("/static", "/web");
            var routes = routeSource.routes().toList();

            assertThat(routes).hasSize(1);
            assertThat(routes.getFirst().method()).isEqualTo(GET);
        }

        @Test
        void createsRouteWithNormalizedPath() {
            var routeSource = staticFiles("/static", "/web");
            var routes = routeSource.routes().toList();

            assertThat(routes.getFirst().path()).isEqualTo("/static/");
        }

        @Test
        void normalizesUrlPrefixWithoutLeadingSlash() {
            var routeSource = staticFiles("static", "/web");
            var routes = routeSource.routes().toList();

            assertThat(routes.getFirst().path()).isEqualTo("/static/");
        }

        @Test
        void normalizesUrlPrefixWithTrailingSlash() {
            var routeSource = staticFiles("/static/", "/web");
            var routes = routeSource.routes().toList();

            assertThat(routes.getFirst().path()).isEqualTo("/static/");
        }

        @Test
        void createsRouteWithHandler() {
            var routeSource = staticFiles("/static", "/web");
            var routes = routeSource.routes().toList();

            assertThat(routes.getFirst().handler()).isNotNull();
        }

        @Test
        void routeSourceImplementsInterface() {
            var routeSource = staticFiles("/static", "/web");

            assertThat(routeSource).isInstanceOf(RouteSource.class);
            assertThat(routeSource).isInstanceOf(StaticFileRouteSource.class);
        }
    }

    @Nested
    class PathValidation {
        @Test
        void staticFilesCreatesValidRouteSource() {
            var routeSource = staticFiles("/assets", "/public");
            var routes = routeSource.routes().toList();

            assertThat(routes).isNotEmpty();
            assertThat(routes.getFirst().method()).isEqualTo(GET);
        }
    }

    @Nested
    class Integration {
        @Test
        void canBeUsedWithRequestRouter() {
            var routeSource = staticFiles("/static", "/web");
            var router = RequestRouter.with(routeSource);

            var route = router.findRoute(GET, "/static/");
            assertThat(route.isPresent()).isTrue();
        }

        @Test
        void canBeUsedWithRoutePrefix() {
            var routeSource = staticFiles("/files", "/resources").withPrefix("/api/v1");
            var routes = routeSource.routes().toList();

            assertThat(routes.getFirst().path()).isEqualTo("/api/v1/files/");
        }

        @Test
        void canBeCombinedWithOtherRoutes() {
            var staticRoutes = staticFiles("/static", "/web");
            var apiRoute = Route.<String>get("/api/health")
                                .to(_ -> org.pragmatica.lang.Promise.success("OK"))
                                .asText();

            var combined = RouteSource.of(staticRoutes, apiRoute);
            var routes = combined.routes().toList();

            assertThat(routes).hasSize(2);
        }
    }
}
