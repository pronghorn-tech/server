package tech.pronghorn.server

import tech.pronghorn.server.handlers.HttpRequestHandler

data class RegisterURLHandlerMessage(val url: String,
                                     val handlerGenerator: () -> HttpRequestHandler)
