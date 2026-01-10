//! Basic usage example for the Renamed Rust SDK.
//!
//! This example demonstrates:
//! - Creating a client with an API key
//! - Getting user information and credits
//! - Renaming a file using AI
//! - Handling errors
//!
//! Usage:
//!   RENAMED_API_KEY=rt_... cargo run --example basic_usage -- invoice.pdf

use renamed::{RenamedClient, RenamedError};
use std::env;
use std::process;

#[tokio::main]
async fn main() {
    // Get API key from environment
    let api_key = match env::var("RENAMED_API_KEY") {
        Ok(key) if !key.is_empty() => key,
        _ => {
            eprintln!("Please set RENAMED_API_KEY environment variable");
            process::exit(1);
        }
    };

    // Check command line arguments
    let args: Vec<String> = env::args().collect();
    if args.len() < 2 {
        eprintln!("Usage: cargo run --example basic_usage -- <file>");
        process::exit(1);
    }

    let file_path = &args[1];

    // Create the client
    let client = RenamedClient::new(&api_key);

    // Get user info
    println!("Fetching user info...");
    match client.get_user().await {
        Ok(user) => {
            println!("User: {}", user.email);
            println!("Credits: {}", user.credits.unwrap_or(0));
            println!();
        }
        Err(e) => {
            handle_error(e);
        }
    }

    // Rename a file
    println!("Renaming: {}", file_path);
    match client.rename(file_path, None).await {
        Ok(result) => {
            println!("\nResult:");
            println!("  Original:  {}", result.original_filename);
            println!("  Suggested: {}", result.suggested_filename);
            if let Some(folder) = &result.folder_path {
                if !folder.is_empty() {
                    println!("  Folder:    {}", folder);
                }
            }
            if let Some(confidence) = result.confidence {
                println!("  Confidence: {:.1}%", confidence * 100.0);
            }
        }
        Err(e) => {
            handle_error(e);
        }
    }
}

fn handle_error(error: RenamedError) {
    match error {
        RenamedError::Authentication { message, .. } => {
            eprintln!("Authentication failed: {}", message);
            eprintln!("Please check your API key");
        }
        RenamedError::InsufficientCredits { message, .. } => {
            eprintln!("Insufficient credits: {}", message);
            eprintln!("Please add more credits at https://renamed.to/dashboard");
        }
        RenamedError::RateLimit { message, retry_after, .. } => {
            eprintln!("Rate limit exceeded: {}", message);
            if let Some(seconds) = retry_after {
                eprintln!("Retry after {} seconds", seconds);
            }
        }
        RenamedError::Validation { message, .. } => {
            eprintln!("Validation error: {}", message);
            eprintln!("Please check your file format");
        }
        _ => {
            eprintln!("Error: {}", error);
        }
    }
    process::exit(1);
}
