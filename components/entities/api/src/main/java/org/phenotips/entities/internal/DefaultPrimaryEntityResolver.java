/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.entities.internal;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.PrimaryEntityResolver;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of the {@link PrimaryEntityResolver} component, which uses all the
 * {@link PrimaryEntityManager entity managers} registered in the component manager.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class DefaultPrimaryEntityResolver implements PrimaryEntityResolver, Initializable
{
    /** The currently available primary entity managers. */
    @Inject
    private Map<String, PrimaryEntityManager> repositories;

    /** Currently available primary entity managers, mapped by their entity ID prefix. */
    private Map<String, PrimaryEntityManager> repositoriesByPrefix;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.repositoriesByPrefix = this.repositories.values().stream()
                .collect(Collectors.toMap(this::getIdPrefix, Function.identity(), this::resolveCollisions));
        } catch (final RuntimeException ex) {
            throw new InitializationException(ex.getMessage());
        }
    }

    @Nullable
    @Override
    public PrimaryEntity resolveEntity(@Nullable final String entityId)
    {
        // Not a valid id.
        if (StringUtils.isBlank(entityId)) {
            return null;
        }
        // Try to get the prefix; don't bother searching if it's blank.
        final String prefix = entityId.replaceAll("^(\\D+)\\d+$", "$1");
        if (StringUtils.isBlank(prefix)) {
            return null;
        }
        // Get the repository by prefix.
        final PrimaryEntityManager repository = this.repositoriesByPrefix.get(prefix);
        // Try to get the entity.
        return repository == null ? null : repository.get(entityId);
    }

    @Nullable
    @Override
    public PrimaryEntityManager getEntityManager(@Nullable final String entityType)
    {
        return StringUtils.isNotBlank(entityType) ? this.repositories.get(entityType) : null;
    }

    @Override
    public boolean hasEntityManager(@Nullable final String entityType)
    {
        return StringUtils.isNotBlank(entityType) && this.repositories.containsKey(entityType);
    }

    /**
     * Tries to get the id prefix from the provided primary entity {@code manager}. Throws an exception if the prefix
     * is blank.
     *
     * @param manager the {@link PrimaryEntityManager} from which the id prefix will be retrieved; must not be null
     * @return the id prefix for the specified {@code manager}
     */
    private String getIdPrefix(@Nonnull final PrimaryEntityManager manager)
    {
        final String prefix = manager.getIdPrefix();
        if (StringUtils.isBlank(prefix)) {
            throw new RuntimeException("No prefix specified for PrimaryEntityManager");
        }
        return prefix;
    }

    /**
     * This method is called iff {@code manager1} and {@code manager2} have the same
     * {@link PrimaryEntityManager#getIdPrefix()}. Throws an exception if called.
     *
     * @param manager1 {@link PrimaryEntityManager}
     * @param manager2 {@link PrimaryEntityManager}
     * @return nothing; always throws a {@link RuntimeException}
     */
    private PrimaryEntityManager resolveCollisions(
        @Nonnull final PrimaryEntityManager manager1,
        @Nonnull final PrimaryEntityManager manager2)
    {
        // If this is called, there are primary entity managers that have the same id prefix. Throw exception.
        throw new RuntimeException("PrimaryEntityManager objects must not have the same ID prefix. "
            + "Duplicate prefix detected: " + manager1.getIdPrefix());
    }
}
