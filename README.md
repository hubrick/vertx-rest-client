# Vert.x Rest client

A fully functional Vert.x REST client with RxJava support and request caching.

The design is inspired by Spring's RestTemplate. 
The handling of MimeTypes and HttpMessageConverters is taken directly from Spring.


## Compatibility
- Java 8+
- Vert.x 3.x.x

 Vert.x version     | Library version
 ------------------ | ----------------
 3.3.3              | 2.2.1

## Dependencies

### Dependency
### Maven
```xml
<dependency>
    <groupId>com.hubrick.vertx</groupId>
    <artifactId>vertx-rest-client</artifactId>
    <version>2.2.1</version>
</dependency>
```

## How to use
#### Simple example 

```java
public class ExampleVerticle extends Verticle {

    @Override
    public void start() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<HttpMessageConverter> httpMessageConverters = ImmutableList.of(
            new FormHttpMessageConverter(), 
            new StringHttpMessageConverter(), 
            new JacksonJsonHttpMessageConverter(objectMapper)
        );
    
        final RestClientOptions restClientOptions = new RestClientOptions()
            .setConnectTimeout(500)
            .setGlobalRequestTimeout(300)
            .setDefaultHost("example.com")
            .setDefaultPort(80)
            .setKeepAlive(true)
            .setMaxPoolSize(500);

        final RestClient restClient = RestClient.create(vertx, restClientOptions, httpMessageConverters);
                                     
        // GET example
        final RestClientRequest getRestClientRequest = restClient.get("/api/users/123", SomeReturnObject.class, getRestResponse -> {
            final SomeReturnObject someReturnObject = getRestResponse.getBody();
            // TODO: Handle response
        });
        getRestClientRequest.exceptionHandler(exception -> {
            // TODO: Handle exception
        });
        getRestClientRequest.end();
        
        // POST example
        final RestClientRequest postRestClientRequest = restClient.post("/api/users/123", SomeReturnObject.class, portRestResponse -> {
            final SomeReturnObject someReturnObject = portRestResponse.getBody();
            // TODO: Handle response
        });
        postRestClientRequest.exceptionHandler(exception -> {
            // TODO: Handle exception
        });
        postRestClientRequest.setContentType(MediaType.TEXT_PLAIN);
        postRestClientRequest.end("Some data");
    }
}
```

#### RxJava example
```java
public class ExampleVerticle extends Verticle {

    @Override
    public void start() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<HttpMessageConverter> httpMessageConverters = ImmutableList.of(
            new FormHttpMessageConverter(), 
            new StringHttpMessageConverter(), 
            new JacksonJsonHttpMessageConverter(objectMapper)
        );
        
        final RestClientOptions restClientOptions = new RestClientOptions()
            .setConnectTimeout(500)
            .setGlobalRequestTimeout(300)
            .setDefaultHost("example.com")
            .setDefaultPort(80)
            .setKeepAlive(true)
            .setMaxPoolSize(500);

        final RxRestClient rxRestClient = RxRestClient.create(vertx, restClientOptions, httpMessageConverters);
                                     
        // GET example
        final Observable<RestClientResponse> getRestClientResponse = rxRestClient.get("/api/users/123", SomeReturnObject.class, restClientRequest -> restClientRequest.end());
        getRestClientResponse.subscribe(
            getRestClientResponse -> { 
                // Handle response
            },
            error -> {
                // Handle exception
            }
        );
    }
}
```

#### RxJava example
```java
public class ExampleVerticle extends Verticle {

    @Override
    public void start() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<HttpMessageConverter> httpMessageConverters = ImmutableList.of(
            new FormHttpMessageConverter(), 
            new StringHttpMessageConverter(), 
            new JacksonJsonHttpMessageConverter(objectMapper)
        );
    
        final RestClient restClient = new DefaultRestClient(vertx, httpMessageConverters)
                                                          .setConnectTimeout(500)
                                                          .setGlobalRequestTimeout(300)
                                                          .setHost("example.com")
                                                          .setPort(80)
                                                          .setMaxPoolSize(500);
                                                          
        final RxRestClient rxRestClient = new DefaultRxRestClient(restClient);
                                     
        // GET example
        final Observable<RestClientResponse> getRestClientResponse = rxRestClient.get("/api/users/123", SomeReturnObject.class, restClientRequest -> restClientRequest.end());
        getRestClientResponse.subscribe(
            getRestClientResponse -> { 
                // Handle response
            },
            error -> {
                // Handle exception
            }
        );
    }
}
```


### Request caching
The request cache can be enabled by setting the RequestCacheOptions in the RestClientOptions or setting it on the RestClientRequest object itself which either enables caching on the whole client or per request. The intentation of this cache is to make RxJava flows simpler which means the same service can be called multiple times in the same RxJava flow without worrying that it will be really called multiple times. 

The cache caches not only the results but also the calls itself in case the data from the cache can't be retrieve since the first fired request which should cache the data still didn't finish. When the first fired request is finished it will distribute the data to all requests which are interested in it.

The cache key is calculated using the uri, headers and body. Which means it will only hit the cache in case it's a identical request. To seperate the same request from different callers it's a good idea to introduce a unique requestId in the HTTP Header.

#### Example
```java
    final RequestCacheOptions requestCacheOptions = new RequestCacheOptions()
        .withExpiresAfterWriteMillis(1000)
        .withExpiresAfterAccessMillis(1000);
```


### How to set exception handlers (non RxJava)
Exception handlers are inherited but can be overridden on every level. You can set exception handlers on:
- RestClient
- RestClientRequest
- RestClientResponse

```java
public class ExampleVerticle extends Verticle {

    @Override
    public void start() {
        ...
        // GET example
        final RestClientRequest getRestClientRequest = restClient.get("/api/users/123", SomeReturnObject.class, getRestResponse -> {
            getRestResponse.exceptionHandler(exception -> {
                // TODO: Handle exception
            });
            
            final SomeReturnObject someReturnObject = getRestResponse.getBody();
            // TODO: Handle response
        });
        getRestClientRequest.exceptionHandler(exception -> {
            // TODO: Handle exception
        });
        restClient.exceptionHandler(exception -> {
            // TODO: Handle exception
        });
        getRestClientRequest.end();
        ...
    }
}
```

They will be inherited in the following way: RestClient -> RestClientRequest -> RestClientResponse.
If you want to have a RestClient per Verticle (the desired way) set an exception handler per RestClientRequest and reuse the RestClient instance.


## Supported HttpMessageConverters
 Name                               | Description
 ---------------------------------- | --------------------------------------------------------------------------------------
 FormHttpMessageConverter           | Url-encodes the params. Content-Type: application/x-www-form-urlencoded
 JacksonJsonHttpMessageConverter    | Encodes the object to JSON. Content-Type: application/json
 StringHttpMessageConverter         | Outputs the plain string in the desired charset. Content-Type: plain/text
 ByteArrayHttpMessageConverter      | Propagates the byte array to the http body. Activated by the presence of a byte[]. Content-Type: default application/octet-stream
 MultipartHttpMessageConverter      | Adds support for mutipart uploads. Content-Type: multipart/form-data
 
## Exceptions
 Name                               | Description
 ---------------------------------- | --------------------------------------------------------------------------------------
 RestClientException                | Base exception
 InvalidMediaTypeException          | When the media type is not valid
 InvalidMimeTypeException           | When the mime type is not valid
 HttpStatusCodeException            | Base class for exceptions when status code is 4xx or 5xx
 HttpClientErrorException           | Thrown in case of a 4xx
 HttpServerErrorException           | Thrown in case of a 5xx
 
## License
Apache License, Version 2.0


