/**
 * Basic file renaming example
 *
 * Usage:
 *   RENAMED_API_KEY=rt_... npx tsx basic-rename.ts invoice.pdf
 */

import { RenamedClient } from "@renamed/sdk";

async function main() {
  const apiKey = process.env.RENAMED_API_KEY;
  if (!apiKey) {
    console.error("Please set RENAMED_API_KEY environment variable");
    process.exit(1);
  }

  const filePath = process.argv[2];
  if (!filePath) {
    console.error("Usage: npx tsx basic-rename.ts <file>");
    process.exit(1);
  }

  const client = new RenamedClient({ apiKey });

  console.log(`Renaming: ${filePath}`);

  const result = await client.rename(filePath);

  console.log("\nResult:");
  console.log(`  Original:  ${result.originalFilename}`);
  console.log(`  Suggested: ${result.suggestedFilename}`);
  if (result.folderPath) {
    console.log(`  Folder:    ${result.folderPath}`);
  }
  console.log(`  Confidence: ${(result.confidence * 100).toFixed(1)}%`);
}

main().catch(console.error);
