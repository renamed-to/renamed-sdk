"""
Basic file renaming example.

Usage:
    RENAMED_API_KEY=rt_... python basic_rename.py invoice.pdf
"""

import os
import sys

from renamed import RenamedClient


def main() -> None:
    api_key = os.environ.get("RENAMED_API_KEY")
    if not api_key:
        print("Please set RENAMED_API_KEY environment variable")
        sys.exit(1)

    if len(sys.argv) < 2:
        print("Usage: python basic_rename.py <file>")
        sys.exit(1)

    file_path = sys.argv[1]

    client = RenamedClient(api_key=api_key)

    print(f"Renaming: {file_path}")

    result = client.rename(file_path)

    print("\nResult:")
    print(f"  Original:  {result.original_filename}")
    print(f"  Suggested: {result.suggested_filename}")
    if result.folder_path:
        print(f"  Folder:    {result.folder_path}")
    print(f"  Confidence: {result.confidence * 100:.1f}%")


if __name__ == "__main__":
    main()
