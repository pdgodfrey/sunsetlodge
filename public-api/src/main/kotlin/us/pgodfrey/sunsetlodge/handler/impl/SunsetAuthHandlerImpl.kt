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
package us.pgodfrey.sunsetlodge.handler.impl

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.ext.auth.User
import io.vertx.ext.auth.authentication.AuthenticationProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.HttpException
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl
import us.pgodfrey.sunsetlodge.handler.SunsetAuthHandler

class SunsetAuthHandlerImpl(authProvider: AuthenticationProvider?, private val returnURLParam: String) :
    AuthenticationHandlerImpl<AuthenticationProvider?>(authProvider), SunsetAuthHandler {
    override fun authenticate(context: RoutingContext, handler: Handler<AsyncResult<User>>) {
        val session = context.session()
        if (session != null) {
            // Now redirect to the login url - we'll get redirected back here after successful login
            session.put(returnURLParam, context.request().uri())
            handler.handle(Future.failedFuture(HttpException(401, "Not authorized")))
        } else {
            handler.handle(Future.failedFuture("No session - did you forget to include a SessionHandler?"))
        }
    }

    override fun performsRedirect(): Boolean {
        return false
    }
}
