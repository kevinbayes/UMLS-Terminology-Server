/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import org.apache.log4j.Logger;

/**
 * Automates JUnit testing of equals and hashcode methods.
 */
public class CopyConstructorTester extends ProxyTester {

  /**
   * Constructs a new getter/setter tester to test objects of a particular
   * class.
   * 
   * @param obj Object to test.
   */
  public CopyConstructorTester(Object obj) {
    super(obj);
  }

  /**
   * Creates an object from the object.
   *
   * @param interfaceType the interface type
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean testCopyConstructor(Class<?> interfaceType) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Test copy constructor - " + clazz.getName());
    Object o1 = createObject(1);
    Object o2 = clazz.getConstructor(new Class<?>[] {
      interfaceType
    }).newInstance(new Object[] {
      o1
    });

    return o1.equals(o2) && o1.hashCode() == o2.hashCode();
  }

  /**
   * Creates an object from the object.
   * @param interfaceType the interface type
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean testCopyConstructorCascadeDeep(Class<?> interfaceType)
    throws Exception {
    Logger.getLogger(getClass()).debug(
        "Test copy constructor - " + clazz.getName());
    Object o1 = createObject(1);
    Object o2 = clazz.getConstructor(new Class<?>[] {
        interfaceType, boolean.class, boolean.class
    }).newInstance(new Object[] {
        o1, false, false
    });
    Logger.getLogger(getClass()).debug(
        "    " + o1.toString() + " = " + o2.toString());
    return o1.equals(o2) && o1.hashCode() == o2.hashCode();
  }

  /**
   * Test copy constructor deep.
   *
   * @param interfaceType the interface type
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean testCopyConstructorDeep(Class<?> interfaceType)
    throws Exception {
    Logger.getLogger(getClass()).debug(
        "Test copy constructor - " + clazz.getName());
    Object o1 = createObject(1);
    Object o2 = clazz.getConstructor(new Class<?>[] {
        interfaceType, boolean.class
    }).newInstance(new Object[] {
        o1, false
    });
    if (o1.equals(o2) && o1.hashCode() == o2.hashCode()) {
      return true;
    } else {
      Logger.getLogger(getClass()).info("o1 =" + o1.hashCode() + ", " + o1);
      Logger.getLogger(getClass()).info("o2 =" + o2.hashCode() + ", " + o2);
      return false;
    }
  }

}
