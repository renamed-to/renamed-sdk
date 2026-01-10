"""
PDF splitting example with progress tracking.

Usage:
    RENAMED_API_KEY=rt_... python pdf_split.py multi-page.pdf output/
"""

import os
import sys
from pathlib import Path

from renamed import RenamedClient


def main() -> None:
    api_key = os.environ.get("RENAMED_API_KEY")
    if not api_key:
        print("Please set RENAMED_API_KEY environment variable")
        sys.exit(1)

    if len(sys.argv) < 2:
        print("Usage: python pdf_split.py <file> [output-dir]")
        sys.exit(1)

    file_path = sys.argv[1]
    output_dir = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("./output")

    client = RenamedClient(api_key=api_key)

    print(f"Splitting: {file_path}")
    print(f"Output directory: {output_dir}\n")

    # Start the split job
    job = client.pdf_split(file_path, mode="auto")

    # Wait for completion with progress updates
    def on_progress(status):
        if status.progress is not None:
            print(f"\rProgress: {status.progress}%", end="", flush=True)

    result = job.wait(on_progress)

    print("\n\nSplit complete!")
    print(f"Total pages: {result.total_pages}")
    print(f"Documents: {len(result.documents)}\n")

    # Create output directory
    output_dir.mkdir(parents=True, exist_ok=True)

    # Download each document
    for doc in result.documents:
        print(f"Downloading: {doc.filename} ({doc.pages})")
        content = client.download_file(doc.download_url)
        (output_dir / doc.filename).write_bytes(content)

    print("\nDone!")


if __name__ == "__main__":
    main()
