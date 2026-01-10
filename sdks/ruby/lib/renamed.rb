# frozen_string_literal: true

require_relative "renamed/version"
require_relative "renamed/errors"
require_relative "renamed/models"
require_relative "renamed/async_job"
require_relative "renamed/client"

# Official Ruby SDK for renamed.to API.
#
# @example Basic usage
#   require "renamed"
#
#   client = Renamed::Client.new(api_key: "rt_your_api_key")
#
#   # Rename a file
#   result = client.rename("invoice.pdf")
#   puts result.suggested_filename
#
#   # Check credits
#   user = client.get_user
#   puts "Credits: #{user.credits}"
#
# @see https://renamed.to/docs API Documentation
module Renamed
end
