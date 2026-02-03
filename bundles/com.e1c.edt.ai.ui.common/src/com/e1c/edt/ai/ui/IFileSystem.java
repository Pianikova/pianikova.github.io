/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public interface IFileSystem
{
    IFile getProjectFile(IProject project, String relativePath);

    Iterable<String> getLines(IFileDocument fileDocument, int firstLineNumber, int linesNumber);

    boolean isPrintable(String text, double threshold);

    /**
     * Determines if the given file path is within a project in the workspace.
     *
     * @param filePath the file path to check (can be absolute or relative)
     * @return the project name if the file is part of a project, null otherwise
     */
    String determineProjectName(String filePath);

    /**
     * Checks if a file exists at the given path.
     *
     * @param filePath the file path to check
     * @return true if the file exists, false otherwise
     * @throws IOException if an I/O error occurs
     */
    boolean fileExists(String filePath) throws IOException;

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
