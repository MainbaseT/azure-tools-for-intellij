/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.po;

import com.microsoft.azure.toolkit.intellij.link.base.ServiceType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public abstract class BaseResourcePO {

    private String id;
    private String resourceId;
    private ServiceType type;

    public String getBusinessUniqueKey() {
        return type + "#" + resourceId;
    }
}
