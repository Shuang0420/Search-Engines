/*
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.1.2.
 */

import java.io.*;
import java.util.*;

/**
 *  PopData is a simple utility class for returning two items (a tuple
 *  of length two) from a method.  The tuple represents data that was
 *  popped from a string, and the remaining part of the string.  This
 *  class would not be necessary if Java strings were not immutable or
 *  if Java methods could return more than a single object.
 */

public class PopData<PopType,RemainingType> {

  private final PopType popped;
  private final RemainingType remaining;

  /**
   *  Constructor.
   *  @param popped The data that was popped from the string.
   *  @param remaining The data that remains in the string after the pop.
   */
  public PopData(PopType popped, RemainingType remaining) {
    this.popped = popped;
    this.remaining = remaining;
  }

  /**
   *  Get the data that was popped from the string.
   *  @return PopType The data that was popped.
   */
  public PopType getPopped() {
    return this.popped;
  }

  /**
   *  Get the data that remains in the string after the pop.
   *  @return RemainingType The data that remains.
   */
  public RemainingType getRemaining() {
    return this.remaining;
  }
}
