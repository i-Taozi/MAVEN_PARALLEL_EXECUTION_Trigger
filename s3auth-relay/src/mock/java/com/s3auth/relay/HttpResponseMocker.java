/**
 * Copyright (c) 2012-2019, s3auth.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the s3auth.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.s3auth.relay;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import org.mockito.Mockito;

/**
 * Mocker of {@link HttpResponse}.
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id: 35b87af2c1e9d6994a7585958c2a2c72b7ddb560 $
 */
public final class HttpResponseMocker {

    /**
     * It's a unility class at the moment.
     */
    private HttpResponseMocker() {
        // intentionally empty
    }

    /**
     * Convert response to string.
     * @param resp The response
     * @return Text form
     * @throws Exception If there is some problem inside
     */
    public static String toString(final HttpResponse resp) throws Exception {
        final Socket socket = Mockito.mock(Socket.class);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Mockito.doReturn(stream).when(socket).getOutputStream();
        resp.send(socket);
        return new String(stream.toByteArray());
    }

}