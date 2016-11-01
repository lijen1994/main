package tars.commons.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import tars.commons.exceptions.IllegalValueException;
import tars.commons.exceptions.InvalidRangeException;

/**
 * Helper functions for handling strings.
 */
public class StringUtil {
    private static final String RANGE_SEPARATOR = "..";
    public static final String EMPTY_STRING = "";
    public static final int EMPTY_STRING_LENGTH = 0;
    
    public static boolean containsIgnoreCase(String source, String query) {
        String[] split = source.toLowerCase().split("\\s+");
        List<String> strings = Arrays.asList(split);
        return strings.stream().filter(s -> s.equals(query.toLowerCase()))
                .count() > 0;
    }

    /**
     * Returns a detailed message of the t, including the stack trace.
     */
    public static String getDetails(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return t.getMessage() + "\n" + sw.toString();
    }

    /**
     * Returns true if s represents an unsigned integer e.g. 1, 2, 3, ... <br>
     * Will return false for null, empty string, "-1", "0", "+1", and " 2 "
     * (untrimmed) "3 0" (contains whitespace).
     * 
     * @param s should be trimmed.
     */
    public static boolean isUnsignedInteger(String s) {
        return s != null && s.matches("^0*[1-9]\\d*$");
    }

    /**
     * Handles three different cases of strings and return them in the
     * appropriate format
     * 
     * @@author A0121533W
     */
    public static String indexString(String s)
            throws InvalidRangeException, IllegalValueException {
        if (s.isEmpty()) {
            return s;
        }
        if (isSingleNumber(s)) {
            return formatSingleNumber(s);
        } else if (isListOfIndexes(s)) {
            return formatListOfIndexes(s);
        } else if (isRangeOfIndexes(s)) {
            return formateRangeOfIndexes(s);
        } else {
            throw new IllegalValueException(
                    "Unexpected error in geting index from String.");
        }
    }

    private static boolean isSingleNumber(String s) {
        return (s.indexOf(" ") == -1 && !s.contains(RANGE_SEPARATOR));
    }

    private static String formatSingleNumber(String s)
            throws IllegalValueException {
        if (!isUnsignedInteger(s)) {
            throw new IllegalValueException("Invalid index entered");
        }
        return s;
    }

    /**
     * Returns true if s is a list of indexes separated by white
     * space
     * 
     * @@author A0121533W
     */
    private static boolean isListOfIndexes(String s) {
        return s.indexOf(" ") != -1 && !s.contains(RANGE_SEPARATOR);
    }

    private static String formatListOfIndexes(String s)
            throws IllegalValueException {
        String indexString = "";
        String[] indexArray = s.split(" ");
        for (int i = 0; i < indexArray.length; i++) {
            if (!isUnsignedInteger(indexArray[i])) {
                throw new IllegalValueException("Invalid index entered");
            }
            indexString += indexArray[i] + " ";
        }
        return indexString.trim();
    }

    /**
     * Returns true if s is a range of indexes e.g. 1..3
     * 
     * @@author A0121533W
     */
    private static boolean isRangeOfIndexes(String s) {
        return s.contains(RANGE_SEPARATOR);
    }

    /**
     * Formats a range of indexes to a list of indexes separated by
     * white space
     * 
     * @@author A0121533W
     */
    private static String formateRangeOfIndexes(String s)
            throws IllegalValueException, InvalidRangeException {
        String rangeToReturn = "";
        
        int toIndex = s.indexOf(RANGE_SEPARATOR);
        String start = s.substring(0, toIndex);
        String end = s.substring(toIndex + RANGE_SEPARATOR.length());

        if (!isUnsignedInteger(start) || !isUnsignedInteger(end)) {
            throw new IllegalValueException("Invalid index entered");
        }

        int startInt = Integer.parseInt(start);
        int endInt = Integer.parseInt(end);

        if (startInt > endInt) {
            throw new InvalidRangeException();
        }

        for (int i = startInt; i <= endInt; i++) {
            rangeToReturn += String.valueOf(i) + " ";
        }

        return rangeToReturn.trim();
    }

}