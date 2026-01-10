/**
 * PDF splitting example with progress tracking
 *
 * Usage:
 *   RENAMED_API_KEY=rt_... npx tsx pdf-split.ts multi-page.pdf output/
 */

import { writeFileSync, mkdirSync } from "fs";
import { join } from "path";
import { RenamedClient } from "@renamed/sdk";

async function main() {
  const apiKey = process.env.RENAMED_API_KEY;
  if (!apiKey) {
    console.error("Please set RENAMED_API_KEY environment variable");
    process.exit(1);
  }

  const filePath = process.argv[2];
  const outputDir = process.argv[3] || "./output";

  if (!filePath) {
    console.error("Usage: npx tsx pdf-split.ts <file> [output-dir]");
    process.exit(1);
  }

  const client = new RenamedClient({ apiKey });

  console.log(`Splitting: ${filePath}`);
  console.log(`Output directory: ${outputDir}\n`);

  // Start the split job
  const job = await client.pdfSplit(filePath, { mode: "auto" });

  // Wait for completion with progress updates
  const result = await job.wait((status) => {
    if (status.progress !== undefined) {
      process.stdout.write(`\rProgress: ${status.progress}%`);
    }
  });

  console.log("\n\nSplit complete!");
  console.log(`Total pages: ${result.totalPages}`);
  console.log(`Documents: ${result.documents.length}\n`);

  // Create output directory
  mkdirSync(outputDir, { recursive: true });

  // Download each document
  for (const doc of result.documents) {
    console.log(`Downloading: ${doc.filename} (${doc.pages})`);
    const buffer = await client.downloadFile(doc.downloadUrl);
    writeFileSync(join(outputDir, doc.filename), buffer);
  }

  console.log("\nDone!");
}

main().catch(console.error);
