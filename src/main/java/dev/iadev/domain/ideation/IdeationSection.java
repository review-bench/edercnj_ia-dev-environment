package dev.iadev.domain.ideation;

public enum IdeationSection {

    VISION_AND_SCOPE(1, "Visão & Escopo"),
    STAKEHOLDERS(2, "Stakeholders & Personas"),
    BUSINESS_REQUIREMENTS(3, "Requisitos de Negócio"),
    CONSTRAINTS(4, "Restrições & Assunções"),
    SUCCESS_CRITERIA(5, "Critérios de Sucesso"),
    RISKS(6, "Riscos & Mitigação"),
    ROADMAP(7, "Roadmap");

    private final int number;
    private final String displayName;

    IdeationSection(int number, String displayName) {
        this.number = number;
        this.displayName = displayName;
    }

    public int number() {
        return number;
    }

    public String displayName() {
        return displayName;
    }
}
