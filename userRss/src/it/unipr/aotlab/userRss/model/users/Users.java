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

package it.unipr.aotlab.userRss.model.users;

/**
 * User: enrico
 * Package: it.unipr.aotlab.userRss.model.users
 * Date: 10/27/11
 * Time: 12:50 PM
 */

import it.unipr.aotlab.userRss.errors.InformationMissing;
import it.unipr.aotlab.userRss.errors.MissingProfileError;
import it.unipr.aotlab.userRss.errors.NetworkError;
import it.unipr.aotlab.userRss.model.hashes.Hash;
import it.unipr.aotlab.userRss.network.Network;
import it.unipr.aotlab.userRss.network.NetworkManager;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Utility methods to manipulate the users.
 */
public class Users {
    /**
     * Creates a new user from given hash. In this case the user is identified in the system by its own hash.
     * @param hash to identify the user
     * @return a new user
     */
    public static User newUser(Hash hash) {
        return newUser(hash.getValue(), hash);
    }

    /**
     * Creates a new user from a given hash and locally associates the user with the given nickname
     * @param hash to identify the user globally
     * @param name to identify the user locally
     * @return the user
     */
    public static User newUser(final String name, final Hash hash) {
        return new UserImpl(name, hash);
    }

    /**
     * Get profile for the specified user.
     *
     * @param user for which to return the profile.
     * @return the user's profile
     * @throws MissingProfileError
     * @throws NetworkError
     */
    public static Profile getProfile(User user) throws MissingProfileError, NetworkError {
        Profile profile;
        try {
            profile = user.getProfile();
            return profile;
        } catch (InformationMissing informationMissing) {
            Network network = NetworkManager.getNetwork();
            // network read profile...
            UserImpl userImpl = (UserImpl)user;
            userImpl.setProfileRequested(true);
            throw new NotImplementedException();
        }
    }

    /**
     * Example method which indicates that we should have stuff to perform searches of users /in memory/
     * @param query
     * @return
     */
    public static User searchUser(String query) {
        throw new NotImplementedException();
    }
}
