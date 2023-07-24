/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.intellij.database.dataSource.url.TypeDescriptor;
import com.intellij.database.dataSource.url.TypesRegistry;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

public class MongoCosmosDbAccountTypeFactory implements TypesRegistry.TypeDescriptorFactory {
    private static final String TYPE_NAME = "cosmos_account_mongo";
    private static final String CAPTION = "Account";

    @Override
    public void createTypeDescriptor(@NotNull Consumer<? super TypeDescriptor> consumer) {
        consumer.consume(TypesRegistry.createTypeDescriptor(TYPE_NAME, ".", CAPTION));
    }
}
