/*
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2016, 2017  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
/**
 * The entry point interfaces and classes.
 * 
 * 
 * @startuml decoder.svg
 * ' ========== Decoder Hierarchy =========
 * interface Decoder<T extends MessageHeader, R extends MessageHeader> {
 *     Result<R> decode(ByteBuffer in, Buffer out, boolean endOfInput)
 *     Optional<T> header()
 * }
 * interface Codec {
 * }
 * Codec <|-- Decoder
 * @enduml
 * 
 * @startuml decoder-result.svg
 * ' ========== Result Hierarchy =========
 * class Codec::Result {
 *     +isOverflow(): boolean
 *     +isUnderflow(): boolean
 *     +closeConnection(): boolean
 * }
 * 
 * class Decoder::Result<R extends MessageHeader> {
 *     +isHeaderCompleted() : boolean
 *     +response() : Optional<R>
 *     +boolean isResponseOnly()
 * }
 * 
 * Codec::Result <|-- Decoder::Result
 * @enduml
 * 
 * @startuml encoder.svg
 * ' ========== Encoder Hierarchy =========
 * interface Encoder {
 *     void encode(T messageHeader)
 *     Result encode(Buffer in, ByteBuffer out, boolean endOfInput)
 *     Result encode(ByteBuffer out)
 * }
 * interface Codec {
 * }
 * Codec <|-- Encoder
 * @enduml
 * 
 * @startuml receive-loop.svg
 * ' ========== Receive loop =========
 * skinparam conditionStyle diamond
 * title Receive Loop
 * start
 * while ( ) is ([connection open])
 *   :Receive data;
 *   while ( ) is ([data in receive buffer\n && connection open])
 *     :Invoke decode;
 *     :Handle decoder result;
 *   endwhile ([else])
 * endwhile ([else])
 * end
 * @enduml
 * 
 * @startuml handle-decode-result.svg
 * ' ========== Receive loop =========
 * skinparam conditionStyle diamond
 * title Handle Decoder Result
 * 
 * start
 * if () then ([result has message])
 *   :Send protocol level
 *   response contained 
 *   in result;
 * else ([else])
 * endif
 * if () then ([close connection])
 *   :Close connection;
 * else ([else])
 * endif
 * if () then ([!response only
 * && header complete])
 *   :Handle message;
 *   note
 *   Includes decoding 
 *   any body data
 *   remaining in 
 *   received chunk
 *   end note
 *   :Send response 
 *   created while 
 *   handling message;
 *   end
 * else ([else])
 *   end
 * endif
 * 
 * @enduml
 * 
 * 
 * @startuml http-decoder.svg
 * ' ========== HTTP decoder =========
 * interface Codec {
 * }
 * 
 * abstract class HttpCodec<T> {
 * }
 * 
 * interface Decoder<T, R> {
 * }
 * 
 * abstract class HttpDecoder<T extends HttpMessageHeader, R extends HttpMessageHeader> {
 *     +HttpDecoder()
 *     +Decoder.Result<R> decode(ByteBuffer in, Buffer out, boolean endOfInput)
 *     +Optional<T> header()
 *     +void setMaxHeaderLength(long maxHeaderLength)
 *     +long maxHeaderLength()
 *     +boolean isClosed()
 * }
 * 
 * Codec <|.. HttpCodec
 * 
 * Codec <|-- Decoder
 * 
 * HttpCodec <|-- HttpDecoder
 * 
 * Decoder <|.. HttpDecoder
 * @enduml
 * 
 * 
 * @startuml http-encoder.svg
 * ' ========== HTTP encoder =========
 * interface Codec {
 * }
 * 
 * abstract class HttpCodec<T> {
 * }
 * 
 * interface Encoder<T> {
 * }
 * 
 * abstract class HttpEncoder<T extends HttpMessageHeader> {
 *     +HttpEncoder()
 *     +void encode(T messageHeader)
 *     +Encoder.Result encode(Buffer in, ByteBuffer out, boolean endOfInput)
 *     +int pendingLimit()
 *     +void setPendingLimit(int pendingLimit)
 *     +boolean isClosed()
 * }
 * 
 * Codec <|.. HttpCodec
 * 
 * Codec <|-- Encoder
 * 
 * HttpCodec <|-- HttpEncoder
 * 
 * Encoder <|.. HttpEncoder
 * @enduml
 * 
 * 
 * @startuml http-messages.svg
 * ' ========== HTTP messages =========
 * 
 * abstract class HttpMessageHeader {
 * }
 * 
 * interface MessageHeader {
 * }
 * 
 * class HttpRequest {
 * }
 * 
 * class HttpResponse {
 * }
 * 
 * class HttpField {
 * }
 * 
 * HttpMessageHeader <|-- HttpResponse
 * 
 * HttpMessageHeader <|-- HttpRequest
 * 
 * HttpMessageHeader *-right- "*" HttpField 
 * 
 * MessageHeader <|.. HttpMessageHeader
 * 
 * @enduml
 * 
 * @startuml responseencoderresult.svg
 * 
 * class HttpResponseEncoder::Result
 * 
 * class HttpEncoder::Result
 * 
 * interface Codec::ProtocolSwitchResult {
 *     newProtocol() : String
 *     newEncoder() : Encoder<?>        
 *     newDecoder() : Decoder<?, ?>
 * }
 * 
 * HttpEncoder::Result <|-- HttpResponseEncoder::Result
 * 
 * Codec::ProtocolSwitchResult <|.. HttpResponseEncoder::Result
 * 
 * @enduml
 */
@org.osgi.annotation.versioning.Version("${api_version}")
package org.jdrupes.httpcodec;
