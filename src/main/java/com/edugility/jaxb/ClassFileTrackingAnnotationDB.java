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

import java.net.URI;
import java.net.URL;

import java.util.Collection;
import java.util.Set;

import javassist.bytecode.annotation.Annotation;

import org.scannotation.AnnotationDB;

import javassist.bytecode.ClassFile;

public abstract class ClassFileTrackingAnnotationDB extends AnnotationDB {
  
  private ClassFile cf;

  private Set<URI> uris;
  
  protected ClassFileTrackingAnnotationDB() {
    this(null, null);
  }
  
  protected ClassFileTrackingAnnotationDB(final Collection<String> ignoredPackages) {
    this(null, ignoredPackages);
  }

  protected ClassFileTrackingAnnotationDB(final Set<URI> uris, final Collection<String> ignoredPackages) {
    super();
    this.setScanParameterAnnotations(false);
    this.setURIs(uris);
    if (ignoredPackages != null && !ignoredPackages.isEmpty()) {
      this.setIgnoredPackages(ignoredPackages.toArray(new String[ignoredPackages.size()]));
    }
  }
  
  public Set<URI> getURIs() {
    return this.uris;
  }

  public void setURIs(final Set<URI> uris) {
    this.uris = uris;
  }

  /**
   * Overrides the superclass' implementation to track the
   * {@link ClassFile} being scanned.
   *
   * @see #populate(Annotation[], ClassFile)
   */
  @Override
  protected final void scanClass(final ClassFile cf) {
    // Overrides this method to keep track of the ClassFile being scanned.
    if (cf == null || !cf.isInterface()) {
      this.cf = cf;
    } else {
      this.cf = null;
    }
    super.scanClass(cf);
    this.cf = null;
  }
  
  /**
   * Overrides the superclass' implementation to track the
   * {@link ClassFile} being scanned.
   *
   * @see #populate(Annotation[], ClassFile)
   */
  @Override
  protected final void scanMethods(final ClassFile cf) {
    // Overrides this method to keep track of the ClassFile being scanned.
    if (cf == null || !cf.isInterface()) {
      this.cf = cf;
    } else {
      this.cf = null;
    }
    super.scanMethods(cf);
    this.cf = null;
  }
  
  /**
   * Overrides the superclass' implementation to track the
   * {@link ClassFile} being scanned.
   *
   * @see #populate(Annotation[], ClassFile)
   */
  @Override
  protected final void scanFields(final ClassFile cf) {
    // Overrides this method to keep track of the ClassFile being scanned.
    if (cf == null || !cf.isInterface()) {
      this.cf = cf;
    } else {
      this.cf = null;
    }
    super.scanFields(cf);
    this.cf = null;
  }
  
  public final void scanArchives() throws IOException {
    this.scanArchives(this.getURIs());
  }

  public final void scanArchives(final Set<URI> uris) throws IOException {
    if (uris != null && !uris.isEmpty()) {
      final URL[] urls = new URL[uris.size()];
      int i = 0;
      for (final URI uri : uris) {
        urls[i++] = uri == null ? null : uri.toURL();
      }
      this.scanArchives(urls);
    }
  }
  
  /**
   * Overrides the superclass' implementation to track the
   * {@link ClassFile} being scanned.
   *
   * @see #populate(Annotation[], ClassFile)
   */
  @Override
  protected final void populate(final Annotation[] annotations, final String className) {
    // All scannotation activity passes through here.
    this.populate(annotations, this.cf);
  }
  
  protected abstract void populate(final Annotation[] annotations, final ClassFile cf);

}
