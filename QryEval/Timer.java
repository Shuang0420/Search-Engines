/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */
import java.math.BigDecimal;

/**
 *  A simple timer.
 */
public class Timer {

  //  Class variables

  private long timeStart=0;
  private long timeStop=0;

  private boolean isRunning=false;
  private boolean hasRun=false;

  private static final BigDecimal MILLION = new BigDecimal ("1000000");

  /**
   *  Start the timer.
   *  @throws IllegalStateException If the timer is started again while running.
   */
  public void start () {

    if (this.isRunning)
      throw new IllegalStateException (
                "The timer must be stopped before it can be started again.");

    this.timeStart = System.nanoTime();
    this.isRunning = true;
  }

  /**
   *  Stop the timer.
   *  @throws IllegalStateException If the timer is stopped before it is started.
   */
  public void stop () {

    if (! this.isRunning)
      throw new IllegalStateException (
                "The timer must be started before it can be stopped.");

    this.timeStop = System.nanoTime();
    this.isRunning = false;
    this.hasRun = true;
  }

  /**
   *  Converts a timing result to a string.
   *  @throws IllegalStateException The timer hasn't been run or is running now.
   */
  @Override public String toString() {

    if (! this.hasRun)
      throw new IllegalStateException(
                "The timer cannot be read because it has not been run.");
    else
      if (this.isRunning)
        throw new IllegalStateException(
                  "The timer cannot be read while it is running.");

    BigDecimal value = new BigDecimal(this.timeStop - this.timeStart);
    value = value.divide (MILLION, 3, BigDecimal.ROUND_HALF_EVEN);

    StringBuilder result = new StringBuilder();
    result.append(value);
    result.append(" ms");

    return result.toString();

  }
  
}
