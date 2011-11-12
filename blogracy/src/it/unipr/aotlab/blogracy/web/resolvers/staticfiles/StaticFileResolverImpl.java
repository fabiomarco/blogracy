/*
 * Copyright (c)  2011 Enrico Franchi, Michele Tomaiuolo and University of Parma.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package it.unipr.aotlab.blogracy.web.resolvers.staticfiles;

import it.unipr.aotlab.blogracy.errors.ServerConfigurationError;
import it.unipr.aotlab.blogracy.errors.URLMappingError;
import it.unipr.aotlab.blogracy.logging.Logger;
import it.unipr.aotlab.blogracy.mime.MimeFinder;
import it.unipr.aotlab.blogracy.mime.MimeFinderFactory;
import it.unipr.aotlab.blogracy.util.FileUtils;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;

import java.io.*;
import java.net.HttpURLConnection;

/**
 * User: enrico
 * Package: it.unipr.aotlab.blogracy.web.resolvers
 * Date: 11/11/11
 * Time: 4:10 PM
 */
public class StaticFileResolverImpl implements StaticFileResolver {
    MimeFinder mimeFinder = MimeFinderFactory.getInstance();
    private File staticFilesDirectory;

    StaticFileResolverImpl(final File staticFilesDirectory) throws ServerConfigurationError {
        checksValidStaticRootAndSetField(staticFilesDirectory);
    }

    StaticFileResolverImpl(final File staticFilesDirectory, MimeFinder mimeFinder)
            throws ServerConfigurationError {
        this(staticFilesDirectory);
        this.mimeFinder = mimeFinder;
    }

    /**
     * Checks if {@param staticRoot} exists and is a directory
     *
     * @param staticRoot is the path to check
     * @throws ServerConfigurationError if {@param staticRoot} does not exist or is not a directory
     */
    private void checksValidStaticRootAndSetField(final File staticRoot)
            throws ServerConfigurationError {
        if (staticRoot.exists()) {
            if (staticRoot.isDirectory()) {
                staticFilesDirectory = staticRoot;

            } else {
                throw new ServerConfigurationError(
                        errorMessageNotDirectory(staticRoot));
            }
        } else {
            throw new ServerConfigurationError(
                    errorMessageNotExists(staticRoot));
        }
    }

    private static String errorMessageNotExists(final File staticRoot) {
        return "Static files root " +
                staticRoot.toString() +
                " does not exist.";
    }

    private static String errorMessageNotDirectory(final File staticRoot) {
        return "Static files root " +
                staticRoot.toString() +
                " exists but is not a directory.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resolve(
            final TrackerWebPageRequest request,
            final TrackerWebPageResponse response)
            throws URLMappingError {
        /*
        * Keep in mind that due to the underlying TrackerWebPageResponseImpl#useFile(String, String)
        * implementation, files without extension cannot be processed (why developers assume everybody is using
        * windows anyway?).
        *
        * If someone feels a compelling reason to resolve a file without extension, he may wrote some crazy code
        * to run around the default implementation of TrackerWebPageResponseImpl, like copy the file to a file with
        * extension and  send that. Then they could go and meet the original developers with an {@code Object}
        * implementing  the {@code Bludgeoning} interface and repeatedly call {@code Bludgeoning#hitOnTheHead} passing
        * the aforementioned developers as actual parameters.
        *
        * Ah, when there is no doc, we are always obliged to read the implementations. So perhaps some day this will
        * not be true anymore.
        *
        * Note to self: when you suppose someone else is reading your code and you are doing
        * some low level stuff like manually checking if a file has an extension, do not use explicative names like
        * {@code fileHasExtension(filename)} and similar over-engineering techniques: since your code is worth being read only by
        * true hackers, rather leave the low level stuff and use an equivalent 6502 asm program to explain your goals.
        *
        * Besides, actually TrackerWebPageResponseImpl#useFile(String, String)
        * does set the right {@code Content-Type} headers. Well, <i>right</i> according to the hash table
        * hardcoded in {@link com.aelitis.azureus.core.util.HTTPUtils}. If your file do not fit I would just
        * suggest the {@code Bludgeoning} trick once again. My whole idea of having a pluggable mime-type resolver
        * is just for fools who think there are more than 27 or so different file-types.
        *
        * Since {@link com.aelitis.azureus.core.util.HTTPUtils#guessContentTypeFromFileType(String)} returns
        * the argument if there is no match and that is what is called by
        * {@link org.gudy.azureus2.pluginsimpl.local.tracker.TrackerWebPageResponseImpl#useStream(String, java.io.InputStream)}
        * to resolve the first String parameter, you may want to use a pluggable mime-type resolver with the "right"
        * mime-type (which being no extension will be used as the true content type). Have bloody fun with it.
        */

        final String url = request.getURL();
        final String staticFilesDirectoryName = staticFilesDirectory.getAbsolutePath();

        try {
            boolean didSendTheFile = response.useFile(staticFilesDirectoryName, url);
            if (!didSendTheFile) {
                if (mayBeDirectoryUrl(url)) {
                    redirectToIndexInDirectory(url, response);
                } else {
                    throw new URLMappingError(
                            HttpURLConnection.HTTP_NOT_FOUND,
                            "Could not find " + url
                    );
                }
            }
        } catch (FileNotFoundException e) {
            throw new URLMappingError(HttpURLConnection.HTTP_NOT_FOUND, e);
        } catch (SecurityException e) {
            throw new URLMappingError(HttpURLConnection.HTTP_FORBIDDEN, e);
        } catch (IOException e) {
            /* we should not get here */
            throw new URLMappingError(HttpURLConnection.HTTP_NOT_FOUND, e);
        }

    }

    private void redirectToIndexInDirectory(final String url, final TrackerWebPageResponse response) {
        Logger.info(url + " may be a directory. Trying to send index.html instead.");
        response.setReplyStatus(HttpURLConnection.HTTP_SEE_OTHER);
        response.setHeader("Location", url + "index.html");
    }

    private boolean mayBeDirectoryUrl(final String url) {
        return url.endsWith("/");
    }

    @Override
    public String resolvePath(final String url) throws URLMappingError {
        final File fileSystemPath = getFileSystemPath(url);
        if (fileSystemPath.exists()) {
            return fileSystemPath.getAbsolutePath();
        } else {
            throw new URLMappingError(
                    HttpURLConnection.HTTP_NOT_FOUND,
                    "Could not find file for " + fileSystemPath.getAbsolutePath()
            );
        }
    }

    private void sendFile(final File actualFile,
                          final OutputStream outputStream) throws IOException {
        FileUtils.copyCompletely(
                new FileReader(actualFile),
                new OutputStreamWriter(outputStream)
        );
        outputStream.flush();
        outputStream.close();
    }

    private File getFileSystemPath(final String url) {
        return new File(staticFilesDirectory, url);
    }

    @Override
    public boolean couldResolve(final String url) {
        File tentativeFile = getFileSystemPath(url);
        return tentativeFile.exists();
    }
}
