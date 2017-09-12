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

import java.util.Collection;
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

    private Map<String, PrimaryEntityManager> repositoriesByPrefix;

    @Override
    public void initialize() throws InitializationException
    {
        this.repositoriesByPrefix = this.repositories.values().stream()
            .collect(Collectors.toMap(PrimaryEntityManager::getIdPrefix, Function.identity()));
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

    @Nonnull
    @Override
    public Collection<PrimaryEntityManager> getEntityManagers()
    {
        return this.repositories.values();
    }
}
