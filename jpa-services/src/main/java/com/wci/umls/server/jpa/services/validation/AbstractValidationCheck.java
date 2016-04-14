/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.services.validation;

import java.util.Properties;

import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.Code;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.Descriptor;
import com.wci.umls.server.services.handlers.ValidationCheck;

/**
 * Abstract validation check to make implementation easier.
 */
public abstract class AbstractValidationCheck implements ValidationCheck {

  /* see superclass */
  @Override
  public void setProperties(Properties p) {
    // n/a
  }

  /* see superclass */
  @Override
  public ValidationResult validate(Concept c) {
    ValidationResult result = new ValidationResultJpa();
    // no checks
    return result;
  }

  /* see superclass */
  @Override
  public ValidationResult validate(Descriptor descriptor) {
    ValidationResult result = new ValidationResultJpa();
    // no checks
    return result;
  }

  /* see superclass */
  @Override
  public ValidationResult validate(Code code) {
    ValidationResult result = new ValidationResultJpa();
    // no checks
    return result;
  }

  /* see superclass */
  @Override
  public ValidationResult validate(Atom atom) {
    ValidationResult result = new ValidationResultJpa();
    // no checks
    return result;
  }

  /* see superclass */
  @Override
  public ValidationResult validateMerge(Concept concept1, Concept concept2) {
    ValidationResult result = new ValidationResultJpa();
    // no checks
    return result;
  }

}
