/*
 * ====================================================================
 * Copyright (c) 2004-2007 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc;

import java.io.File;
import java.io.FilenameFilter;


/**
 * @version 1.0
 * @author  TMate Software Ltd.
 */
public class SVNFileListUtil {

    /**
     * This method is a replacement for file.listFiles(), which composes
     * decomposed file names (e.g. umlauts in file names on the Mac).
     */
    public static File[] listFiles(File directory) {
        if (!SVNFileUtil.isOSX) {
            return directory.listFiles();
        }
        final String[] fileNames = list(directory);
        if (fileNames == null) {
            return null;
        }

        return updateFiles(directory, fileNames);
    }

    /**
     * This method is a replacement for file.listFiles(), which composes
     * decomposed file names (e.g. umlauts in file names on the Mac).
     */
    public static File[] listFiles(File directory, FilenameFilter filter) {
        if (!SVNFileUtil.isOSX) {
            return directory.listFiles(filter);
        }
        final String[] fileNames = list(directory, filter);
        if (fileNames == null) {
            return null;
        }

        return updateFiles(directory, fileNames);
    }
    
    private static File[] updateFiles(File parent, final String[] fileNames) {
        File[] files = new File[fileNames.length];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(parent.getPath(), fileNames[i]);
        }
        return files;
    }
    
    /**
     * This method is a replacement for file.list(), which composes decomposed 
     * file names (e.g. umlauts in file names on the Mac).
     */
    public static String[] list(File directory) {
        if (!SVNFileUtil.isOSX) {
            return directory.list();
        }
        final String[] fileNames = directory.list();
        if (fileNames == null) {
            return null;
        }
        
        return compose(fileNames);
    }

    /**
     * This method is a replacement for file.list(FilenameFilter), which
     * composes decomposed file names (e.g. umlauts in file names on the Mac).
     */
    public static String[] list(File directory, FilenameFilter filter) {
        if (!SVNFileUtil.isOSX) {
            return directory.list(filter);
        }
        final String[] fileNames = directory.list(new ComposingFilenameFilter(filter));
        if (fileNames == null) {
            return null;
        }
        
        return compose(fileNames);
    }
    
    private static String[] compose(String[] decomposedStrings) {
        final String[] composedStrings = new String[decomposedStrings.length];
        for (int i = 0; i < composedStrings.length; i++) {
            // Note: Normalizer class would be better than manually implemented
            // compose() method, but unfortunately it was sun.text.Normalizer in
            // Java 1.5 and became java.text.Normalizer in 1.6 along with API
            // changes - so it cannot be used cross-platform and with all Java
            // versions >= 1.5
//            composedFileNames[i] = Normalizer.normalize(fileNames[i], Normalizer.COMPOSE, 0);
            composedStrings[i] = compose(decomposedStrings[i]);
//            System.out.println("File [" + i + "]: '" + fileNames[i] + "' => '" + composedFileNames[i] + "'");
        }
        return composedStrings;
    }

    private static String compose(String decomposedString) {
        if (decomposedString == null) {
            return null;
        }

        StringBuffer buffer = null;
        for (int i = 1, length = decomposedString.length(); i < length; i++) {
            final char chr = decomposedString.charAt(i);
            if (chr == '\u0300') { // grave `
                buffer = compose(i, "AaEeIiOoUu", "\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9", decomposedString, buffer);
            }
            else if (chr == '\u0301') { // acute '
                buffer = compose(i, "AaEeIiOoUuYy", "\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA\u00DD\u00FD", decomposedString, buffer);
            }
            else if (chr == '\u0302') { // circumflex ^
                buffer = compose(i, "AaEeIiOoUuYy", "\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177", decomposedString, buffer);
            }
            else if (chr == '\u0303') { // tilde ~
                buffer = compose(i, "AaNnOoUu", "\u00C3\u00E3\u00D1\u00F1\u00D5\u00F5\u0168\u0169", decomposedString, buffer);
            }
            else if (chr == '\u0308') { // umlaut/dieresis (two dots above)
                buffer = compose(i, "AaEeIiOoUuYy", "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF", decomposedString, buffer);
            }
            else if (chr == '\u030A') { // ring above (as in Angstrom)
                buffer = compose(i, "Aa", "\u00C5\u00E5", decomposedString, buffer);
            }
            else if (chr == '\u0327') { // cedilla ,
                buffer = compose(i, "Cc", "\u00C7\u00E7", decomposedString, buffer);
            }
            else if (buffer != null) {
                buffer.append(chr);
            }
        }

        if (buffer == null) {
            return decomposedString;
        }

        return buffer.toString();
    }

    // Utils ==================================================================

    private static StringBuffer compose(int i, String decomposedChars, String composedChars, String decomposedString, StringBuffer buffer) {
        final char previousChar = decomposedString.charAt(i - 1);
        final int decomposedIndex = decomposedChars.indexOf(previousChar);
        if (decomposedIndex >= 0) {
            if (buffer == null) {
                buffer = new StringBuffer(decomposedString.length() + 2);
                buffer.append(decomposedString.substring(0, i - 1));
            }
            else {
                buffer.delete(buffer.length() - 1, buffer.length());
            }

            buffer.append(composedChars.charAt(decomposedIndex));
        }
        else {
            if (buffer == null) {
                buffer = new StringBuffer(decomposedString.length() + 2);
                buffer.append(decomposedString.substring(0, i));
            }
        }
        return buffer;
    }

    /**
     * Helper class for composing filenames in a FilenameFilter. Wraps around
     * another (user-given) filename filter and ensures to pass composed file
     * names in the accept method.
     *
     */
    public static class ComposingFilenameFilter implements FilenameFilter {
        
        FilenameFilter filter;

        public ComposingFilenameFilter(FilenameFilter filter) {
            this.filter = filter;
        }

        public boolean accept(File dir, String name) {
            return filter.accept(dir, compose(name));
        }

    }

}
