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
package com.s3auth.rest;

import com.jcabi.urn.URN;
import com.s3auth.hosts.User;
import java.io.IOException;
import java.net.URI;
import lombok.EqualsAndHashCode;
import org.takes.Request;
import org.takes.facets.auth.Identity;
import org.takes.facets.auth.RqAuth;
import org.takes.rq.RqWrap;

/**
 * User retriever from request.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id: 906b02ff66b404002c1865d8b9df06fe0f60c483 $
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@EqualsAndHashCode(callSuper = true)
final class RqUser extends RqWrap {

    /**
     * Ctor.
     * @param req Request
     */
    RqUser(final Request req) {
        super(req);
    }

    /**
     * Has alias?
     * @return TRUE if alias is there
     * @throws IOException If fails
     */
    public boolean has() throws IOException {
        return !new RqAuth(this).identity().equals(Identity.ANONYMOUS);
    }

    /**
     * Get user.
     * @return User
     * @throws IOException If fails
     */
    public User user() throws IOException {
        final Identity identity = new RqAuth(this).identity();
        final User user;
        if (identity.equals(Identity.ANONYMOUS)) {
            user = User.ANONYMOUS;
        } else {
            user = new User() {
                @Override
                public URN identity() {
                    return URN.create(identity.urn());
                }
                @Override
                public String name() {
                    return identity.properties().get("name");
                }
                @Override
                public URI photo() {
                    return URI.create(
                        identity.properties().get("avatar")
                    );
                }
            };
        }
        return user;
    }

}