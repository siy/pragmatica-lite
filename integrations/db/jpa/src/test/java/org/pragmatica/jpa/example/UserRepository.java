/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.jpa.example;

import jakarta.persistence.EntityManager;
import org.pragmatica.jpa.JpaError;
import org.pragmatica.jpa.JpaOperations;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;

/// Example adapter leaf demonstrating JpaOperations usage.
/// Pure adapter - no business logic, only I/O operations.
/// Converts JPA exceptions to typed JpaError causes.
record UserRepository(JpaOperations ops, EntityManager em) {

    static UserRepository userRepository(EntityManager em) {
        return new UserRepository(JpaOperations.jpaOperations(em), em);
    }

    /// Find user by ID.
    ///
    /// @param id User ID
    ///
    /// @return Promise with optional user
    Promise<Option<User>> findById(Long id) {
        return ops.findById(JpaError::fromException, User.class, id);
    }

    /// Find user by email.
    ///
    /// @param email User email
    ///
    /// @return Promise with optional user
    Promise<Option<User>> findByEmail(String email) {
        var query = em.createQuery(
            "SELECT u FROM User u WHERE u.email = :email",
            User.class
        ).setParameter("email", email);

        return ops.queryOptional(JpaError::fromException, query);
    }

    /// Create new user.
    ///
    /// @param user User entity to persist
    ///
    /// @return Promise with persisted user
    Promise<User> create(User user) {
        return ops.persist(JpaError::fromException, user);
    }

    /// Update existing user.
    ///
    /// @param user User entity to merge
    ///
    /// @return Promise with merged user
    Promise<User> update(User user) {
        return ops.merge(JpaError::fromException, user);
    }

    /// Delete user by email.
    ///
    /// @param email User email
    ///
    /// @return Promise with number of deleted rows
    Promise<Integer> deleteByEmail(String email) {
        var query = em.createQuery("DELETE FROM User u WHERE u.email = :email")
            .setParameter("email", email);

        return ops.executeUpdate(JpaError::fromException, query);
    }
}
