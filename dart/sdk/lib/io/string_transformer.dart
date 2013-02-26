// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of dart.io;

/**
 * String encodings.
 */
class Encoding {
  static const Encoding UTF_8 = const Encoding._internal("UTF-8");
  static const Encoding ISO_8859_1 = const Encoding._internal("ISO-8859-1");
  static const Encoding ASCII = const Encoding._internal("ASCII");
  /**
   * SYSTEM encoding is the current code page on Windows and UTF-8 on
   * Linux and Mac.
   */
  static const Encoding SYSTEM = const Encoding._internal("SYSTEM");
  const Encoding._internal(String this.name);
  final String name;
}


/**
 * Stream transformer that can decode a stream of bytes into a stream of
 * strings using [encoding].
 *
 * Invalid or forbidden byte-sequences will not produce errors, but will instead
 * insert [replacementChar] in the decoded strings.
 */
class StringDecoder implements StreamTransformer<List<int>, String> {
  var _decoder;

  /**
   * Create a new [StringDecoder] with an optional [encoding] and
   * [replacementChar].
   */
  StringDecoder([Encoding encoding = Encoding.UTF_8, int replacementChar]) {
    switch (encoding) {
      case Encoding.UTF_8:
        if (replacementChar == null) {
          replacementChar = UNICODE_REPLACEMENT_CHARACTER_CODEPOINT;
        }
        _decoder = new Utf8DecoderTransformer(replacementChar);
        break;
      case Encoding.ASCII:
        if (replacementChar == null) {
          replacementChar = '?'.codeUnitAt(0);
        } else if (replacementChar > 127) {
          throw new ArgumentError("Invalid replacement character for ASCII");
        }
        _decoder = new _AsciiDecoder(replacementChar);
        break;
      case Encoding.ISO_8859_1:
        if (replacementChar == null) {
          replacementChar = '?'.codeUnitAt(0);
        } else if (replacementChar > 255) {
          throw new ArgumentError(
              "Invalid replacement character for ISO_8859_1");
        }
        _decoder = new _Latin1Decoder(replacementChar);
        break;
      case Encoding.SYSTEM:
        if (Platform.operatingSystem == "windows") {
          _decoder = new _WindowsCodePageDecoder();
        } else {
          if (replacementChar != null) {
            // TODO(ajohnsen): Handle replacement character.
            throw new UnsupportedError(
                "Replacement character is not supported for SYSTEM encoding");
          }
          _decoder = new Utf8DecoderTransformer();
        }
        break;
      default:
        throw new ArgumentError("Unsupported encoding '$encoding'");
    }
  }

  Stream<String> bind(Stream<List<int>> stream) => _decoder.bind(stream);
}


/**
 * Stream transformer that can encode a stream of strings info a stream of
 * bytes using [encoding].
 *
 * Strings that cannot be represented in the given encoding will result in an
 * error and a close event on the stream.
 */
class StringEncoder implements StreamTransformer<String, List<int>> {
  var _encoder;

  /**
   * Create a new [StringDecoder] with an optional [encoding] and
   * [replacementChar].
   */
  StringEncoder([Encoding encoding = Encoding.UTF_8]) {
    switch (encoding) {
      case Encoding.UTF_8:
        _encoder = new Utf8EncoderTransformer();
        break;
      case Encoding.ASCII:
        _encoder = new _AsciiEncoder();
        break;
      case Encoding.ISO_8859_1:
        _encoder = new _Latin1Encoder();
        break;
      case Encoding.SYSTEM:
        if (Platform.operatingSystem == "windows") {
          _encoder = new _WindowsCodePageEncoder();
        } else {
          _encoder = new Utf8EncoderTransformer();
        }
        break;
      default:
        throw new ArgumentError("Unsupported encoding '$encoding'");
    }
  }

  Stream<List<int>> bind(Stream<String> stream) => _encoder.bind(stream);
}


// Utility function to synchronously decode a list of bytes.
String _decodeString(List<int> bytes, [Encoding encoding = Encoding.UTF_8]) {
  if (bytes.length == 0) return "";
  var string;
  var controller = new StreamController();
  controller.stream
    .transform(new StringDecoder(encoding))
    .listen((data) => string = data);
  controller.add(bytes);
  controller.close();
  assert(string != null);
  return string;
}


// Utility function to synchronously encode a String.
// Will throw an exception if the encoding is invalid.
List<int> _encodeString(String string, [Encoding encoding = Encoding.UTF_8]) {
  if (string.length == 0) return [];
  var bytes;
  var controller = new StreamController();
  controller.stream
    .transform(new StringEncoder(encoding))
    .listen((data) => bytes = data);
  controller.add(string);
  controller.close();
  assert(bytes != null);
  return bytes;
}


