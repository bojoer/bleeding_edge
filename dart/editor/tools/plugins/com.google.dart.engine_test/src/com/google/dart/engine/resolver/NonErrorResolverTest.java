/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.engine.resolver;

import com.google.dart.engine.source.Source;

public class NonErrorResolverTest extends ResolverTestCase {
  public void test_argumentDefinitionTestNonParameter_formalParameter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(var v) {",
        "  return ?v;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_argumentDefinitionTestNonParameter_namedParameter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f({var v : 0}) {",
        "  return ?v;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_argumentDefinitionTestNonParameter_optionalParameter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f([var v]) {",
        "  return ?v;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_breakWithoutLabelInSwitch() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  void m(int i) {",
        "    switch (i) {",
        "      case 0:",
        "        break;",
        "    }",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_builtInIdentifierAsType_dynamic() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  dynamic x;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_caseExpressionTypeImplementsEquals_int() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(int i) {",
        "  switch(i) {",
        "    case(1) : return 1;",
        "    default: return 0;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_caseExpressionTypeImplementsEquals_Object() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class IntWrapper {",
        "  final int value;",
        "  const IntWrapper(this.value);",
        "}",
        "",
        "f(IntWrapper intWrapper) {",
        "  switch(intWrapper) {",
        "    case(const IntWrapper(1)) : return 1;",
        "    default: return 0;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_caseExpressionTypeImplementsEquals_String() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(String s) {",
        "  switch(s) {",
        "    case('1') : return 1;",
        "    default: return 0;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_const() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  const int x;",
        "  const A() {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_final() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  final int x;",
        "  const A() {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_syntheticField() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  const A();",
        "  set x(value) {}",
        "  get x {return 0;}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_defaultValueInFunctionTypeAlias() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "typedef F([x]);"));
    resolve(source);
    assertErrors();
    verify(source);
  }

  public void test_duplicateDefinition_getter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "bool get a => true;"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_fieldInitializedInInitializerAndDeclaration_fieldNotFinal() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x = 0;",
        "  A() : x = 1 {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_fieldInitializedInInitializerAndDeclaration_finalFieldNotSet() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  final int x;",
        "  A() : x = 1 {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_fieldInitializerOutsideConstructor() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "  A(this.x) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_fieldInitializerOutsideConstructor_defaultParameters() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "  A([this.x]) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invalidAssignment() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  var x;",
        "  var y;",
        "  x = y;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invalidAssignment_toDynamic() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  var g;",
        "  g = () => 0;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invocationOfNonFunction_dynamic() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  var f;",
        "}",
        "class B extends A {",
        "  g() {",
        "    f();",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invocationOfNonFunction_getter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  var g;",
        "}",
        "f() {",
        "  A a;",
        "  a.g();",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invocationOfNonFunction_localVariable() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  var g;",
        "  g();",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_newWithAbstractClass_factory() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "abstract class A {",
        "  factory A() { return new B(); }",
        "}",
        "class B implements A {",
        "  B() {}",
        "}",
        "A f() {",
        "  return new A();",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonBoolExpression_assert_bool() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  assert(true);",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonBoolExpression_assert_functionType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "bool makeAssertion() => true;",
        "f() {",
        "  assert(makeAssertion);",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_rethrowOutsideCatch() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  void m() {",
        "    try {} catch (e) {rethrow;}",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_returnOfInvalidType_dynamic() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static void testLogicalOp() {",
        "    testOr(a, b, onTypeError) {",
        "      try {",
        "        return a || b;",
        "      } on TypeError catch (t) {",
        "        return onTypeError;",
        "      }",
        "    }",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_returnOfInvalidType_subtype() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B extends A {}",
        "A f(B b) { return b; }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_returnOfInvalidType_supertype() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B extends A {}",
        "B f(A a) { return a; }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_const() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B extends A {}",
        "class G<E extends A> {",
        "  const G() {}",
        "}",
        "f() { return const G<B>(); }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_new() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B extends A {}",
        "class G<E extends A> {}",
        "f() { return new G<B>(); }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }
}