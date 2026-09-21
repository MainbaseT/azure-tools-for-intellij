/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service;

import com.intellij.util.net.JdkProxyProvider;
import com.intellij.util.net.ssl.CertificateManager;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to check for CVE (Common Vulnerabilities and Exposures) issues in Maven dependencies.
 * Uses GitHub's Security Advisory API to fetch vulnerability information.
 * 
 * This implementation is aligned with the TypeScript version in vscode-java-dependency.
 * @see <a href="https://github.com/microsoft/vscode-java-dependency/blob/main/src/upgrade/cve.ts">cve.ts</a>
 */
@Slf4j
public class CVECheckService {
    
    private static final String GITHUB_API_BASE = "https://api.github.com/advisories";
    private static final int BATCH_SIZE = 30;
    private static final int REQUEST_TIMEOUT_SECONDS = 30;
    
    /**
     * Severity levels ordered by criticality (higher number = more critical).
     * Aligned with GitHub Security Advisory severity levels.
     */
    public enum Severity {
        UNKNOWN(0),
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4);
        
        private final int level;
        
        Severity(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
        
        public static Severity fromString(String severity) {
            if (severity == null) return UNKNOWN;
            return switch (severity.toLowerCase()) {
                case "critical" -> CRITICAL;
                case "high" -> HIGH;
                case "medium" -> MEDIUM;
                case "low" -> LOW;
                default -> UNKNOWN;
            };
        }
    }
    
    /**
     * Represents a CVE (Common Vulnerabilities and Exposures) entry.
     */
    @Data
    @Builder
    public static class CVE {
        private String id;
        private String ghsaId;
        private Severity severity;
        private String summary;
        private String description;
        private String htmlUrl;
        private List<AffectedDependency> affectedDeps;
    }
    
    /**
     * Represents a dependency affected by a CVE.
     */
    @Data
    @Builder
    public static class AffectedDependency {
        private String name;
        private String vulnerableVersionRange;
        private String patchedVersion;
    }
    
    /**
     * Represents a dependency coordinate (groupId:artifactId:version).
     */
    @Data
    @Builder
    public static class DependencyCoordinate {
        private String groupId;
        private String artifactId;
        private String version;
        
        public String getName() {
            return groupId + ":" + artifactId;
        }
        
        public String getCoordinate() {
            return groupId + ":" + artifactId + ":" + version;
        }
    }
    
    private static CVECheckService instance;
    private final HttpClient httpClient;
    private final Gson gson;
    
