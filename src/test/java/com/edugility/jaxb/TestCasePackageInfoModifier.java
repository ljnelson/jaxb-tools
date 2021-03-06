/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright (c) 2013 Edugility LLC.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * The original copy of this license is available at
 * http://www.opensource.org/license/mit-license.html.
 */
package com.edugility.jaxb;

import java.io.*;

import java.lang.annotation.Annotation;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.HashMap;
import java.util.Map;

import java.util.logging.Level;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import javassist.ClassPool;
import javassist.CtClass;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestCasePackageInfoModifier {

  private PackageInfoModifier modifier;

  public TestCasePackageInfoModifier() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    this.modifier = new PackageInfoModifier();
    assertNotNull(this.modifier.logger);
    this.modifier.logger.setLevel(Level.FINER);
    final Map<String, String> bindings = new HashMap<String, String>();
    bindings.put(Person.class.getName(), AnyTypeAdapter.class.getName());
    this.modifier.setBindings(bindings);
  }

  @After
  public void tearDown() throws Exception {
    final CtClass packageInfoCtClass = ClassPool.getDefault().get("com.edugility.jaxb.package-info");
    assertNotNull(packageInfoCtClass);
    packageInfoCtClass.detach();
  }
  
  public File getTestOutputDirectory() {
    final File directory = new File(System.getProperty("maven.project.build.testOutputDirectory", System.getProperty("project.build.testOutputDirectory", "target/test-classes")));
    assertTrue(directory.isDirectory());
    assertTrue(directory.canWrite());
    return directory;
  }

  @Test
  public void testGeneration() throws Exception {
    final PackageInfoModifier.Modification mod = this.modifier.modify("com.edugility.jaxb.generation");
    assertNotNull(mod);
    assertEquals(PackageInfoModifier.Modification.Kind.GENERATED, mod.getKind());
    validateOneAnnotation(mod);
  }

  @Test
  public void testPackageInfoWithNoXmlJavaTypeAdaptersAnnotation() throws Exception {
    final PackageInfoModifier.Modification mod = modifier.modify("com.edugility.jaxb.noxmljavatypeadaptersannotation");
    assertNotNull(mod);
    assertEquals(PackageInfoModifier.Modification.Kind.MODIFIED, mod.getKind());
    validateTwoAnnotations(mod);
  }

  private final void validateOneAnnotation(final PackageInfoModifier.Modification mod) throws Exception {
    assertNotNull(mod);
    final byte[] newClass = mod.toByteArray();
    assertNotNull(newClass);
    assertTrue(newClass.length > 0);

    final Class<?> c = new ClassDefiner().define(mod.getPackageName(), newClass);
    assertNotNull(c);

    final Annotation[] annotations = c.getAnnotations();
    assertNotNull(annotations);
    assertTrue(annotations.length == 1);

    Annotation a = annotations[0];
    assertNotNull(a);
    assertTrue(a instanceof XmlJavaTypeAdapters);
    final XmlJavaTypeAdapters adaptersAnnotation = (XmlJavaTypeAdapters)a;
    final XmlJavaTypeAdapter[] adapters = adaptersAnnotation.value();
    assertNotNull(adapters);
    assertEquals(1, adapters.length);
    final XmlJavaTypeAdapter adapter = adapters[0];
    assertNotNull(adapter);
    assertEquals(Person.class, adapter.type());
    assertEquals(AnyTypeAdapter.class, adapter.value());
  }

  private final void validateTwoAnnotations(final PackageInfoModifier.Modification mod) throws Exception {
    assertNotNull(mod);
    final byte[] newClass = mod.toByteArray();
    assertNotNull(newClass);
    assertTrue(newClass.length > 0);

    final Class<?> c = new ClassDefiner().define(mod.getPackageName(), newClass);
    assertNotNull(c);
    
    final Annotation[] annotations = c.getAnnotations();
    assertNotNull(annotations);
    assertTrue(annotations.length == 2);

    Annotation a = annotations[0];
    assertNotNull(a);
    if (a instanceof Deprecated) {
      a = annotations[1];
      assertNotNull(a);
    }
    assertTrue(a instanceof XmlJavaTypeAdapters);
    final XmlJavaTypeAdapters adaptersAnnotation = (XmlJavaTypeAdapters)a;
    final XmlJavaTypeAdapter[] adapters = adaptersAnnotation.value();
    assertNotNull(adapters);
    assertEquals(1, adapters.length);
    final XmlJavaTypeAdapter adapter = adapters[0];
    assertNotNull(adapter);
    assertEquals(Person.class, adapter.type());
    assertEquals(AnyTypeAdapter.class, adapter.value());
  }

  @Test
  public void testPackageInfoWithEmptyXmlJavaTypeAdaptersAnnotation() throws Exception {
    final PackageInfoModifier.Modification mod = modifier.modify("com.edugility.jaxb.emptyxmljavatypeadaptersannotation");
    assertNotNull(mod);
    assertEquals(PackageInfoModifier.Modification.Kind.MODIFIED, mod.getKind());
    validateOneAnnotation(mod);
  }

  @Test
  public void testPackageInfoWithUnmodifiedXmlJavaTypeAdaptersAnnotationWithValue() throws Exception {
    final PackageInfoModifier.Modification mod = modifier.modify("com.edugility.jaxb.unmodifiedxmljavatypeadaptersannotation.withvalue");
    assertNotNull(mod);
    assertEquals(PackageInfoModifier.Modification.Kind.UNMODIFIED, mod.getKind());
    validateOneAnnotation(mod);
  }

  @Test
  public void testModification() throws Exception {
    final PackageInfoModifier.Modification mod = modifier.modify("com.edugility.jaxb");
    assertNotNull(mod);
    assertEquals(PackageInfoModifier.Modification.Kind.MODIFIED, mod.getKind());
    validateTwoAnnotations(mod);
  }

  @Test(expected = JAXBException.class)
  public void testJAXBContextWithInterfaceThrowsException() throws Exception {
    final JAXBContext c = JAXBContext.newInstance(Person.class);
  }

  private static final class ClassDefiner extends ClassLoader {

    public final Class<?> define(final String packageName, final byte[] classBytes) throws Exception {
      final Class<?> c = this.defineClass(String.format("%s.package-info", packageName), classBytes, 0, classBytes.length);
      assertNotNull(c);
      this.resolveClass(c);
      return c;
    }

  }

}
