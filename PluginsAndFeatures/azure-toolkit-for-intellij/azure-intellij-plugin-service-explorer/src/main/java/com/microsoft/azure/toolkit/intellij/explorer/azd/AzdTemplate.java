package com.microsoft.azure.toolkit.intellij.explorer.azd;

import java.util.List;

/**
 * Represents a template in the Azure Developer CLI (azd) ecosystem.
 * <p>
 * This class encapsulates metadata for an azd template, including its title, description, preview image, author information,
 * source, tags, and supported languages. It provides getter and setter methods for each property, allowing for fluent configuration.
 * </p>
 */
public final class AzdTemplate {

    private String title;
    private String description;
    private String preview;
    private String authorUrl;
    private String author;
    private String source;
    private List<String> tags;
    private List<String> languages;

    /**
     * Gets the title of the azd template.
     *
     * @return the template title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the azd template.
     *
     * @param title the template title
     * @return the current AzdTemplate instance
     */
    public AzdTemplate setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Gets the description of the azd template.
     *
     * @return the template description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the azd template.
     *
     * @param description the template description
     * @return the current AzdTemplate instance
     */
    public AzdTemplate setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Gets the preview image URL of the azd template.
     *
     * @return the preview image URL
     */
    public String getPreview() {
        return preview;
    }

    /**
     * Sets the preview image URL of the azd template.
     *
     * @param preview the preview image URL
     * @return the current AzdTemplate instance
     */
    public AzdTemplate setPreview(String preview) {
        this.preview = preview;
        return this;
    }

    /**
     * Gets the author URL of the azd template.
     *
     * @return the author URL
     */
    public String getAuthorUrl() {
        return authorUrl;
    }

    /**
     * Sets the author URL of the azd template.
     *
     * @param authorUrl the author URL
     * @return the current AzdTemplate instance
     */
    public AzdTemplate setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
        return this;
    }

    /**
     * Gets the author of the azd template.
     *
     * @return the author name
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the author of the azd template.
     *
     * @param author the author name
     * @return the current AzdTemplate instance
     */
    public AzdTemplate setAuthor(String author) {
        this.author = author;
        return this;
    }

    /**
     * Gets the source URL of the azd template.
     *
     * @return the source URL
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the source URL of the azd template.
     *
     * @param source the source URL
     * @return the current AzdTemplate instance
     */
    public AzdTemplate setSource(String source) {
        this.source = source;
        return this;
    }

    /**
     * Gets the tags associated with the azd template.
     *
     * @return a list of tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Sets the tags associated with the azd template.
     *
     * @param tags a list of tags
     * @return the current AzdTemplate instance
     */
    public AzdTemplate setTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * Gets the supported languages for the azd template.
     *
     * @return a list of supported languages
     */
    public List<String> getLanguages() {
        return languages;
    }

    /**
     * Sets the supported languages for the azd template.
     *
     * @param languages a list of supported languages
     * @return the current AzdTemplate instance
     */
    public AzdTemplate setLanguages(List<String> languages) {
        this.languages = languages;
        return this;
    }
}