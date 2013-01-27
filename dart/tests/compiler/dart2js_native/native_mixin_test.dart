// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Test that native classes can use ordinary Dart classes as mixins.

class A native "*A" {
  foo() => 42;
  baz() => 99;
}

class B extends A with M native "*B" {
  bar() => baz();
}

class M {
  foo() => 87;
  bar() => 101;
}

A makeA() native;
B makeB() native;

void setup() native """
function A() {}
function B() {}
makeA = function(){return new A;};
makeB = function(){return new B;};
""";

main() {
  setup();
  A a = makeA();
  Expect.equals(42, a.foo());
  Expect.throws(() => a.bar(), (error) => error is NoSuchMethodError);
  Expect.equals(99, a.baz());
  Expect.isTrue(a is A);
  Expect.isFalse(a is B);
  Expect.isFalse(a is M);

  B b = makeB();
  Expect.equals(87, b.foo());
  Expect.equals(99, b.bar());
  Expect.equals(99, b.baz());
  Expect.isTrue(b is A);
  Expect.isTrue(b is B);
  Expect.isTrue(b is M);

  M m = new M();
  Expect.equals(87, m.foo());
  Expect.equals(101, m.bar());
  Expect.throws(() => m.baz(), (error) => error is NoSuchMethodError);
  Expect.isFalse(m is A);
  Expect.isFalse(m is B);
  Expect.isTrue(m is M);
}