    private CVECheckService() {
        final HttpClient.Builder builder = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS));
        try {
            // Configure proxy using IntelliJ's JdkProxyProvider
            // This respects the IDE's proxy settings (Settings → HTTP Proxy)
            builder.proxy(JdkProxyProvider.getInstance().getProxySelector());
        } catch (Throwable e) {
            log.error("Failed to configure proxy from IntelliJ proxy settings", e);
        }
        try{
            builder.sslContext(CertificateManager.getInstance().getSslContext());
        } catch (Throwable e) {
            // Failed to get IntelliJ SSL context, using default
            log.error("Failed to configure HTTP client SSL context from IntelliJ CertificateManager, using default.", e);
        }
        this.httpClient = builder.build();
        this.gson = new Gson();
    }
    
    public static synchronized CVECheckService getInstance() {
        if (instance == null) {
            instance = new CVECheckService();
        }
        return instance;
    }
    
    /**
     * Batch check CVE issues for a list of Maven coordinates.
     * Aligned with batchGetCVEIssues() from cve.ts.
     *
     * @param coordinates List of Maven coordinates in format "groupId:artifactId:version"
     * @return List of CVE-related upgrade issues
     */
    @Nonnull
    public List<JavaUpgradeIssue> batchGetCVEIssues(@Nonnull List<String> coordinates) {
        final List<JavaUpgradeIssue> allIssues = new ArrayList<>();
        
        // Process dependencies in batches to avoid URL length limits
        for (int i = 0; i < coordinates.size(); i += BATCH_SIZE) {
            final List<String> batch = coordinates.subList(i, Math.min(i + BATCH_SIZE, coordinates.size()));
            try {
                final List<JavaUpgradeIssue> batchIssues = getCveUpgradeIssues(batch);
                allIssues.addAll(batchIssues);
            } catch (Exception e) {
                log.error("Error fetching CVE issues for batch starting at index " + i, e);
            }
        }
        
        return allIssues;
    }
    
    /**
     * Get CVE upgrade issues for a batch of coordinates.
     */
    @Nonnull
    private List<JavaUpgradeIssue> getCveUpgradeIssues(@Nonnull List<String> coordinates) {
        if (coordinates.isEmpty()) {
            return Collections.emptyList();
        }
        
        final List<DependencyCoordinate> deps = coordinates.stream()
            .map(this::parseCoordinate)
            .filter(Objects::nonNull)
            .filter(d -> StringUtils.isNotBlank(d.getVersion()))
            .collect(Collectors.toList());
        
        if (deps.isEmpty()) {
            return Collections.emptyList();
        }
        
        final List<DepsCves> depsCves = fetchCves(deps);
        return mapCvesToUpgradeIssues(depsCves);
    }
    
    /**
     * Parse a Maven coordinate string into a DependencyCoordinate object.
     */
    @Nullable
    private DependencyCoordinate parseCoordinate(@Nonnull String coordinate) {
        final String[] parts = coordinate.split(":", 3);
        if (parts.length < 3) {
            return null;
        }
        return DependencyCoordinate.builder()
            .groupId(parts[0])
            .artifactId(parts[1])
            .version(parts[2])
            .build();
    }
    
    /**
     * Represents a dependency with its associated CVEs.
     */
    @Data
    @Builder
    private static class DepsCves {
        private String dep;
        private String version;
        private List<CVE> cves;
    }
    
    /**
     * Fetch CVEs from GitHub Security Advisory API.
     */
    @Nonnull
    private List<DepsCves> fetchCves(@Nonnull List<DependencyCoordinate> deps) {
        if (deps.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            final List<CVE> allCves = retrieveVulnerabilityData(deps);
            
            if (allCves.isEmpty()) {
                return Collections.emptyList();
            }
            
            // Group the CVEs by coordinate
            final List<DepsCves> depsCves = new ArrayList<>();
            
            for (DependencyCoordinate dep : deps) {
                final List<CVE> depCves = allCves.stream()
                    .filter(cve -> isCveAffectingDep(cve, dep.getName(), dep.getVersion()))
                    .collect(Collectors.toList());
                
                if (!depCves.isEmpty()) {
                    depsCves.add(DepsCves.builder()
                        .dep(dep.getName())
                        .version(dep.getVersion())
                        .cves(depCves)
                        .build());
                }
            }
            
            return depsCves;
        } catch (Exception e) {
            // Error fetching CVEs
            log.error("Error fetching CVEs from GitHub Security Advisory API", e);
            return Collections.emptyList();
        }
    }
    


    /**
     * Retrieve vulnerability data from GitHub Security Advisory API.
     * Only fetches critical and high severity CVEs for Maven ecosystem.
     */
    @Nonnull
    private List<CVE> retrieveVulnerabilityData(@Nonnull List<DependencyCoordinate> deps) {
        if (deps.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Build the affects parameter: package@version format
            // Based on TS: deps.map((p) => `${p.name}@${p.version}`) passed as array to octokit.
            // Octokit usually serializes array as repeated params affects=a&affects=b OR comma-sep.
            // GitHub API docs say "iterable". Standard approach for "iterable" in query string is repeated params.
            // But previous Java code used comma. I will stick to comma as it likely works, or change if needed.
            // TS Octokit behavior for "affects" param -> comma separated string is often accepted.
            final String affects = deps.stream()
                .map(d -> URLEncoder.encode(d.getName() + "@" + d.getVersion(), StandardCharsets.UTF_8))
                .collect(Collectors.joining(","));
            
            final List<CVE> allCves = new ArrayList<>();
            int page = 1;
            
            while (true) {
                final String url = String.format(
                    "%s?ecosystem=maven&affects=%s&per_page=100&sort=published&direction=asc&page=%d",
                    GITHUB_API_BASE, affects, page
                );
                
                final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .GET()
                    .build();
                
                final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    // GitHub API returned non-200 status
                    break;
                }
                
                final JsonArray advisories = gson.fromJson(response.body(), JsonArray.class);
                if (advisories.isEmpty()) {
                    break;
                }
                
                // Parse and add to list
                allCves.addAll(parseAdvisories(advisories));
                
                if (advisories.size() < 100) {
                    break;
                }
                
                page++;
            }
            
            return allCves;
            
        } catch (Exception e) {
            // Error retrieving vulnerability data from GitHub
            log.error("Error retrieving vulnerability data from GitHub Security Advisory API", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Parse GitHub Security Advisory API response into CVE objects.
     */
    @Nonnull
    private List<CVE> parseAdvisories(@Nonnull JsonArray advisories) {
        final List<CVE> cves = new ArrayList<>();
        
        try {
            for (JsonElement element : advisories) {
                final JsonObject advisory = element.getAsJsonObject();
                
                // Skip withdrawn advisories
                if (advisory.has("withdrawn_at") && !advisory.get("withdrawn_at").isJsonNull()) {
                    continue;
                }
                
                final String severity = getStringOrNull(advisory, "severity");
                final Severity severityEnum = Severity.fromString(severity);
                
                // Only consider critical and high severity CVEs
                if (severityEnum != Severity.CRITICAL && severityEnum != Severity.HIGH) {
                    continue;
                }
                
                final String cveId = getStringOrNull(advisory, "cve_id");
                final String ghsaId = getStringOrNull(advisory, "ghsa_id");
                final String id = StringUtils.isNotBlank(cveId) ? cveId : ghsaId;
                
                final List<AffectedDependency> affectedDeps = new ArrayList<>();
                if (advisory.has("vulnerabilities") && !advisory.get("vulnerabilities").isJsonNull()) {
                    final JsonArray vulnerabilities = advisory.getAsJsonArray("vulnerabilities");
                    for (JsonElement vulnElement : vulnerabilities) {
                        final JsonObject vuln = vulnElement.getAsJsonObject();
                        String packageName = null;
                        if (vuln.has("package") && !vuln.get("package").isJsonNull()) {
                            packageName = getStringOrNull(vuln.getAsJsonObject("package"), "name");
                        }
                        
                        affectedDeps.add(AffectedDependency.builder()
                            .name(packageName)
                            .vulnerableVersionRange(getStringOrNull(vuln, "vulnerable_version_range"))
                            .patchedVersion(getStringOrNull(vuln, "first_patched_version"))
                            .build());
                    }
                }
                
                cves.add(CVE.builder()
                    .id(id)
                    .ghsaId(ghsaId)
                    .severity(severityEnum)
                    .summary(getStringOrNull(advisory, "summary"))
                    .description(getStringOrNull(advisory, "description"))
                    .htmlUrl(getStringOrNull(advisory, "html_url"))
                    .affectedDeps(affectedDeps)
                    .build());
            }
            
        } catch (Exception e) {
            // Error parsing advisory JSON
            log.error("Error parsing advisories JsonArray from GitHub Security Advisory API", e);
        }
        
        return cves;
    }
    
    /**
     * Parse GitHub Security Advisory API response into CVE objects.
     * Legacy method for backward compatibility if needed, or helper
     */
    @Nonnull
    private List<CVE> parseAdvisories(@Nonnull String jsonResponse) {
        try {
            return parseAdvisories(gson.fromJson(jsonResponse, JsonArray.class));
        } catch (Exception e) {
            // Error parsing advisory JSON
            log.error("Error parsing advisories JsonString from GitHub Security Advisory API", e);
            return Collections.emptyList();
        }
    }
    
    @Nullable
    private String getStringOrNull(@Nonnull JsonObject obj, @Nonnull String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }
    
    /**
     * Map CVEs to upgrade issues.
     */
    @Nonnull
    private List<JavaUpgradeIssue> mapCvesToUpgradeIssues(@Nonnull List<DepsCves> depsCves) {
        if (depsCves.isEmpty()) {
            return Collections.emptyList();
        }
        
        return depsCves.stream()
            .map(this::mapDepCvesToUpgradeIssue)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Map a single dependency's CVEs to an upgrade issue.
     * Uses the most critical CVE for the issue details.
     */
    @Nullable
    private JavaUpgradeIssue mapDepCvesToUpgradeIssue(@Nonnull DepsCves depCve) {
        if (depCve.getCves() == null || depCve.getCves().isEmpty()) {
            return null;
        }
        
        // Find the most critical CVE
        final CVE mostCriticalCve = depCve.getCves().stream()
            .max(Comparator.comparingInt(cve -> cve.getSeverity().getLevel()))
            .orElse(depCve.getCves().get(0));
        
        // Build the message
        final String message = String.format(
            "Security vulnerability %s detected in %s %s. %s",
            mostCriticalCve.getId() != null ? mostCriticalCve.getId() : "CVE",
            depCve.getDep(),
            depCve.getVersion(),
            StringUtils.isNotBlank(mostCriticalCve.getSummary()) ? 
                mostCriticalCve.getSummary() : 
                "Please upgrade to a patched version."
        );
        
        // Determine suggested version from patched versions
        String suggestedVersion = null;
        if (mostCriticalCve.getAffectedDeps() != null) {
            suggestedVersion = mostCriticalCve.getAffectedDeps().stream()
                .filter(ad -> depCve.getDep().equals(ad.getName()))
                .map(AffectedDependency::getPatchedVersion)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);
        }
        
        return JavaUpgradeIssue.builder()
            .packageId(depCve.getDep())
            .packageDisplayName(depCve.getDep())
            .upgradeReason(JavaUpgradeIssue.UpgradeReason.CVE)
            .severity(mapCveSeverityToIssueSeverity(mostCriticalCve.getSeverity()))
            .currentVersion(depCve.getVersion())
            .suggestedVersion(suggestedVersion)
            .message(message)
            .learnMoreUrl(mostCriticalCve.getHtmlUrl())
            .cveId(mostCriticalCve.getId())
            .build();
    }
    
    /**
     * Map CVE severity to issue severity.
     */
    @Nonnull
    private JavaUpgradeIssue.Severity mapCveSeverityToIssueSeverity(@Nonnull Severity cveSeverity) {
        return switch (cveSeverity) {
            case CRITICAL -> JavaUpgradeIssue.Severity.CRITICAL;
            case HIGH -> JavaUpgradeIssue.Severity.WARNING;
            default -> JavaUpgradeIssue.Severity.INFO;
        };
    }
    
    /**
     * Check if a CVE affects a specific dependency at a specific version.
     * Aligned with isCveAffectingDep() from cve.ts.
     */
    private boolean isCveAffectingDep(@Nonnull CVE cve, @Nonnull String depName, @Nonnull String depVersion) {
        if (cve.getAffectedDeps() == null || cve.getAffectedDeps().isEmpty()) {
            return false;
        }
        
        return cve.getAffectedDeps().stream()
            .anyMatch(ad -> depName.equals(ad.getName()) && 
                            isVersionInRange(depVersion, ad.getVulnerableVersionRange()));
    }
    
    /**
     * Check if a version satisfies a vulnerability version range.
     * Handles common range formats like ">= 1.0, < 2.0", "< 3.0", etc.
     */
    private boolean isVersionInRange(@Nonnull String version, @Nullable String range) {
        if (StringUtils.isBlank(range)) {
            return false;
        }
        
        try {
            final ComparableVersion currentVersion = new ComparableVersion(version);
            
            // Split by comma for compound ranges (e.g., ">= 1.0, < 2.0")
            final String[] conditions = range.split(",");
            
            for (String condition : conditions) {
                condition = condition.trim();
                
                if (!satisfiesCondition(currentVersion, condition)) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            // Error checking version range
            log.error("Error checking if version {} is in range {}", version, range, e);
            return false;
        }
    }
    
    /**
     * Check if a version satisfies a single condition.
     */
    private boolean satisfiesCondition(@Nonnull ComparableVersion version, @Nonnull String condition) {
        condition = condition.trim();
        
        if (condition.startsWith(">=")) {
            final ComparableVersion min = new ComparableVersion(condition.substring(2).trim());
            return version.compareTo(min) >= 0;
        } else if (condition.startsWith(">")) {
            final ComparableVersion min = new ComparableVersion(condition.substring(1).trim());
            return version.compareTo(min) > 0;
        } else if (condition.startsWith("<=")) {
            final ComparableVersion max = new ComparableVersion(condition.substring(2).trim());
            return version.compareTo(max) <= 0;
        } else if (condition.startsWith("<")) {
            final ComparableVersion max = new ComparableVersion(condition.substring(1).trim());
            return version.compareTo(max) < 0;
        } else if (condition.startsWith("=")) {
            final ComparableVersion exact = new ComparableVersion(condition.substring(1).trim());
            return version.compareTo(exact) == 0;
        } else {
            // Exact version match
            final ComparableVersion exact = new ComparableVersion(condition);
            return version.compareTo(exact) == 0;
        }
    }
}
