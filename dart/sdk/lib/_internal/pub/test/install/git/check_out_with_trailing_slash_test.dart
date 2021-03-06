// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub_tests;

import 'dart:io';

import 'package:scheduled_test/scheduled_test.dart';

import '../../descriptor.dart' as d;
import '../../test_pub.dart';

main() {
  initConfig();
  group("(regression)", () {
    integration('checks out a package from Git with a trailing slash', () {
      ensureGit();

      d.git('foo.git', [
        d.libDir('foo'),
        d.libPubspec('foo', '1.0.0')
      ]).create();

      d.appDir([{"git": "../foo.git/"}]).create();

      schedulePub(args: ['install'],
          output: new RegExp(r"Dependencies installed!$"));

      d.dir(cachePath, [
        d.dir('git', [
          d.dir('cache', [d.gitPackageRepoCacheDir('foo')]),
          d.gitPackageRevisionCacheDir('foo')
        ])
      ]).validate();

      d.dir(packagesPath, [
        d.dir('foo', [
          d.file('foo.dart', 'main() => "foo";')
        ])
      ]).validate();
    });
  });
}