class LineTransformer extends StreamEventTransformer<String, String> {
  const int _LF = 10;
  const int _CR = 13;

  final StringBuffer _buffer = new StringBuffer();
  String _carry;

  void _handle(String data, StreamSink<String> sink, bool isClosing) {
    if (_carry != null) {
      data = _carry.concat(data);
      _carry = null;
    }
    int startPos = 0;
    int pos = 0;
    while (pos < data.length) {
      int skip = 0;
      int char = data.codeUnitAt(pos);
      if (char == _LF) {
        skip = 1;
      } else if (char == _CR) {
        skip = 1;
        if (pos + 1 < data.length) {
          if (data.codeUnitAt(pos + 1) == _LF) {
            skip = 2;
          }
        } else if (!isClosing) {
          _carry = data.substring(startPos);
          return;
        }
      }
      if (skip > 0) {
        _buffer.add(data.substring(startPos, pos));
        sink.add(_buffer.toString());
        _buffer.clear();
        startPos = pos = pos + skip;
      } else {
        pos++;
      }
    }
    if (pos != startPos) {
      // Add remaining
      _buffer.add(data.substring(startPos, pos));
    }
    if (isClosing && !_buffer.isEmpty) {
      sink.add(_buffer.toString());
      _buffer.clear();
    }
  }

  void handleData(String data, StreamSink<String> sink) {
    _handle(data, sink, false);
  }

  void handleDone(StreamSink<String> sink) {
    _handle("", sink, true);
    sink.close();
  }
}


abstract class _SingleByteDecoder
    extends StreamEventTransformer<List<int>, String> {
  final int _replacementChar;

  _SingleByteDecoder(this._replacementChar);

  void handleData(List<int> data, StreamSink<String> sink) {
    var buffer = new List<int>.fixedLength(data.length);
    for (int i = 0; i < data.length; i++) {
      int char = _decodeByte(data[i]);
      if (char < 0) char = _replacementChar;
      buffer[i] = char;
    }
    sink.add(new String.fromCharCodes(buffer));
  }

  int _decodeByte(int byte);
}


// Utility class for decoding ascii data delivered as a stream of
// bytes.
class _AsciiDecoder extends _SingleByteDecoder {
  _AsciiDecoder(int replacementChar) : super(replacementChar);

  int _decodeByte(int byte) => ((byte & 0x7f) == byte) ? byte : -1;
}


// Utility class for decoding Latin-1 data delivered as a stream of
// bytes.
class _Latin1Decoder extends _SingleByteDecoder {
  _Latin1Decoder(int replacementChar) : super(replacementChar);

  int _decodeByte(int byte) => ((byte & 0xFF) == byte) ? byte : -1;
}


abstract class _SingleByteEncoder
    extends StreamEventTransformer<String, List<int>> {
  void handleData(String data, StreamSink<List<int>> sink) {
    var bytes = _encode(data);
    if (bytes == null) {
      sink.signalError(
          new AsyncError(
              new FormatException("Invalid character for encoding")));
      sink.close();
    } else {
      sink.add(bytes);
    }
  }

  List<int> _encode(String string);
}


// Utility class for encoding a string into an ASCII byte stream.
class _AsciiEncoder extends _SingleByteEncoder {
  List<int> _encode(String string) {
    var bytes = string.codeUnits;
    for (var byte in bytes) {
      if (byte > 127) return null;
    }
    return bytes;
  }
}


// Utility class for encoding a string into a Latin1 byte stream.
class _Latin1Encoder extends _SingleByteEncoder {
  List<int> _encode(String string) {
    var bytes = string.codeUnits;
    for (var byte in bytes) {
      if (byte > 255) return null;
    }
    return bytes;
  }
}


// Utility class for encoding a string into a current windows
// code page byte list.
// Implemented on top of a _SingleByteEncoder, even though it's not really a
// single byte encoder, to avoid copying boilerplate.
class _WindowsCodePageEncoder extends _SingleByteEncoder {
  List<int> _encode(String string) => _encodeString(string);

  external static List<int> _encodeString(String string);
}


// Utility class for decoding Windows current code page data delivered
// as a stream of bytes.
class _WindowsCodePageDecoder extends StreamEventTransformer<List<int>, String> {
  void handleData(List<int> data, StreamSink<String> sink) {
    sink.add(_decodeBytes(data));
  }

  external static String _decodeBytes(List<int> bytes);
}
