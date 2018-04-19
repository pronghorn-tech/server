# Pronghorn HTTP Server
The Pronghorn HTTP Server is a low-level, high performance HTTP server written in [Kotlin](https://kotlinlang.org/). It utilizes the Pronghorn Coroutine Framework to fully utilize available system resources with minimal overhead. No external dependencies are required, however, functionality can be enhanced through optional [plugins](#plugins) which may contain external dependencies.

_Note: The Pronghorn HTTP Server is early in development, and is in many ways a testbed for the [Pronghorn Coroutine Framework](https://github.com/pronghorn-tech/coroutines).  As such it is not currently recommended in production environments.  Documentation will improve as it matures._

## Use Cases
Pronghorn is best suited for applications where high throughput and/or low latency with minimal cpu overhead are critical requirements.

## Quick Start
The following is the simple Hello World server example using Pronghorn.

```kotlin
fun main(args: Array<String>) {
    val helloWorldResponse = HttpResponses.OK("Hello, World!", CommonContentTypes.TextPlain)
    val helloWorldHandler = StaticHttpRequestHandler(helloWorldResponse)

    val server = HttpServer("localhost", 8080)
    server.registerUrlHandler("/hello", helloWorldHandler)
    server.start()
}
```

### Configuration
Alternatively to the above, The HttpServer class can be constructed with an instance of HttpServerConfig

```kotlin
val config = HttpServerConfig(address = ..., ...)
val server = HttpServer(config)
```

#### Full Configuration Options
* __address__ - The address to bind to.
* __workerCount__ - The number of worker threads to utilize, should likely be the number of cores available _(default: number of logical cpu cores)_
* __sendServerHeader__ - If true, the Server header is automatically sent with each response _(default: true)_
* __sendDateHeader__ - If true, the Date header is automatically sent with each response _(default: true)_
* __serverName__ - The value to send in the Server response header if sendServerHeader is true _(default: "Pronghorn")_
* __reusableBufferSize__ - The size of pooled read/write buffers, should be at least as large as the average expected request _(default: 64 KiB)_
* __reusePort__ - If true, the SO_REUSEPORT socket option is used and each worker uses a dedicated socket _(default: auto-detected)_
* __listenBacklog__ - The value for the accept queue for the server socket _(default: 128)_
* __acceptGrouping__ - How many connections should be accepted in a batch, usually equal to the listen backlog _(default: 128)_
* __maxPipelinedRequests__ - The maximum number of http requests allowed to be pipelined on a single connection _(default: 64)_
* __maxRequestSize__ - The maximum acceptable size of a single http request _(default: 1 MiB)_
* __useDirectByteBuffers__ - Whether socket read/write buffers should be direct ByteBuffers _(default: true)_

# Plugins
Pronghorn ships with three optional plugins

### SLF4J Logging Plugin
Utilizes the popular [SLF4J](https://www.slf4j.org/) library for logging. See [https://www.slf4j.org/manual.html](https://www.slf4j.org/manual.html) for more information.

### JCTools Collections Plugin
This plugin offers high performance alternatives for collection types used at critical points throughout Pronghorn. The excellent [JCTools](https://github.com/JCTools/JCTools) library provides wait free and lock less implementations of many concurrent data structures. Utilizing these in place of Java standard library collections results in performance improvements for some workloads.

### OpenHFT Hashing Plugin
This plugin utilizes the [OpenHFT Zero Allocation Hashing](https://github.com/OpenHFT/Zero-Allocation-Hashing) library to provide high performance hashing ByteArrays. This improves Pronghorn performance under some workloads and configurations.

## Enabling Plugins
There are two ways to configure Pronghorn to utilize a plugin implementation.

### Resource File Plugin Configuration
By default Pronghorn looks for a resource file named "pronghorn.properties" in [Java properties file format](https://en.wikipedia.org/wiki/.properties). The keys of this file should be the Plugin class for which an implementation is being specified, with the value being the implementation.

For example, if all of the above plugin dependencies have been included, the _pronghorn.properties_ file would appear as:

    pronghorn.plugins.LoggingPlugin       = tech.pronghorn.plugins.Slf4jLoggingPlugin
    pronghorn.plugins.SpscQueuePlugin     = tech.pronghorn.plugins.JCToolsSpscQueuePlugin
    pronghorn.plugins.MpscQueuePlugin     = tech.pronghorn.plugins.JCToolsMpscQueuePlugin
    pronghorn.plugins.ConcurrentMapPlugin = tech.pronghorn.plugins.JCToolsConcurrentMapPlugin
    pronghorn.plugins.ConcurrentSetPlugin = tech.pronghorn.plugins.JCToolsConcurrentSetPlugin
    pronghorn.plugins.ArrayHasherPlugin   = tech.pronghorn.plugins.OpenHFTArrayHasherPlugin

### Programmatic Plugin Configuration
Alternatively, plugins can be configured programmatically utilizing the _setPlugin_ method as in this example:

    LoggingPlugin.setPlugin(Slf4jLoggingPlugin)

# License
Copyright 2017 Pronghorn Technology LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
