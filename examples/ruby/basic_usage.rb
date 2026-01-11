# frozen_string_literal: true

# Basic usage example for the Renamed Ruby SDK.
#
# This example demonstrates:
# - Creating a client with an API key
# - Getting user information and credits
# - Renaming a file using AI
# - Handling errors
#
# Usage:
#   RENAMED_API_KEY=rt_... ruby basic_usage.rb invoice.pdf

require "renamed"

# Get API key from environment
api_key = ENV["RENAMED_API_KEY"]
if api_key.nil? || api_key.empty?
  warn "Please set RENAMED_API_KEY environment variable"
  exit 1
end

# Check command line arguments
if ARGV.empty?
  warn "Usage: ruby basic_usage.rb <file>"
  exit 1
end

file_path = ARGV[0]

# Create the client
client = Renamed::Client.new(api_key: api_key)

begin
  # Get user info
  puts "Fetching user info..."
  user = client.get_user
  puts "User: #{user.email}"
  puts "Credits: #{user.credits}"
  puts

  # Rename a file
  puts "Renaming: #{file_path}"
  result = client.rename(file_path)

  puts "\nResult:"
  puts "  Original:  #{result.original_filename}"
  puts "  Suggested: #{result.suggested_filename}"
  puts "  Folder:    #{result.folder_path}" if result.folder_path
  puts format("  Confidence: %.1f%%", result.confidence * 100) if result.confidence

rescue Renamed::AuthenticationError => e
  warn "Authentication failed: #{e.message}"
  warn "Please check your API key"
  exit 1

rescue Renamed::InsufficientCreditsError => e
  warn "Insufficient credits: #{e.message}"
  warn "Please add more credits at https://www.renamed.to/settings/billing"
  exit 1

rescue Renamed::RateLimitError => e
  warn "Rate limit exceeded: #{e.message}"
  warn "Please wait before making more requests"
  exit 1

rescue Renamed::ValidationError => e
  warn "Validation error: #{e.message}"
  warn "Please check your file format"
  exit 1

rescue Renamed::Error => e
  warn "Error: #{e.message}"
  exit 1
end
