// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub_tests;

import 'dart:io';

import '../../descriptor.dart' as d;
import '../../test_pub.dart';

main() {
  initConfig();
  integration('checks out a package at a specific branch from Git', () {
    ensureGit();

    var repo = d.git('foo.git', [
      d.libDir('foo', 'foo 1'),
      d.libPubspec('foo', '1.0.0')
    ]);
    repo.create();
    repo.runGit(["branch", "old"]);

    d.git('foo.git', [
      d.libDir('foo', 'foo 2'),
      d.libPubspec('foo', '1.0.0')
    ]).commit();

    d.appDir([{"git": {"url": "../foo.git", "ref": "old"}}]).create();

    schedulePub(args: ['install'],
        output: new RegExp(r"Dependencies installed!$"));

    d.dir(packagesPath, [
      d.dir('foo', [
        d.file('foo.dart', 'main() => "foo 1";')
      ])
    ]).validate();
  });
}
