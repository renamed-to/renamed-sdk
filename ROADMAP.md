# Renamed SDK Roadmap

> **Current Version**: 0.1.5 (January 2026)
> **Target**: 1.0.0 GA Release

## Current State

- **Languages**: 9 SDKs (TypeScript, Python, Go, Java, C#, Ruby, Rust, Swift, PHP)
- **Features**: File renaming, PDF splitting, data extraction
- **Status**: Well-tested, documented, all core features complete

---

## Month 1: Stabilization & Developer Experience

### Week 1-2: Path to 1.0 Stable

- [ ] Add integration test suites against live API (sandboxed)
- [ ] Implement retry logic consistency audit across all 9 SDKs
- [ ] Add connection pooling/keep-alive optimizations
- [ ] Standardize error messages across languages

### Week 3-4: Developer Experience

- [ ] Create interactive API playground/demo site
- [ ] Add streaming progress callbacks for large file uploads
- [ ] Implement request/response logging middleware
- [ ] Add OpenTelemetry/tracing support for observability
- [ ] Create CLI tool for quick testing (`renamed-cli`)

**Milestone**: Release `0.2.0-beta` with DX improvements

---

## Month 2: Feature Expansion & Ecosystem

### Week 1-2: New Capabilities

- [ ] **Batch operations** - Process multiple files in single request
- [ ] **Webhook support** - Callbacks for async job completion
- [ ] **Template library** - Pre-built extraction schemas (invoices, receipts, contracts)
- [ ] **Local caching** - Cache rename suggestions for identical files

### Week 3-4: Framework Integrations

- [ ] Django/Flask middleware for Python
- [ ] Express/NestJS middleware for Node.js
- [ ] Spring Boot starter for Java
- [ ] ASP.NET Core middleware for C#
- [ ] Laravel service provider for PHP

**Milestone**: Release `0.3.0-beta` with batch ops + 2 framework integrations

---

## Month 3: Production Readiness & Community

### Week 1-2: Enterprise Features

- [ ] Rate limiting awareness with automatic backoff
- [ ] Configurable circuit breaker pattern
- [ ] Multi-region endpoint support
- [ ] Request signing/verification
- [ ] Audit logging hooks

### Week 3: Documentation & Tutorials

- [ ] Video tutorials for each language
- [ ] Migration guide from beta to stable
- [ ] Performance benchmarks documentation
- [ ] Common patterns cookbook
- [ ] Troubleshooting guide

### Week 4: GA Release

- [ ] Security audit completion
- [ ] Load testing at scale
- [ ] Deprecation policy documentation
- [ ] Changelog automation
- [ ] Release `1.0.0` stable across all 9 SDKs

**Milestone**: **v1.0.0 GA Release**

---

## Priority Matrix

| Priority | Item | Impact | Effort |
|----------|------|--------|--------|
| P0 | Batch operations | High | Medium |
| P0 | Webhook support | High | Medium |
| P1 | CLI tool | Medium | Low |
| P1 | Template library | High | Low |
| P1 | Framework integrations | Medium | Medium |
| P2 | OpenTelemetry | Medium | Medium |
| P2 | Multi-region | Low | High |

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Weekly downloads | 1,000+ across registries |
| Test coverage | >90% maintained |
| GitHub stars | 50+ |
| Breaking changes post-1.0 | Zero |

---

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for how to get involved with roadmap items.
