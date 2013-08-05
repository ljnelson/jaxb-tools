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

import java.text.DateFormat;

import java.util.Date;
import java.util.Locale;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * An {@link XmlAdapter} that can {@linkplain #marshal(Date) marshal}
 * a {@link Date} into a {@link Locale}-sensitive {@link String}.
 *
 * @author <a href="http://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
public class DateAdapter extends XmlAdapter<String, Date> {


  /*
   * Instance fields.
   */


  /**
   * The {@link DateFormat} to use to format an arbitrary {@link Date}
   * into a {@link Date} suitable for consumption by a client.
   *
   * <p>This field may be {@code null}.</p>
   */
  private DateFormat marshalingDateFormat;

  /**
   * The {@link DateFormat} to use to format an arbitrary {@link
   * String} coming from a client into a {@link Date} suitable for
   * consumption by the {@link Locale}-independent server.
   *
   * <p>This field may be {@code null}, in which case the return value
   * from {@link DateFormat#getInstance(Locale)
   * DateFormat#getInstance(Locale.getDefault())} will be used
   * instead.</p>
   */
  private DateFormat unmarshalingDateFormat;

  private boolean synchronize;

  /*
   * Constructors.
   */


  public DateAdapter() {
    this(null, false);
  }

  public DateAdapter(final DateFormat marshalingDateFormat) {
    this(marshalingDateFormat, false);
  }

  public DateAdapter(final DateFormat marshalingDateFormat, final boolean synchronize) {
    super();
    this.setSynchronize(synchronize);
    this.setMarshalingDateFormat(marshalingDateFormat);
  }


  /*
   * Instance methods.
   */


  public boolean getSynchronize() {
    return this.synchronize;
  }

  public void setSynchronize(final boolean synchronize) {
    this.synchronize = synchronize;
  }

  public DateFormat getMarshalingDateFormat() {
    return this.marshalingDateFormat;
  }

  public void setMarshalingDateFormat(final DateFormat marshalingDateFormat) {
    this.marshalingDateFormat = marshalingDateFormat;
  }

  public DateFormat getUnmarshalingDateFormat() {
    return this.unmarshalingDateFormat;
  }

  public void setUnmarshalingDateFormat(final DateFormat unmarshalingDateFormat) {
    this.unmarshalingDateFormat = unmarshalingDateFormat;
  }

  @Override
  public String marshal(Date date) throws Exception {
    final String localizedDate;
    if (date == null) {
      date = new Date(0L);
    }
    assert date != null;
    DateFormat marshalingDateFormat = this.getMarshalingDateFormat();
    if (marshalingDateFormat == null) {
      marshalingDateFormat = DateFormat.getInstance();
    }
    if (marshalingDateFormat == null) {
      localizedDate = date.toString();
    } else if (this.synchronize) {
      synchronized (marshalingDateFormat) {
        localizedDate = marshalingDateFormat.format(date);
      }
    } else {
      localizedDate = marshalingDateFormat.format(date);
    }
    return localizedDate;
  }

  @Override
  public Date unmarshal(String localizedDate) throws Exception {
    final Date date;
    if (localizedDate == null) {
      date = new Date(0L);
    } else {
      localizedDate = localizedDate.trim();
      DateFormat unmarshalingDateFormat = this.getUnmarshalingDateFormat();
      if (unmarshalingDateFormat == null) {
        unmarshalingDateFormat = DateFormat.getInstance();
      }
      if (unmarshalingDateFormat == null) {
        final DateFormat dateFormat = DateFormat.getInstance();
        if (dateFormat == null) {
          @SuppressWarnings("deprecation")
          final Date d = new Date(Date.parse(localizedDate));
          date = d;
        } else {
          date = dateFormat.parse(localizedDate);
        }
      } else if (this.synchronize) {
        synchronized (unmarshalingDateFormat) {
          date = unmarshalingDateFormat.parse(localizedDate);
        }
      } else {
        date = unmarshalingDateFormat.parse(localizedDate);
      }
    }
    return date;
  }

}
