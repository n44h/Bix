package bix.utils;

import java.util.Arrays;

public class Utils {
    private Utils(){} // Enforce non-instantiability of this class.

    /**
     * Clears the terminal.
     */
    public static void clearScreen() {
        try {
            // For Windows systems.
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();

                // For Unix-based systems.
            else
                System.out.print("\033\143");
        }
        catch (Exception e) {
            System.out.println("\nError: Unable to clear terminal");
            e.printStackTrace();
        }
    }

    /**
     * Clear character arrays from memory by setting its elements to null characters
     */
    public static void clearCharArrayFromMemory(char[] char_array){
        // Setting every character in the array to null character('\0') using Arrays.fill().
        Arrays.fill(char_array,'\0');
    }

    /**
     * Function to pause code execution for set amount of time.
     *
     * @param duration the duration to sleep in seconds
     */
    public static void sleep(int duration) {
        try { Thread.sleep(duration * 1000L); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
