package org.pfj.http.server.error;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.pfj.http.server.error.CauseMapper;
import org.pfj.http.server.error.CompoundCause;
import org.pfj.lang.Causes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CauseMapperTest {
    @Test
    void defaultMapperPassesCompoundCauseThrough() {
        var cause = CompoundCause.from(HttpResponseStatus.MOVED_PERMANENTLY, Causes.cause("Message"));

        assertSame(cause, CauseMapper.defaultConverter(cause));
    }

    @Test
    void defaultMapperConvertsSimpleCauseIntoCompoundCause() {
        var cause = Causes.cause("Some issue");

        assertEquals(CompoundCause.from(HttpResponseStatus.INTERNAL_SERVER_ERROR, cause), CauseMapper.defaultConverter(cause));
    }
}