/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.IOException;
import java.io.Reader;

public interface IFileSystem
{
    Iterable<String> getLines(IFileDocument fileDocument, int firstLineNumber, int linesNumber);

    Iterable<String> getLines(Reader reader);

    boolean isPrintable(String text, double threshold);

    /**
     * Checks if a file exists at the given path.
     *
     * @param filePath the file path to check
     * @return true if the file exists, false otherwise
     * @throws IOException if an I/O error occurs
     */
    boolean fileExists(String filePath) throws IOException;

    /**
     * Checks if a file at the given path is empty.
     *
     * @param filePath the file path to check
     * @return true if the file exists and is empty, false otherwise
     * @throws IOException if an I/O error occurs
     */
    boolean isFileEmpty(String filePath) throws IOException;

    /**
     * Reads all bytes from a file.
     *
     * @param filePath the file path to read from
     * @return the file contents as a byte array
     * @throws IOException if an I/O error occurs
     */
    byte[] readAllBytes(String filePath) throws IOException;

    /**
     * Writes bytes to a file.
     *
     * @param filePath the file path to write to
     * @param data the data to write
     * @throws IOException if an I/O error occurs
     */
    void writeAllBytes(String filePath, byte[] data) throws IOException;

    /**
     * Deletes a file.
     *
     * @param filePath the file path to delete
     * @throws IOException if an I/O error occurs
     */
    void deleteFile(String filePath) throws IOException;
}
