// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:io';
import 'dart:json' as json;

import 'package:scheduled_test/scheduled_test.dart';
import 'package:scheduled_test/scheduled_server.dart';

import '../../lib/src/io.dart';
import '../../lib/src/utils.dart';
import '../descriptor.dart' as d;
import '../test_pub.dart';
import 'utils.dart';

main() {
  initConfig();
  integration('with a malformed credentials.json, authenticates again and '
      'saves credentials.json', () {
    d.validPackage.create();

    var server = new ScheduledServer();
    d.dir(cachePath, [
      d.file('credentials.json', '{bad json')
    ]).create();

    var pub = startPublish(server);
    confirmPublish(pub);
    authorizePub(pub, server, "new access token");

    server.handle('GET', '/packages/versions/new.json', (request) {
      expect(request.headers.value('authorization'),
          equals('Bearer new access token'));

      request.response.close();
    });

    // After we give pub an invalid response, it should crash. We wait for it to
    // do so rather than killing it so it'll write out the credentials file.
    pub.shouldExit(1);

    d.credentialsFile(server, 'new access token').validate();
  });
}