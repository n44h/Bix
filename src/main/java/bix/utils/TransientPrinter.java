package bix.utils;

import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;

import static bix.utils.Utils.clearScreen;

import static bix.utils.Constants.TIMED_DISPLAY_DURATION_LOWER_LIMIT;
import static bix.utils.Constants.TIMED_DISPLAY_DURATION_UPPER_LIMIT;

public final class TransientPrinter {
    private static final Scanner SCANNER = new Scanner(System.in);

    // DISPLAY_DURATION dictates how long to display the values
    private static int DISPLAY_DURATION;

    private static boolean CLEAR_SCREEN_TASK_COMPLETED;

    /* Timer object starts a background thread.
     * TimerTask is a task that can be scheduled and linked to the Timer object.
     *
     * The purpose of this class is print an output to the terminal and, after a
     * pre-determined amount of time, clear the terminal (determined by DISPLAY_DURATION).
     *
     * The terminal can also be cleared earlier if the user hits the enter key.
     *
     * The "timer task" in this case is clearing the terminal.
     */
    private final Timer DISPLAY_TIMER = new Timer();

    // Creating Anonymous Inner class based on TimeTask class.
    private final TimerTask CLEAR_SCREEN_TASK = new TimerTask() {
        @Override
        public void run() {
            // Perform the clear screen task if it has not been completed yet.
            if (!CLEAR_SCREEN_TASK_COMPLETED) {
                // Clear the credentials form the screen.
                clearScreen();

                // Indicate task completion.
                CLEAR_SCREEN_TASK_COMPLETED = true;
            }

            // Kill the thread.
            DISPLAY_TIMER.cancel();
        }
    };


    /**
     * TransientPrinter constructor.
     *
     * @param displayDuration duration the output should be displayed
     */
    public TransientPrinter(int displayDuration) {
        // Ensure that the new Display Duration is within the upper and lower limits.
        DISPLAY_DURATION = Math.min(
                TIMED_DISPLAY_DURATION_UPPER_LIMIT,
                Math.max(TIMED_DISPLAY_DURATION_LOWER_LIMIT, displayDuration));

        // Initialize to false, set to true when either of the task execution conditions are triggered.
        CLEAR_SCREEN_TASK_COMPLETED = false;
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
    private void startDisplayTimer() {
        // Cancel any existing display timers.
        try { DISPLAY_TIMER.cancel(); }
        catch (IllegalStateException ignored) {}

        // Scheduling the clear screen task with the display timer.
        // Multiply DISPLAY_DURATION by 1000 because Timer.schedule() takes arguments in milliseconds.
        DISPLAY_TIMER.schedule(CLEAR_SCREEN_TASK, DISPLAY_DURATION * 1000L);
    }

    public void display(String arg) {
        // Display the argument.
        System.out.println(arg);

        // Start the timer for the clear screen task.
        startDisplayTimer();

        // If an enter key (a.k.a. return key) press is registered, clear the screen.
        System.out.println("\nPress the Enter key to clear the screen");
        SCANNER.nextLine(); // Reads for an Enter key press.

        // Perform clear screen task if it has not been completed already.
        if(!CLEAR_SCREEN_TASK_COMPLETED) {
            // Clear credentials from the screen.
            clearScreen();

            // Indicate task completion.
            CLEAR_SCREEN_TASK_COMPLETED = true;
        }

        // Cancel the Display Timer.
        DISPLAY_TIMER.cancel();

    }

    public void displayCredentials(char[] username, char[] password, String associatedEmail) {
        // Print credentials to Terminal.
        System.out.printf("\nUsername         : %s", Arrays.toString(username));
        System.out.printf("\nPassword         : %s", Arrays.toString(password));
        System.out.printf("\nAssociated email : %s", associatedEmail == null ? "nil" : associatedEmail);

        // Start the display timer.
        startDisplayTimer();

        // If an enter key (a.k.a. return key) press is registered, clear the screen.
        System.out.println("\nPress the Enter key to clear the screen");
        SCANNER.nextLine(); // Reads for an Enter key press.

        // Perform clear screen task if it has not been completed already.
        if(!CLEAR_SCREEN_TASK_COMPLETED) {
            // Clear credentials from the screen.
            clearScreen();

            // Indicate task completion.
            CLEAR_SCREEN_TASK_COMPLETED = true;
        }

        // Cancel the Display Timer.
        DISPLAY_TIMER.cancel();
    }

} // class TransientPrinter
