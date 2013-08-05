package com.edugility.jaxb;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import java.net.JarURLConnection;
import java.net.UnknownServiceException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import java.util.jar.JarFile;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

public class ImplementationClassBinder implements ImplementationClassDiscoveryListener {

  private String adapterClassNameTemplate;

  private final InterfaceDecorator interfaceDecorator;

  private final Map<String, Collection<InterfaceDecorator.Modification>> modifications;

  public ImplementationClassBinder() {
    super();
    this.setAdapterClassNameTemplate("%s.%sTo%sAdapter");
    this.modifications = new HashMap<String, Collection<InterfaceDecorator.Modification>>();
    this.interfaceDecorator = new InterfaceDecorator();
  }

  @Override
  public void discoveryStarted(final ImplementationClassDiscoveryEvent event) {
    this.modifications.clear();
  }
  
  @Override
  public void discoveryEnded(final ImplementationClassDiscoveryEvent event) {
    try {
      this.processModifications();
    } catch (final CannotCompileException wrapMe) {
      throw new RuntimeException(wrapMe);
    } catch (final IOException wrapMe) {
      throw new RuntimeException(wrapMe);
    } catch (final NotFoundException wrapMe) {
      throw new RuntimeException(wrapMe);
    } finally {
      this.modifications.clear();
    }
  }

  @Override
  public void implementationClassDiscovered(final ImplementationClassDiscoveryEvent event) {
    if (event != null) {
      final String interfaceName = event.getInterfaceName();
      if (interfaceName != null) {
        final String implementationClassName = event.getImplementationClassName();
        if (implementationClassName != null) {
          final String adapterPackageName = this.getAdapterPackageName(interfaceName, implementationClassName);
          final String adapterClassName = this.getAdapterClassName(adapterPackageName, interfaceName, implementationClassName);
          if (adapterClassName != null) {
            try {
              final InterfaceDecorator.Modification decoration = this.interfaceDecorator.modify(interfaceName, adapterClassName);
              if (decoration != null && decoration.isModified()) {
                if (this.shouldModifyImmediately(decoration.getInterfaceCtClass())) {
                  this.recordModification(decoration);
                } else {
                  this.queueUpModification(decoration);
                }
              }
            } catch (final CannotCompileException kaboom) {
              throw new RuntimeException(kaboom);
            } catch (final IOException kaboom) {
              throw new RuntimeException(kaboom);
            } catch (final NotFoundException kaboom) {
              throw new RuntimeException(kaboom);
            }
          }
        }
      }
    }
  }

