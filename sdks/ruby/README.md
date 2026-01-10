# Renamed Ruby SDK

Official Ruby SDK for the [renamed.to](https://renamed.to) API. Rename files intelligently using AI, split PDFs, and extract structured data from documents.

## Installation

Add this line to your application's Gemfile:

```ruby
gem 'renamed'
```

And then execute:

```bash
bundle install
```

Or install it yourself as:

```bash
gem install renamed
```

## Quick Start

```ruby
require 'renamed'

# Initialize the client with your API key
client = Renamed::Client.new(api_key: 'rt_your_api_key')

# Rename a file using AI
result = client.rename('invoice.pdf')
puts result.suggested_filename  # => "2025-01-15_AcmeCorp_INV-12345.pdf"
puts result.folder_path         # => "Invoices/2025/January"
```

## Features

### Rename Files

Intelligently rename files using AI analysis:

```ruby
# Basic rename
result = client.rename('document.pdf')
puts result.suggested_filename
puts result.folder_path
puts result.confidence

# With custom template
result = client.rename('invoice.pdf', template: '{date}_{vendor}_{type}')
puts result.suggested_filename
```

### Split PDFs

Split multi-page PDFs into separate documents:

```ruby
# Auto-detect document boundaries
job = client.pdf_split('multi-page.pdf', mode: 'auto')

# Wait for completion with progress updates
result = job.wait do |status|
  puts "Progress: #{status.progress}%"
end

# Download the split documents
result.documents.each do |doc|
  puts "#{doc.filename} (pages #{doc.pages})"
  content = client.download_file(doc.download_url)
  File.binwrite(doc.filename, content)
end

# Split by fixed page count
job = client.pdf_split('document.pdf', mode: 'pages', pages_per_split: 5)
result = job.wait

# Split at blank pages
job = client.pdf_split('scanned.pdf', mode: 'blank')
result = job.wait
```

### Extract Data

Extract structured data from documents:

```ruby
# Extract with natural language prompt
result = client.extract('invoice.pdf', prompt: 'Extract the vendor name, invoice number, and total amount')
puts result.data
# => {"vendor" => "Acme Corp", "invoice_number" => "INV-12345", "total" => 1250.00}

# Extract with JSON schema
schema = {
  type: 'object',
  properties: {
    vendor: { type: 'string' },
    invoice_number: { type: 'string' },
    line_items: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          description: { type: 'string' },
          quantity: { type: 'number' },
          price: { type: 'number' }
        }
      }
    },
    total: { type: 'number' }
  }
}

result = client.extract('invoice.pdf', schema: schema)
puts result.data
puts result.confidence
```

### Check User Credits

```ruby
user = client.get_user
puts "Email: #{user.email}"
puts "Credits remaining: #{user.credits}"
puts "Team: #{user.team&.name}"
```

## Configuration

```ruby
client = Renamed::Client.new(
  api_key: 'rt_your_api_key',
  base_url: 'https://www.renamed.to/api/v1',  # Default
  timeout: 30,                                  # Request timeout in seconds
  max_retries: 2                                # Number of retries for failed requests
)
```

## Error Handling

The SDK provides specific error classes for different error conditions:

```ruby
begin
  result = client.rename('document.pdf')
rescue Renamed::AuthenticationError => e
  puts "Invalid API key: #{e.message}"
rescue Renamed::InsufficientCreditsError => e
  puts "Not enough credits: #{e.message}"
rescue Renamed::RateLimitError => e
  puts "Rate limited. Retry after: #{e.retry_after} seconds"
rescue Renamed::ValidationError => e
  puts "Invalid request: #{e.message}"
  puts "Details: #{e.details}"
rescue Renamed::NetworkError => e
  puts "Network error: #{e.message}"
rescue Renamed::TimeoutError => e
  puts "Request timed out: #{e.message}"
rescue Renamed::JobError => e
  puts "Job failed: #{e.message}"
  puts "Job ID: #{e.job_id}"
rescue Renamed::Error => e
  puts "General error: #{e.message}"
  puts "Code: #{e.code}"
  puts "Status: #{e.status_code}"
end
```

## File Input Types

All file methods accept multiple input types:

```ruby
# String path
result = client.rename('/path/to/document.pdf')

# File object
File.open('document.pdf', 'rb') do |file|
  result = client.rename(file)
end

# IO object
io = StringIO.new(pdf_content)
result = client.rename(io)
```

## Supported File Types

- PDF (`.pdf`)
- JPEG (`.jpg`, `.jpeg`)
- PNG (`.png`)
- TIFF (`.tif`, `.tiff`)

## Development

After checking out the repo, run `bin/setup` to install dependencies. Then, run `rake test` to run the tests.

```bash
bundle install
bundle exec rake test
```

To generate documentation:

```bash
bundle exec yard doc
```

## Contributing

Bug reports and pull requests are welcome on GitHub at https://github.com/renamed-to/renamed-sdk.

## License

The gem is available as open source under the terms of the [MIT License](https://opensource.org/licenses/MIT).
