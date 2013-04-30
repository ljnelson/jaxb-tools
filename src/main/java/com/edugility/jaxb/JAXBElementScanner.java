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

import java.net.URI;
import java.net.URL;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.bytecode.annotation.Annotation;

import javassist.bytecode.ClassFile;

import org.scannotation.AnnotationDB;

/**
 * A class that efficiently scans class bytecode looking for classes
 * annotated with JAXB annotations that optionally implement a given
 * interface.
 *
 * @author <a href="http://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #scan()
 *
 * @see XmlAdapterBytecodeGenerator
 *
 * @see PackageInfoModifier
 */
public class JAXBElementScanner implements Serializable {

  private static final long serialVersionUID = 1L;

  private Set<URI> uris;

  private Set<String> ignoredPackages;

  private BindingFilter bindingFilter;

  public JAXBElementScanner() {
    super();
  }

  public Set<String> getIgnoredPackages() {
    return this.ignoredPackages;
  }

  public void setIgnoredPackages(final Set<String> ignoredPackages) {
    this.ignoredPackages = ignoredPackages;
  }

  /**
   * Returns a {@link Map} of efficiently discovered JAXB-annotated
   * implementation class names indexed by the interface names they
   * implement.
   *
   * <p>The default implementation of this method does no classloading
   * but scans the bytecode directly.</p>
   *
   * @return a non-{@code null} {@link Map} of names of discovered
   * JAXB-annotated classes indexed by the names of interfaces they
   * implement
   *
   * @exception IOException if an error occurs during the processing
   * of class files
   */
  public Map<String, String> scan() throws IOException {
    final SortedMap<String, String> bindings = new TreeMap<String, String>();
    final Set<URI> uris = this.getURIs();
    if (uris != null && !uris.isEmpty()) {

      final ClassFileTrackingAnnotationDB db = new ClassFileTrackingAnnotationDB(this.getIgnoredPackages()) {        
          private static final long serialVersionUID = 1L;
          @Override
          protected final void populate(final Annotation[] annotations, final ClassFile cf) {
            if (annotations != null && annotations.length > 0 && cf != null && !cf.isInterface()) {
              final BindingFilter bindingFilter = getBindingFilter();            
              for (final Annotation a : annotations) {
                if (a != null) {
                  final String typeName = a.getTypeName();
                  assert typeName != null;
                  if (typeName.startsWith("javax.xml.bind.annotation.")) {
                    // OK, we have a class with JAXB annotations on it.
                    // Get its interfaces efficiently.
                    boolean atLeastOneInterfaceProcessed = false;
                    final String[] interfaces = cf.getInterfaces();
                    if (interfaces != null && interfaces.length > 0) {
                      for (final String interfaceName : interfaces) {
                        assert interfaceName != null;
                        final String implementationClassName = cf.getName();
                        if (bindingFilter == null || bindingFilter.accept(interfaceName, implementationClassName)) {
                          atLeastOneInterfaceProcessed = true;
                          if (bindings.containsKey(interfaceName)) {
                            // TODO: warn
                          }
                          bindings.put(interfaceName, implementationClassName);
                        }
                      }
                    }
                    if (atLeastOneInterfaceProcessed) {
                      break; // out of the annotation processing loop
                    }
                  }
                }
              }
            }
          }
        };

      try {
        // Scans the URIs and places the results in the bindings map
        db.scanArchives(uris);
      } catch (final IllegalStateException unwrapMe) {
        final Throwable cause = unwrapMe.getCause();
        if (cause instanceof IOException) {
          throw (IOException)cause;
        } else {
          throw unwrapMe;
        }
      }
    }
    return bindings;
  }

  public BindingFilter getBindingFilter() {
    return this.bindingFilter;
  }

  public void setBindingFilter(final BindingFilter bindingFilter) {
    this.bindingFilter = bindingFilter;
  }

  public Set<URI> getURIs() {
    return this.uris;
  }

  public void setURIs(final Set<URI> uris) {
    this.uris = uris;
  }


  /*
   * Inner and nested classes.
   */


  public interface BindingFilter {
    
    public boolean accept(final String interfaceName, final String implementationClassName);

  }


  public static abstract class AbstractRegexBindingFilter implements BindingFilter, Serializable {

    private static final long serialVersionUID = 1L;

    protected final Pattern regex;

    protected AbstractRegexBindingFilter(final String regex) {
      this(Pattern.compile(regex));
    }

    protected AbstractRegexBindingFilter(final Pattern regex) {
      super();
      this.regex = regex;
    }

  }


  /**
   * A {@link BindingFilter} that <strong>does not accept</strong> interface names that match
   * a regular expression.
   */
  public static final class BlacklistRegexBindingFilter extends AbstractRegexBindingFilter {
    
    private static final long serialVersionUID = 1L;

    public BlacklistRegexBindingFilter(final String regex) {
      super(regex);
    }

    public BlacklistRegexBindingFilter(final Pattern regex) {
      super(regex);
    }

    /**
     * Returns {@code true} if the supplied {@code interfaceName} does
     * <em>not</em> match the regular expression supplied to this
     * {@link WhitelistRegexBindingFilter} at construction time;
     * {@code false} otherwise.
     */
    @Override
    public final boolean accept(final String interfaceName, final String implementationClassName) {
      boolean result = interfaceName != null && implementationClassName != null;
      if (result && regex != null) {
        final Matcher matcher = this.regex.matcher(interfaceName);
        result = matcher != null && !matcher.find();
      }
      return result;
    }

  }


  /**
   * A {@link BindingFilter} that <strong>accepts</strong> interface names that match
   * a regular expression.
   */
  public static final class WhitelistRegexBindingFilter extends AbstractRegexBindingFilter {
    
    private static final long serialVersionUID = 1L;

    public WhitelistRegexBindingFilter(final String regex) {
      super(regex);
    }

    public WhitelistRegexBindingFilter(final Pattern regex) {
      super(regex);
    }

    /**
     * Returns {@code true} if the supplied {@code interfaceName}
     * matches the regular expression supplied to this {@link
     * WhitelistRegexBindingFilter} at construction time; {@code
     * false} otherwise.
     */
    @Override
    public final boolean accept(final String interfaceName, final String implementationClassName) {
      boolean result = interfaceName != null && implementationClassName != null && this.regex != null;
      if (result) {
        final Matcher matcher = this.regex.matcher(interfaceName);
        result = matcher != null && matcher.find();
      }
      return result;
    }

  }

}
