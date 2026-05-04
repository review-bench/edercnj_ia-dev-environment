package dev.iadev.domain.ideation;

import java.util.Map;

public final class IdeationTemplate {

    private final String title;
    private final Map<IdeationSection, String> sections;

    private IdeationTemplate(Builder builder) {
        this.title = builder.title;
        this.sections = Map.copyOf(builder.sections);
    }

    public String title() {
        return title;
    }

    public Map<IdeationSection, String> sections() {
        return sections;
    }

    public boolean hasSection(IdeationSection section) {
        return sections.containsKey(section);
    }

    public String sectionContent(IdeationSection section) {
        return sections.getOrDefault(section, "");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title = "";
        private Map<IdeationSection, String> sections = Map.of();

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder sections(Map<IdeationSection, String> sections) {
            this.sections = sections;
            return this;
        }

        public IdeationTemplate build() {
            return new IdeationTemplate(this);
        }
    }
}
