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

public class DateAdapter extends XmlAdapter<String, Date> {

  /**
   * The {@link DateFormat} to use to format an arbitrary {@link Date}
   * into a {@link Date} suitable for consumption by a client.
   *
   * <p>This field may be {@code null}.</p>
   */
  private DateFormat dateFormat;

  private boolean synchronize;

  public DateAdapter() {
    this(null, false);
  }

  public DateAdapter(final DateFormat dateFormat) {
    this(dateFormat, false);
  }

  public DateAdapter(final DateFormat dateFormat, final boolean synchronize) {
    super();
    this.setSynchronize(synchronize);
    this.setDateFormat(dateFormat);
  }

  public boolean getSynchronize() {
    return this.synchronize;
  }

  public void setSynchronize(final boolean synchronize) {
    this.synchronize = synchronize;
  }

  public DateFormat getDateFormat() {
    return this.dateFormat;
  }

  public void setDateFormat(final DateFormat dateFormat) {
    this.dateFormat = dateFormat;
  }

  @Override
  public String marshal(final Date date) throws Exception {
    String localizedDate = null;
    if (date != null) {
      final DateFormat dateFormat = this.getDateFormat();
      if (dateFormat != null) {
        if (this.synchronize) {
          synchronized (dateFormat) {
            localizedDate = dateFormat.format(date);
          }
        } else {
          localizedDate = dateFormat.format(date);
        }
      }
    }
    return localizedDate;
  }

  @Override
  public Date unmarshal(final String localizedDate) throws Exception {
    Date date = null;
    if (localizedDate != null) {
      final DateFormat dateFormat = this.getDateFormat();
      if (dateFormat != null) {
        if (this.synchronize) {
          synchronized (dateFormat) {
            date = dateFormat.parse(localizedDate);
          }
        } else {
          date = dateFormat.parse(localizedDate);
        }
      }
    }
    return date;
  }

}
