/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.redis;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.RedisCache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class RedisNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Redis Caches";
    private static final String ICON = AzureIcons.RedisCache.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureRedis.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureRedis ||
            data instanceof RedisCache;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureRedis) {
            final Function<AzureRedis, List<RedisCache>> caches = asc -> asc.list().stream().flatMap(m -> m.caches().list().stream())
                .collect(Collectors.toList());
            return new AzServiceNode<>(az(AzureRedis.class))
                .withIcon(ICON)
                .withLabel(NAME)
                .withActions(RedisActionsContributor.SERVICE_ACTIONS)
                .addChildren(caches, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof RedisCache) {
            return new AzResourceNode<>((RedisCache) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .onDoubleClicked(ResourceCommonActionsContributor.SHOW_PROPERTIES)
                .withActions(RedisActionsContributor.REDIS_ACTIONS);
        }
        return null;
    }
}
