package org.pragmatica.http.routing;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;

import static org.pragmatica.lang.Result.all;

/**
 * Request context providing access to HTTP request data and path parameter matching.
 */
@SuppressWarnings("unused")
public interface RequestContext {
    Result<String> NOT_FOUND = HttpStatus.NOT_FOUND.with("Unknown request path")
                                        .result();
    Route< ?> route();
    String requestId();
    ByteBuf body();
    String bodyAsString();
    <T> Result<T> fromJson(TypeToken<T> literal);
    List<String> pathParams();
    Map<String, List<String>> queryParams();
    Map<String, String> requestHeaders();
    HttpHeaders responseHeaders();

    default Result<String> pathParam(int index) {
        return pathParams()
                         .size() > index
               ? Result.success(pathParams()
                                          .get(index))
               : NOT_FOUND;
    }

    default <T1> Result.Mapper1<T1> matchPath(PathParameter<T1> p1) {
        return all(pathParam(0)
                            .flatMap(p1::parse));
    }

    default <T1, T2> Result.Mapper2<T1, T2> matchPath(PathParameter<T1> p1,
                                                      PathParameter<T2> p2) {
        return all(pathParam(0)
                            .flatMap(p1::parse), pathParam(1)
                                                          .flatMap(p2::parse));
    }

    default <T1, T2, T3> Result.Mapper3<T1, T2, T3> matchPath(PathParameter<T1> p1,
                                                              PathParameter<T2> p2,
                                                              PathParameter<T3> p3) {
        return all(pathParam(0)
                            .flatMap(p1::parse), pathParam(1)
                                                          .flatMap(p2::parse), pathParam(2)
                                                                                        .flatMap(p3::parse));
    }

    default <T1, T2, T3, T4> Result.Mapper4<T1, T2, T3, T4> matchPath(PathParameter<T1> p1,
                                                                      PathParameter<T2> p2,
                                                                      PathParameter<T3> p3,
                                                                      PathParameter<T4> p4) {
        return all(pathParam(0)
                            .flatMap(p1::parse),
                   pathParam(1)
                            .flatMap(p2::parse),
                   pathParam(2)
                            .flatMap(p3::parse),
                   pathParam(3)
                            .flatMap(p4::parse));
    }

    default <T1, T2, T3, T4, T5> Result.Mapper5<T1, T2, T3, T4, T5> matchPath(PathParameter<T1> p1,
                                                                              PathParameter<T2> p2,
                                                                              PathParameter<T3> p3,
                                                                              PathParameter<T4> p4,
                                                                              PathParameter<T5> p5) {
        return all(pathParam(0)
                            .flatMap(p1::parse),
                   pathParam(1)
                            .flatMap(p2::parse),
                   pathParam(2)
                            .flatMap(p3::parse),
                   pathParam(3)
                            .flatMap(p4::parse),
                   pathParam(4)
                            .flatMap(p5::parse));
    }

    default <T1, T2, T3, T4, T5, T6> Result.Mapper6<T1, T2, T3, T4, T5, T6> matchPath(PathParameter<T1> p1,
                                                                                      PathParameter<T2> p2,
                                                                                      PathParameter<T3> p3,
                                                                                      PathParameter<T4> p4,
                                                                                      PathParameter<T5> p5,
                                                                                      PathParameter<T6> p6) {
        return all(pathParam(0)
                            .flatMap(p1::parse),
                   pathParam(1)
                            .flatMap(p2::parse),
                   pathParam(2)
                            .flatMap(p3::parse),
                   pathParam(3)
                            .flatMap(p4::parse),
                   pathParam(4)
                            .flatMap(p5::parse),
                   pathParam(5)
                            .flatMap(p6::parse));
    }

    default <T1, T2, T3, T4, T5, T6, T7> Result.Mapper7<T1, T2, T3, T4, T5, T6, T7> matchPath(PathParameter<T1> p1,
                                                                                              PathParameter<T2> p2,
                                                                                              PathParameter<T3> p3,
                                                                                              PathParameter<T4> p4,
                                                                                              PathParameter<T5> p5,
                                                                                              PathParameter<T6> p6,
                                                                                              PathParameter<T7> p7) {
        return all(pathParam(0)
                            .flatMap(p1::parse),
                   pathParam(1)
                            .flatMap(p2::parse),
                   pathParam(2)
                            .flatMap(p3::parse),
                   pathParam(3)
                            .flatMap(p4::parse),
                   pathParam(4)
                            .flatMap(p5::parse),
                   pathParam(5)
                            .flatMap(p6::parse),
                   pathParam(6)
                            .flatMap(p7::parse));
    }

    default <T1, T2, T3, T4, T5, T6, T7, T8> Result.Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> matchPath(PathParameter<T1> p1,
                                                                                                      PathParameter<T2> p2,
                                                                                                      PathParameter<T3> p3,
                                                                                                      PathParameter<T4> p4,
                                                                                                      PathParameter<T5> p5,
                                                                                                      PathParameter<T6> p6,
                                                                                                      PathParameter<T7> p7,
                                                                                                      PathParameter<T8> p8) {
        return all(pathParam(0)
                            .flatMap(p1::parse),
                   pathParam(1)
                            .flatMap(p2::parse),
                   pathParam(2)
                            .flatMap(p3::parse),
                   pathParam(3)
                            .flatMap(p4::parse),
                   pathParam(4)
                            .flatMap(p5::parse),
                   pathParam(5)
                            .flatMap(p6::parse),
                   pathParam(6)
                            .flatMap(p7::parse),
                   pathParam(7)
                            .flatMap(p8::parse));
    }

    default <T1, T2, T3, T4, T5, T6, T7, T8, T9> Result.Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> matchPath(PathParameter<T1> p1,
                                                                                                              PathParameter<T2> p2,
                                                                                                              PathParameter<T3> p3,
                                                                                                              PathParameter<T4> p4,
                                                                                                              PathParameter<T5> p5,
                                                                                                              PathParameter<T6> p6,
                                                                                                              PathParameter<T7> p7,
                                                                                                              PathParameter<T8> p8,
                                                                                                              PathParameter<T9> p9) {
        return all(pathParam(0)
                            .flatMap(p1::parse),
                   pathParam(1)
                            .flatMap(p2::parse),
                   pathParam(2)
                            .flatMap(p3::parse),
                   pathParam(3)
                            .flatMap(p4::parse),
                   pathParam(4)
                            .flatMap(p5::parse),
                   pathParam(5)
                            .flatMap(p6::parse),
                   pathParam(6)
                            .flatMap(p7::parse),
                   pathParam(7)
                            .flatMap(p8::parse),
                   pathParam(8)
                            .flatMap(p9::parse));
    }
}
