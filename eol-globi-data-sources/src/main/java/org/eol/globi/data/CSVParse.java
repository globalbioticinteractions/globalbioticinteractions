package org.eol.globi.data;

import com.Ostermiller.util.BadDelimiterException;
import com.Ostermiller.util.BadQuoteException;

import java.io.Closeable;
import java.io.IOException;

public interface CSVParse extends Closeable {

    /**
     * Get the line number that the last token came from.
     *
     * @return line number or -1 if no tokens have been returned yet.
     *
     * @since ostermillerutils 1.00.00
     */
    int lastLineNumber();

    /**
     * Get all the values from a line.
     * <p>
     * If the line has already been partially read, only the
     * values that have not already been read will be included.
     *
     * @return all the values from the line or null if there are no more values.
     * @throws IOException if an error occurs while reading.
     *
     * @since ostermillerutils 1.00.00
     */
    String[] getLine() throws IOException;

    /**
     * Get the line number that the last token came from.
     * <p>
     * New line breaks that occur in the middle of a token are not
     * counted in the line number count.
     *
     * @return line number or -1 if no tokens have been returned yet.
     *
     * @since ostermillerutils 1.00.00
     */
    int getLastLineNumber();


    /**
     * Change this parser so that it uses a new delimiter.
     * <p>
     * The initial character is a comma, the delimiter cannot be changed
     * to a quote or other character that has special meaning in CSV.
     *
     * @param newDelim delimiter to which to switch.
     * @throws BadDelimiterException if the character cannot be used as a delimiter.
     *
     * @since ostermillerutils 1.02.08
     */
    void changeDelimiter(char newDelim) throws BadDelimiterException;


}
