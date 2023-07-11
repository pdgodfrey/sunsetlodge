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
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.ext.auth.User
import io.vertx.ext.auth.authentication.AuthenticationProvider
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.FormLoginHandler
import io.vertx.ext.web.handler.HttpException
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl

/**
 * @author [Tim Fox](http://tfox.org)
 */
class JsonBodyLoginHandlerImpl(
    authProvider: AuthenticationProvider?, private var usernameParam: String, private var passwordParam: String,
    private var returnURLParam: String?, private var directLoggedInOKURL: String?
) : AuthenticationHandlerImpl<AuthenticationProvider?>(authProvider), FormLoginHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun setUsernameParam(usernameParam: String): FormLoginHandler {
        this.usernameParam = usernameParam
        return this
    }

    override fun setPasswordParam(passwordParam: String): FormLoginHandler {
        this.passwordParam = passwordParam
        return this
    }

    override fun setReturnURLParam(returnURLParam: String): FormLoginHandler {
        this.returnURLParam = returnURLParam
        return this
    }

    override fun setDirectLoggedInOKURL(directLoggedInOKURL: String): FormLoginHandler {
        this.directLoggedInOKURL = directLoggedInOKURL
        return this
    }

    override fun authenticate(context: RoutingContext, handler: Handler<AsyncResult<User>>) {
        val req = context.request()
        if (req.method() !== HttpMethod.POST) {
            handler.handle(Future.failedFuture(BAD_METHOD)) // Must be a POST
        } else {
            if (!context.body().available()) {
                handler.handle(Future.failedFuture("BodyHandler is required to process POST requests"))
            } else {
                val data = context.body().asJsonObject()
                val username = data.getString(usernameParam)
                val password = data.getString(passwordParam)
                if (username == null || password == null) {
                    handler.handle(Future.failedFuture(BAD_REQUEST))
                } else {
                    authProvider!!.authenticate(
                        UsernamePasswordCredentials(
                            username,
                            password
                        )
                    ) { authn: AsyncResult<User> ->
                        if (authn.failed()) {
                            handler.handle(Future.failedFuture(HttpException(401, authn.cause())))
                        } else {
                            handler.handle(authn)
                        }
                    }
                }
            }
        }
    }

    override fun postAuthentication(ctx: RoutingContext) {
        val req = ctx.request()
        val session = ctx.session()

        if(ctx.user() != null){
          req.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
            .end(DEFAULT_DIRECT_LOGGED_IN_OK_PAGE)
        }

        if (session != null) {
          if(returnURLParam != null){
            val returnURL = session.remove<String?>(returnURLParam)
            if (returnURL != null) {
              // Now redirect back to the original url
              ctx.redirect(returnURL)
              return
            }
          }
        }
        // Either no session or no return url
        if (directLoggedInOKURL != null) {
            // Redirect to the default logged in OK page - this would occur
            // if the user logged in directly at this URL without being redirected here first from another
            // url
            ctx.redirect(directLoggedInOKURL)
        } else {
            // Just show a basic page
            req.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
                .end(DEFAULT_DIRECT_LOGGED_IN_OK_PAGE)
        }
    }

    companion object {
        val UNAUTHORIZED = HttpException(401)
        val BAD_REQUEST = HttpException(400)
        val BAD_METHOD = HttpException(405)
        private const val DEFAULT_DIRECT_LOGGED_IN_OK_PAGE = "<html><body><h1>Login successful</h1></body></html>"
    }
}
