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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javassist.bytecode.annotation.Annotation;

import javassist.bytecode.ClassFile;

public class ImplementationClassFinder extends ClassFileTrackingAnnotationDB {

  private static final long serialVersionUID = 1L;
  
  private Collection<ImplementationClassDiscoveryListener> listeners;
  
  public ImplementationClassFinder() {
    super();
  }

  public ImplementationClassFinder(final Set<URI> uris, final Collection<String> ignoredPackages) {
    super(uris, ignoredPackages);
  }
  
  public void addImplementationClassDiscoveryListener(final ImplementationClassDiscoveryListener l) {
    if (l != null) {
      if (this.listeners == null) {
        this.listeners = new ArrayList<ImplementationClassDiscoveryListener>();
      }
      this.listeners.add(l);
    }
  }
  
  public void removeImplementationClassDiscoveryListener(final ImplementationClassDiscoveryListener l) {
    if (l != null && this.listeners != null) {
      this.listeners.remove(l);
    }
  }
  
  public ImplementationClassDiscoveryListener[] getImplementationClassDiscoveryListeners() {
    return this.listeners.toArray(new ImplementationClassDiscoveryListener[this.listeners.size()]);
  }
  
  @Override
  public void scanArchives(final URL... urls) throws IOException {
    final ImplementationClassDiscoveryEvent event;
    ImplementationClassDiscoveryListener[] listeners = this.getImplementationClassDiscoveryListeners();
    if (listeners != null && listeners.length > 0) {
      event = new ImplementationClassDiscoveryEvent(this);
      for (final ImplementationClassDiscoveryListener l : listeners) {
        if (l != null) {
          l.discoveryStarted(event);
        }
      }
    } else {
      event = null;
    }
    super.scanArchives(urls);
    listeners = this.getImplementationClassDiscoveryListeners();
    if (listeners != null && listeners.length > 0) {
      for (final ImplementationClassDiscoveryListener l : listeners) {
        if (l != null) {
          l.discoveryEnded(event);
        }
      }
    }
  }

  @Override
  protected void populate(final Annotation[] annotations, final ClassFile cf) {
    if (annotations != null && annotations.length > 0 && cf != null && !cf.isInterface()) {
      final ImplementationClassDiscoveryListener[] listeners = this.getImplementationClassDiscoveryListeners();
      if (listeners != null && listeners.length > 0) {
        final String implementationClassName = cf.getName();
        if (!this.shouldIgnore(implementationClassName)) {
          assert implementationClassName != null;
          for (final Annotation a : annotations) {
            if (a != null) {
              final String typeName = a.getTypeName();
              if (typeName != null && typeName.startsWith("javax.xml.bind.annotation.")) {
                // OK, we have a class with JAXB annotations on it.
                // Get its interfaces efficiently.
                boolean atLeastOneInterfaceProcessed = false;
                final String[] interfaces = cf.getInterfaces();
                if (interfaces != null && interfaces.length > 0) {
                  for (final String interfaceName : interfaces) {
                    if (!this.shouldIgnore(interfaceName)) {
                      assert interfaceName != null;
                      final ImplementationClassDiscoveryEvent event = new ImplementationClassDiscoveryEvent(this, interfaceName, implementationClassName);
                      for (final ImplementationClassDiscoveryListener l : listeners) {
                        if (l != null) {
                          atLeastOneInterfaceProcessed = true;
                          l.implementationClassDiscovered(event);
                        }
                      }
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
    }
  }

  public boolean shouldIgnore(final String className) {
    boolean skip = className == null;
    if (!skip) {
      final String[] ignoredPackages = this.getIgnoredPackages();
      if (ignoredPackages != null && ignoredPackages.length > 0) {
        for (final String pkg : ignoredPackages) {
          if (pkg != null && className.startsWith(pkg)) {
            skip = true;
            break;
          }
        }
      }
    }
    return skip;
  }
  
}
