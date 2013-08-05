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
import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;

import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.MemberValue;

public class InterfaceDecorator {

  protected transient Logger logger;

  public InterfaceDecorator() {
    super();
    this.logger = this.createLogger();
    if (this.logger == null) {
      this.logger = Logger.getLogger(this.getClass().getName());
    }
  }

  protected Logger createLogger() {
    return Logger.getLogger(this.getClass().getName());
  }

  public Modification modify(final String interfaceName, final String adapterClassName) throws CannotCompileException, IOException, NotFoundException {
    if (this.logger != null && this.logger.isLoggable(Level.FINER)) {
      this.logger.entering(this.getClass().getName(), "modify", interfaceName);
    }
    if (interfaceName == null) {
      throw new IllegalArgumentException("interfaceName", new NullPointerException("interfaceName"));
    }
    if (adapterClassName == null) {
      throw new IllegalArgumentException("adapterClassName", new NullPointerException("adapterClassName"));
    }

    ClassPool classPool = this.getClassPool(interfaceName);
    if (classPool == null) {
      classPool = ClassPool.getDefault();
    }
    assert classPool != null;

    final CtClass interfaceCtClass = classPool.getOrNull(interfaceName);
    if (interfaceCtClass == null) {
      throw new IllegalArgumentException("interfaceName");
    }
    assert interfaceCtClass != null;

    final boolean modified = this.installXmlJavaTypeAdapter(interfaceCtClass, adapterClassName);
    
    final byte[] bytes = interfaceCtClass.toBytecode();
    assert bytes != null;
    assert bytes.length > 0;

    final Modification returnValue = new Modification(interfaceCtClass, modified ? Modification.Kind.MODIFIED : Modification.Kind.UNMODIFIED, bytes);

    if (this.logger != null && this.logger.isLoggable(Level.FINER)) {
      this.logger.exiting(this.getClass().getName(), "modify", returnValue);
    }
    return returnValue;
  }

  /**
   * Returns a Javassist {@link ClassPool} that is appropriate for the
   * supplied class name.
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
   * @param className the class name for which the returned {@link
   * ClassPool} might be appropriate; may be {@code null} and may
   * safely be ignored; provided for contextual information only
   *
   * @return a {@link ClassPool} instance, or {@code null}
   *
   * @see ClassPool
   *
   * @see ClassPool#getDefault()
   */
  protected ClassPool getClassPool(final String className) {
    return ClassPool.getDefault();
  }

  /**
   * Installs an {@link XmlJavaTypeAdapter} annotation on the supplied
   * {@link CtClass} or modifies an existing one.
   *
   * @param interfaceCtClass the {@link CtClass} to decorate; must not
   * be {@code null}
   *
   * @param adapterClassName the name of the {@link
   * XmlJavaTypeAdapter} class to use as the value for the {@link
   * XmlJavaTypeAdapter#value()} attribute; must not be {@code null}
   *
   * @return {@code true} if the supplied {@link CtClass} was
   * modified; {@code false} otherwise
   *
   * @exception NotFoundException if Javassist couldn't find something
   */
  private final boolean installXmlJavaTypeAdapter(final CtClass interfaceCtClass, final String adapterClassName) throws NotFoundException {
    if (interfaceCtClass == null) {
      throw new IllegalArgumentException("interfaceCtClass", new NullPointerException("interfaceCtClass"));
    }
    if (adapterClassName == null) {
      throw new IllegalArgumentException("adapterClassName", new NullPointerException("adapterClassName"));
    }

    boolean modified = false;

    final ClassFile interfaceClassFile = interfaceCtClass.getClassFile();
    assert interfaceClassFile != null;

    final ConstPool constantPool = interfaceClassFile.getConstPool();
    assert constantPool != null;

    AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute)interfaceClassFile.getAttribute(AnnotationsAttribute.visibleTag);
    if (annotationsAttribute == null) {
      annotationsAttribute = new AnnotationsAttribute(constantPool, AnnotationsAttribute.visibleTag);
      interfaceClassFile.addAttribute(annotationsAttribute);
      assert annotationsAttribute == interfaceClassFile.getAttribute(AnnotationsAttribute.visibleTag);
      modified = true;
    }
    assert annotationsAttribute != null;

    Annotation adapterAnnotation = annotationsAttribute.getAnnotation(XmlJavaTypeAdapter.class.getName());
    if (adapterAnnotation == null) {
      ClassPool classPool = this.getClassPool(XmlJavaTypeAdapter.class.getName());
      if (classPool == null) {
        classPool = ClassPool.getDefault();
      }
      assert classPool != null;
      final CtClass xmlJavaTypeAdapterCtClass = classPool.getOrNull(XmlJavaTypeAdapter.class.getName());
      assert xmlJavaTypeAdapterCtClass != null;
      adapterAnnotation = new Annotation(constantPool, xmlJavaTypeAdapterCtClass);
      modified = true;
    } else if (adapterAnnotation.getMemberValue("value") == null) {
      final ArrayMemberValue amv = new ArrayMemberValue(constantPool);
      amv.setValue(new AnnotationMemberValue[0]);
      adapterAnnotation.addMemberValue("value", amv);
      modified = true;
    }
    assert adapterAnnotation != null;
    assert adapterAnnotation.getMemberValue("value") != null;

    modified = this.installXmlJavaTypeAdapter(adapterAnnotation, adapterClassName, constantPool) || modified;

