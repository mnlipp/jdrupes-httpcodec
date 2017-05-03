HTTP codecs for non-blocking I/O.

JDrupes HTTP Codecs
===================

[TOC formatted]

The HTTP codecs are modeled after the Java 
{@link java.nio.charset.CharsetDecoder} and 
{@link java.nio.charset.CharsetEncoder}.
An HTTP decoder is an engine that transforms a sequence
of bytes into a sequence of HTTP requests or responses (and streams
their body data). An HTTP encoder transforms an HTTP request or
response (including streamed body data) into a sequence of bytes. 

The main difference between the Charset codecs and the HTTP codecs API
is due to the type of the decoded data. For Charset codecs this is a 
homogeneous stream of `chars`, which is easy to handle. For HTTP codecs, 
it's a mixture of headers and body data which can again consist 
of either `byte`s or `char`s.

Decoders
--------

Decoders realize the {@link org.jdrupes.httpcodec.Decoder} interface.

![Decoder diagram](decoder.svg)

Binary data received from the network stream is passed to the  
{@link org.jdrupes.httpcodec.Decoder#decode} method in 
a {@link java.nio.ByteBuffer}. The method consumes as much data 
as possible from the buffer and returns the result of the decoding 
process.

![Decoder result](decoder-result.svg)

<img align="right" src="handle-decode-result.svg">

The basic information provided by the decoding process (defined in 
{@link org.jdrupes.httpcodec.Codec.Result}) is
known from the Charset codecs. "Underflow" indicates that more input
data is needed in order to complete the decoding of the message.
"Overflow" indicates that the output buffer is full. "Close connection"
is usually only set by encoders and indicates that the connection
should be closed. This is explained in more detail in the next section.

Besides streams with body data, decoders such as an HTTP decoder
provide the headers that precede this (payload) data. The successful decoding 
of a header is indicated in the result by
{@link org.jdrupes.httpcodec.Decoder.Result#isHeaderCompleted}. The 
decoded header can be retrieved with
{@link org.jdrupes.httpcodec.Decoder#header}. Of course, if the
receive buffer is rather small and the header rather big, it may
take several decoder invocations before a header becomes available.

Sometimes, HTTP requires a provisional feedback to be sent after receiving
the message header. Because the decoder cannot send this feedback
itself, it provides the message to be sent in such cases
with {@link org.jdrupes.httpcodec.Decoder.Result#response()}.

If a received message violates the protocol or represents
some kind of "ping" message, sending back the prepared response message 
may be all that has to be done. These cases are indicated by
{@link org.jdrupes.httpcodec.Decoder.Result#isResponseOnly}).

Encoders
--------

Encoders realize the {@link org.jdrupes.httpcodec.Encoder} interface.

![Decoder diagram](encoder.svg)

Encoding is started with a call to 
{@link org.jdrupes.httpcodec.Encoder#encode(MessageHeader)}. Subsequent
calls to 
{@link org.jdrupes.httpcodec.Encoder#encode(Buffer, ByteBuffer, boolean)}
fill the output buffer with the encoded header and the body data.
If the information in the header indicates that the message does not
have a body, {@link org.jdrupes.httpcodec.Encoder#encode(ByteBuffer)}
can be called.

The result of the encode method is a `Codec.Result` that indicates
whether the output buffer is full and/or further body data is required.
In addition, {@link org.jdrupes.httpcodec.Codec.Result#closeConnection} 
may indicate that the connection, to which the message is sent, should 
be closed after sending the message. This indication 
is needed because closing the connection is sometimes required by HTTP to
complete a message exchange. As an encoder cannot close the connection 
itself, this must be done by the invoker (the manager of the connection).

Why Generics?
-------------

While the previous sections explain the interfaces and classes with
reference to HTTP, you don't find "HTTP" in the names or methods
of the types discussed. The reason is that the API presented above
can be used to handle any "HTTP like" protocol (header with payload).
We need such a general interface because modern HTTP provides the 
upgrade mechanism that allows the client or server to switch to another
protocol. This is currently mostly used for the web socket protocol.
More about that later.

HTTP Codecs
-----------

An HTTP decoder is a special decoder that returns 
{@link org.jdrupes.httpcodec.protocols.http.HttpMessageHeader}s
in its {@link org.jdrupes.httpcodec.protocols.http.HttpDecoder#header()} 
method (type parameter `T`). Of course, if
the result of the `decode` method  includes a response,
it's also of type {@link org.jdrupes.httpcodec.protocols.http.HttpMessageHeader}
(type parameter `R`).  

![HTTP Decoder](http-decoder.svg)

In addition, it is possible to specify a maximum header length to
prevent a malicious request from filling all your memory. And you can
{@linkplain org.jdrupes.httpcodec.protocols.http.HttpDecoder#isClosed() query}
if the decoder has reached the closed state, i.e. won't decode more messages,
because the connection should be closed (if indicated by the result) or
will be closed at the other end after sending a final response.

The HTTP encoder is derived in a similar way.

![HTTP Decoder](http-encoder.svg)

See the {@linkplain org.jdrupes.httpcodec.protocols.http.HttpEncoder#pendingLimit 
method description} for the meaning of "pending limit". 

As you can see, we still haven't reached the goal yet to get concrete
HTTP codecs. This is because there is a difference between HTTP request
messages and HTTP response messages.

![HTTP request and response messages](http-messages.svg)

Now we have all the pieces together. In order to write an HTTP server
you need an `HTTPDecoder` parameterized with `HTTPRequest` as type of the 
decoded message and `HTTPResponse` as type of any preliminary
feedback (optionally provided by the `Decoder.Result`). This is
what makes up an 
{@link org.jdrupes.httpcodec.protocols.http.server.HttpRequestDecoder}.
And you need an `HttpEncoder` parameterized with `HTTPRequest` as type
of the messages to be encode, in short an
{@link org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder}.

For implementing an HTTP client, you need an {@link org.jdrupes.httpcodec.protocols.http.client.HttpRequestEncoder}
and an {@link org.jdrupes.httpcodec.protocols.http.client.HttpResponseDecoder}.

Have a look at the classes javadoc to find out what kind of support each
of the codecs provides regarding header field handling. 

Protocol switching
------------------

HTTP supports a client initiated upgrade from the HTTP protocol to some
other protocol on the same connection. If the upgrade request is confirmed
by the server, subsequent messages from the client are sent using the new
protocol. This, of course, requires using different codecs.

Those codecs, or at least a subset of their functionality, is actually 
already required when the confirmation response is encoded. HTTP allows 
the confirmation response to contain information that is related 
to the new protocol. Obviously, this information cannot be provided by 
the HTTP encoder, because it knows nothing about the new protocol.

The HTTP encoder therefore takes the following approach. When the header
to be encoded contains the confirmation of a protocol switch, it
uses the {@link java.util.ServiceLoader} to find an appropriate
protocol provider. Protocol providers must be derived from 
{@link org.jdrupes.httpcodec.plugin.ProtocolProvider}. Whether a 
protocol provider supports a given protocol can be checked with the
method {@link org.jdrupes.httpcodec.plugin.ProtocolProvider#supportsProtocol}.
The library contains by default the
{@link org.jdrupes.httpcodec.protocols.websocket.WsProtocolProvider},
the probably best known use case for an HTTP protocol upgrade.
  
If the {@link org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder}
cannot find a suitable protocol provider, it modifies the response 
to deny the protocol switch. Else, it asks the provider to 
{@link org.jdrupes.httpcodec.plugin.ProtocolProvider#augmentInitialResponse
apply any require changes} to the confirming response.

The {@link org.jdrupes.httpcodec.protocols.http.server.HttpResponseEncoder}
returns an extended result type that implements the
{@link org.jdrupes.httpcodec.Codec.ProtocolSwitchResult} interface.

![ProtocolSwitchResult](responseencoderresult.svg)

When the encoder finishes the encoding of an upgrade confirmation,
{@link org.jdrupes.httpcodec.Codec.ProtocolSwitchResult#newProtocol}
returns the name of the new protocol (in all other cases it returns
`null`). In addition, the result also provides new codecs obtained
from the plugin provider. These codecs must be used for all subsequent
requests and responses.

Engines
-------

The codecs provided here are deliberatly restricted to using
{@link java.nio.Buffer}s at their interface. They cannot acquire or
send such buffers, as this would tie this library with stream
mechanisms beyond the passing of `Buffer`s. It is therefore not 
possible to provide autonomous engine functionality 
such as automatically sending a preliminary response (see described above).

Nevertheless, the package includes a 
{@link org.jdrupes.httpcodec.ClientEngine} and a
{@link org.jdrupes.httpcodec.ServerEngine}. Both simply group
together a decoder and an encoder as required for client-side
or server-side operation. To support the implementation of
a server, the {@link org.jdrupes.httpcodec.ServerEngine}
automatically adapts the engine to any protocol change, i.e.
it replaces the engine's codecs if the encoder result includes
new ones.

Integration
-----------

The [demo server code](https://github.com/mnlipp/jdrupes-httpcodec/blob/master/demo/org/jdrupes/httpcodec/demo/Connection.java) demonstrates how the HTTP codecs can be used
to implement a single threaded, blocking HTTP server. Of course, this
is not what this library is intended for. It should, however, give you
an idea how to integrate the HTTP codecs in your streaming environment.

An example of integrating this library in an event driven framework
can be found in the [JGrapes project](http://mnlipp.github.io/jgrapes/).


@startuml decoder.svg
' ========== Decoder Hierarchy =========
interface Decoder<T extends MessageHeader, R extends MessageHeader> {
    Result<R> decode(ByteBuffer in, Buffer out, boolean endOfInput)
    Optional<T> header()
}
interface Codec {
}
Codec <|-- Decoder
@enduml

@startuml decoder-result.svg
' ========== Result Hierarchy =========
class Codec::Result {
    +isOverflow(): boolean
    +isUnderflow(): boolean
    +closeConnection(): boolean
}

class Decoder::Result<R extends MessageHeader> {
    +isHeaderCompleted() : boolean
    +response() : Optional<R>
    +boolean isResponseOnly()
}

Codec::Result <|-- Decoder::Result
@enduml

@startuml encoder.svg
' ========== Encoder Hierarchy =========
interface Encoder {
    void encode(T messageHeader)
    Result encode(Buffer in, ByteBuffer out, boolean endOfInput)
    Result encode(ByteBuffer out)
}
interface Codec {
}
Codec <|-- Encoder
@enduml

@startuml receive-loop.svg
' ========== Receive loop =========
skinparam conditionStyle diamond
title Receive Loop
start
while ( ) is ([connection open])
  :Receive data;
  while ( ) is ([data in receive buffer\n && connection open])
    :Invoke decode;
    :Handle decoder result;
  endwhile ([else])
endwhile ([else])
end
@enduml

@startuml handle-decode-result.svg
' ========== Receive loop =========
skinparam conditionStyle diamond
title Handle Decoder Result
start
if () then ([result has message])
  :Send response
  contained in
  result;
else ([else])
endif
if () then ([!response only
&& !connection closed
&& header complete])
  :Handle message;
  note
  Includes decoding 
  any remaining body 
  data
  end note
  :Send response 
  created while 
  handling message;
  end
else ([else])
  end
endif

@enduml


@startuml http-decoder.svg
' ========== HTTP decoder =========
interface Codec {
}

abstract class HttpCodec<T> {
}

interface Decoder<T, R> {
}

abstract class HttpDecoder<T extends HttpMessageHeader, R extends HttpMessageHeader> {
    +HttpDecoder()
    +Decoder.Result<R> decode(ByteBuffer in, Buffer out, boolean endOfInput)
    +Optional<T> header()
    +void setMaxHeaderLength(long maxHeaderLength)
    +long maxHeaderLength()
    +boolean isClosed()
}

Codec <|.. HttpCodec

Codec <|-- Decoder

HttpCodec <|-- HttpDecoder

Decoder <|.. HttpDecoder
@enduml


@startuml http-encoder.svg
' ========== HTTP encoder =========
interface Codec {
}

abstract class HttpCodec<T> {
}

interface Encoder<T> {
}

abstract class HttpEncoder<T extends HttpMessageHeader> {
    +HttpEncoder()
    +void encode(T messageHeader)
    +Encoder.Result encode(Buffer in, ByteBuffer out, boolean endOfInput)
    +int pendingLimit()
    +void setPendingLimit(int pendingLimit)
    +boolean isClosed()
}

Codec <|.. HttpCodec

Codec <|-- Encoder

HttpCodec <|-- HttpEncoder

Encoder <|.. HttpEncoder
@enduml


@startuml http-messages.svg
' ========== HTTP messages =========

abstract class HttpMessageHeader {
}

interface MessageHeader {
}

class HttpRequest {
}

class HttpResponse {
}

class HttpField {
}

HttpMessageHeader <|-- HttpResponse

HttpMessageHeader <|-- HttpRequest

HttpMessageHeader *-right- "*" HttpField 

MessageHeader <|.. HttpMessageHeader

@enduml

@startuml responseencoderresult.svg

class HttpResponseEncoder::Result

class HttpEncoder::Result

interface Codec::ProtocolSwitchResult {
    newProtocol() : String
    newEncoder() : Encoder<?>        
    newDecoder() : Decoder<?, ?>
}

HttpEncoder::Result <|-- HttpResponseEncoder::Result

Codec::ProtocolSwitchResult <|.. HttpResponseEncoder::Result

@enduml
