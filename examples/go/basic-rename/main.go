// Basic file renaming example.
//
// Usage:
//
//	RENAMED_API_KEY=rt_... go run main.go invoice.pdf
package main

import (
	"context"
	"fmt"
	"os"

	"github.com/renamed-to/renamed-sdk/sdks/go/renamed"
)

func main() {
	apiKey := os.Getenv("RENAMED_API_KEY")
	if apiKey == "" {
		fmt.Fprintln(os.Stderr, "Please set RENAMED_API_KEY environment variable")
		os.Exit(1)
	}

	if len(os.Args) < 2 {
		fmt.Fprintln(os.Stderr, "Usage: go run main.go <file>")
		os.Exit(1)
	}

	filePath := os.Args[1]

	client := renamed.NewClient(apiKey)
	ctx := context.Background()

	fmt.Printf("Renaming: %s\n", filePath)

	result, err := client.Rename(ctx, filePath, nil)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	fmt.Println("\nResult:")
	fmt.Printf("  Original:  %s\n", result.OriginalFilename)
	fmt.Printf("  Suggested: %s\n", result.SuggestedFilename)
	if result.FolderPath != "" {
		fmt.Printf("  Folder:    %s\n", result.FolderPath)
	}
	fmt.Printf("  Confidence: %.1f%%\n", result.Confidence*100)
}
