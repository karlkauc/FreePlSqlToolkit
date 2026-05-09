package org.fxt.freeplsql.appsvc.dependency;

/** One row from {@code ALL_DEPENDENCIES}: source object → referenced object. */
public record DependencyEdge(String sourceOwner,
                             String sourceName,
                             String sourceType,
                             String targetOwner,
                             String targetName,
                             String targetType) {
}
