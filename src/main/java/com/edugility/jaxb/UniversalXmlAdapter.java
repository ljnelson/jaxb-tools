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

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter; // for javadoc only
import javax.xml.bind.annotation.adapters.XmlRootElement; // for javadoc only

/**
 * An {@link XmlAdapter} that helps JAXB handle an otherwise
 * unbindable type when it is known that that unbindable type is
 * implemented in a given application by one and only one bindable
 * type.
 *
 * <p>For example, perhaps you know that in a given application all
 * {@code Foobar} instances will in fact be {@code Whizbang}
 * instances&mdash;a class, let's say, that is annotated with {@code
 * @}{@link XmlRootElement} (and other JAXB annotations) and
 * implements {@code Foobar}.  But you do not want to write your API
 * in terms of {@code Whizbang}s&mdash;you want to write it in terms
 * of {@code Foobar}s.  That is, you want JAXB to behave as though
 * somehow suddenly it could handle {@code Foobar} as a type that it
 * can express as an XML construct.  (Because {@code Foobar} is an
 * interface, JAXB cannot natively handle it.)</p>
 *
 * <p>If though, it is truly known that in your application all
 * instances of {@code Foobar} that JAXB will be asked to bind are
 * actually {@code Whizbang}s&mdash;a concrete, JAXB-bindable type,
 * then you could annotate your {@code Foobar} class with {@code
 * @}{@link XmlJavaTypeAdapter} and specify this class as the {@link
 * XmlAdapter} implementation that would ensure that all {@code
 * Foobar}s would be treated as {@code Whizbang}s internally by
 * JAXB.</p>
 *
 * <p>This class is "universal" only in that it can adapt any two
 * classes provided that one is a subtype of the other.</p>
 *
 * @param <B> the interface type (or other type) that JAXB needs to be
 * able to handle but can't yet
 *
 * @param <V> the implementation type that JAXB can handle that
 * implements the {@code <B>} type
 *
 * @author <a href="http://about.me/lairdnelson">Laird Nelson</a>
 *
 * @see XmlAdapter
 *
 * @see XmlJavaTypeAdapter
 *
 * @see <a
 * href="https://jaxb.java.net/guide/Mapping_interfaces.html">The
 * Unofficial JAXB Guide's section on mapping interfaces</a>
 *
 * @see <a
 * href="http://blog.bdoughan.com/2010/07/xmladapter-jaxbs-secret-weapon.html">Blaise
 * Doughan's article on <code>XmlAdapter</code> usage</a>
 */
public abstract class UniversalXmlAdapter<B, V extends B> extends XmlAdapter<V, B> {
  
  /**
   * Creates a new {@link UniversalXmlAdapter}.
   */
  public UniversalXmlAdapter() {
    super();
  }

  /**
   * Converts the supplied {@code objectThatJaxbCanHandle}&mdash;a
   * type that JAXB knows how to handle natively&mdash;into an
   * instance of its supertype&mdash;which JAXB may not known how to
   * handle natively.
   *
   * <p>This method is called when transforming an XML document into
   * Java objects.</p>
   *
   * <p>This implementation simply returns the supplied {@code
   * objectThatJaxbCanHandle}.  The type parameters supplied to this
   * class at construction time ensure that this is always a valid
   * thing to do.</p>
   *
   * @param objectThatJaxbCanHandle the object to unmarshal or
   * deserialize; may be {@code null}
   *
   * @return the supplied {@code objectThatJaxbCanHandle}
   */
  @Override
  public final B unmarshal(final V objectThatJaxbCanHandle) {
    return objectThatJaxbCanHandle;
  }

  /**
   * Converts the supplied {@code objectThatJaxbCannotHandleNatively}
   * into an object that JAXB can handle natively by attempting to
   * simply cast the supplied {@code
   * objectThatJaxbCannotHandleNatively} to the return type of this
   * method.
   *
   * <p>This conversion should succeed provided that the type
   * constraints of this class are respected.</p>
   *
   * @param objectThatJaxbCannotHandleNatively an object that for some
   * reason or another JAXB is incapable of handling natively; may be
   * {@code null}
   *
   * @return the result of casting the supplied {@code
   * objectThatJaxbCannotHandleNatively} to the return type of this
   * method
   *
   * @exception ClassCastException if for some reason the cast could
   * not take place
   */
  @Override
  @SuppressWarnings("unchecked")
  public final V marshal(final B objectThatJaxbCannotHandleNatively) {
    return (V)objectThatJaxbCannotHandleNatively;
  }
}
