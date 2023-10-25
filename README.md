# apm-grizzly-asynchttpclient-plugin
An Elastic APM agent plugin that adds support for Grizzly AsyncHttpClient.

Links to library below:
- https://github.com/javaee/grizzly-ahc
- https://github.com/eclipse-ee4j/grizzly-ahc

## Supported versions
| Plugin | Elastic APM Agent | Grizzly AsyncHttpClient |
|:-------|:------------------|:------------------------|
| 0.1+   | 1.43.0+           | 1.12+                   |

## Installation
Set the [`plugins_dir`](https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-plugins-dir) agent configuration option and copy the plugin to specified directory.


## Additional info
### Screenshots of span:

#### Transaction info
![1_transaction.png](images%2F1_transaction.png)

#### Span stacktrace
![2_span_stacktrace.png](images%2F2_span_stacktrace.png)

#### Span metadata
![3_span_metadata.png](images%2F3_span_metadata.png)

#### Span json data example
[span.json](example%2Fspan.json)
