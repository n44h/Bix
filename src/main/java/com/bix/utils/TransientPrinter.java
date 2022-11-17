package com.bix.utils;

import java.util.*;

import static com.bix.utils.Controller.clearScreen;
import static com.bix.utils.Controller.clearCharArrayFromMemory;

public class TransientPrinter {
    /**
     * keyPressListener class that is listens for the Enter key press and clears the screen.
     */
    private static class KeyPressListener extends Thread {
        @Override
        public void run() {
            final Scanner scanner = new Scanner(System.in);

            // If a return key (enter key) press is registered, clear the screen.
            System.out.println("\nPress the Enter key to clear the screen");
            // Reads for an Enter key press.
            scanner.nextLine();

            // Cancel the Display Timer.
            DISPLAY_TIMER.cancel();

            // Clear credentials from the screen.
            clearScreen();
        }
    }

    private static final KeyPressListener LISTENER = new KeyPressListener();

    // DISPLAY_DURATION dictates how long to display the values
    private static int DISPLAY_DURATION;

    /* Timer object starts a background thread.
     * TimerTask is a task that can be scheduled and linked to the Timer object.
     * Our goal for this class is to terminate the current Bix session if the
     * session has been idle for a certain period of time (determined by idle_timeout).
     *
     * The task in our case is terminating the current Bix session.
     * And so, the TimerTask object calls the Controller.terminateSession().
     */
    private static final Timer DISPLAY_TIMER = new Timer();
    private static final TimerTask CLEAR_SCREEN_TASK = new TimerTask() {
        @Override
        public void run() {
            // Clear the credentials form the screen.
            clearScreen();

            // Kill the keyPressListener thread.
            LISTENER.interrupt();
        }
    };

    /**
     * Sets the Credential Display Duration. Default duration is 120 seconds (2 minutes).
     * As a reasonable security measure, the new timeout cannot exceed 600 seconds (10 minutes).
     * New timeout can also not be less than 1 second.
     *
     * @param duration credential display duration in seconds
     */
    public static void setDisplayDuration(int duration) {
        if(duration < 1)
            DISPLAY_DURATION = 1;
        else
            DISPLAY_DURATION = Math.min(duration, 10 * 60);
    }

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
    private static void startSessionMonitor() {
        // Cancel any existing display timers.
        DISPLAY_TIMER.cancel();

        // Scheduling the clear screen task with the display timer.
        // Multiply DISPLAY_DURATION by 1000 because Timer.schedule() takes arguments in milliseconds.
        DISPLAY_TIMER.schedule(CLEAR_SCREEN_TASK, DISPLAY_DURATION * 1000L);

        // Start the key press listener thread to read Enter key presses, if an enter key is pressed, the
        // display timer is cancelled and the screen is cleared immediately.
        LISTENER.start();
    }

    static void display(String arg) {
        // Display the argument.
        System.out.println(arg);

        // Set arg to null.
        arg = null;

        // Start the display timer.
        startSessionMonitor();
    }

    static void displayCredentials(char[] username, char[] password, String associatedEmail) {
        // Print credentials to Terminal.
        System.out.printf("\nUsername         : %s", Arrays.toString(username));
        System.out.printf("\nPassword         : %s", Arrays.toString(password));
        System.out.printf("\nAssociated email : %s", associatedEmail == null ? "nil" : associatedEmail);

        // Clear credentials from memory.
        clearCharArrayFromMemory(username);
        clearCharArrayFromMemory(password);
        associatedEmail = null;

        // Start the display timer.
        startSessionMonitor();
    }
}
