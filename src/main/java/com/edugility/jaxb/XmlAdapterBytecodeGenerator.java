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

import java.io.IOException;

import java.util.Formatter; // for javadoc only

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.SignatureAttribute;

/**
 * A generator of bytecode for {@link UniversalXmlAdapter} subclasses.
 *
 * @author <a href="http://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #generate(String, String, String)
 *
 * @see UniversalXmlAdapter
 */
public class XmlAdapterBytecodeGenerator {

  /**
   * An empty {@code byte} array suitable for edge cases encountered
   * by the {@link #generate(String, String, String)} method.  This
   * field is never {@code null}.
   */
  private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  /**
   * A {@linkplain Formatter format string} used to name a {@link
   * UniversalXmlAdapter} subclass.  This field may be {@code null}.
   *
   * @see #getAdapterClassNameTemplate()
   *
   * @see #setAdapterClassNameTemplate(String)
   *
   * @see #getAdapterClassName(String, String, String)
   */
  private String adapterClassNameTemplate;

  /**
   * Creates a new {@link XmlAdapterBytecodeGenerator} and {@linkplain
   * #setAdapterClassNameTemplate(String) sets the default adapter
   * class name template} to be {@code %s.%sTo%sAdapter}.
   */
  public XmlAdapterBytecodeGenerator() {
    super();
    this.setAdapterClassNameTemplate("%s.%sTo%sAdapter");
  }

  /**
   * Returns a {@linkplain Formatter format string} for use by the
   * {@link #getAdapterClassName(String, String, String)} method.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @return a {@link String} representing a {@linkplain Formatter
   * format string} or {@code null}
   *
   * @see #setAdapterClassNameTemplate(String)
   *
   * @see #getAdapterClassName(String, String, String)
   */
  public String getAdapterClassNameTemplate() {
    return this.adapterClassNameTemplate;
  }

  /**
   * Sets the {@linkplain Formatter format string} for use by the
   * {@link #getAdapterClassNameTemplate()} method.
   *
   * @param adapterClassNameTemplate the {@linkplain Formatter format
   * string}; must not be {@code null}
   *
   * @exception IllegalArgumentException if {@code
   * adapterClassNameTemplate} is {@code null}
   *
   * @see #getAdapterClassNameTemplate()
   *
   * @see #getAdapterClassName(String, String, String)
   */
  public void setAdapterClassNameTemplate(final String adapterClassNameTemplate) {
    if (adapterClassNameTemplate == null) {
      throw new IllegalArgumentException("adapterClassNameTemplate", new NullPointerException("adapterClassNameTemplate"));
    }
    this.adapterClassNameTemplate = adapterClassNameTemplate;
  }

  /**
   * Returns the name for a {@link UniversalXmlAdapter} subclass, as
   * formatted appropriately for a package named by the supplied
   * {@code packageName} parameter, a {@link Class} to be adapted
   * named by the supplied {@code interfaceName} parameter and an
   * implementation {@link Class} named by the supplied {@code
   * className} parameter.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param packageName the name of a package; must not be {@code
   * null}
   *
   * @param interfaceName a valid Java {@linkplain Class#getName()
   * class name}; must not be {@code null}
   *
   * @param className a valid Java {@linkplain Class#getName() class
   * name}; must not be {@code null}
   *
   * @return a non-{@code null} Java {@linkplain Class#getName() class
   * name}
   *
   * @exception IllegalArgumentException if any of the parameters is
   * {@code null}
   *
   * @exception IllegalStateException if {@link
   * #getAdapterClassNameTemplate()} returns {@code null}
   *
   * @see #getAdapterClassNameTemplate()
   *
   * @see #setAdapterClassNameTemplate(String)
   */
  public final String getAdapterClassName(final String packageName, final String interfaceName, final String className) {
    if (packageName == null) {
      throw new IllegalArgumentException("packageName", new NullPointerException("packageName"));
    }
    if (interfaceName == null) {
      throw new IllegalArgumentException("interfaceName", new NullPointerException("interfaceName"));
    }
    if (className == null) {
      throw new IllegalArgumentException("className", new NullPointerException("className"));
    }
    final String template = this.getAdapterClassNameTemplate();
    if (template == null) {
      throw new IllegalStateException("The adapterClassNameTemplate property was null");
    }
    return String.format(template, packageName, this.getSimpleName(interfaceName), this.getSimpleName(className));
  }

