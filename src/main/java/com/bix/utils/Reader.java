package com.bix.utils;

import com.bix.enums.ExitCode;

import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>
 * Class to monitor the Bix session for idle sessions (i.e. the user is inactive).
 * User activity is defined as the user providing input to Bix; and so, all user inputs
 * for Bix are controlled through this class.
 *
 * </p>
 * <p>
 * Since we cannot listen for keyboard presses and keystrokes on the console with Java,
 * we instead monitor user inputs as a means to record user activity.
 * If the user is inactive for a period of time, then the Bix session is terminated.
 * </p>
 */

public class Reader {
    private static final Scanner SCANNER = new Scanner(System.in);

    /* idle_timeout stores how long a session can be idle in milliseconds.
     * Default is 10 minutes.
     * Lower limit is 1 minutes. Upper limit is 20 minutes.
     *
     * Multiply by 1000 because the Timer.schedule() method
     * takes milliseconds as the parameter.
     */
    private static long idle_timeout = 600 * 1000;

    /* Timer object starts a background thread.
     * TimerTask is a task that can be scheduled and linked to the Timer object.
     * Our goal for this class is to terminate the current Bix session if the
     * session has been idle for a certain period of time (determined by idle_timeout).
     *
     * The task in our case is terminating the current Bix session.
     * And so, the TimerTask object calls the Handler.terminateSession().
     */
    private static final Timer idle_session_timer = new Timer();
    private static final TimerTask terminate_idle_session_task = new TimerTask() {
        @Override
        public void run() {
            SCANNER.close();
            Handler.terminateSession(ExitCode.TERMINATE_IDLE_SESSION);
        }
    };

    /**
     * Method to read a String input from the user.
     * @param prompt The prompt to be printed to the user to get the input.
     * @return {@code String} input from the user.
     */
    public static String readString(String prompt){
        System.out.print(prompt);
        startIdleSessionMonitor();
        return SCANNER.nextLine().trim();
    }

    /**
     * Method to read a char input from the user.
     * @param prompt The prompt to be printed to the user to get the input.
     * @return {@code char} input from the user.
     */
    public static char readChar(String prompt){
        System.out.print(prompt);
        startIdleSessionMonitor();
        return SCANNER.nextLine().trim().charAt(0);
    }

    /**
     * Method to read an integer input from the user.
     * @param prompt The prompt to be printed to the user to get the input.
     * @return {@code int} input from the user.
     */
    public static int readInt(String prompt){
        System.out.print(prompt);
        return SCANNER.nextInt();
    }

    /**
     * Sets a new idle session timeout. Default timeout is 420 seconds (7 minutes).
     * As a reasonable security measure, the new timeout cannot exceed 1200 seconds (20 minutes).
     * New timeout can also not be less than 60 seconds (1 minute).
     *
     * @param new_timeout The new idle session timeout in seconds.
     */
    public static void setIdleTimeout(int new_timeout){
        if(new_timeout < 60)
            idle_timeout = 60 * 1000;
        else
            idle_timeout = Math.min(new_timeout, 1200) * 1000;
    } // setIdleTimeout()

    /**
     * <p>
     * This method schedules the Bix session termination task to be executed
     * after {@code idle_timeout} amount of time has passed.
     * </p>
     * <p>
     * Every time the user provides an input to Bix, any previously scheduled session
     * termination task is cancelled by calling {@code idle_session_timer.cancel()}.
     * The task is then scheduled (or rescheduled) by calling
     * {@code idle_session_timer.schedule(terminate_idle_session_task, idle_timeout)}.
     * </p>
     */
    public static void startIdleSessionMonitor(){
        // Cancel any existing scheduled tasks in the thread.
        // This is useful when restarting the timer after user activity is detected.
        idle_session_timer.cancel();

        // Scheduling the idle session termination task again.
        idle_session_timer.schedule(terminate_idle_session_task, idle_timeout);
    } // startIdleSessionTimer()
} // class
