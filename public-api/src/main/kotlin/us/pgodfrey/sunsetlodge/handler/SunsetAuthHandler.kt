/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package us.pgodfrey.sunsetlodge.handler

import io.vertx.ext.auth.authentication.AuthenticationProvider
import io.vertx.ext.web.handler.AuthenticationHandler
import us.pgodfrey.sunsetlodge.handler.impl.SunsetAuthHandlerImpl

interface SunsetAuthHandler : AuthenticationHandler {
    companion object {

        fun create(authProvider: AuthenticationProvider): SunsetAuthHandler {
            return SunsetAuthHandlerImpl(authProvider, DEFAULT_RETURN_URL_PARAM)
        }

        fun create(
            authProvider: AuthenticationProvider?,
            returnURLParam: String
        ): SunsetAuthHandler? {
            return SunsetAuthHandlerImpl(authProvider, returnURLParam)
        }

        const val DEFAULT_RETURN_URL_PARAM = "return_url"
    }
}