  /**
   * Returns the last segment of a period-separated name, or the
   * supplied {@code name} itself if it is {@code null} or not
   * period-separated.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @param name the name to parse; may be {@code null}
   *
   * @return the last segment of the supplied {@code name} if it is
   * period-separated, or the supplied {@code name} itself if it is
   * either {@code null} or not period-separated
   */
  private static final String getSimpleName(final String name) {
    String returnValue = null;
    if (name != null) {
      final int lastDotIndex = name.lastIndexOf('.');
      if (lastDotIndex < 0) {
        returnValue = name;
      } else {
        assert name.length() > lastDotIndex + 1;
        returnValue = name.substring(lastDotIndex + 1);
      }
    }
    return returnValue;
  }

  /**
   * Calls the {@link #generate(String, String, String)} method,
   * supplying it with the supplied {@code adapterClassName}, the
   * {@linkplain Class#getName() name} of the supplied {@code
   * interfaceClass} and the {@linkplain Class#getName() name} of the
   * supplied {@code implementationClass}, and returns its results.
   *
   * <p>This method does not validate that the supplied {@code
   * interfaceClass} parameter} {@linkplain Class#isInterface() is an
   * interface}, only that the supplied {@code implementationClass}
   * {@linkplain Class#isAssignableFrom(Class) implements or extends}
   * it.</p>
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param <I> a type to be adapted
   *
   * @param <C> an implementation type that implements the {@code <I>}
   * type
   *
   * @param adapterClassName the name of the {@link
   * UniversalXmlAdapter} subclass; must not be {@code null}
   *
   * @param interfaceClass the interface being adapted; must not be
   * {@code null}
   *
   * @param implementationClass the {@link Class} that is (a) not an
   * interface and (b) guaranteed to be an implementation of the
   * supplied {@code interfaceClass}; must not be {@code null}
   *
   * @return a non-{@code null} array of {@code byte}s representing
   * the {@link UniversalXmlAdapter} subclass suitable for feeding to
   * {@link ClassLoader#defineClass(String, byte[], int, int)} or for
   * serialization to storage as a valid class file
   *
   * @exception BadBytecode if <a
   * href="http://javassist.org/">Javassist</a> had problems with
   * bytecode generation
   *
   * @exception CannotCompileException if <a
   * href="http://javassist.org/">Javassist</a> had problems with
   * bytecode generation
   *
   * @exception IOException if there was a problem reading or writing
   *
   * @exception NotFoundException if <a
   * href="http://javassist.org/">Javassist</a> had problems with
   * bytecode generation
   *
   * @see #generate(String, String, String)
   *
   * @see #getAdapterClassName(String, String, String)
   */
  public final <I, C extends I> byte[] generate(final String adapterClassName, final Class<I> interfaceClass, final Class<C> implementationClass) throws BadBytecode, CannotCompileException, IOException, NotFoundException {
    if (adapterClassName == null) {
      throw new IllegalArgumentException("adapterClassName", new NullPointerException("adapterClassName"));
    }
    if (interfaceClass == null) {
      throw new IllegalArgumentException("interfaceClass", new NullPointerException("interfaceClass"));
    }    
    if (implementationClass == null) {
      throw new IllegalArgumentException("implementationClass", new NullPointerException("implementationClass"));
    }
    if (!(interfaceClass.isAssignableFrom(implementationClass))) {
      throw new IllegalArgumentException("!(interfaceClass.isAssignableFrom(implementationClass)): !(" + interfaceClass.getName() + ".isAssignableFrom(" + implementationClass.getName() + "))");
    }
    return this.generate(adapterClassName, interfaceClass.getName(), implementationClass.getName());
  }

