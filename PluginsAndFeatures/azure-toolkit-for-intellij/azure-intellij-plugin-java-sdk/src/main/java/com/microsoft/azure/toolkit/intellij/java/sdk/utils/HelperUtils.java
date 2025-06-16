package com.microsoft.azure.toolkit.intellij.java.sdk.utils;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiWhiteSpace;
import java.util.List;

/*
 * Utility class to provide helper methods for the Azure SDK rules.
 */
public class HelperUtils {
    private static final String AZURE_PACKAGE_NAME = "com.azure";

    /**
     * Checks if the method call is following a specific method call like `subscribe`.
     *
     * @param expression The method call expression to analyze.
     *
     * @return True if the method call is following a `subscribe` method call, false otherwise.
     */
    public static boolean isFollowedBySubscribe(PsiMethodCallExpression expression) {
        if (expression == null) {
            return false;
        }

        // Case 1: Chained Call -> Check if any method in the chain is subscribe()
        if (isAnyMethodInChainSubscribe(expression)) {
            return true;
        }

        // Case 2: Sequential Call -> Check if the method call result is stored in a variable and later subscribed
        return isVariableStoredAndSubscribed(expression);
    }

    // Case 1: Checks if any method in a chain is "subscribe()"
    private static boolean isAnyMethodInChainSubscribe(PsiMethodCallExpression expression) {
        PsiElement current = expression;

        while (current instanceof PsiMethodCallExpression methodCall) {
            PsiReferenceExpression methodExpression = methodCall.getMethodExpression();
            if ("subscribe".equals(methodExpression.getReferenceName())) {
                return true; // Found subscribe()
            }

            // Move up the PSI tree
            PsiElement parent = current.getParent();

            // Handle cases where subscribe is a reference and not yet a method call
            if (parent instanceof PsiReferenceExpression referenceExpression) {
                PsiElement grandParent = referenceExpression.getParent();
                if (grandParent instanceof PsiMethodCallExpression grandMethodCall &&
                    "subscribe".equals(referenceExpression.getReferenceName())) {
                    return true; // Reference becomes a method call
                }
            }

            // Continue traversal
            current = parent;
        }

        return false;
    }


    // Case 2: Checks if result is assigned to a variable and later subscribed
    private static boolean isVariableStoredAndSubscribed(PsiMethodCallExpression expression) {
        PsiElement parent = expression.getParent();

        // Ensure the method call is assigned to a variable
        if (!(parent instanceof PsiLocalVariable variable)) {
            return false;
        }

        // Get the variable name
        String variableName = variable.getName();
        if (variableName == null) {
            return false;
        }

        // Find if "subscribe" is later called on this variable
        PsiElement current = variable.getParent();
        while (current != null) {
            PsiElement nextSibling = current.getNextSibling();
            while (nextSibling instanceof PsiWhiteSpace || nextSibling instanceof PsiComment) {
                nextSibling = nextSibling.getNextSibling();
            }

            if (nextSibling instanceof PsiExpressionStatement exprStmt) {
                PsiExpression expr = exprStmt.getExpression();
                if (expr instanceof PsiMethodCallExpression methodCall) {
                    PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
                    if (qualifier instanceof PsiReferenceExpression refExpr && variableName.equals(refExpr.getReferenceName())
                        && "subscribe".equals(methodCall.getMethodExpression().getReferenceName())) {
                        return true;
                    }
                }
            }

            current = current.getParent();
        }

        return false;
    }

    /**
     * Checks if the class qualified name is an Azure package.
     * @param classQualifiedName The qualified name of the class.
     * @return True if the class is in an Azure package, false otherwise.
     */
    public static boolean isAzurePackage(String classQualifiedName) {
        return classQualifiedName.startsWith(AZURE_PACKAGE_NAME);
    }

    /**
     * Get the resolved method from a method call expression.
     * @param element The method call expression.
     * @return The resolved method, or null if the method is not resolved.
     */
    public static PsiMethod getResolvedMethod(PsiElement element) {
        // Ensure the element is a method call
        if (!(element instanceof PsiMethodCallExpression methodCallExpression)) {
            return null;
        }

        // Resolve the method being called
        PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();
        PsiElement resolvedMethod = methodExpression.resolve();
        if (!(resolvedMethod instanceof PsiMethod method)) {
            return null;
        }
        return method;
    }

    /**
     * Checks if the method is in the list of usages.
     * @param usages The list of usages.
     * @param methodName The name of the method to check.
     * @return True if the method is in the list of usages, false otherwise.
     */
    public static boolean checkIfInUsages(List<String> usages, String methodName) {
        if (usages.isEmpty()) {
            return true;
        }
        return usages.stream().anyMatch(usage -> usage.equals(methodName));
    }

    /**
     * Checks if the class qualified name is in the list of scopes.
     * @param scope The list of scopes.
     * @param classQualifiedName The qualified name of the class to check.
     * @return True if the class is in the list of scopes, false otherwise.
     */
    public static boolean checkIfInScope(List<String> scope, String classQualifiedName) {
        if (scope.isEmpty()) {
            return true;
        }
        return scope.stream().anyMatch(classQualifiedName::contains);
    }

    /**
     * Checks if the method is a discouraged API.
     * @param method The method to check.
     * @param usages The list of usages.
     * @param scopes The list of scopes.
     * @return True if the method is a discouraged API, false otherwise.
     */
    public static boolean isItDiscouragedAPI(PsiMethod method, List<String> usages, List<String> scopes) {
        if (method == null) {
            return false;
        }

        // Check if the method is a discouraged API
        String methodName = method.getName();
        if (!checkIfInUsages(usages, methodName)) {
            return false;
        }

        // Get qualified name of the containing class
        String classQualifiedName = HelperUtils.getContainingClassOfMethod(method);
        if (classQualifiedName == null) {
            return false;
        }

        // Verify package name and scope
        if (!HelperUtils.isAzurePackage(classQualifiedName)) {
            return false;
        }

        // Verify scope
        return checkIfInScope(scopes, classQualifiedName);
    }

    /**
     * Checks if the element is a discouraged client.
     * @param element The element to check.
     * @param usages The list of usages.
     * @return True if the element is a discouraged client, false otherwise.
     */
    public static boolean isItDiscouragedClient(PsiTypeElement element, List<String> usages) {
        PsiType psiType = element.getType();
        if (psiType instanceof PsiClassType) {
            PsiClass psiClass = ((PsiClassType) psiType).resolve();
            if (psiClass != null) {
                String qualifiedName = psiClass.getQualifiedName();
                if (qualifiedName != null) {
                    return isAzurePackage(qualifiedName) && usages.contains(psiClass.getName());
                }
            }
        }
        return false;
    }

    private static String getContainingClassOfMethod(PsiMethod method) {
        if (method == null) {
            return null;
        }

        // Get the containing class of the method
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return null;
        }

        // Get qualified name of the containing class
        String classQualifiedName = containingClass.getQualifiedName();
        if (classQualifiedName == null) {
            return null;
        }
        return classQualifiedName;

    }
}
