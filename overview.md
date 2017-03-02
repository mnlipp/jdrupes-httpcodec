HTTP codecs for non-blocking I/O.

JDrupes HTTP Codecs
===================

[TOC formatted]

The HTTP codecs are modeled after the Java 
{@link java.nio.charset.CharsetDecoder} and 
{@link java.nio.charset.CharsetEncoder}.
A decoder is an engine that transforms a sequence
of bytes into a sequence of (initially) HTTP requests or responses.
An encoder transforms an HTTP request or
response (including the payload data) into a sequence of bytes. 

The main difference between the Charset codecs and the HTTP codecs API
is due to the type of the decoded data. For Charset codecs this is a 
homogeneous stream of `chars`, which is easy to handle. For HTTP codecs, 
it's a mixture of headers and body data which can again consist 
of `byte`s or `char`s.

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
"Overflow" indicates that the output buffer is full. In addition,
{@link org.jdrupes.httpcodec.Codec.Result#getCloseConnection} may indicate
that the connection, from which the data is obtained, should be closed
after handling the received message[^closing]. This indication is needed 
because closing the connection is sometimes required by HTTP to
complete a message exchange. As a codec cannot close the connection 
itself, this must be done by the invoker (the supplier of the data stream).

[^closing]: If the decoded message is a request, the connection must 
be closed after sending the response. If the decoded message is
a response, no more data must be sent before closing the connection. 

Besides streams with body data, decoders such as an HTTP decoder
provide the headers that precede this (payload) data. The successful decoding 
of a header is indicated in the result by
{@link org.jdrupes.httpcodec.Decoder.Result#isHeaderCompleted}. The 
decoded header can be retrieved with
{@link org.jdrupes.httpcodec.Decoder#getHeader}.

Sometimes, HTTP requires a provisional feedback to be sent after receiving
the message header. Because the decoder cannot send this feedback
itself, it provides the message to be sent in such cases
with {@link org.jdrupes.httpcodec.Decoder.Result#getResponse}.

If a received message violates the protocol or represents
some kind of "ping" message, sending back the prepared response message 
may be all that has to be done. These cases are indicated by
{@link org.jdrupes.httpcodec.Decoder.Result#isResponseOnly}).

A sample usage of the decoder can be found in the 
[demo code](https://github.com/mnlipp/jdrupes-httpcodec/blob/master/demo/org/jdrupes/httpcodec/demo/Connection.java).

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

The result of the encode method is simply a `Codec.Result` that indicates
whether the output buffer is full and/or further body data is required.

Why Generics?
-------------



@startuml decoder.svg
' ========== Decoder Hierarchy =========
interface Decoder<T extends MessageHeader, R extends MessageHeader> {
    Result<R> decode(ByteBuffer in, Buffer out, boolean endOfInput)
    Optional<T> getHeader()
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
    +getCloseConnection(): boolean
}

class Decoder::Result<R extends MessageHeader> {
    +isHeaderCompleted() : boolean
    +getResponse() : Optional<R>
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
  while ( ) is ([data in receive buffer])
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
  :Send response message;
else ([else])
endif
if () then ([not response only 
&& header complete])
  :Handle message;
  note
  Includes decoding 
  any remaining body 
  data
  end note
else ([else])
endif
if () then ([close connection])
  :Close connection;
else ([else])
endif
end

@enduml