  /**
   * Generates and returns the bytecode for a {@link
   * UniversalXmlAdapter} subclass with the supplied {@code
   * adapterClassName} as its {@linkplain Class#getName() name} that
   * relies upon the fact that {@link Object}s that implement an
   * interface with the supplied {@code interfaceClassName} will be
   * instances of a {@link Class} with the supplied {@code
   * implementationClassName}.
   *
   * <p>This method does not validate that the supplied {@code
   * interfaceClassName} parameter actually identifies a loadable
   * {@link Class} that {@linkplain Class#isInterface() is an
   * interface} nor that the supplied {@code implementationClassName}
   * actually identifies a loadable {@link Class} that {@linkplain
   * Class#isInterface() is not an interface} and an implementation of
   * the interface named by the supplied {@code interfaceClassName}.
   * Consequently the resulting bytecode may not be functional in
   * certain cases.</p>
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param adapterClassName the name of the {@link
   * UniversalXmlAdapter} subclass; must not be {@code null}
   *
   * @param interfaceClassName the name of the interface being
   * adapted; must not be {@code null}
   *
   * @param implementationClassName the name of a {@link Class} that
   * is (a) not an interface and (b) guaranteed to be an
   * implementation of the interface designated by the {@code
   * interfaceClassName} parameter; must not be {@code null}
   *
   * @return a non-{@code null} array of {@code byte}s representing
   * the {@link UniversalXmlAdapter} subclass suitable for feeding to
   * {@link ClassLoader#defineClass(String, byte[], int, int)} or for
   * serialization to storage as a valid class file
   *
   * @exception BadBytecode if <a
   * href="http://javassist.org/">Javassist</a> had problems with
   * bytecode generation
   *
   * @exception CannotCompileException if <a
   * href="http://javassist.org/">Javassist</a> had problems with
   * bytecode generation
   *
   * @exception IOException if there was a problem reading or writing
   *
   * @exception NotFoundException if <a
   * href="http://javassist.org/">Javassist</a> had problems with
   * bytecode generation
   *
   * @see #getAdapterClassName(String, String, String)
   */
  public byte[] generate(final String adapterClassName, final String interfaceClassName, final String implementationClassName) throws BadBytecode, CannotCompileException, IOException, NotFoundException {
    if (adapterClassName == null) {
      throw new IllegalArgumentException("adapterClassName", new NullPointerException("adapterClassName"));
    }
    if (interfaceClassName == null) {
      throw new IllegalArgumentException("interfaceClassName", new NullPointerException("interfaceClassName"));
    }
    if (implementationClassName == null) {
      throw new IllegalArgumentException("implementationClassName", new NullPointerException("implementationClassName"));
    }
    
    ClassPool classPool = this.getClassPool(adapterClassName);
    if (classPool == null) {
      classPool = ClassPool.getDefault();
    }
    assert classPool != null;

    final CtClass universalXmlAdapterCtClass = classPool.get(UniversalXmlAdapter.class.getName());
    assert universalXmlAdapterCtClass != null;

    final CtClass adapterCtClass = classPool.makeClass(adapterClassName, universalXmlAdapterCtClass);
    assert adapterCtClass != null;
    final ClassFile adapterClassFile = adapterCtClass.getClassFile();
    assert adapterClassFile != null;

    assert adapterClassFile.getAttribute(SignatureAttribute.tag) == null; // we just created it after all

    final SignatureAttribute adapterClassSignatureAttribute = new SignatureAttribute(adapterClassFile.getConstPool(), String.format("L%s<L%s;L%s;>;", UniversalXmlAdapter.class.getName().replace('.', '/'), interfaceClassName.replace('.', '/'), implementationClassName.replace('.', '/')));
    adapterClassFile.addAttribute(adapterClassSignatureAttribute);

    assert adapterClassFile.getAttribute(SignatureAttribute.tag) != null;
    
    byte[] returnValue = adapterCtClass.toBytecode();
    if (returnValue == null) {
      returnValue = EMPTY_BYTE_ARRAY;
    }
    return returnValue;

  }

  /**
   * Returns a <a href="http://javassist.org/">Javassist</a> {@link
   * ClassPool} that is appropriate for the supplied class name.
   *
   * <p>The default implementation of this method ignores the {@code
   * className} parameter and returns the return value of {@link
   * ClassPool#getDefault()}.  For nearly all cases, this is the
   * correct behavior and this method should not be overridden.</p>
   *
   * <p>If overrides of this method opt to return {@code null}, the
   * return value of {@link ClassPool#getDefault()} will be used
   * internally instead.</p>
   *
   * <p>This method is {@code protected}, not {@code private}, only
   * because the actual semantics of the {@link
   * ClassPool#getDefault()} are underspecified.  This allows
   * consumers of this class to override this behavior if they are
   * expecting {@link ClassPool}s to be produced in different
   * ways.</p>
   *
   * @param className the {@link Class#getName() class name} for which
   * the returned {@link ClassPool} might be appropriate; may be
   * {@code null} and may safely be ignored; provided for contextual
   * information only
   *
   * @return a {@link ClassPool} instance, or {@code null}, since the
   * {@link ClassPool#getDefault()} does not document whether it is
   * prohibited from returning {@code null}
   *
   * @see ClassPool
   *
   * @see ClassPool#getDefault()
   */
  protected ClassPool getClassPool(final String className) {
    return ClassPool.getDefault();
  }

}
