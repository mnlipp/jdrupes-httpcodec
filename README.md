JDrupes non-blocking HTTP Codec
===============================

[![Build Status](https://travis-ci.org/mnlipp/jdrupes-httpcodec.svg?branch=master)](https://travis-ci.org/mnlipp/jdrupes-httpcodec) 
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/0d9e648d1d904ec6a1f0ca713ca30c5c)](https://www.codacy.com/app/mnlipp/jdrupes-httpcodec?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=mnlipp/jdrupes-httpcodec&amp;utm_campaign=Badge_Grade)
[![Release](https://jitpack.io/v/mnlipp/jdrupes-httpcodec.svg)](https://jitpack.io/#mnlipp/jdrupes-httpcodec)

The goal of this package is to provide easy to use HTTP 
encoders and decoders for non-blocking I/O
that use Java `Buffer`s for handling the data.

I'm well aware that such libraries already exist (searching easily reveals
implementations such as the 
[Apache Codecs](https://hc.apache.org/httpcomponents-core-ga/httpcore-nio/apidocs/org/apache/http/impl/nio/codecs/package-summary.html) 
or the 
[Netty Codes](http://netty.io/4.0/api/io/netty/handler/codec/http/package-summary.html)).
However, I found all of them to be too closely integrated with their respective
frameworks, which didn't go well with my intention to write my own
[event driven framework](http://mnlipp.github.io/jgrapes/). 
An implementation that comes very close to what I needed is 
[HTTP Kit](https://github.com/http-kit/http-kit), which has, however,
dependencies on Clojure, that prohibit its usage for my purpose.

This library can be used with Java 8 SE. Binaries are currently made
available as snapshots.

```gradle
repositories {
	maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}

dependencies {
	compile 'org.jdrupes.httpcodec:httpcodec:0.9.X-SNAPSHOT'
}
```

(You can lookup the latest patch version [here](https://oss.sonatype.org/content/repositories/snapshots/org/jdrupes/httpcodec/httpcodec/)). 

I plan to improve documentation over time. For now, the best starting
point is to have a look at the 
[JavaDoc](https://mnlipp.github.io/jdrupes-httpcodec/javadoc/index.html) 
and at the source code in the 
[`demo`](https://github.com/mnlipp/jdrupes-httpcodec/tree/master/demo/org/jdrupes/httpcodec/demo) folder.

Contributions and bug reports are welcome. Please provide them as
GitHub issues.
