package tech.pronghorn.server

import tech.pronghorn.server.handlers.HttpRequestHandler

data class RegisterUrlHandlersMessage(val handlers: Map<String, () -> HttpRequestHandler>)
