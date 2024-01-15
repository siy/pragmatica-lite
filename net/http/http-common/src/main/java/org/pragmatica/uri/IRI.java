/*
 *  Copyright (c) 2022 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.pragmatica.uri;

import org.pragmatica.dns.DomainName;
import org.pragmatica.http.protocol.QueryParameters;
import org.pragmatica.lang.Option;
import org.pragmatica.net.InetPort;
import org.pragmatica.uri.util.Decoder;
import org.pragmatica.uri.util.Encoder;

import java.util.regex.Pattern;

import static org.pragmatica.dns.DomainName.domainName;
import static org.pragmatica.lang.Option.empty;
import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.option;

/**
 * The implementation of the IRI (Internationalized Resource Identifier) as defined in RFC 3987.
 * See <a href="https://cr.openjdk.org/~dfuchs/writeups/updating-uri/">article</a> for motivation.
 */
@SuppressWarnings("unused")
public record IRI(
    Option<String> scheme,
    Option<UserInfo> userInfo,
    Option<DomainName> domain,
    Option<InetPort> port,
    Option<String> path,
    QueryParameters queryParameters,
    Option<String> fragment
) {
    private static final Pattern URI_PATTERN = Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");
    private static final Pattern AUTHORITY_PATTERN = Pattern.compile("((.*)@)?([^:]*)(:(\\d+))?");
    private static final IRI EMPTY = new IRI(empty(), empty(), empty(), empty(), empty(), QueryParameters.parameters(), empty());

    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    public static IRI iri(Option<String> scheme, Option<UserInfo> userInfo, Option<DomainName> domain, Option<InetPort> port,
                          Option<String> path, Option<QueryParameters> queryParameters, Option<String> fragment) {
        return new IRI(scheme, userInfo, domain, port, path, queryParameters.or(QueryParameters::parameters), fragment);
    }

    public static IRI fromString(String url) {
        if (url == null || url.isEmpty()) {
            return EMPTY;
        }

        var matcher = URI_PATTERN.matcher(url);

        if (!matcher.find()) {
            return EMPTY;
        }

        var userInfo = Option.<UserInfo>empty();
        var domain = Option.<DomainName>empty();
        var port = Option.<InetPort>empty();
        var scheme = option(matcher.group(2));

        if (matcher.group(4) != null) {
            var n = AUTHORITY_PATTERN.matcher(matcher.group(4));

            if (n.find()) {
                userInfo = option(n.group(2)).map(Decoder::decodeUserInfo);
                //domain = option(n.group(3)).map(IDN::toUnicode);
                domain = option(domainName(n.group(3)));
                port = option(n.group(5)).flatMap(Decoder::parsePort);
            }
        } else {
            // Special handling for JDBC URLs, which are not compliant with RFC 3986 and have "double" scheme.
            // Note that they don't have authority part, so we don't need to handle that case.
            if (matcher.group(2).equals("jdbc")) {
                IRI iri = fromString(url.substring("jdbc:".length()));

                var newScheme = iri.scheme()
                                   .map(s -> STR."jdbc:\{s}").or("jdbc");

                return iri.withScheme(newScheme);
            }
        }

        return new IRI(scheme, userInfo, domain, port,
                       option(matcher.group(5)).map(Decoder::decodePath),
                       option(matcher.group(7)).map(Decoder::parseQueryString).or(QueryParameters::parameters),
                       option(matcher.group(9)).map(Decoder::decodeFragment));
    }

    public StringBuilder toString(StringBuilder out) {
        scheme.onPresent(scheme -> out.append(scheme).append(':'));
        domain.onPresent(hostName -> {
            out.append("//");
            userInfo.onPresent(userInfo -> out.append(Encoder.encodeUserInfo(userInfo)).append('@'));
            //out.append(IDN.toASCII(domain));
            out.append(hostName.name());
        });

        port.onPresent(port -> out.append(':').append(port));
        path.onPresent(path -> {
            if (domain.isPresent() && !path.isEmpty() && path.charAt(0) != '/') {
                /* RFC 3986 section 3.3: If a URI contains an authority component, then the path component
                   must either be empty or begin with a slash ("/") character. */
                out.append('/');
            }
            out.append(Encoder.encodePath(path));
        });
        queryParameters.encode(out);
        fragment.onPresent(fragment -> out.append('#').append(Encoder.encodeFragment(fragment)));

        return out;
    }

    public boolean isAbsolute() {
        return scheme.isPresent() && domain.isPresent();
    }

    public boolean isSecure() {
        return scheme.filter(HTTPS::equals).isPresent();
    }

    public boolean isHttp() {
        return Option.any(scheme.filter(HTTP::equals),
                          scheme.filter(HTTPS::equals))
                     .isPresent();
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public IRI pathAndQuery() {
        return new IRI(none(), none(), none(), none(), path, queryParameters, fragment);
    }

    public IRI withScheme(String scheme) {
        return new IRI(option(scheme), userInfo, domain, port, path, queryParameters, fragment);
    }

    public IRI withUserInfo(String userInfo) {
        return new IRI(scheme, option(userInfo).map(Decoder::decodeUserInfo), domain, port, path, queryParameters, fragment);
    }

    public IRI withDomain(DomainName domain) {
        //return new IRI(scheme, userInfo, option(IDN.toUnicode(name)), port, path, queryParameters, fragment);
        return new IRI(scheme, userInfo, option(domain), port, path, queryParameters, fragment);
    }

    public IRI withPort(InetPort port) {
        return new IRI(scheme, userInfo, domain, option(port), path, queryParameters, fragment);
    }

    public IRI withPath(String path) {
        return new IRI(scheme, userInfo, domain, port, option(path), queryParameters, fragment);
    }

    public IRI withQuery(QueryParameters query) {
        return new IRI(scheme, userInfo, domain, port, path, option(query).or(QueryParameters.parameters()), fragment);
    }

    public IRI withQuery(String query) {
        return new IRI(scheme, userInfo, domain, port, path, Decoder.parseQueryString(query), fragment);
    }

    public IRI withParameters(QueryParameters parameters) {
        return withQuery(parameters);
    }

    public IRI addParameter(String key, String value) {
        return new IRI(scheme, userInfo, domain, port, path, queryParameters.deepCopy().add(key, value), fragment);
    }

    public IRI setParameter(String key, String value) {
        return new IRI(scheme, userInfo, domain, port, path, queryParameters.deepCopy().replace(key, value), fragment);
    }

    public IRI removeParameter(String key, String value) {
        return new IRI(scheme, userInfo, domain, port, path, queryParameters.deepCopy().remove(key, value), fragment);
    }

    public IRI removeParameters(String key) {
        return new IRI(scheme, userInfo, domain, port, path, queryParameters.deepCopy().remove(key), fragment);
    }

    public IRI withFragment(String fragment) {
        return new IRI(scheme, userInfo, domain, port, path, queryParameters, option(fragment));
    }

    public IRI addPathSegments(String... pathSegments) {
        var sb = new StringBuilder();

        path.onPresent(sb::append);

        for (var p : pathSegments) {
            var lastChar = sb.charAt(sb.length() - 1);
            var firstChar = p.charAt(0);

            if ('/' == lastChar && '/' == firstChar) {
                sb.append(p.substring(1));
            } else if ('/' == lastChar || '/' == firstChar) {
                sb.append(p);
            } else {
                sb.append('/');
                sb.append(p);
            }
        }

        return new IRI(scheme, userInfo, domain, port, option(sb.toString()), queryParameters, fragment);
    }
}
