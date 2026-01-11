# frozen_string_literal: true

require_relative "lib/renamed/version"

Gem::Specification.new do |spec|
  spec.name = "renamed"
  spec.version = Renamed::VERSION
  spec.authors = ["renamed.to"]
  spec.email = ["support@renamed.to"]

  spec.summary = "Official Ruby SDK for renamed.to API"
  spec.description = "Ruby client library for the renamed.to API. Rename files intelligently using AI, split PDFs, and extract structured data from documents."
  spec.homepage = "https://www.renamed.to"
  spec.license = "MIT"
  spec.required_ruby_version = ">= 3.0.0"

  spec.metadata["homepage_uri"] = spec.homepage
  spec.metadata["source_code_uri"] = "https://github.com/renamed-to/renamed-sdk"
  spec.metadata["changelog_uri"] = "https://github.com/renamed-to/renamed-sdk/blob/main/sdks/ruby/CHANGELOG.md"
  spec.metadata["documentation_uri"] = "https://www.renamed.to/docs"
  spec.metadata["rubygems_mfa_required"] = "true"

  # Specify which files should be added to the gem
  spec.files = Dir.chdir(__dir__) do
    Dir["{lib}/**/*", "LICENSE", "README.md", "CHANGELOG.md"].reject { |f| File.directory?(f) }
  end

  spec.bindir = "exe"
  spec.executables = spec.files.grep(%r{\Aexe/}) { |f| File.basename(f) }
  spec.require_paths = ["lib"]

  # Runtime dependencies
  spec.add_dependency "faraday", ">= 2.0", "< 3.0"
  spec.add_dependency "faraday-multipart", "~> 1.0"

  # Development dependencies
  spec.add_development_dependency "bundler", "~> 2.0"
  spec.add_development_dependency "minitest", "~> 5.0"
  spec.add_development_dependency "rake", "~> 13.0"
  spec.add_development_dependency "rubocop", "~> 1.0"
  spec.add_development_dependency "webmock", "~> 3.0"
  spec.add_development_dependency "yard", "~> 0.9"
end
