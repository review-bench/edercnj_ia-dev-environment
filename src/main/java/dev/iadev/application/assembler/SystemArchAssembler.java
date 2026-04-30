package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import java.nio.file.Path;
import java.util.List;

/**
 * Assembles {@code docs/architecture/system.md} from the system architecture template.
 *
 * <p>Delegates rendering to {@link DocsAssembler#assembleSystemArchitecture}. Idempotent: skips
 * generation if the output file already exists (incremental updates are handled by the
 * {@code /x-arch-system-update} skill — EPIC-0070 story-0070-0006).
 *
 * @see DocsAssembler
 */
public final class SystemArchAssembler implements Assembler {

    private final DocsAssembler delegate;

    /** Creates a SystemArchAssembler using classpath resources. */
    public SystemArchAssembler() {
        this.delegate = new DocsAssembler();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> assemble(ProjectConfig config, TemplateEngine engine, Path outputDir) {
        return delegate.assembleSystemArchitecture(config, engine, outputDir);
    }
}