  public void processModifications() throws CannotCompileException, IOException, NotFoundException {
    final Iterable<Entry<String, Collection<InterfaceDecorator.Modification>>> modificationEntries = this.modifications.entrySet();
    if (modificationEntries != null) {
      for (final Entry<String, Collection<InterfaceDecorator.Modification>> entry : modificationEntries) {
        if (entry != null) {
          final String location = entry.getKey();
          if (location != null) {
            final Collection<InterfaceDecorator.Modification> mods = entry.getValue();
            System.out.println("*** working on " + location + " = " + mods);
            if (mods != null && !mods.isEmpty()) {
              final URL locationURL = new URL(location);
              final String scheme = locationURL.getProtocol();
              assert scheme != null;
              DataOutputStream outputStream = null;
              JarFile jarFile = null;
              try {
                if ("file".equals(scheme)) {
                  // Boy, there better only be one modification
                  if (mods.size() != 1) {
                    throw new IllegalStateException("file: scheme and more than one modification");
                  }
                  final InterfaceDecorator.Modification mod = mods.iterator().next();
                  if (mod != null) {
                    File f;
                    try {
                      f = new File(locationURL.toURI());
                    } catch (final URISyntaxException e) {
                      f = new File(locationURL.getPath());
                    }
                    outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
                    this.recordModification(mod, outputStream);
                  }
                } else if ("jar".equals(scheme)) {
                  final URLConnection urlConnection = locationURL.openConnection();
                  assert urlConnection instanceof JarURLConnection;
                  jarFile = ((JarURLConnection)urlConnection).getJarFile();
                  this.recordModifications(mods, jarFile);
                } else {
                  throw new UnknownServiceException(scheme);
                }
              } finally {
                if (outputStream != null) {
                  try {
                    outputStream.close();
                  } catch (final IOException ohWell) {
                    ohWell.printStackTrace();
                  }
                }
                if (jarFile != null) {
                  try {
                    jarFile.close();
                  } catch (final IOException ohWell) {
                    ohWell.printStackTrace();
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  public void queueUpModification(final InterfaceDecorator.Modification modification) throws NotFoundException {
    if (modification != null) {
      final CtClass c = modification.getInterfaceCtClass();
      if (c != null) {
        final URL location = c.getURL();
        if (location != null) {
          Collection<InterfaceDecorator.Modification> mods = this.modifications.get(location.toString());
          if (mods == null) {
            mods = new ArrayList<InterfaceDecorator.Modification>();
            this.modifications.put(location.toString(), mods);
          }
          mods.add(modification);
        }
      }
    }
  }

  public void recordModifications(final Iterable<InterfaceDecorator.Modification> mods, final JarFile jarFile) throws IOException {
    if (jarFile != null && mods != null) {
      final File tempFile = File.createTempFile("icb", ".targetjar");
      assert tempFile != null;
      tempFile.deleteOnExit();
      // TODO: implement
    }
  }

  public void recordModification(final InterfaceDecorator.Modification modification) throws CannotCompileException, IOException, NotFoundException {
    if (modification != null && modification.isModified()) {
      final CtClass c = modification.getInterfaceCtClass();
      if (c != null) {
        final URL location = c.getURL();
        if (location == null) {
          throw new IllegalArgumentException("c.getURL() == null");
        }
        final String scheme = location.getProtocol();
        assert scheme != null;
        JarFile jarFile = null;
        DataOutputStream outputStream = null;
        try {
          if ("file".equals(scheme)) {
            File f;
            try {
              f = new File(location.toURI());
            } catch (final URISyntaxException e) {
              f = new File(location.getPath());
            }
            outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            this.recordModification(modification, outputStream);
          } else {
            URLConnection urlConnection = null;
            urlConnection = location.openConnection();
            assert urlConnection != null;
            if (urlConnection instanceof JarURLConnection) {
              jarFile = ((JarURLConnection)urlConnection).getJarFile();
              this.recordModifications(Collections.singleton(modification), jarFile);
            } else {
              // Blindly try to open an output stream?
              outputStream = new DataOutputStream(new BufferedOutputStream(urlConnection.getOutputStream()));
              this.recordModification(modification, outputStream);
            }
          }
        } finally {
          if (outputStream != null) {
            try {
              outputStream.close();
            } catch (final IOException kaboom) {
              kaboom.printStackTrace();
            }
          }
          if (jarFile != null) {
            try {
              jarFile.close();
            } catch (final IOException boom) {
              boom.printStackTrace();
            }
          }
        }
      }
    }
  }

  public void recordModification(final InterfaceDecorator.Modification mod, final DataOutputStream outputStream) throws CannotCompileException, IOException, NotFoundException {
    if (mod != null) {
      final CtClass c = mod.getInterfaceCtClass();
      if (c != null) {
        c.toBytecode(outputStream);
      }
    }
  }

  public String getAdapterPackageName(final String interfaceName, final String implementationClassName) {
    return "foo"; // todo implement for realz
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

  public boolean shouldModifyImmediately(final CtClass cls) throws NotFoundException {
    return false;
    /*
    if (cls == null) {
      throw new IllegalArgumentException("cls", new NullPointerException("cls"));
    }
    boolean returnValue = false;
    final URL location = cls.getURL();
    if (location != null) {
      returnValue = "file".equals(location.getProtocol());
    }
    return returnValue;
    */
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
      final int length = name.length();
      if (lastDotIndex < 0) {
        returnValue = name;
      } else if (lastDotIndex + 1 >= length) {
        returnValue = "";
      } else {
        assert length > lastDotIndex + 1;
        returnValue = name.substring(lastDotIndex + 1);
      }
    }
    return returnValue;
  }

  public static final String getPackageName(final String name) {
    String returnValue = null;
    if (name != null) {
      final int lastDotIndex = name.lastIndexOf('.');
      final int length = name.length();
      if (lastDotIndex < 0) {
        returnValue = "";
      } else {
        returnValue = name.substring(0, lastDotIndex);
      }
    }
    return returnValue;
  }

}
