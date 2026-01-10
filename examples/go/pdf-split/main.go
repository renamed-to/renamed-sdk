// PDF splitting example with progress tracking.
//
// Usage:
//
//	RENAMED_API_KEY=rt_... go run main.go multi-page.pdf output/
package main

import (
	"context"
	"fmt"
	"os"
	"path/filepath"

	"github.com/renamed-to/renamed-sdk/sdks/go/renamed"
)

func main() {
	apiKey := os.Getenv("RENAMED_API_KEY")
	if apiKey == "" {
		fmt.Fprintln(os.Stderr, "Please set RENAMED_API_KEY environment variable")
		os.Exit(1)
	}

	if len(os.Args) < 2 {
		fmt.Fprintln(os.Stderr, "Usage: go run main.go <file> [output-dir]")
		os.Exit(1)
	}

	filePath := os.Args[1]
	outputDir := "./output"
	if len(os.Args) > 2 {
		outputDir = os.Args[2]
	}

	client := renamed.NewClient(apiKey)
	ctx := context.Background()

	fmt.Printf("Splitting: %s\n", filePath)
	fmt.Printf("Output directory: %s\n\n", outputDir)

	// Start the split job
	job, err := client.PDFSplit(ctx, filePath, &renamed.PdfSplitOptions{
		Mode: renamed.SplitModeAuto,
	})
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}

	// Wait for completion with progress updates
	result, err := job.Wait(ctx, func(status *renamed.JobStatusResponse) {
		fmt.Printf("\rProgress: %d%%", status.Progress)
	})
	if err != nil {
		fmt.Fprintf(os.Stderr, "\nError: %v\n", err)
		os.Exit(1)
	}

	fmt.Println("\n\nSplit complete!")
	fmt.Printf("Total pages: %d\n", result.TotalPages)
	fmt.Printf("Documents: %d\n\n", len(result.Documents))

	// Create output directory
	if err := os.MkdirAll(outputDir, 0755); err != nil {
		fmt.Fprintf(os.Stderr, "Error creating output dir: %v\n", err)
		os.Exit(1)
	}

	// Download each document
	for _, doc := range result.Documents {
		fmt.Printf("Downloading: %s (%s)\n", doc.Filename, doc.Pages)
		content, err := client.DownloadFile(ctx, doc.DownloadURL)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error downloading %s: %v\n", doc.Filename, err)
			continue
		}
		if err := os.WriteFile(filepath.Join(outputDir, doc.Filename), content, 0644); err != nil {
			fmt.Fprintf(os.Stderr, "Error saving %s: %v\n", doc.Filename, err)
			continue
		}
	}

	fmt.Println("\nDone!")
}