    /*
     * You would think this line would be required ONLY in the case
     * where the annotation itself was not found.  But you actually
     * have to add it to its containing AnnotationsAttribute in ALL
     * cases.  This doesn't make any sense.  See
     * http://stackoverflow.com/questions/8689156/why-does-javassist-insist-on-looking-for-a-default-annotation-value-when-one-is/8689214#8689214
     * for details.
     *
     * Additionally, you must re-add the annotation as the last
     * operation here in all cases.  Otherwise the changes made by the
     * installXmlJavaTypeAdapter() method above are not actually made
     * permanent.
     */
    if (modified) {
      annotationsAttribute.addAnnotation(adapterAnnotation);
    }

    return modified;
  }

  private final boolean installXmlJavaTypeAdapter(Annotation adapterAnnotation, final String adapterClassName, final ConstPool constantPool) throws NotFoundException {
    if (this.logger != null && this.logger.isLoggable(Level.FINER)) {
      this.logger.entering(this.getClass().getName(), "installXmlJavaTypeAdapter", new Object[] { adapterAnnotation, constantPool });
    }
    if (adapterClassName == null) {
      throw new IllegalArgumentException("adapterClassName", new NullPointerException("adapterClassName"));
    }
    if (!XmlJavaTypeAdapter.class.getName().equals(adapterAnnotation.getTypeName())) {
      throw new IllegalArgumentException("Wrong annotation: " + adapterAnnotation.getTypeName());
    }

    boolean modified = false;

    ClassPool classPool = this.getClassPool(XmlJavaTypeAdapter.class.getName());
    if (classPool == null) {
      classPool = ClassPool.getDefault();
    }
    assert classPool != null;
    
    final CtClass xmlJavaTypeAdapterCtClass = classPool.get(XmlJavaTypeAdapter.class.getName());
    assert xmlJavaTypeAdapterCtClass != null;

    if (adapterAnnotation != null) {
      // Preexisting
      final ClassMemberValue v = (ClassMemberValue)adapterAnnotation.getMemberValue("value");
      assert v != null;
      final String existingClassName = v.getValue();
      if (adapterClassName.equals(existingClassName)) {
        // The annotation is already correctly specified
        // TODO do something; return?
        return false;
      }
    } else {
      adapterAnnotation = this.newXmlJavaTypeAdapter(constantPool);
      modified = true;
    }

    assert adapterAnnotation != null;

    assert XmlJavaTypeAdapter.class.getName().equals(adapterAnnotation.getTypeName());

    modified = setXmlAdapter(adapterAnnotation, adapterClassName) || modified;
    assert adapterClassName.equals(((ClassMemberValue)adapterAnnotation.getMemberValue("value")).getValue());

    System.out.println("Modified: " + modified);
    return modified;
  }

  private final Annotation newXmlJavaTypeAdapter(final ConstPool constantPool) throws NotFoundException {

    ClassPool classPool = this.getClassPool(XmlJavaTypeAdapter.class.getName());
    if (classPool == null) {
      classPool = ClassPool.getDefault();
    }
    assert classPool != null;
    
    return new Annotation(constantPool, classPool.getOrNull(XmlJavaTypeAdapter.class.getName()));
  }

  private static final boolean setXmlAdapter(final Annotation adapterAnnotation, final String adapterClassName) {
    if (adapterClassName == null) {
      throw new IllegalArgumentException("adapterClassName", new NullPointerException("adapterClassName"));
    }
    if (adapterAnnotation == null) {
      throw new IllegalArgumentException("adapterAnnotation", new NullPointerException("adapterAnnotation"));
    }
    if (!XmlJavaTypeAdapter.class.getName().equals(adapterAnnotation.getTypeName())) {
      throw new IllegalArgumentException("adapterAnnotation does not represent " + XmlJavaTypeAdapter.class.getName());
    }

    // Retrieve the "holder" for the value() annotation
    // attribute ("FooToFooImplAdapter.class" in the
    // following sample:
    // 
    //   @XmlJavaTypeAdapter(type = Foo.class, value = FooToFooImplAdapter.class)
    //
    final ClassMemberValue adapterClassHolder = (ClassMemberValue)adapterAnnotation.getMemberValue("value");
    assert adapterClassHolder != null;

    final String old = adapterClassHolder.getValue();

    // Set the holder's value, thus installing the
    // annotation's value() value.
    adapterClassHolder.setValue(adapterClassName);

    if (old == null) {
      return adapterClassName != null;
    }
    return !old.equals(adapterClassName);
  }

  public static final class Modification implements Serializable {

    private static final long serialVersionUID = 1L;

    public static enum Kind {
      UNMODIFIED, MODIFIED;
    }

    private final CtClass interfaceCtClass;

    private final byte[] bytes;

    private final Kind kind;

    private Modification(final CtClass interfaceCtClass, final Kind kind, final byte[] bytes) {
      super();
      if (interfaceCtClass == null) {
        throw new IllegalArgumentException("interfaceCtClass", new NullPointerException("interfaceCtClass"));
      }
      this.interfaceCtClass = interfaceCtClass;
      assert interfaceCtClass.isFrozen();
      if (kind == null) {
        this.kind = Kind.MODIFIED;
      } else {
        this.kind = kind;
      }
      if (bytes == null) {
        this.bytes = new byte[0];
      } else {
        this.bytes = bytes;
      }
    }

    public CtClass getInterfaceCtClass() {
      return this.interfaceCtClass;
    }

    public boolean isModified() {
      return Kind.MODIFIED.equals(this.getKind());
    }

    public final Kind getKind() {
      return this.kind;
    }

    public final byte[] toByteArray() {
      return this.bytes;
    }

  }

}
