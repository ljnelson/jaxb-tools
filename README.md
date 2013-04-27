<!-- -*- markdown -*- -->
# jaxb-tools

## Tools for [JAXB][1]

### April 27, 2013

### [Laird Nelson][2]

`jaxb-tools` is a toolkit that helps you work with the Java
Architecture for XML Binding APIs.

`jaxb-tools` offers the following classes and tools:

 * `PackageInfoModifier`

   This class helps install a `XmlJavaTypeAdapters` annotation on an
   existing `package-info.class` file.
 
 * `UniversalXmlAdapter`
 
   A JAXB `XmlAdapter` that helps JAXB substitute an implementation
   class for an interface.
 
 * `XmlAdapterBytecodeGenerator`
 
   A generator of `UniversalXmlAdapter` subclass bytecode.

[1]: http://about.me/lairdnelson
[2]: http://jaxb.java.net/
